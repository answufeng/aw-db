# aw-db

[![](https://jitpack.io/v/answufeng/aw-db.svg)](https://jitpack.io/#answufeng/aw-db)

Room 数据库工具库，提供 DSL 风格的数据库构建器、通用 DAO、事务辅助、类型转换器和结果包装类。

## 特性

- 🏗️ **DSL 数据库构建器** — 链式配置迁移、回调、日志模式、预打包数据库等
- 📦 **通用 BaseDao** — 继承即获得完整 CRUD + Upsert 操作，减少样板代码
- 🔒 **事务辅助** — `runInTransaction`、`safeTransaction`、`batchExecute`（支持 SKIP/FAIL_FAST 策略）
- 🔄 **DbResult 密封类** — 统一 Loading/Success/Failure 状态，支持 Flow 和 LiveData
- 🔀 **Migration DSL** — 简化迁移和回调创建，告别样板代码
- 🧩 **类型转换器** — Date、List<String>、List<Long>、Map<String, String>、Boolean 开箱即用

## 环境要求

- minSdk 24+
- Kotlin 2.0+
- Room 2.6.1+

## 引入

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-db:1.0.0")
}
```

## 快速开始

### 1. 定义实体

```kotlin
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int
)
```

### 2. 定义 DAO

```kotlin
@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>

    @Query("SELECT * FROM User WHERE id = :id")
    abstract suspend fun getById(id: Long): User?

    @Query("SELECT * FROM User")
    abstract fun observeAll(): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM User")
    abstract suspend fun count(): Int

    @Query("DELETE FROM User")
    abstract suspend fun deleteAll()
}
```

### 3. 定义数据库

```kotlin
@Database(entities = [User::class], version = 1)
@TypeConverters(AwConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

### 4. 构建数据库

```kotlin
val db = AwDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2)
    fallbackToDestructiveMigration()
}
```

## API 文档

### AwDatabase — 数据库构建器

#### 基本构建

```kotlin
val db = AwDatabase.build<AppDatabase>(context, "app.db")
```

#### 内存数据库（适合测试）

```kotlin
val db = AwDatabase.buildInMemory<AppDatabase>(context)
```

#### 完整配置

```kotlin
val db = AwDatabase.build<AppDatabase>(context, "app.db") {
    // 迁移
    addMigrations(
        migration(1, 2) {
            execSQL("ALTER TABLE User ADD COLUMN email TEXT")
        }
    )

    // 回调
    addCallback(onCreateCallback {
        execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')")
    })
    addCallback(onOpenCallback {
        // 每次打开数据库时执行
    })

    // 销毁式迁移（⚠️ 会丢失数据）
    fallbackToDestructiveMigration()

    // 从指定版本允许销毁式迁移
    fallbackToDestructiveMigrationFrom(1, 2)

    // WAL 日志模式
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)

    // 预打包数据库
    createFromAsset("databases/prepopulated.db")

    // 从文件创建
    createFromFile(File("/path/to/db"))

    // ⚠️ 仅用于测试！生产环境会导致 ANR
    // allowMainThreadQueries()
}
```

### BaseDao — 通用 CRUD

继承 `BaseDao<T>` 即可获得以下操作：

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `insert(entity)` | 插入，冲突替换 | 行 ID |
| `insertAll(entities)` | 批量插入，冲突替换 | 行 ID 列表 |
| `insertOrIgnore(entity)` | 插入，冲突忽略 | 行 ID（冲突返回 -1） |
| `update(entity)` | 更新 | 受影响行数 |
| `updateAll(entities)` | 批量更新 | 受影响行数 |
| `delete(entity)` | 删除 | 受影响行数 |
| `deleteAll(entities)` | 批量删除 | 受影响行数 |
| `upsert(entity)` | 插入或更新（Room 原生 @Upsert） | 行 ID（更新返回 -1） |
| `upsertAll(entities)` | 批量插入或更新 | 行 ID 列表 |

> **注意**：所有方法均为 `suspend`，应在协程中调用，推荐使用 `Dispatchers.IO`。

### DbResult — 结果状态包装

`DbResult<T>` 是一个密封类，包含三种状态：

```kotlin
sealed class DbResult<out T> {
    data object Loading : DbResult<Nothing>()
    data class Success<T>(val data: T) : DbResult<T>()
    data class Failure(val error: Throwable) : DbResult<Nothing>()
}
```

#### 状态判断

```kotlin
result.isSuccess   // 是否成功
result.isFailure   // 是否失败
result.isLoading   // 是否加载中
```

#### 数据获取

```kotlin
result.getOrNull()           // 成功返回数据，否则 null
result.getOrDefault(default) // 成功返回数据，否则返回默认值
result.getOrElse(default)    // 同 getOrDefault
result.getOrThrow()          // 成功返回数据，失败抛异常，加载中返回 null
```

#### 链式回调

```kotlin
result.onSuccess { data -> showData(data) }
      .onFailure { error -> showError(error) }
      .onLoading { showLoading() }
```

#### 数据转换

```kotlin
val names: DbResult<List<String>> = result.map { users -> users.map { it.name } }
```

#### 错误恢复

```kotlin
val recovered = result.recover { error -> emptyList() }
val alternative = result.recoverWith { DbResult.Success(emptyList()) }
```

#### Fold 模式匹配

```kotlin
val text = result.fold(
    onLoading = { "Loading..." },
    onSuccess = { "${it.size} items" },
    onFailure = { "Error: ${it.message}" }
)
```

#### Flow 扩展

```kotlin
// 不含 Loading 状态
userDao.observeAll().asDbResult()
    .collect { result -> /* Success 或 Failure */ }

// 含 Loading 状态（推荐用于 UI）
userDao.observeAll().asDbResultWithLoading()
    .collect { result ->
        result.onLoading { showLoading() }
              .onSuccess { showData(it) }
              .onFailure { showError(it) }
    }

// 转为 LiveData
val liveData = userDao.observeAll()
    .asDbResultWithLoading()
    .asDbResultLiveData()
```

#### 一次性操作包装

```kotlin
val result = dbResultOf { userDao.getById(id) }
result.onSuccess { user -> showUser(user) }
      .onFailure { error -> showError(error) }
```

### 事务辅助

#### runInTransaction

在事务中执行操作，任何异常导致回滚：

```kotlin
val (userId, orderId) = db.runInTransaction {
    val userId = userDao.insert(user)
    val orderId = orderDao.insert(order.copy(userId = userId))
    Pair(userId, orderId)
}
```

#### safeTransaction

安全事务，自动捕获异常并包装为 `Result`：

```kotlin
val result = db.safeTransaction {
    userDao.deleteAll()
    userDao.insertAll(newUsers)
}
result.onSuccess { println("同步完成") }
      .onFailure { println("同步失败: ${it.message}") }
```

#### batchExecute

批量执行操作，支持两种失败策略：

```kotlin
// SKIP 策略（默认）：跳过失败项，返回成功条数
val count = db.batchExecute(users) { user ->
    userDao.insert(user)
}

// FAIL_FAST 策略：任一失败则全部回滚
val result = db.batchExecute(users, BatchFailureStrategy.FAIL_FAST) { user ->
    userDao.insert(user)
}
```

### Migration DSL

#### 创建迁移

```kotlin
val MIGRATION_1_2 = migration(1, 2) {
    execSQL("ALTER TABLE User ADD COLUMN email TEXT")
}

val MIGRATION_2_3 = migration(2, 3) {
    execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY NOT NULL, `user_id` INTEGER NOT NULL)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_orders_user_id` ON `orders` (`user_id`)")
}
```

#### 数据库回调

```kotlin
// 首次创建时
val onCreate = onCreateCallback {
    execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')")
}

