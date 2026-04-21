package com.answufeng.db

import android.util.Base64
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Date

/**
 * Room 通用类型转换器，覆盖常用的集合、日期和二进制类型。
 *
 * 在数据库类上注册即可全局生效：
 *
 * ```kotlin
 * @Database(entities = [...], version = 1)
 * @TypeConverters(AwConverters::class)
 * abstract class AppDatabase : RoomDatabase()
 * ```
 *
 * 支持的类型转换：
 * - `Date` ↔ `Long`（毫秒时间戳）
 * - `List<String>` / `List<Long>` / `List<Int>` ↔ `String`（JSON 数组）
 * - `Set<String>` / `Set<Long>` / `Set<Int>` ↔ `String`（JSON 数组）
 * - `Map<String, String>` / `Map<String, Long>` / `Map<String, Int>` ↔ `String`（JSON 对象）
 * - `Boolean` ↔ `Int`（0/1）
 * - `ByteArray` ↔ `String`（Base64）
 *
 * JSON 解析失败时抛出 [IllegalArgumentException]，确保数据损坏可被感知。
 *
 * JSON 配置说明：
 * - `ignoreUnknownKeys = true`：当数据库 schema 演进新增字段后，旧版本读取时忽略未知字段，避免反序列化失败。
 *   如果需要严格模式（任何未知字段都报错），请自行创建 Json 实例并设置 ignoreUnknownKeys = false。
 */
object AwConverters {

    /**
     * 内部使用的 JSON 序列化实例。
     *
     * 配置了 `ignoreUnknownKeys = true` 以兼容 schema 演进。
     * 供所有 JSON 类型的 TypeConverter 方法共享使用。
     */
    internal val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for List<String>: $value", e)
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        if (list == null) return null
        if (list.isEmpty()) return "[]"
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromLongList(value: String?): List<Long>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for List<Long>: $value", e)
        }
    }

    @TypeConverter
    fun longListToString(list: List<Long>?): String? {
        if (list == null) return null
        if (list.isEmpty()) return "[]"
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<Int>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for List<Int>: $value", e)
        }
    }

    @TypeConverter
    fun intListToString(list: List<Int>?): String? {
        if (list == null) return null
        if (list.isEmpty()) return "[]"
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromStringSet(value: String?): Set<String>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Set<String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Set<String>: $value", e)
        }
    }

    @TypeConverter
    fun stringSetToString(set: Set<String>?): String? {
        if (set == null) return null
        if (set.isEmpty()) return "[]"
        return json.encodeToString(set)
    }

    @TypeConverter
    fun fromLongSet(value: String?): Set<Long>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Set<Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Set<Long>: $value", e)
        }
    }

    @TypeConverter
    fun longSetToString(set: Set<Long>?): String? {
        if (set == null) return null
        if (set.isEmpty()) return "[]"
        return json.encodeToString(set)
    }

    @TypeConverter
    fun fromIntSet(value: String?): Set<Int>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Set<Int>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Set<Int>: $value", e)
        }
    }

    @TypeConverter
    fun intSetToString(set: Set<Int>?): String? {
        if (set == null) return null
        if (set.isEmpty()) return "[]"
        return json.encodeToString(set)
    }

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Map<String, String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Map<String, String>: $value", e)
        }
    }

    @TypeConverter
    fun stringMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        if (map.isEmpty()) return "{}"
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromLongMap(value: String?): Map<String, Long>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Map<String, Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Map<String, Long>: $value", e)
        }
    }

    @TypeConverter
    fun longMapToString(map: Map<String, Long>?): String? {
        if (map == null) return null
        if (map.isEmpty()) return "{}"
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromIntMap(value: String?): Map<String, Int>? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<Map<String, Int>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Map<String, Int>: $value", e)
        }
    }

    @TypeConverter
    fun intMapToString(map: Map<String, Int>?): String? {
        if (map == null) return null
        if (map.isEmpty()) return "{}"
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromIntToBoolean(value: Int?): Boolean? = value?.let { it != 0 }

    @TypeConverter
    fun booleanToInt(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    fun fromBase64(value: String?): ByteArray? = value?.let { Base64.decode(it, Base64.DEFAULT) }

    @TypeConverter
    fun byteArrayToBase64(value: ByteArray?): String? = value?.let { Base64.encodeToString(it, Base64.NO_PADDING) }
}

/**
 * Enum 类型转换器基类，用于为具体的 Enum 类型创建 Room TypeConverter。
 *
 * Room 的 TypeConverter 不支持泛型（运行时类型擦除），
 * 因此需要为每个 Enum 类型创建一个具体的子类：
 *
 * ```kotlin
 * class StatusConverter : EnumConverter<Status>(Status::class.java)
 *
 * @TypeConverters(AwConverters::class, StatusConverter::class)
 * abstract class AppDatabase : RoomDatabase()
 * ```
 *
 * @param T Enum 类型
 * @param enumClass Enum 的 [Class] 对象
 */
abstract class EnumConverter<T : Enum<T>>(private val enumClass: Class<T>) {

    @TypeConverter
    fun fromEnum(value: T?): String? = value?.name

    @TypeConverter
    fun toEnum(value: String?): T? = value?.let { name ->
        enumClass.enumConstants?.find { it.name == name }
    }
}
