package com.answufeng.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * java.time 类型转换器，支持 JSR-310 时间类型。
 *
 * 需要启用 `coreLibraryDesugaring` 才能在 minSdk < 26 的设备上使用。
 *
 * ```kotlin
 * @Database(entities = [...], version = 1)
 * @TypeConverters(AwConverters::class, AwJavaTimeConverters::class)
 * abstract class AppDatabase : RoomDatabase()
 * ```
 *
 * 支持的类型转换：
 * - `Instant` ↔ `Long`（毫秒时间戳）
 * - `LocalDateTime` ↔ `String`（ISO 格式）
 * - `LocalDate` ↔ `String`（ISO 格式）
 */
object AwJavaTimeConverters {

    @TypeConverter
    fun fromInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun fromLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()
}
