package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库调试辅助工具，提供表信息查询功能。
 *
 * ```kotlin
 * val tables = db.tableList()
 * val count = db.rowCount("users")
 * val columns = db.tableSchema("users")
 * ```
 *
 * 注意：这些方法会直接访问 SQLite 数据库，应在调试阶段使用，不建议在生产环境频繁调用。
 */
object DbDebugHelper {

    /**
     * 获取所有用户表名（过滤 sqlite_ 和 room_ 前缀的系统表）。
     *
     * @param db Room 数据库实例
     * @return 用户表名列表
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
     * 获取表行数。
     *
     * @param db Room 数据库实例
     * @param table 表名（仅允许字母数字和下划线，防止 SQL 注入）
     * @return 行数
     */
    fun rowCount(db: RoomDatabase, table: String): Long {
        require(table.all { it.isLetterOrDigit() || it == '_' }) {
            "Invalid table name: $table. Only alphanumeric characters and underscores are allowed."
        }
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM `$table`"
        )
        cursor.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return 0L
    }

    /**
     * 获取表结构信息。
     *
     * @param db Room 数据库实例
     * @param table 表名（仅允许字母数字和下划线，防止 SQL 注入）
     * @return 列信息列表
     */
    fun tableSchema(db: RoomDatabase, table: String): List<TableColumnInfo> {
        require(table.all { it.isLetterOrDigit() || it == '_' }) {
            "Invalid table name: $table. Only alphanumeric characters and underscores are allowed."
        }
        val columns = mutableListOf<TableColumnInfo>()
        val cursor = db.openHelper.readableDatabase.query(
            "PRAGMA table_info(`$table`)"
        )
        cursor.use {
            while (it.moveToNext()) {
                columns.add(
                    TableColumnInfo(
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
 * 表列结构信息。
 *
 * @property name 列名
 * @property type 列类型（如 TEXT、INTEGER、REAL）
 * @property notNull 是否非空
 * @property defaultValue 默认值
 */
data class TableColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?
)

/** 获取所有用户表名。 */
fun RoomDatabase.tableList(): List<String> = DbDebugHelper.tableList(this)

/** 获取表行数。 */
fun RoomDatabase.rowCount(table: String): Long = DbDebugHelper.rowCount(this, table)

/** 获取表结构信息。 */
fun RoomDatabase.tableSchema(table: String): List<TableColumnInfo> = DbDebugHelper.tableSchema(this, table)
