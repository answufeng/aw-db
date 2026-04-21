package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test

class MigrationDslTest {

    @Test
    fun `migration creates valid Migration object`() {
        val m = migration(1, 2) {}
        assertEquals(1, m.startVersion)
        assertEquals(2, m.endVersion)
    }

    @Test
    fun `migration allows non-consecutive versions`() {
        val m = migration(1, 5) {}
        assertEquals(1, m.startVersion)
        assertEquals(5, m.endVersion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `migration rejects startVersion less than 1`() {
        migration(0, 1) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `migration rejects negative startVersion`() {
        migration(-1, 1) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `migration rejects endVersion equal to startVersion`() {
        migration(1, 1) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `migration rejects endVersion less than startVersion`() {
        migration(2, 1) {}
    }

    @Test
    fun `onCreateCallback creates valid Callback`() {
        val callback = onCreateCallback {}
        assertNotNull(callback)
    }

    @Test
    fun `onOpenCallback creates valid Callback`() {
        val callback = onOpenCallback {}
        assertNotNull(callback)
    }
}