// 每次打开时
val onOpen = onOpenCallback {
    // 执行数据库健康检查等
}
```

### AwConverters — 类型转换器

在数据库类上注册即可全局生效：

```kotlin
@Database(entities = [...], version = 1)
@TypeConverters(AwConverters::class)
abstract class AppDatabase : RoomDatabase()
```

| 类型 | 存储格式 | 说明 |
|------|---------|------|
| `Date` ↔ `Long` | 时间戳（毫秒） | null 安全 |
| `List<String>` ↔ `String` | JSON 数组 | 支持包含逗号/特殊字符的字符串 |
| `List<Long>` ↔ `String` | JSON 数组 | null 安全 |
| `Map<String, String>` ↔ `String` | JSON 对象 | null 安全 |
| `Boolean` ↔ `Int` | 0/1 | SQLite 原生不支持 Boolean |

> **注意**：`List<String>` 和 `List<Long>` 使用 JSON 数组格式（而非逗号分隔），确保字符串内容包含逗号时不会出错。

### PagedResult — 分页查询

`PagedResult<T>` 提供分页查询结果的封装：

```kotlin
data class PagedResult<T>(
    val items: List<T>,    // 当前页数据
    val total: Int,        // 总记录数
    val page: Int,         // 页码（从 0 开始）
    val pageSize: Int,     // 每页大小
    val hasMore: Boolean   // 是否还有更多数据
)
```

#### 手动分页

```kotlin
@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User ORDER BY name ASC LIMIT :limit OFFSET :offset")
    abstract suspend fun getPage(limit: Int, offset: Int): List<User>

    @Query("SELECT COUNT(*) FROM User")
    abstract suspend fun count(): Int
}

