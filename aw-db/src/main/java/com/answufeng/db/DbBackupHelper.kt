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
     * 恢复前会先关闭当前数据库实例，拷贝备份文件后再重新打开。
     * 如果使用 [DatabaseManager] 管理数据库，恢复后需要重新调用 [DatabaseManager.getOrCreate]。
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
        DatabaseManager.release(name)
        val dbFile = context.getDatabasePath(name)
        copyFile(backupFile, dbFile)
        return DatabaseManager.getOrCreate(context, name, block)
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
