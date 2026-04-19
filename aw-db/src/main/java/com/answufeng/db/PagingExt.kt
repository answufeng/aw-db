package com.answufeng.db

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 将 Room [PagingSource] 工厂转换为 [Flow]<[PagingData]>，集成 AndroidX Paging 3。
 *
 * ```kotlin
 * @Dao
 * abstract class UserDao : BaseDao<User>() {
 *     @Query("SELECT * FROM User ORDER BY name ASC")
 *     abstract fun pagingSource(): PagingSource<Int, User>
 * }
 *
 * val pagingFlow = userDao::pagingSource.asPagingFlow(pageSize = 20)
 * ```
 *
 * @param T 数据类型
 * @param pageSize 每页大小，默认 20
 * @param enablePlaceholders 是否启用占位符，默认 true
 * @param initialLoadSize 初始加载大小，默认为 pageSize 的 3 倍
 * @return 分页数据 Flow
 */
fun <T : Any> (() -> PagingSource<Int, T>).asPagingFlow(
    pageSize: Int = 20,
    enablePlaceholders: Boolean = true,
    initialLoadSize: Int = pageSize * 3
): Flow<PagingData<T>> {
    return Pager(
        config = PagingConfig(
            pageSize = pageSize,
            enablePlaceholders = enablePlaceholders,
            initialLoadSize = initialLoadSize
        ),
        pagingSourceFactory = { this() }
    ).flow
}

/**
 * 将 Room [PagingSource] 工厂转换为 [Flow]<[PagingData]<[DbResult]<T>>>，
 * 每个分页项都包装为 [DbResult.Success]。
 *
 * @param T 数据类型
 * @param pageSize 每页大小，默认 20
 * @param enablePlaceholders 是否启用占位符，默认 true
 * @param initialLoadSize 初始加载大小，默认为 pageSize 的 3 倍
 * @return 包装了 DbResult 的分页数据 Flow
 */
fun <T : Any> (() -> PagingSource<Int, T>).asDbResultPagingFlow(
    pageSize: Int = 20,
    enablePlaceholders: Boolean = true,
    initialLoadSize: Int = pageSize * 3
): Flow<PagingData<DbResult<T>>> {
    return asPagingFlow(pageSize, enablePlaceholders, initialLoadSize)
        .map { pagingData -> pagingData.map { DbResult.Success(it) } }
}

/**
 * 转换 [DbResult] 分页数据中的成功数据。
 *
 * @param T 原始数据类型
 * @param R 转换后的数据类型
 * @param transform 转换函数
 * @return 转换后的分页数据 Flow
 */
fun <T : Any, R : Any> Flow<PagingData<DbResult<T>>>.mapResult(
    transform: (T) -> R
): Flow<PagingData<DbResult<R>>> {
    return this.map { pagingData ->
        pagingData.map { result -> result.map(transform) }
    }
}
