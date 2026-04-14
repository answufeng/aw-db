package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test

class TransactionHelperTest {

    @Test
    fun `BatchResult Skipped holds correct counts`() {
        val result = BatchResult.Skipped<String>(successCount = 8, failedCount = 2)
        assertEquals(8, result.successCount)
        assertEquals(2, result.failedCount)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun `BatchResult Skipped with failures`() {
        val failures = listOf(
            IndexedValue(0, RuntimeException("item 0 failed")),
            IndexedValue(3, RuntimeException("item 3 failed"))
        )
        val result = BatchResult.Skipped<String>(successCount = 3, failedCount = 2, failures = failures)
        assertEquals(2, result.failures.size)
        assertEquals(0, result.failures[0].index)
        assertEquals(3, result.failures[1].index)
    }

    @Test
    fun `BatchResult AllOrNothing success`() {
        val result = BatchResult.AllOrNothing<String>(Result.success(10))
        assertTrue(result.result.isSuccess)
        assertEquals(10, result.result.getOrNull())
    }

    @Test
    fun `BatchResult AllOrNothing failure`() {
        val ex = RuntimeException("batch failed")
        val result = BatchResult.AllOrNothing<String>(Result.failure(ex))
        assertTrue(result.result.isFailure)
    }

    @Test
    fun `BatchFailureStrategy enum values`() {
        assertEquals(2, BatchFailureStrategy.values().size)
        assertEquals(BatchFailureStrategy.SKIP, BatchFailureStrategy.valueOf("SKIP"))
        assertEquals(BatchFailureStrategy.FAIL_FAST, BatchFailureStrategy.valueOf("FAIL_FAST"))
    }
}
