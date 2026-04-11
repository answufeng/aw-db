package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test

/**
 * DbResult 密封类的状态判断、操作符和链式调用测试。
 */
class DbResultTest {

    // ==================== 状态属性 ====================

    @Test
    fun `Loading state properties`() {
        val result: DbResult<String> = DbResult.Loading
        assertTrue(result.isLoading)
        assertFalse(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `Success state properties`() {
        val result: DbResult<String> = DbResult.Success("data")
        assertTrue(result.isSuccess)
        assertFalse(result.isLoading)
        assertFalse(result.isFailure)
    }

    @Test
    fun `Failure state properties`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        assertTrue(result.isFailure)
        assertFalse(result.isLoading)
        assertFalse(result.isSuccess)
    }

    // ==================== getOrNull ====================

    @Test
    fun `getOrNull returns data on Success`() {
        assertEquals("hello", DbResult.Success("hello").getOrNull())
    }

    @Test
    fun `getOrNull returns null on Loading`() {
        assertNull(DbResult.Loading.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Failure`() {
        assertNull(DbResult.Failure(RuntimeException()).getOrNull())
    }

    // ==================== getOrDefault ====================

    @Test
    fun `getOrDefault returns data on Success`() {
        assertEquals("hello", DbResult.Success("hello").getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default on Loading`() {
        val loading: DbResult<String> = DbResult.Loading
        assertEquals("default", loading.getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default on Failure`() {
        val failure: DbResult<String> = DbResult.Failure(RuntimeException())
        assertEquals("default", failure.getOrDefault("default"))
    }

    // ==================== map ====================

    @Test
    fun `map transforms Success data`() {
        val result = DbResult.Success(42).map { it.toString() }
        assertEquals("42", (result as DbResult.Success).data)
    }

    @Test
    fun `map preserves Loading`() {
        val result = DbResult.Loading.map { "ignored" }
        assertTrue(result.isLoading)
    }

    @Test
    fun `map preserves Failure`() {
        val ex = RuntimeException("err")
        val result = DbResult.Failure(ex).map { "ignored" }
        assertTrue(result.isFailure)
        assertSame(ex, (result as DbResult.Failure).error)
    }

    // ==================== 链式回调 ====================

    @Test
    fun `onSuccess fires for Success`() {
        var captured: String? = null
        DbResult.Success("data").onSuccess { captured = it }
        assertEquals("data", captured)
    }

    @Test
    fun `onSuccess does not fire for Failure`() {
        var called = false
        DbResult.Failure(RuntimeException()).onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onFailure fires for Failure`() {
        var captured: Throwable? = null
        val ex = RuntimeException("err")
        DbResult.Failure(ex).onFailure { captured = it }
        assertSame(ex, captured)
    }

    @Test
    fun `onFailure does not fire for Success`() {
        var called = false
        DbResult.Success("data").onFailure { called = true }
        assertFalse(called)
    }

    @Test
    fun `onLoading fires for Loading`() {
        var called = false
        DbResult.Loading.onLoading { called = true }
        assertTrue(called)
    }

    @Test
    fun `chained callbacks all execute`() {
        var loadingCalled = false
        var successCalled = false
        var failureCalled = false

        DbResult.Success("data")
            .onLoading { loadingCalled = true }
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }

        assertFalse(loadingCalled)
        assertTrue(successCalled)
        assertFalse(failureCalled)
    }
}
