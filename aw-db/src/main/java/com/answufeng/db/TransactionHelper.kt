package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * 数据库事务辅助工具，支持跨 Dao 的多步原子操作。
 *
 * ### 使用示例
 *
 * #### 基本事务
 * ```kotlin
 * val result = database.runInTransaction {
 *     val userId = userDao.insert(user)
 *     val orderId = orderDao.insert(order.copy(userId = userId))
 *     Pair(userId, orderId)
 * }
 * ```
 *
 * #### 安全事务（捕获异常）
 * ```kotlin
 * val result = database.safeTransaction {
 *     userDao.insert(user)
 *     orderDao.insert(order)
 * }
 * result.onSuccess { println("事务成功") }
 *       .onFailure { println("事务失败: ${it.message}") }
 * ```
 */

/**
 * 在数据库事务中执行挂起操作。
 *
 * 如果 [block] 中的任何操作抛出异常，整个事务将回滚。
 *
 * @param T 数据库类型
 * @param R 返回值类型
 * @param block 在事务中执行的挂起操作
 * @return 事务执行的结果
 */
suspend fun <T : RoomDatabase, R> T.runInTransaction(block: suspend T.() -> R): R {
    return withTransaction { block() }
}

/**
 * 安全执行数据库事务，自动捕获异常并包装为 [Result]。
 *
 * ```kotlin
 * val result = database.safeTransaction {
 *     userDao.deleteAll()
 *     userDao.insertAll(newUsers)
 * }
 * if (result.isSuccess) showToast("同步完成")
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
        Result.failure(e)
    }
}

/**
 * 批量执行操作并返回成功条数。
 *
 * ```kotlin
 * val successCount = database.batchInsert(users) { user ->
 *     userDao.insert(user)
 * }
 * ```
 *
 * @param T 数据库类型
 * @param E 数据元素类型
 * @param items 要处理的数据列表
 * @param action 对每个元素执行的操作
 * @return 成功处理的条数
 */
suspend fun <T : RoomDatabase, E> T.batchInsert(
    items: List<E>,
    action: suspend (E) -> Unit
): Int {
    var count = 0
    withTransaction {
        for (item in items) {
            try {
                action(item)
                count++
            } catch (e: Exception) {
                android.util.Log.w("aw-db", "batchInsert: skipped item due to ${e.message}")
            }
        }
    }
    return count
}
