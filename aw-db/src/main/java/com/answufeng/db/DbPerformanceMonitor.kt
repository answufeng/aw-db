package com.answufeng.db

import androidx.room.RoomDatabase

interface DbPerformanceListener {
    /**
     * @param durationMs 查詢耗時毫秒；若為 `-1` 表示目前實作僅記錄 SQL（Room QueryCallback 無可靠耗時鉤子）。
     */
    fun onQueryExecuted(sql: String, durationMs: Long)

    fun onSlowQuery(sql: String, durationMs: Long) {}
}

object DbPerformanceMonitor : RoomDatabase.QueryCallback {

    @Volatile
    private var enabled = false

    @Volatile
    var slowQueryThresholdMs: Long = 500L
        private set

    private val listeners =
        java.util.concurrent.CopyOnWriteArrayList<DbPerformanceListener>()

    private var queryCounter = 0L

    fun enable(thresholdMs: Long = 500L) {
        slowQueryThresholdMs = thresholdMs
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun isEnabled(): Boolean = enabled

    fun addListener(listener: DbPerformanceListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: DbPerformanceListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    override fun onQuery(sql: String, bindArgs: List<Any?>) {
        if (!enabled) return
        queryCounter++
        // Room 的 QueryCallback 僅提供「即將執行」的 SQL，無法在此取得可靠耗時；
        // 先前使用未閉環的 map 會造成內存洩漏。此處改為僅分發 SQL 記錄，耗時傳 -1。
        val durationMs = -1L
        for (l in listeners) {
            l.onQueryExecuted(sql, durationMs)
        }
    }
}
