package com.answufeng.db

import androidx.room.TypeConverter
import org.json.JSONObject
import java.util.Date

/**
 * Room 常用类型转换器合集。
 *
 * 在 Database 类上注册即可全局生效：
 *
 * ```kotlin
 * @Database(entities = [...], version = 1)
 * @TypeConverters(BrickConverters::class)
 * abstract class AppDatabase : RoomDatabase() { ... }
 * ```
 *
 * 包含的转换：
 * - [Date] ↔ [Long]（时间戳）
 * - [List]<[String]> ↔ [String]（逗号分隔）
 * - [Map]<[String], [String]> ↔ [String]（JSON 格式）
 * - [List]<[Long]> ↔ [String]（逗号分隔）
 */
class BrickConverters {

    // ==================== Date ↔ Long ====================

    /**
     * Long 时间戳 → Date 对象。
     *
     * @param value 时间戳（毫秒），null 时返回 null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Date 对象 → Long 时间戳。
     *
     * @param date Date 对象，null 时返回 null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // ==================== List<String> ↔ String ====================

    /**
     * 逗号分隔字符串 → List<String>。
     *
     * @param value 逗号分隔的字符串，null 或空时返回空列表
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()
    }

    /**
     * List<String> → 逗号分隔字符串。
     *
     * @param list 字符串列表，null 时返回 null
     */
    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    // ==================== List<Long> ↔ String ====================

    /**
     * 逗号分隔字符串 → List<Long>。
     *
     * @param value 逗号分隔的字符串，null 或空时返回空列表
     */
    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        return value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?: emptyList()
    }

    /**
     * List<Long> → 逗号分隔字符串。
     *
     * @param list Long 列表，null 时返回 null
     */
    @TypeConverter
    fun longListToString(list: List<Long>?): String? {
        return list?.joinToString(",")
    }

    // ==================== Map<String, String> ↔ String ====================

    /**
     * JSON 字符串 → Map<String, String>。
     *
     * @param value JSON 字符串，null 或空时返回空 Map
     */
    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(value)
            val map = mutableMapOf<String, String>()
            for (key in json.keys()) {
                map[key] = json.getString(key)
            }
            map
        } catch (e: Exception) {
            android.util.Log.w("aw-db", "Failed to parse JSON map: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Map<String, String> → JSON 字符串。
     *
     * @param map 字符串 Map，null 时返回 null
     */
    @TypeConverter
    fun stringMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return JSONObject(map).toString()
    }
}
