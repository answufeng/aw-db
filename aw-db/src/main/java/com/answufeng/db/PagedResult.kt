package com.answufeng.db

/**
 * 手动分页查询结果封装。
 *
 * 适用于不使用 Paging 3 的场景，通过 SQL `LIMIT`/`OFFSET` 手动分页。
 *
 * ```kotlin
 * val items = userDao.getPage(pageSize, page * pageSize)
 * val total = userDao.count()
 * val result = items.toPagedResult(page, pageSize, total)
 * ```
 *
 * @param T 数据类型
 * @property items 当前页数据
 * @property total 总记录数，-1 表示未知
 * @property page 页码（从 0 开始）
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
    /** 总页数，[total] 为 -1 时返回 -1。 */
    val totalPages: Int get() = if (total >= 0 && pageSize > 0) {
        ((total.toLong() + pageSize - 1) / pageSize).toInt().coerceAtMost(Int.MAX_VALUE)
    } else -1

    /** 是否为首页（page == 0）。 */
    val isFirstPage: Boolean get() = page == 0

    /** 当前页是否为空。 */
    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * 将列表转换为 [PagedResult]，自动计算 [PagedResult.hasMore]。
 *
 * @param page 页码（从 0 开始）
 * @param pageSize 每页大小
 * @param total 总记录数，-1 表示未知（此时通过 items.size == pageSize 启发式判断是否有更多数据）
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
