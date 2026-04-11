# aw-db

Room 数据库工具库，提供 DSL 风格的数据库构建器、通用 DAO、事务辅助、类型转换器和结果包装类。

## 引入

在模块级 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-db:1.0.0")
}
```

确保根 `settings.gradle.kts` 中配置了 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

## 功能特性

- DSL 风格的 Room 数据库构建器，支持迁移和回调配置
- 通用 `BaseDao<T>`，提供 CRUD + upsert 操作
- 事务辅助：`runInTransaction`、`safeTransaction`、`batchInsert`
- Room 类型转换器：Date、List<String>、List<Long>、Map<String, String>
- `DbResult<T>` 密封类，支持响应式数据观察（Loading/Success/Failure）
- 简化的 Migration/Callback 创建，使用 Kotlin DSL

## 使用示例

```kotlin
// 构建数据库
val db = BrickDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(migration(1, 2) { executeSql("ALTER TABLE User ADD COLUMN email TEXT") })
    addCallback(onCreateCallback { /* 初始化数据 */ })
    fallbackToDestructiveMigration()
}

// 通用 DAO
@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>
}

// 事务操作
val result = db.safeTransaction { userDao().insert(User(name = "test")) }

// DbResult 响应式观察
db.userDao().observeAll().asDbResult().collect { result ->
    result.onLoading { showLoading() }
        .onSuccess { showData(it) }
        .onFailure { showError(it) }
}
```

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。
