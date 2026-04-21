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

# DatabaseManager
-keep class com.answufeng.db.DatabaseManager { *; }
-keep class com.answufeng.db.DatabaseManager$* { *; }

# AwDatabase (DSL builder)
-keep class com.answufeng.db.AwDatabase { *; }
-keep class com.answufeng.db.AwDatabase$* { *; }

# MigrationHelper
-keep class com.answufeng.db.MigrationHelper { *; }
-keep class com.answufeng.db.MigrationHelper$* { *; }

# TransactionHelper
-keep class com.answufeng.db.TransactionHelper { *; }
-keep class com.answufeng.db.TransactionHelper$* { *; }
-keep class com.answufeng.db.TransactionHelperKt { *; }

# PagingExt
-keep class com.answufeng.db.PagingExtKt { *; }

# DbDebugHelper
-keep class com.answufeng.db.DbDebugHelper { *; }
-keep class com.answufeng.db.DbDebugHelperKt { *; }

# BaseDao
-keep class com.answufeng.db.BaseDao { *; }

# DSL builder
-keep class com.answufeng.db.DatabaseConfig { *; }
-keep class com.answufeng.db.DatabaseConfig$Builder { *; }
-keep class com.answufeng.db.DatabaseConfig$* { *; }
-keep interface com.answufeng.db.AwDbDsl { *; }

# DbResult extension functions
-keep class com.answufeng.db.DbResultKt { *; }

# Kotlin metadata
-keepattributes Signature, *Annotation*
