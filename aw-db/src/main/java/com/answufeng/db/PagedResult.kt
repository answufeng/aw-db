package com.answufeng.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PagedResult<T>(
    val items: List<T>,
    val total: Int = -1,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
) {
    val totalPages: Int get() = if (total >= 0 && pageSize > 0) {
        ((total.toLong() + pageSize - 1) / pageSize).toInt().coerceAtMost(Int.MAX_VALUE)
    } else -1

    val isFirstPage: Boolean get() = page == 0
}

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

@Deprecated(
    message = "In-memory pagination is inefficient for large datasets. Use Room PagingSource with Paging 3 instead (see PagingExtensions.kt).",
    level = DeprecationLevel.WARNING
)
fun <T> Flow<List<T>>.paginate(page: Int, pageSize: Int): Flow<List<T>> {
    val offset = page * pageSize
    return this.map { list ->
        val fromIndex = offset.coerceAtMost(list.size)
        val toIndex = (offset + pageSize).coerceAtMost(list.size)
        list.subList(fromIndex, toIndex)
    }
}
