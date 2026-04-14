package com.answufeng.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import java.util.concurrent.Executor

object AwDatabase {

    inline fun <reified T : RoomDatabase> build(
        context: Context,
        name: String,
        block: DatabaseConfig.() -> Unit = {}
    ): T {
        val config = DatabaseConfig().apply(block)
        val builder = Room.databaseBuilder(
            context.applicationContext,
            T::class.java,
            name
        )

        config.apply(builder)

        return builder.build()
    }

    inline fun <reified T : RoomDatabase> buildInMemory(
        context: Context,
        block: DatabaseConfig.() -> Unit = {}
    ): T {
        val config = DatabaseConfig().apply(block)
        val builder = Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            T::class.java
        )

        config.apply(builder)

        return builder.build()
    }
}

class DatabaseConfig {

    private val migrations = mutableListOf<Migration>()
    private val callbacks = mutableListOf<RoomDatabase.Callback>()
    private var destructiveMigration = false
    private var destructiveMigrationFrom: IntArray? = null
    private var allowMainThread = false
    private var journalMode: RoomDatabase.JournalMode? = null
    private var assetFilePath: String? = null
    private var databaseFile: java.io.File? = null
    private var queryExecutor: Executor? = null
    private var transactionExecutor: Executor? = null
    private var multiInstanceInvalidation: Boolean = false

    fun addMigrations(vararg migration: Migration) {
        migrations.addAll(migration)
    }

    fun addCallback(callback: RoomDatabase.Callback) {
        callbacks.add(callback)
    }

    fun fallbackToDestructiveMigration() {
        destructiveMigration = true
    }

    fun fallbackToDestructiveMigrationFrom(vararg startVersions: Int) {
        destructiveMigrationFrom = startVersions
    }

    fun allowMainThreadQueries() {
        allowMainThread = true
    }

    fun setJournalMode(mode: RoomDatabase.JournalMode) {
        journalMode = mode
    }

    fun createFromAsset(assetFilePath: String) {
        this.assetFilePath = assetFilePath
    }

    fun createFromFile(databaseFile: java.io.File) {
        this.databaseFile = databaseFile
    }

    fun setQueryExecutor(executor: Executor) {
        queryExecutor = executor
    }

    fun setTransactionExecutor(executor: Executor) {
        transactionExecutor = executor
    }

    fun enableMultiInstanceInvalidation() {
        multiInstanceInvalidation = true
    }

    @PublishedApi
    internal fun <T : RoomDatabase> apply(builder: RoomDatabase.Builder<T>) {
        if (migrations.isNotEmpty()) {
            builder.addMigrations(*migrations.toTypedArray())
        }
        callbacks.forEach { builder.addCallback(it) }
        if (destructiveMigration) {
            builder.fallbackToDestructiveMigration()
        }
        destructiveMigrationFrom?.let {
            builder.fallbackToDestructiveMigrationFrom(*it)
        }
        if (allowMainThread) {
            builder.allowMainThreadQueries()
        }
        journalMode?.let {
            builder.setJournalMode(it)
        }
        assetFilePath?.let {
            builder.createFromAsset(it)
        }
        databaseFile?.let {
            builder.createFromFile(it)
        }
        queryExecutor?.let {
            builder.setQueryExecutor(it)
        }
        transactionExecutor?.let {
            builder.setTransactionExecutor(it)
        }
        if (multiInstanceInvalidation) {
            builder.enableMultiInstanceInvalidation()
        }
    }
}
