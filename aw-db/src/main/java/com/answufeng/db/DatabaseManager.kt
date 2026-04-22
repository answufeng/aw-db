package com.answufeng.db

import android.content.Context
import androidx.room.RoomDatabase
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger

/**
 * 数据库生命周期管理器，提供引用计数的单例管理。
 *
 * Room 官方建议数据库实例应为单例，多实例打开同一数据库文件会导致 SQLite 锁竞争和数据损坏。
 * [DatabaseManager] 使用引用计数模式，确保最后一个使用者释放时才关闭数据库。
 *
 * ### 使用示例
 *
 * ```kotlin
 * // 获取数据库实例（自动引用计数）
 * val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") {
 *     addMigrations(MIGRATION_1_2)
 *     setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
 * }
 *
 * // 释放引用（计数归零时自动关闭数据库）
 * DatabaseManager.release("app.db")
 *
 * // 关闭所有数据库实例
 * DatabaseManager.closeAll()
 * ```
 *
 * ### 作用域内自动 release（推荐短生命周期场景）
 *
 * ```kotlin
 * DatabaseManager.acquireScoped<AppDatabase>(context, "app.db").use { handle ->
 *     handle.database.someDao().query()
 * }
 * ```
 *
 * 每调用一次 [acquireScoped] 会使引用计数 +1，[ScopedDatabaseHandle.close]（或 `use` 结束）时 -1；
 * 可与嵌套 `use` 安全配对。
 */
object DatabaseManager {

    /** 供 [getOrCreate] 等 public `inline` 实现访问；非稳定 ABI，勿直接使用。 */
    @PublishedApi
    internal val instances = mutableMapOf<String, ManagedDatabase<*>>()

    /**
     * 获取或创建数据库实例。
     *
     * 首次调用时创建实例并设置引用计数为 1，后续调用递增引用计数。
     * 线程安全。
     *
     * **注意**：[block] 仅在**首次**创建该 [name] 对应实例时执行；已存在实例时 [block] 会被忽略
     * （迁移、回调等以首次成功创建时的配置为准）。
     *
     * **注意**：同一 [name] 必须始终使用**同一个** [RoomDatabase] 子类；若已存在实例却请求
     * 另一种数据库类型，将抛出 [IllegalStateException]。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param name 数据库文件名，同时作为实例的唯一标识
     * @param block 可选的配置 DSL（仅首次创建时生效）
     * @return 数据库实例
     * @throws IllegalStateException 当 [name] 已被其他 [RoomDatabase] 类型占用时
     */
    inline fun <reified T : RoomDatabase> getOrCreate(
        context: Context,
        name: String,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): T {
        synchronized(this) {
            val existing = instances[name]
            if (existing != null) {
                checkDatabaseType(name, existing.database, T::class.java)
                existing.refCount.incrementAndGet()
                @Suppress("UNCHECKED_CAST")
                return existing.database as T
            }
            val database = AwDatabase.build<T>(context, name, block)
            val managed = ManagedDatabase(database)
            instances[name] = managed
            managed.refCount.incrementAndGet()
            return database
        }
    }

    /**
     * 获取或创建数据库实例，使用类名作为数据库名。
     *
     * 适用于只有一个数据库的简单场景，省去手动指定名称的步骤。
     *
     * @see getOrCreate 关于 [block] 与类型约束的说明同样适用
     */
    inline fun <reified T : RoomDatabase> getOrCreate(
        context: Context,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): T {
        return getOrCreate(context, T::class.java.simpleName, block)
    }

    /**
     * 释放数据库引用。
     *
     * 引用计数递减，归零时自动关闭数据库并移除实例。
     * 如果 [name] 对应的实例不存在，则不做任何操作。
     *
     * @param name 数据库文件名
     */
    fun release(name: String) {
        synchronized(this) {
            val managed = instances[name] ?: return
            if (managed.refCount.decrementAndGet() <= 0) {
                managed.database.close()
                instances.remove(name)
            }
        }
    }

