package com.answufeng.db

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移辅助工具，简化 Migration 的创建。
 *
 * ### 使用示例
 *
 * #### 单条 SQL 迁移
 * ```kotlin
 * val MIGRATION_1_2 = migration(1, 2) {
 *     execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
 * }
 * ```
 *
 * #### 多条 SQL 迁移
 * ```kotlin
 * val MIGRATION_2_3 = migration(2, 3) {
 *     execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY NOT NULL, `user_id` INTEGER NOT NULL)")
 *     execSQL("CREATE INDEX IF NOT EXISTS `index_orders_user_id` ON `orders` (`user_id`)")
 * }
 * ```
 *
 * #### 注册到数据库
 * ```kotlin
 * val db = AwDatabase.build<AppDatabase>(context, "app.db") {
 *     addMigrations(MIGRATION_1_2, MIGRATION_2_3)
 * }
 * ```
 */
fun migration(
    startVersion: Int,
    endVersion: Int,
    migrate: SupportSQLiteDatabase.() -> Unit
): Migration {
    require(startVersion >= 1) { "startVersion must be >= 1, got $startVersion" }
    require(endVersion > startVersion) { "endVersion ($endVersion) must be > startVersion ($startVersion)" }
    return object : Migration(startVersion, endVersion) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.migrate()
        }
    }
}

/**
 * 数据库创建回调，可用于预填充数据。
 *
 * ```kotlin
 * val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
 *     .addCallback(onCreateCallback {
 *         execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')")
 *     })
 *     .build()
 * ```
 *
 * @param block 数据库首次创建时执行的操作
 * @return RoomDatabase.Callback
 */
fun onCreateCallback(block: SupportSQLiteDatabase.() -> Unit): RoomDatabase.Callback {
    return object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.block()
        }
    }
}

/**
 * 数据库打开回调。
 *
 * @param block 每次打开数据库时执行的操作
 * @return RoomDatabase.Callback
 */
fun onOpenCallback(block: SupportSQLiteDatabase.() -> Unit): RoomDatabase.Callback {
    return object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            db.block()
        }
    }
}
