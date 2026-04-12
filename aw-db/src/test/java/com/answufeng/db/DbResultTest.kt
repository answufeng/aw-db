package com.answufeng.db

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

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
        val result: DbResult<String> = DbResult.Failure(RuntimeException())
        assertNull(result.getOrNull())
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

    // ==================== getOrThrow ====================

    @Test
    fun `getOrThrow returns data on Success`() {
        assertEquals("hello", DbResult.Success("hello").getOrThrow())
    }

    @Test
    fun `getOrThrow returns null on Loading`() {
        assertNull(DbResult.Loading.getOrThrow())
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws on Failure`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        result.getOrThrow()
    }

    // ==================== getOrElse ====================

    @Test
    fun `getOrElse returns data on Success`() {
        assertEquals("data", DbResult.Success("data").getOrElse("fallback"))
    }

    @Test
    fun `getOrElse returns fallback on Failure`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException())
        assertEquals("fallback", result.getOrElse("fallback"))
    }

    @Test
    fun `getOrElse returns fallback on Loading`() {
        val result: DbResult<String> = DbResult.Loading
        assertEquals("fallback", result.getOrElse("fallback"))
    }

    // ==================== map ====================

    @Test
    fun `map transforms Success data`() {
        val result = DbResult.Success(42).map { it.toString() }
        assertEquals("42", (result as DbResult.Success).data)
    }

    @Test
    fun `map preserves Loading`() {
        val result: DbResult<String> = DbResult.Loading
        val mapped = result.map { "ignored" }
        assertTrue(mapped.isLoading)
    }

    @Test
    fun `map preserves Failure`() {
        val ex = RuntimeException("err")
        val result: DbResult<String> = DbResult.Failure(ex)
        val mapped = result.map { "ignored" }
        assertTrue(mapped.isFailure)
        assertSame(ex, (mapped as DbResult.Failure).error)
    }

    // ==================== recover ====================

    @Test
    fun `recover returns Success with recovered data on Failure`() {
        val failure: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        val recovered = failure.recover { "fallback" }
        assertTrue(recovered.isSuccess)
        assertEquals("fallback", (recovered as DbResult.Success).data)
    }

    @Test
    fun `recover preserves Success`() {
        val success = DbResult.Success("data")
        val recovered = success.recover { "fallback" }
        assertSame(success, recovered)
    }

    @Test
    fun `recover preserves Loading`() {
        val loading: DbResult<String> = DbResult.Loading
        val recovered = loading.recover { "fallback" }
        assertSame(loading, recovered)
    }

    // ==================== recoverWith ====================

    @Test
    fun `recoverWith returns alternative DbResult on Failure`() {
        val failure: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        val recovered = failure.recoverWith { DbResult.Success("fallback") }
        assertTrue(recovered.isSuccess)
        assertEquals("fallback", (recovered as DbResult.Success).data)
    }

    @Test
    fun `recoverWith can return Failure on Failure`() {
        val failure: DbResult<String> = DbResult.Failure(RuntimeException("original"))
        val recovered = failure.recoverWith { DbResult.Failure(RuntimeException("recovered")) }
        assertTrue(recovered.isFailure)
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
        val result: DbResult<String> = DbResult.Failure(RuntimeException())
        result.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onFailure fires for Failure`() {
        var captured: Throwable? = null
        val ex = RuntimeException("err")
        val result: DbResult<String> = DbResult.Failure(ex)
        result.onFailure { captured = it }
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

    // ==================== fold ====================

    @Test
    fun `fold on Loading`() {
        val result: DbResult<String> = DbResult.Loading
        val output = result.fold(
            onLoading = { "loading" },
            onSuccess = { it },
            onFailure = { it.message ?: "error" }
        )
        assertEquals("loading", output)
    }

    @Test
    fun `fold on Success`() {
        val result: DbResult<String> = DbResult.Success("data")
        val output = result.fold(
            onLoading = { "loading" },
            onSuccess = { it },
            onFailure = { it.message ?: "error" }
        )
        assertEquals("data", output)
    }

    @Test
    fun `fold on Failure`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException("oops"))
        val output = result.fold(
            onLoading = { "loading" },
            onSuccess = { it },
            onFailure = { it.message ?: "error" }
        )
        assertEquals("oops", output)
    }

    // ==================== dbResultOf ====================

    @Test
    fun `dbResultOf returns Success on success`() = runBlocking {
        val result = dbResultOf { "hello" }
        assertTrue(result.isSuccess)
        assertEquals("hello", (result as DbResult.Success).data)
    }

    @Test
    fun `dbResultOf returns Failure on exception`() = runBlocking {
        val result = dbResultOf<String> { throw RuntimeException("oops") }
        assertTrue(result.isFailure)
        assertEquals("oops", (result as DbResult.Failure).error.message)
    }
}
