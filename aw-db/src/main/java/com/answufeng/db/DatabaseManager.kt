package com.answufeng.db

import android.content.Context
import androidx.room.RoomDatabase
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
 */
object DatabaseManager {

    @PublishedApi
    internal val instances = mutableMapOf<String, ManagedDatabase<*>>()
    @PublishedApi
    internal val lock = Any()

    /**
     * 获取或创建数据库实例。
     *
     * 首次调用时创建实例并设置引用计数为 1，后续调用递增引用计数。
     * 线程安全。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param name 数据库文件名，同时作为实例的唯一标识
     * @param block 可选的配置 DSL
     * @return 数据库实例
     */
    inline fun <reified T : RoomDatabase> getOrCreate(
        context: Context,
        name: String,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): T {
        synchronized(lock) {
            val managed = instances.getOrPut(name) {
                val database = AwDatabase.build<T>(context, name, block)
                ManagedDatabase(database)
            }
            managed.refCount.incrementAndGet()
            @Suppress("UNCHECKED_CAST")
            return managed.database as T
        }
    }

    /**
     * 获取或创建数据库实例，使用类名作为数据库名。
     *
     * 适用于只有一个数据库的简单场景，省去手动指定名称的步骤。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param block 可选的配置 DSL
     * @return 数据库实例
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
        synchronized(lock) {
            val managed = instances[name] ?: return
            if (managed.refCount.decrementAndGet() <= 0) {
                managed.database.close()
                instances.remove(name)
            }
        }
    }

    /**
     * 关闭所有数据库实例并清空缓存。
     *
     * 通常在应用退出或测试清理时调用。
     */
    fun closeAll() {
        synchronized(lock) {
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
     * 获取已存在的数据库实例，不创建新实例。
     *
     * 适用于只想使用已初始化的数据库而不想触发创建的场景。
     * 如果数据库尚未初始化，返回 null。
     *
     * @param T 数据库类型，必须继承 [RoomDatabase]
     * @param name 数据库文件名
     * @return 数据库实例，如果未初始化则返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : RoomDatabase> getOrNull(name: String): T? {
        synchronized(lock) {
            return instances[name]?.database as? T
        }
    }

    /**
     * 检查指定名称的数据库是否正在被管理。
     *
     * @param name 数据库文件名
     * @return 是否存在该数据库的管理实例
     */
    fun isManaged(name: String): Boolean {
        synchronized(lock) {
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
        synchronized(lock) {
            return instances[name]?.refCount?.get() ?: 0
        }
    }

    @PublishedApi
    internal class ManagedDatabase<T : RoomDatabase>(
        val database: T,
        val refCount: AtomicInteger = AtomicInteger(0)
    )
}
