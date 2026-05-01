package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException

/**
 * 在数据库事务中执行挂起操作，支持跨 Dao 的多步原子操作。
 *
 * 如果 [block] 中的任何操作抛出异常，整个事务将回滚。更多示例见 [safeTransaction]、[batchExecute]。
 *
 * ```kotlin
 * val result = database.withTx {
 *     val userId = userDao.insert(user)
 *     val orderId = orderDao.insert(order.copy(userId = userId))
 *     Pair(userId, orderId)
 * }
 * ```
 *
 * @param T 数据库类型
 * @param R 返回值类型
 * @param block 在事务中执行的挂起操作
 * @return 事务执行的结果
 */
suspend fun <T : RoomDatabase, R> T.withTx(block: suspend T.() -> R): R {
    return withTransaction { block() }
}

/**
 * 安全执行数据库事务，自动捕获异常并包装为 [Result]（[kotlinx.coroutines.CancellationException] 会原样抛出不当作失败）。
 *
 * ```kotlin
 * val result = database.safeTransaction {
 *     userDao.deleteAll()
 *     userDao.insertAll(newUsers)
 * }
 * result.onSuccess { showToast("同步完成") }
 *       .onFailure { err -> showToast("失败: ${err.message}") }
 * ```
 *
 * @param T 数据库类型
 * @param R 返回值类型
 * @param block 在事务中执行的挂起操作
 * @return [Result] 包装的结果
 */
suspend fun <T : RoomDatabase, R> T.safeTransaction(block: suspend T.() -> R): Result<R> {
    return try {
        Result.success(withTransaction { block() })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}

/**
 * 批量失败策略。
 */
enum class BatchFailureStrategy {
    /**
     * 跳过失败项，继续处理后续项。
     * 事务整体不会回滚，返回 [BatchResult.Skipped]。
     */
    SKIP,

    /**
     * 任何一项失败则回滚整个事务。
     * 事务失败时返回 [BatchResult.AllOrNothing]。
     */
    FAIL_FAST
}

/**
 * 批量执行结果，使用密封类保证类型安全。
 *
 * - [Skipped]: SKIP 策略的结果，包含成功/失败计数及失败详情
 * - [AllOrNothing]: FAIL_FAST 策略的结果，全部成功或全部回滚
 */
sealed class BatchResult<out T> {

    /**
     * SKIP 策略的结果：跳过失败项，继续处理。
     *
     * @property successCount 成功处理的条数
     * @property failedCount 失败的条数
     * @property failures 失败项的索引和异常信息
     */
    data class Skipped<T>(
        val successCount: Int,
        val failedCount: Int,
        val failures: List<IndexedValue<Throwable>> = emptyList()
    ) : BatchResult<T>()

    /**
     * FAIL_FAST 策略的结果：全部成功或全部回滚。
     *
     * @property result 成功时包含处理的条数，失败时包含异常
     */
    data class AllOrNothing<T>(val result: Result<Int>) : BatchResult<T>()
}

/**
 * 批量执行操作，支持失败策略。
 *
 * ```kotlin
 * // 跳过失败项
 * val result = database.batchExecute(users) { user ->
 *     userDao.insert(user)
 * }
 * when (result) {
 *     is BatchResult.Skipped -> println("成功 ${result.successCount} 条")
 *     is BatchResult.AllOrNothing -> {}
 * }
 *
 * // 任一失败则全部回滚
 * val result = database.batchExecute(users, BatchFailureStrategy.FAIL_FAST) { user ->
 *     userDao.insert(user)
 * }
 * ```
 *
 * @param T 数据库类型
 * @param E 数据元素类型
 * @param items 要处理的数据列表
 * @param strategy 失败策略，默认 [BatchFailureStrategy.SKIP]
 * @param batchSize 分批大小，默认 0 表示不分批。设置大于 0 时，每 N 条提交一次事务，避免长事务导致 ANR。
 * 仅对 [BatchFailureStrategy.SKIP] 生效；[BatchFailureStrategy.FAIL_FAST] 要求整批在单事务中完成，若 `batchSize > 0` 将抛出 [IllegalArgumentException]。
 * @param action 对每个元素执行的操作
 * @return [BatchResult] 类型安全的批量执行结果
 *
 * **注意**：SKIP 策略下，SQLite 约束冲突（如 UNIQUE 违反）会被 catch 并跳过，
 * 但其他严重错误（如磁盘 I/O 错误）可能导致当前事务无法继续。
 * 如果需要严格的原子性保证，请使用 FAIL_FAST 策略。
 *
 * SKIP 策略适合少量数据或需要精细错误处理的场景；对于大批量数据，
 * 推荐使用 [BaseDao.insertAll] / [BaseDao.insertOrIgnoreAll] 等 Room 原生批量操作以获得更好的性能。
 */
suspend fun <T : RoomDatabase, E> T.batchExecute(
    items: List<E>,
    strategy: BatchFailureStrategy = BatchFailureStrategy.SKIP,
    batchSize: Int = 0,
    action: suspend (E) -> Unit
): BatchResult<E> {
    validateBatchExecuteParams(strategy, batchSize)
    return when (strategy) {
    BatchFailureStrategy.SKIP -> {
        if (batchSize > 0 && items.size > batchSize) {
            var totalSuccess = 0
            val allFailures = mutableListOf<IndexedValue<Throwable>>()
            items.chunked(batchSize).forEachIndexed { chunkIndex, chunk ->
                var chunkSuccess = 0
                val chunkFailures = mutableListOf<IndexedValue<Throwable>>()
                val chunkOffset = chunkIndex * batchSize
                withTransaction {
                    chunk.forEachIndexed { index, item ->
                        try {
                            action(item)
                            chunkSuccess++
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            chunkFailures.add(IndexedValue(chunkOffset + index, e))
                        }
                    }
                }
                totalSuccess += chunkSuccess
                allFailures.addAll(chunkFailures)
            }
            BatchResult.Skipped(totalSuccess, items.size - totalSuccess, allFailures)
        } else {
            var successCount = 0
            val failures = mutableListOf<IndexedValue<Throwable>>()
            // SKIP：每條獨立事務，避免單一事務內某條失敗後 SQLite 後續語句不可用
            items.forEachIndexed { index, item ->
                try {
                    withTransaction {
                        action(item)
                    }
                    successCount++
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    failures.add(IndexedValue(index, e))
                }
            }
            BatchResult.Skipped(successCount, items.size - successCount, failures)
        }
    }

    BatchFailureStrategy.FAIL_FAST -> {
        BatchResult.AllOrNothing(
            try {
                Result.success(withTransaction {
                    items.forEach { item -> action(item) }
                    items.size
                })
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        )
    }
    }
}

/** 供 [batchExecute] 校验 [strategy] 与 [batchSize] 的组合。 */
@PublishedApi
internal fun validateBatchExecuteParams(
    strategy: BatchFailureStrategy,
    batchSize: Int
) {
    require(!(strategy == BatchFailureStrategy.FAIL_FAST && batchSize > 0)) {
        "batchExecute: FAIL_FAST cannot be combined with batchSize > 0 (would break single-transaction " +
            "all-or-nothing semantics). Use SKIP with batching, or FAIL_FAST with batchSize = 0."
    }
}
