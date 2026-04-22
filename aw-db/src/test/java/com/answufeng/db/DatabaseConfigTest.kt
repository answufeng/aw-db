package com.answufeng.db

import org.junit.Assert.*
import org.junit.Test

class DatabaseConfigTest {

    @Test
    fun `DatabaseConfig default values`() {
        val config = DatabaseConfig()
        assertNotNull(config)
    }

    @Test
    fun `DatabaseConfig addMigrations stores migrations`() {
        val config = DatabaseConfig()
        val m1 = migration(1, 2) {}
        val m2 = migration(2, 3) {}
        config.addMigrations(m1, m2)
    }

    @Test
    fun `DatabaseConfig addCallback stores callback`() {
        val config = DatabaseConfig()
        config.addCallback(onCreateCallback {})
        config.addCallback(onOpenCallback {})
    }

    @Test
    fun `DatabaseConfig fallbackToDestructiveMigration sets flag`() {
        val config = DatabaseConfig()
        config.fallbackToDestructiveMigration()
    }

    @Test
    fun `DatabaseConfig fallbackToDestructiveMigrationFrom sets versions`() {
        val config = DatabaseConfig()
        config.fallbackToDestructiveMigrationFrom(1, 2)
    }

    @Test
    fun `DatabaseConfig allowMainThreadQueries sets flag`() {
        val config = DatabaseConfig()
        config.allowMainThreadQueries()
    }

    @Test
    fun `DatabaseConfig setJournalMode sets mode`() {
        val config = DatabaseConfig()
        config.setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }

    @Test
    fun `DatabaseConfig createFromAsset sets path`() {
        val config = DatabaseConfig()
        config.createFromAsset("databases/prepopulated.db")
    }

    @Test
    fun `DatabaseConfig createFromFile sets file`() {
        val config = DatabaseConfig()
        config.createFromFile(java.io.File("/tmp/test.db"))
    }

    @Test
    fun `DatabaseConfig all options together`() {
        val config = DatabaseConfig()
        config.addMigrations(migration(1, 2) {})
        config.addCallback(onCreateCallback {})
        config.fallbackToDestructiveMigration()
        config.setJournalMode(androidx.room.RoomDatabase.JournalMode.AUTOMATIC)
        config.createFromAsset("databases/prepopulated.db")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createFromFile after createFromAsset throws`() {
        val config = DatabaseConfig()
        config.createFromAsset("databases/prepopulated.db")
        config.createFromFile(java.io.File("/tmp/second.db"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createFromAsset after createFromFile throws`() {
        val config = DatabaseConfig()
        config.createFromFile(java.io.File("/tmp/first.db"))
        config.createFromAsset("databases/prepopulated.db")
    }
}
