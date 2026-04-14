package com.answufeng.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Base64
import java.util.Date

object AwConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for List<String>: $value", e)
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        if (list == null) return null
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for List<Long>: $value", e)
        }
    }

    @TypeConverter
    fun longListToString(list: List<Long>?): String? {
        if (list == null) return null
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromStringSet(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return try {
            json.decodeFromString<Set<String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Set<String>: $value", e)
        }
    }

    @TypeConverter
    fun stringSetToString(set: Set<String>?): String? {
        if (set == null) return null
        return json.encodeToString(set)
    }

    @TypeConverter
    fun fromLongSet(value: String?): Set<Long> {
        if (value.isNullOrBlank()) return emptySet()
        return try {
            json.decodeFromString<Set<Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Set<Long>: $value", e)
        }
    }

    @TypeConverter
    fun longSetToString(set: Set<Long>?): String? {
        if (set == null) return null
        return json.encodeToString(set)
    }

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Map<String, String>: $value", e)
        }
    }

    @TypeConverter
    fun stringMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromLongMap(value: String?): Map<String, Long> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, Long>>(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse JSON for Map<String, Long>: $value", e)
        }
    }

    @TypeConverter
    fun longMapToString(map: Map<String, Long>?): String? {
        if (map == null) return null
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromIntToBoolean(value: Int?): Boolean? = value?.let { it != 0 }

    @TypeConverter
    fun booleanToInt(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    fun fromBase64(value: String?): ByteArray? = value?.let { Base64.getDecoder().decode(it) }

    @TypeConverter
    fun byteArrayToBase64(value: ByteArray?): String? = value?.let { Base64.getEncoder().withoutPadding().encodeToString(it) }
}
