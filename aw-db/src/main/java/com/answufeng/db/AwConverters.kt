package com.answufeng.db

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Room 常用类型转换器合集。
 *
 * 在 Database 类上注册即可全局生效：
 *
 * ```kotlin
 * @Database(entities = [...], version = 1)
 * @TypeConverters(AwConverters::class)
 * abstract class AppDatabase : RoomDatabase() { ... }
 * ```
 *
 * 包含的转换：
 * - [Date] ↔ [Long]（时间戳）
 * - [List]<[String]> ↔ [String]（JSON 数组）
 * - [List]<[Long]> ↔ [String]（JSON 数组）
 * - [Map]<[String], [String]> ↔ [String]（JSON 对象）
 * - [Boolean] ↔ [Int]（0/1）
 */
class AwConverters {

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
     * JSON 数组字符串 → List<String>。
     *
     * @param value JSON 数组字符串，null 或空时返回空列表
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * List<String> → JSON 数组字符串。
     *
     * @param list 字符串列表，null 时返回 null
     */
    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    // ==================== List<Long> ↔ String ====================

    /**
     * JSON 数组字符串 → List<Long>。
     *
     * @param value JSON 数组字符串，null 或空时返回空列表
     */
    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * List<Long> → JSON 数组字符串。
     *
     * @param list Long 列表，null 时返回 null
     */
    @TypeConverter
    fun longListToString(list: List<Long>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    // ==================== Map<String, String> ↔ String ====================

    /**
     * JSON 对象字符串 → Map<String, String>。
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
            emptyMap()
        }
    }

    /**
     * Map<String, String> → JSON 对象字符串。
     *
     * @param map 字符串 Map，null 时返回 null
     */
    @TypeConverter
    fun stringMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return JSONObject(map).toString()
    }

    // ==================== Boolean ↔ Int ====================

    /**
     * Int → Boolean。
     *
     * @param value 整数值，1 为 true，其余为 false
     */
    @TypeConverter
    fun fromIntToBoolean(value: Int?): Boolean? {
        return value?.let { it != 0 }
    }

    /**
     * Boolean → Int。
     *
     * @param value 布尔值，true 为 1，false 为 0
     */
    @TypeConverter
    fun booleanToInt(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }
}
