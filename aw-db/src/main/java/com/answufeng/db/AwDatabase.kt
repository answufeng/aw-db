package com.answufeng.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Room 数据库构建器，提供 DSL 方式快速创建数据库实例。
 *
 * ### 基本用法
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db")
 * ```
 *
 * ### 带迁移策略
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db") {
 *     addMigrations(MIGRATION_1_2, MIGRATION_2_3)
 *     fallbackToDestructiveMigration()
 * }
 * ```
 *
 * ### 内存数据库（适合测试）
 * ```kotlin
 * val db = AwDatabase.buildInMemory<AppDatabase>(context)
 * ```
 *
 * ### 预打包数据库
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db") {
 *     createFromAsset("databases/prepopulated.db")
 * }
 * ```
 */
object AwDatabase {

    /**
     * 构建 Room 数据库。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param name 数据库文件名
     * @param block 可选的配置 DSL
     * @return 数据库实例
     */
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

    /**
     * 构建内存数据库（数据不持久化，适合测试场景）。
     *
     * @param T 数据库类型
     * @param context 任意 Context
     * @param block 可选的配置 DSL
     * @return 数据库实例
     */
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

/**
 * 数据库配置类，提供 DSL 方式配置 Room 数据库构建器。
 *
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db") {
 *     addMigrations(MIGRATION_1_2)
 *     addCallback(onCreateCallback { /* 预填充数据 */ })
 *     fallbackToDestructiveMigration()
 *     setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
 *     createFromAsset("databases/prepopulated.db")
 * }
 * ```
 */
class DatabaseConfig {

    private val migrations = mutableListOf<Migration>()
    private val callbacks = mutableListOf<RoomDatabase.Callback>()
    private var destructiveMigration = false
    private var destructiveMigrationFrom: IntArray? = null
    private var allowMainThread = false
    private var journalMode: RoomDatabase.JournalMode? = null
    private var assetFilePath: String? = null
    private var databaseFile: java.io.File? = null

    /**
     * 添加数据库版本迁移。
     *
     * ```kotlin
     * addMigrations(MIGRATION_1_2, MIGRATION_2_3)
     * ```
     */
    fun addMigrations(vararg migration: Migration) {
        migrations.addAll(migration)
    }

    /**
     * 添加数据库回调。
     *
     * ```kotlin
     * addCallback(onCreateCallback { execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')") })
     * ```
     */
    fun addCallback(callback: RoomDatabase.Callback) {
        callbacks.add(callback)
    }

    /**
     * 当没有找到合适的迁移路径时，销毁并重建数据库。
     *
     * **警告**：这会丢失所有数据！仅在可接受数据丢失时使用。
     */
    fun fallbackToDestructiveMigration() {
        destructiveMigration = true
    }

    /**
     * 从指定版本开始允许销毁式迁移。
     *
     * @param startVersions 允许销毁式迁移的起始版本号
     */
    fun fallbackToDestructiveMigrationFrom(vararg startVersions: Int) {
        destructiveMigrationFrom = startVersions
    }

    /**
     * 允许在主线程执行数据库操作。
     *
     * **警告**：此选项仅用于测试！生产环境使用会导致 ANR。
     */
    fun allowMainThreadQueries() {
        allowMainThread = true
    }

    /**
     * 设置 WAL 日志模式。
     *
     * @param mode 日志模式，默认 [RoomDatabase.JournalMode.AUTOMATIC]
     */
    fun setJournalMode(mode: RoomDatabase.JournalMode) {
        journalMode = mode
    }

    /**
     * 从 assets 目录中的预打包数据库文件创建。
     *
     * 适用于首次安装时需要预填充数据的场景。
     *
     * @param assetFilePath assets 目录下的相对路径，如 "databases/prepopulated.db"
     */
    fun createFromAsset(assetFilePath: String) {
        this.assetFilePath = assetFilePath
    }

    /**
     * 从文件系统中的预打包数据库文件创建。
     *
     * @param databaseFile 预打包数据库文件
     */
    fun createFromFile(databaseFile: java.io.File) {
        this.databaseFile = databaseFile
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
    }
}