    /**
     * 无视引用计数，立即关闭并移除指定名称的数据库实例。
     *
     * 用于 [DbBackupHelper.restore] 等必须在替换数据库文件**之前**确保持有连接全部关闭的场景。
     * 普通业务请使用 [release] 成对管理引用；滥用本方法会导致仍持有 [RoomDatabase] 引用的
     * 代码在关闭后使用数据库而崩溃。
     *
     * @param name 数据库文件名（与 [getOrCreate] 中的 [name] 一致）
     */
    fun forceClose(name: String) {
        synchronized(this) {
            val managed = instances.remove(name) ?: return
            managed.database.close()
        }
    }

    /**
     * 关闭所有数据库实例并清空缓存。
     *
     * 通常在应用退出或测试清理时调用。
     */
    fun closeAll() {
        synchronized(this) {
            val errors = mutableListOf<Exception>()
            instances.values.forEach { managed ->
                try {
                    managed.database.close()
                } catch (e: Exception) {
                    errors.add(e)
                }
            }
            instances.clear()
            if (errors.isNotEmpty()) {
                throw IllegalStateException("关闭数据库时发生 ${errors.size} 个错误，首个错误: ${errors.first().message}", errors.first())
            }
        }
    }

    /**
     * 获取已存在的数据库实例，不创建新实例；Java 或无法使用内联的调用方可使用
     * [getOrNull] 与 [ofClass] 重载。
     *
     * @param ofClass 期望的 [RoomDatabase] 子类
     * @return 实例，若未初始化则返回 null
     * @throws IllegalStateException 当 [name] 已存在实例且类型与 [ofClass] 不一致时
     */
    fun <T : RoomDatabase> getOrNull(name: String, ofClass: Class<T>): T? {
        synchronized(this) {
            val managed = instances[name] ?: return null
            checkDatabaseType(name, managed.database, ofClass)
            @Suppress("UNCHECKED_CAST")
            return managed.database as T
        }
    }

    /**
     * 获取已存在的数据库实例，不创建新实例。
     *
     * @see getOrNull(String, Class)
     */
    inline fun <reified T : RoomDatabase> getOrNull(name: String): T? =
        getOrNull(name, T::class.java)

    /**
     * 检查指定名称的数据库是否正在被管理。
     *
     * @param name 数据库文件名
     * @return 是否存在该数据库的管理实例
     */
    fun isManaged(name: String): Boolean {
        synchronized(this) {
            return instances.containsKey(name)
        }
    }

    /**
     * 获取指定数据库的当前引用计数。
     *
     * 主要用于调试，生产代码不应依赖此值做逻辑判断（并发环境下值可能随时变化）。
     *
     * @param name 数据库文件名
     * @return 引用计数，如果数据库未被管理则返回 0
     */
    fun getReferenceCount(name: String): Int {
        synchronized(this) {
            return instances[name]?.refCount?.get() ?: 0
        }
    }

    /**
     * 获取数据库并返回可在 `use { }` 结束时自动 [release] 的句柄。
     *
     * @see ScopedDatabaseHandle
     */
    inline fun <reified T : RoomDatabase> acquireScoped(
        context: Context,
        name: String,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): ScopedDatabaseHandle<T> {
        val db = getOrCreate<T>(context, name, block)
        return ScopedDatabaseHandle(name, db)
    }

    @PublishedApi
    internal class ManagedDatabase<T : RoomDatabase>(
        val database: T,
        val refCount: AtomicInteger = AtomicInteger(0)
    )
}

/**
 * [DatabaseManager.acquireScoped] 返回的句柄；实现 [Closeable]，便于 Kotlin `use { }`。
 */
class ScopedDatabaseHandle<T : RoomDatabase> @PublishedApi internal constructor(
    private val name: String,
    /** 当前持有的 [RoomDatabase] 实例（与 [DatabaseManager.getOrCreate] 返回的相同）。 */
    val database: T
) : Closeable {

    override fun close() {
        DatabaseManager.release(name)
    }
}

@PublishedApi
internal fun checkDatabaseType(
    name: String,
    database: RoomDatabase,
    expected: Class<out RoomDatabase>
) {
    if (!expected.isInstance(database)) {
        throw IllegalStateException(
            "Database name '$name' is already in use as ${database::class.java.name}, " +
                "but ${expected.name} was requested. Use a distinct database name or the same type."
        )
    }
}
