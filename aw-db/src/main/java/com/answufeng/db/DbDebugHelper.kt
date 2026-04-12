package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库调试辅助工具。
 *
 * ### 使用示例
 *
 * ```kotlin
 * val tables = db.tableList()
 * val count = db.rowCount("users")
 * val schema = db.tableSchema("users")
 * ```
 */
object DbDebugHelper {

    /**
     * 获取数据库中所有表名。
     *
     * @param db RoomDatabase 实例
     * @return 表名列表
     */
    fun tableList(db: RoomDatabase): List<String> {
        val tables = mutableListOf<String>()
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'"
        )
        cursor.use {
            while (it.moveToNext()) {
                tables.add(it.getString(0))
            }
        }
        return tables
    }

    /**
     * 获取指定表的行数。
     *
     * @param db RoomDatabase 实例
     * @param table 表名
     * @return 行数
     */
    fun rowCount(db: RoomDatabase, table: String): Long {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM `$table`"
        )
        cursor.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return 0L
    }

    /**
     * 获取指定表的列信息。
     *
     * @param db RoomDatabase 实例
     * @param table 表名
     * @return 列信息列表（列名、类型、非空、默认值）
     */
    fun tableSchema(db: RoomDatabase, table: String): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        val cursor = db.openHelper.readableDatabase.query(
            "PRAGMA table_info(`$table`)"
        )
        cursor.use {
            while (it.moveToNext()) {
                columns.add(
                    ColumnInfo(
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        type = it.getString(it.getColumnIndexOrThrow("type")),
                        notNull = it.getInt(it.getColumnIndexOrThrow("notnull")) == 1,
                        defaultValue = it.getString(it.getColumnIndexOrThrow("dflt_value"))
                    )
                )
            }
        }
        return columns
    }
}

/**
 * 列信息。
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?
)

/**
 * 获取数据库中所有表名。
 */
fun RoomDatabase.tableList(): List<String> = DbDebugHelper.tableList(this)

/**
 * 获取指定表的行数。
 */
fun RoomDatabase.rowCount(table: String): Long = DbDebugHelper.rowCount(this, table)

/**
 * 获取指定表的列信息。
 */
fun RoomDatabase.tableSchema(table: String): List<ColumnInfo> = DbDebugHelper.tableSchema(this, table)
