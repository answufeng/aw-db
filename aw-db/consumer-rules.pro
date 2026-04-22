# aw-db consumer ProGuard rules
# 这些规则通过 consumerProguardFiles 自动应用到宿主应用
# R8 full mode：建议在宿主 release 用 -printusage 抽样，逐步收紧 -keep（Room/Gson 反射边界以官方规则为准）

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# TypeConverters — 保留标注了 @TypeConverter 的方法
-keepclassmembers class com.answufeng.db.AwConverters {
    @androidx.room.TypeConverter *;
}
-keepclassmembers class com.answufeng.db.AwJavaTimeConverters {
    @androidx.room.TypeConverter *;
}
-keep class * extends com.answufeng.db.EnumConverter {
    @androidx.room.TypeConverter *;
}

# kotlinx.serialization — 库使用了 kotlinx.serialization JSON
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class com.answufeng.db.AwConverters {
    kotlinx.serialization.json.Json json;
}

# 密封类 — 保留构造方法和数据类成员（反射反序列化需要）
-keepclassmembers class com.answufeng.db.DbResult$Success {
    public <init>(java.lang.Object);
}
-keepclassmembers class com.answufeng.db.DbResult$Failure {
    public <init>(java.lang.Throwable);
}

-keepclassmembers class com.answufeng.db.BatchResult$Skipped {
    public <init>(int, int, java.util.List);
}
-keepclassmembers class com.answufeng.db.BatchResult$AllOrNothing {
    public <init>(kotlin.Result);
}

# 库公开 API — 保持可访问性（反射/调试场景）
-keep class com.answufeng.db.DatabaseManager { *; }
-keep class com.answufeng.db.AwDatabase { *; }
-keep class com.answufeng.db.DatabaseConfig { *; }
-keep class com.answufeng.db.BaseDao { *; }
-keep class com.answufeng.db.DbDebugHelper { *; }

# Kotlin 元数据
-keepattributes Signature, *Annotation*, KotlinDebugMetadata
-keep class kotlin.Metadata { *; }
