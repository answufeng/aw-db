package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test

class PagedResultTest {

    @Test
    fun `toPagedResult with known total`() {
        val items = listOf("a", "b", "c")
        val result = items.toPagedResult(page = 0, pageSize = 5, total = 10)
        assertEquals(3, result.items.size)
        assertEquals(10, result.total)
        assertEquals(0, result.page)
        assertEquals(5, result.pageSize)
        assertTrue(result.hasMore)
        assertTrue(result.isFirstPage)
        assertEquals(2, result.totalPages)
    }

    @Test
    fun `toPagedResult last page`() {
        val items = listOf("a", "b")
        val result = items.toPagedResult(page = 1, pageSize = 5, total = 7)
        assertFalse(result.hasMore)
        assertEquals(2, result.totalPages)
    }

    @Test
    fun `toPagedResult with unknown total uses size heuristic`() {
        val items = listOf("a", "b", "c")
        val result = items.toPagedResult(page = 0, pageSize = 3)
        assertEquals(-1, result.total)
        assertTrue(result.hasMore)
    }

    @Test
    fun `toPagedResult with unknown total and partial page`() {
        val items = listOf("a", "b")
        val result = items.toPagedResult(page = 0, pageSize = 5)
        assertFalse(result.hasMore)
    }

    @Test
    fun `totalPages with large total does not overflow`() {
        val result = PagedResult<String>(
            items = emptyList(),
            total = Int.MAX_VALUE,
            page = 0,
            pageSize = 1,
            hasMore = true
        )
        assertEquals(Int.MAX_VALUE, result.totalPages)
    }

    @Test
    fun `totalPages with negative total returns -1`() {
        val result = PagedResult<String>(
            items = emptyList(),
            total = -1,
            page = 0,
            pageSize = 10,
            hasMore = false
        )
        assertEquals(-1, result.totalPages)
    }

    @Test
    fun `isFirstPage only for page 0`() {
        val result0 = PagedResult<String>(items = emptyList(), total = 0, page = 0, pageSize = 10, hasMore = false)
        val result1 = PagedResult<String>(items = emptyList(), total = 0, page = 1, pageSize = 10, hasMore = false)
        assertTrue(result0.isFirstPage)
        assertFalse(result1.isFirstPage)
    }
}
