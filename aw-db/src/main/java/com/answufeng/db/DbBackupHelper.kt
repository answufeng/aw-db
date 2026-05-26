package com.answufeng.db

import android.content.Context
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import org.json.JSONObject

object DbBackupHelper {

    private const val META_SUFFIX = ".meta"
    private const val SQLITE_HEADER = "SQLite format 3\u0000"

    fun backup(db: RoomDatabase, backupFile: File) {
        checkpoint(db)
        val dbPath = db.openHelper.readableDatabase.path
            ?: throw IllegalStateException("Database path is null, cannot backup")
        copyFile(File(dbPath), backupFile)
    }

    fun backupWithMetadata(db: RoomDatabase, backupFile: File) {
        backup(db, backupFile)
        val version = db.openHelper.readableDatabase.version
        val meta = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("version", version)
            put("dbName", backupFile.nameWithoutExtension)
        }
        val metaFile = File(backupFile.parent, backupFile.name + META_SUFFIX)
        metaFile.writeText(meta.toString())
    }

    fun verifyBackup(backupFile: File): Boolean {
        if (!backupFile.exists() || backupFile.length() < SQLITE_HEADER.length) return false
        return try {
            FileInputStream(backupFile).use { input ->
                val header = ByteArray(SQLITE_HEADER.length)
                val read = input.read(header)
                read == SQLITE_HEADER.length && String(header) == SQLITE_HEADER
            }
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupMetadata(backupFile: File): BackupMetadata? {
        val metaFile = File(backupFile.parent, backupFile.name + META_SUFFIX)
        if (!metaFile.exists()) return null
        return try {
            val json = JSONObject(metaFile.readText())
            BackupMetadata(
                timestamp = json.optLong("timestamp", 0L),
                version = json.optInt("version", 0),
                dbName = json.optString("dbName", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    inline fun <reified T : RoomDatabase> restore(
        context: Context,
        name: String,
        backupFile: File,
        requireVersionMatch: Boolean = false,
        noinline block: DatabaseConfig.() -> Unit = {}
    ): T {
        if (requireVersionMatch) {
            val meta = readBackupMetadata(backupFile)
            require(meta != null) {
                "requireVersionMatch=true but no .meta file found for ${backupFile.name}"
            }
        }
        synchronized(DatabaseManager) {
            DatabaseManager.forceClose(name)
            val dbFile = context.getDatabasePath(name)
            deleteDatabaseFiles(dbFile)
            copyFile(backupFile, dbFile)
            val db = DatabaseManager.getOrCreate<T>(context, name, block)
            if (requireVersionMatch) {
                val meta = readBackupMetadata(backupFile)!!
                val actual = db.openHelper.readableDatabase.version
                check(actual == meta.version) {
                    "Restored DB version $actual does not match backup metadata version ${meta.version}"
                }
            }
            return db
        }
    }

    /** 删除主库文件及 WAL/SHM 附属文件，避免还原后读到旧 WAL 状态。 */
    @PublishedApi
    internal fun deleteDatabaseFiles(dbFile: File) {
        dbFile.delete()
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()
        File("${dbFile.path}-journal").delete()
    }

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

data class BackupMetadata(
    val timestamp: Long,
    val version: Int,
    val dbName: String
)

fun RoomDatabase.backupTo(backupFile: File) = DbBackupHelper.backup(this, backupFile)

fun RoomDatabase.backupToWithMetadata(backupFile: File) = DbBackupHelper.backupWithMetadata(this, backupFile)

fun RoomDatabase.checkpoint() = DbBackupHelper.checkpoint(this)
