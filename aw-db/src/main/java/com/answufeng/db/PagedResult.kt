package com.answufeng.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 分页查询结果。
 *
 * @param T 数据类型
 * @property items 当前页数据
 * @property total 总记录数（如果可用）
 * @property page 当前页码（从 0 开始）
 * @property pageSize 每页大小
 * @property hasMore 是否还有更多数据
 */
data class PagedResult<T>(
    val items: List<T>,
    val total: Int = -1,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
) {
    /** 总页数（仅在 total >= 0 时可用） */
    val totalPages: Int get() = if (total >= 0 && pageSize > 0) (total + pageSize - 1) / pageSize else -1

    /** 是否为第一页 */
    val isFirstPage: Boolean get() = page == 0
}

/**
 * 将 Flow<List<T>> 转换为分页 Flow。
 *
 * ```kotlin
 * @Dao
 * abstract class UserDao : BaseDao<User>() {
 *     @Query("SELECT * FROM User ORDER BY name ASC LIMIT :limit OFFSET :offset")
 *     abstract suspend fun getPage(limit: Int, offset: Int): List<User>
 *
 *     @Query("SELECT COUNT(*) FROM User")
 *     abstract suspend fun count(): Int
 * }
 *
 * // 使用
 * val paged = userDao.getPage(pageSize, page * pageSize)
 * val total = userDao.count()
 * val result = paged.toPagedResult(page, pageSize, total)
 * ```
 *
 * @param T 数据类型
 * @param page 页码（从 0 开始）
 * @param pageSize 每页大小
 * @param total 总记录数（-1 表示未知）
 */
fun <T> List<T>.toPagedResult(page: Int, pageSize: Int, total: Int = -1): PagedResult<T> {
    val hasMore = if (total >= 0) {
        (page + 1) * pageSize < total
    } else {
        this.size == pageSize
    }
    return PagedResult(
        items = this,
        total = total,
        page = page,
        pageSize = pageSize,
        hasMore = hasMore
    )
}

/**
 * 将 Flow<List<T>> 转换为分页 Flow<List<T>>，自动截取指定范围。
 *
 * 适用于已经加载全部数据但需要分页展示的场景。
 *
 * ```kotlin
 * userDao.observeAll()
 *     .paginate(page = 0, pageSize = 20)
 *     .collect { pageItems -> showPage(pageItems) }
 * ```
 *
 * @param page 页码（从 0 开始）
 * @param pageSize 每页大小
 */
fun <T> Flow<List<T>>.paginate(page: Int, pageSize: Int): Flow<List<T>> {
    val offset = page * pageSize
    return this.map { list ->
        val fromIndex = offset.coerceAtMost(list.size)
        val toIndex = (offset + pageSize).coerceAtMost(list.size)
        list.subList(fromIndex, toIndex)
    }
}
