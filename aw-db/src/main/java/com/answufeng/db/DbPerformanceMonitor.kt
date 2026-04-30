package com.answufeng.db

import androidx.room.RoomDatabase

interface DbPerformanceListener {
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

    private val queryStartTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()
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
        val key = "${queryCounter++}"
        queryStartTimes[key] = System.nanoTime()
    }

    internal fun onQueryCompleted(sql: String) {
        if (!enabled) return
        val now = System.nanoTime()
        val entry = queryStartTimes.entries.firstOrNull() ?: return
        queryStartTimes.remove(entry.key)
        val durationMs = (now - entry.value) / 1_000_000
        for (l in listeners) {
            l.onQueryExecuted(sql, durationMs)
        }
        if (durationMs > slowQueryThresholdMs) {
            for (l in listeners) {
                l.onSlowQuery(sql, durationMs)
            }
        }
    }
}
