package com.answufeng.db

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DbResultTest {

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

    @Test
    fun `getOrThrow returns data on Success`() {
        assertEquals("hello", DbResult.Success("hello").getOrThrow())
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws on Loading`() {
        DbResult.Loading.getOrThrow()
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws on Failure`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        result.getOrThrow()
    }

    @Test
    fun `getOrElse returns data on Success`() {
        assertEquals("data", DbResult.Success("data").getOrElse { "fallback" })
    }

    @Test
    fun `getOrElse returns fallback on Failure`() {
        val result: DbResult<String> = DbResult.Failure(RuntimeException())
        assertEquals("fallback", result.getOrElse { "fallback" })
    }

    @Test
    fun `getOrElse returns fallback on Loading`() {
        val result: DbResult<String> = DbResult.Loading
        assertEquals("fallback", result.getOrElse { "fallback" })
    }

    @Test
    fun `getOrElse lazy evaluation`() {
        var evaluated = false
        val result: DbResult<String> = DbResult.Failure(RuntimeException())
        result.getOrElse { evaluated = true; "fallback" }
        assertTrue(evaluated)

        evaluated = false
        DbResult.Success("data").getOrElse { evaluated = true; "fallback" }
        assertFalse(evaluated)
    }

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

    @Test
    fun `flatMap chains Success`() {
        val result = DbResult.Success(42).flatMap { DbResult.Success(it.toString()) }
        assertEquals("42", (result as DbResult.Success).data)
    }

    @Test
    fun `flatMap preserves Failure`() {
        val ex = RuntimeException("err")
        val result: DbResult<Int> = DbResult.Failure(ex)
        val flatMapped = result.flatMap { DbResult.Success(it.toString()) }
        assertTrue(flatMapped.isFailure)
        assertSame(ex, (flatMapped as DbResult.Failure).error)
    }

    @Test
    fun `flatMap preserves Loading`() {
        val result: DbResult<Int> = DbResult.Loading
        val flatMapped = result.flatMap { DbResult.Success(it.toString()) }
        assertTrue(flatMapped.isLoading)
    }

    @Test
    fun `mapFailure transforms error`() {
        val original = RuntimeException("original")
        val result: DbResult<String> = DbResult.Failure(original)
        val mapped = result.mapFailure { IllegalArgumentException(it.message ?: "") }
        assertTrue(mapped.isFailure)
        assertTrue((mapped as DbResult.Failure).error is IllegalArgumentException)
    }

    @Test
    fun `mapFailure preserves Success`() {
        val result = DbResult.Success("data").mapFailure { IllegalArgumentException("x") }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `filter keeps matching Success`() {
        val result = DbResult.Success(42).filter { it > 0 }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `filter converts non-matching Success to Failure`() {
        val result = DbResult.Success(42).filter { it < 0 }
        assertTrue(result.isFailure)
    }

    @Test
    fun `filter preserves Failure`() {
        val result: DbResult<Int> = DbResult.Failure(RuntimeException())
        val filtered = result.filter { it > 0 }
        assertTrue(filtered.isFailure)
    }

    @Test
    fun `onEach executes for Success`() {
        var captured = 0
        DbResult.Success(42).onEach { captured = it }
        assertEquals(42, captured)
    }

    @Test
    fun `onEach does not execute for Failure`() {
        var called = false
        val result: DbResult<Int> = DbResult.Failure(RuntimeException())
        result.onEach { called = true }
        assertFalse(called)
    }

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

    @Test
    fun `recoverWith returns alternative DbResult on Failure`() {
        val failure: DbResult<String> = DbResult.Failure(RuntimeException("err"))
        val recovered = failure.recoverWith { DbResult.Success("fallback") }
        assertTrue(recovered.isSuccess)
        assertEquals("fallback", (recovered as DbResult.Success).data)
    }

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

    @Test
    fun `combineDbResults two successes`() {
        val combined = combineDbResults(
            DbResult.Success(1),
            DbResult.Success(2)
        ) { a, b -> a + b }
        assertEquals(3, (combined as DbResult.Success).data)
    }

    @Test
    fun `combineDbResults first failure`() {
        val ex = RuntimeException("err")
        val failure: DbResult<Int> = DbResult.Failure(ex)
        val combined = combineDbResults(
            failure,
            DbResult.Success(2)
        ) { a: Int, b: Int -> a + b }
        assertTrue(combined.isFailure)
    }

    @Test
    fun `combineDbResults loading`() {
        val combined = combineDbResults(
            DbResult.Loading,
            DbResult.Success(2)
        ) { a: Int, b: Int -> a + b }
        assertTrue(combined.isLoading)
    }

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

    @Test(expected = CancellationException::class)
    fun `dbResultOf rethrows CancellationException`() = runBlocking {
        dbResultOf { throw CancellationException() }
    }
}
