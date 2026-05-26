package com.answufeng.db

import androidx.room.RoomDatabase

/**
 * SQL 查询日志监听器。
 *
 * 基于 Room [RoomDatabase.QueryCallback]，在查询**即将执行**时分发 SQL 与绑定参数。
 * Room 不提供可靠的单次查询耗时，[onQueryExecuted] 的 [durationMs] 恒为 [DURATION_UNKNOWN]。
 */
fun interface SqlQueryListener {
    /**
     * @param durationMs 查询耗时毫秒；当前实现恒为 [DURATION_UNKNOWN]（-1），表示仅记录 SQL。
     */
    fun onQueryExecuted(sql: String, bindArgs: List<Any?>, durationMs: Long)
}

/**
 * Room SQL 执行日志，实现 [RoomDatabase.QueryCallback]。
 *
 * 在 [DatabaseConfig.setQueryCallback] 中注册：
 *
 * ```kotlin
 * AwDatabase.build<AppDatabase>(context, "app.db") {
 *     setQueryCallback(SqlQueryLogger)
 * }
 * ```
 */
object SqlQueryLogger : RoomDatabase.QueryCallback {

    const val DURATION_UNKNOWN: Long = -1L

    @Volatile
    private var enabled = false

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<SqlQueryListener>()

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun isEnabled(): Boolean = enabled

    fun addListener(listener: SqlQueryListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SqlQueryListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    override fun onQuery(sql: String, bindArgs: List<Any?>) {
        if (!enabled) return
        for (l in listeners) {
            l.onQueryExecuted(sql, bindArgs, DURATION_UNKNOWN)
        }
    }
}
