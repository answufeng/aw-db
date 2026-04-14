package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DbDebugHelper {

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

    fun tableSchema(db: RoomDatabase, table: String): List<ColumnInfo> {
        require(table.all { it.isLetterOrDigit() || it == '_' }) {
            "Invalid table name: $table. Only alphanumeric characters and underscores are allowed."
        }
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

data class ColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?
)

fun RoomDatabase.tableList(): List<String> = DbDebugHelper.tableList(this)

fun RoomDatabase.rowCount(table: String): Long = DbDebugHelper.rowCount(this, table)

fun RoomDatabase.tableSchema(table: String): List<ColumnInfo> = DbDebugHelper.tableSchema(this, table)