// 使用
val page = 0
val pageSize = 20
val items = userDao.getPage(pageSize, page * pageSize)
val total = userDao.count()
val result = items.toPagedResult(page, pageSize, total)

result.items.forEach { showUser(it) }
if (result.hasMore) loadMore()
```

#### Flow 分页

适用于已加载全部数据但需要分页展示的场景：

```kotlin
userDao.observeAll()
    .paginate(page = 0, pageSize = 20)
    .collect { pageItems -> showPage(pageItems) }
```

### DbDebugHelper — 调试工具

提供数据库调试辅助功能：

```kotlin
// 获取所有表名
val tables = db.tableList()

// 获取表行数
val count = db.rowCount("users")

// 获取表结构
val columns = db.tableSchema("users")
columns.forEach { col ->
    println("${col.name} ${col.type} ${if (col.notNull) "NOT NULL" else ""} ${col.defaultValue ?: ""}")
}
```

## 最佳实践

### 协程调度

所有 `BaseDao` 的方法均为 `suspend`，Room 默认使用 IO 调度器。在 ViewModel 或 Presenter 中调用时：

```kotlin
// ✅ Room 自动在 IO 线程执行，无需额外指定 Dispatcher
viewModelScope.launch {
    val users = userDao.getAll()
}

// ✅ 如果需要与其他 IO 操作组合
viewModelScope.launch {
    val users = withContext(Dispatchers.IO) { userDao.getAll() }
}
```

### 数据库生命周期

- **单例模式**：数据库实例应该是单例的，避免重复创建导致资源浪费
- **关闭数据库**：通常不需要手动关闭，系统会在进程退出时自动处理

```kotlin
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: AwDatabase.build<AppDatabase>(
                context.applicationContext, "app.db"
            ).also { instance = it }
        }
    }
}
```

### 迁移策略

- 始终为每个版本升级提供 `Migration`
- 仅在开发阶段使用 `fallbackToDestructiveMigration()`
- 生产环境务必提供完整的迁移路径

### Flow 观察

```kotlin
// ✅ 使用 asDbResultWithLoading() 在 UI 层展示完整状态
userDao.observeAll()
    .asDbResultWithLoading()
    .collect { result ->
        result.onLoading { binding.progressBar.isVisible = true }
              .onSuccess { binding.progressBar.isVisible = false; showData(it) }
              .onFailure { binding.progressBar.isVisible = false; showError(it) }
    }
```

## 常见问题

### Q: 为什么不用逗号分隔存储 List？

逗号分隔在字符串本身包含逗号时会出错。aw-db 使用 JSON 数组格式，确保数据完整性。

### Q: upsert 和 insert 有什么区别？

- `insert`：冲突时替换整行（`OnConflictStrategy.REPLACE`）
- `upsert`：冲突时仅更新指定列（Room 原生 `@Upsert`），更高效且不会触发级联删除

### Q: batchExecute 和 insertAll 有什么区别？

- `insertAll`：Room 原生批量插入，性能最优，但冲突策略固定
- `batchExecute`：逐条执行自定义操作，支持 SKIP/FAIL_FAST 策略，适用于需要自定义逻辑的场景

### Q: 什么时候用 asDbResult() vs asDbResultWithLoading()？

- `asDbResult()`：不需要 Loading 状态的场景（如后台同步）
- `asDbResultWithLoading()`：需要在 UI 展示加载状态的场景（如列表页面）

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。
