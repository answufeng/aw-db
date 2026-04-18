package com.answufeng.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.migration.AutoMigrationSpec
import java.util.concurrent.Executor

@DslMarker
annotation class AwDbDsl

/**
 * Room 数据库 DSL 构建器，简化 [Room.databaseBuilder] 的配置流程。
 *
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db") {
 *     addMigrations(MIGRATION_1_2)
 *     fallbackToDestructiveMigration()
 *     setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
 * }
 * ```
 */
object AwDatabase {

    /**
     * 构建持久化数据库实例。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param name 数据库文件名
     * @param block 可选的 [DatabaseConfig] DSL 配置
     * @return 构建完成的数据库实例
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
     * 构建内存数据库实例（数据不会持久化，适合测试）。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param block 可选的 [DatabaseConfig] DSL 配置
     * @return 构建完成的内存数据库实例
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
 * 数据库构建 DSL 配置类，用于链式配置 Room 数据库的各种选项。
 *
 * 在 [AwDatabase.build] 的 lambda 中使用：
 *
 * ```kotlin
 * AwDatabase.build<AppDatabase>(context, "app.db") {
 *     addMigrations(migration(1, 2) { execSQL("...") })
 *     addCallback(onCreateCallback { execSQL("...") })
 *     setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
 * }
 * ```
 */
@AwDbDsl
class DatabaseConfig {

    private val migrations = mutableListOf<Migration>()
    private val autoMigrationSpecs = mutableListOf<AutoMigrationSpec>()
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

    /** 添加数据库迁移。 */
    fun addMigrations(vararg migration: Migration) {
        migrations.addAll(migration)
    }

    /** 添加自动迁移规范（Room 2.4+ 的 @AutoMigration 所需）。 */
    fun addAutoMigrationSpec(vararg spec: AutoMigrationSpec) {
        autoMigrationSpecs.addAll(spec)
    }

    /** 添加数据库回调（如 onCreate、onOpen）。 */
    fun addCallback(callback: RoomDatabase.Callback) {
        callbacks.add(callback)
    }

    /** 允许销毁式迁移（⚠️ 会丢失数据，仅开发阶段使用）。 */
    fun fallbackToDestructiveMigration() {
        destructiveMigration = true
    }

    /** 从指定版本允许销毁式迁移。 */
    fun fallbackToDestructiveMigrationFrom(vararg startVersions: Int) {
        destructiveMigrationFrom = startVersions
    }

    /** 允许主线程查询（⚠️ 仅用于测试，生产环境会导致 ANR）。 */
    fun allowMainThreadQueries() {
        allowMainThread = true
    }

    /** 设置数据库日志模式（如 WAL）。 */
    fun setJournalMode(mode: RoomDatabase.JournalMode) {
        journalMode = mode
    }

    /** 从 Asset 文件创建预打包数据库。 */
    fun createFromAsset(assetFilePath: String) {
        this.assetFilePath = assetFilePath
    }

    /** 从文件创建预打包数据库。 */
    fun createFromFile(databaseFile: java.io.File) {
        this.databaseFile = databaseFile
    }

    /** 设置查询执行器。 */
    fun setQueryExecutor(executor: Executor) {
        queryExecutor = executor
    }

    /** 设置事务执行器。 */
    fun setTransactionExecutor(executor: Executor) {
        transactionExecutor = executor
    }

    /**
     * 启用多实例失效通知。
     *
     * 当多个进程打开同一数据库时，一个进程的数据变更需要通知其他进程。
     * 单进程应用无需启用，以减少开销。
     */
    fun enableMultiInstanceInvalidation() {
        multiInstanceInvalidation = true
    }

    @PublishedApi
    internal fun <T : RoomDatabase> apply(builder: RoomDatabase.Builder<T>) {
        if (migrations.isNotEmpty()) {
            builder.addMigrations(*migrations.toTypedArray())
        }
        autoMigrationSpecs.forEach { builder.addAutoMigrationSpec(it) }
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

inline fun <reified T : RoomDatabase> Context.buildDatabase(
    name: String,
    block: DatabaseConfig.() -> Unit = {}
): T = AwDatabase.build(this, name, block)

inline fun <reified T : RoomDatabase> Context.buildInMemoryDatabase(
    block: DatabaseConfig.() -> Unit = {}
): T = AwDatabase.buildInMemory(this, block)
