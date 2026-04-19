# aw-db consumer ProGuard rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# TypeConverters
-keepclassmembers class com.answufeng.db.AwConverters {
    @androidx.room.TypeConverter *;
}
-keepclassmembers class com.answufeng.db.AwJavaTimeConverters {
    @androidx.room.TypeConverter *;
}
-keep class * extends com.answufeng.db.EnumConverter {
    @androidx.room.TypeConverter *;
}

# DbResult sealed class
-keep class com.answufeng.db.DbResult { *; }
-keep class com.answufeng.db.DbResult$* { *; }

# BatchResult sealed class
-keep class com.answufeng.db.BatchResult { *; }
-keep class com.answufeng.db.BatchResult$* { *; }

# PagedResult
-keep class com.answufeng.db.PagedResult { *; }

# Kotlin metadata
-keepattributes Signature, *Annotation*
