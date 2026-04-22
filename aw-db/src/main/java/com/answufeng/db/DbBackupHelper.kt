package com.answufeng.db

import android.content.Context
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 数据库备份与恢复工具。
 *
 * 备份时会先执行 WAL checkpoint 将所有待写数据刷入主数据库文件，确保备份完整性。
 *
 * **注意**：[restore] 方法不是线程安全的，调用方需确保恢复期间不会有其他线程访问数据库。
 * 如果使用 [DatabaseManager] 管理数据库，[restore] 内部会调用 [DatabaseManager.forceClose]
 * 立即关闭并移除该名称对应实例（**无视引用计数**），再拷贝文件，因此恢复后所有调用方
 * 须通过 [DatabaseManager.getOrCreate] 重新取得数据库。
 *
 * ```kotlin
 * // 备份数据库到文件
 * val backupFile = File(context.filesDir, "backup/app.db")
 * DbBackupHelper.backup(db, backupFile)
 *
 * // 从备份文件恢复
 * DbBackupHelper.restore<AppDatabase>(context, "app.db", backupFile) {
 *     addMigrations(MIGRATION_1_2)
 * }
 * ```
 */
object DbBackupHelper {

    /**
     * 备份数据库到指定文件。
     *
     * 内部会先执行 WAL checkpoint，确保所有数据写入主数据库文件后再拷贝。
     * 备份期间数据库仍可正常使用，但建议在低峰期执行以避免大文件拷贝影响性能。
     *
     * @param db Room 数据库实例
     * @param backupFile 备份目标文件（父目录必须存在）
     * @throws IOException 文件操作失败时抛出
     */
    fun backup(db: RoomDatabase, backupFile: File) {
        checkpoint(db)
        val dbPath = db.openHelper.readableDatabase.path
            ?: throw IllegalStateException("Database path is null, cannot backup")
        copyFile(File(dbPath), backupFile)
    }

    /**
     * 从备份文件恢复数据库。
     *
     * 若通过 [DatabaseManager] 管理，内部会对该 [name] 调用 [DatabaseManager.forceClose]（无视引用计数），
     * 再覆盖数据库文件，因此恢复前仍持有 [RoomDatabase] 引用的旧代码不得再继续使用；恢复后
     * 应重新 [DatabaseManager.getOrCreate] 获取新实例。
     *
     * **注意**：此方法不是线程安全的，调用方需确保恢复期间不会有其他线程访问数据库。
     *
     * @param T 数据库类型
     * @param context Context
     * @param name 数据库文件名
     * @param backupFile 备份源文件
     * @param block 数据库配置 DSL
     * @return 恢复后的数据库实例
     * @throws IOException 文件操作失败时抛出
     */
    inline fun <reified T : RoomDatabase> restore(
        context: Context,
        name: String,
        backupFile: File,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): T {
        synchronized(DatabaseManager) {
            DatabaseManager.forceClose(name)
            val dbFile = context.getDatabasePath(name)
            copyFile(backupFile, dbFile)
            return DatabaseManager.getOrCreate(context, name, block)
        }
    }

    /**
     * 执行 WAL checkpoint，将 WAL 日志中的数据写入主数据库文件。
     *
     * 在备份前调用可确保备份包含所有已提交的数据。
     */
    fun checkpoint(db: RoomDatabase) {
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
    }

    @PublishedApi
    internal fun copyFile(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        FileInputStream(src).use { input ->
            FileOutputStream(dst).use { output ->
                input.channel.transferTo(0, input.channel.size(), output.channel)
            }
        }
    }
}

fun RoomDatabase.backupTo(backupFile: File) = DbBackupHelper.backup(this, backupFile)

fun RoomDatabase.checkpoint() = DbBackupHelper.checkpoint(this)
