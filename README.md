# aw-db

[![](https://jitpack.io/v/answufeng/aw-db.svg)](https://jitpack.io/#answufeng/aw-db)

Room 数据库工具库，提供 DSL 风格的数据库构建器、通用 DAO、事务辅助、类型转换器和结果包装类。

## 特性

- 🏗️ **DSL 数据库构建器** — 链式配置迁移、回调、日志模式、预打包数据库、自定义 Executor 等
- 📦 **通用 BaseDao** — 继承即获得完整 CRUD + Upsert 操作，批量操作自带 `@Transaction` 保证原子性
- 🔒 **事务辅助** — `withTx`、`safeTransaction`、`batchExecute`（类型安全的 `BatchResult` 密封类，支持 SKIP/FAIL_FAST 策略）
- 🔄 **DbResult 密封类** — 统一 Loading/Success/Failure 状态，支持 `flatMap`/`mapFailure`/`filter`/`combineDbResults` 等函数式操作
- 🔀 **Migration DSL** — 简化迁移和回调创建，告别样板代码
- 🧩 **类型转换器** — Date、Instant、LocalDateTime、LocalDate、List、Set、Map、Boolean、ByteArray、Enum 开箱即用
- 📄 **Paging 3 集成** — `asPagingFlow()`/`asDbResultPagingFlow()` 直接对接 AndroidX Paging
- 🏛️ **数据库生命周期管理** — `DatabaseManager` 引用计数单例，防止多实例问题

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
    ksp("androidx.room:room-compiler:2.6.1") // Room 编译器，需与 aw-db 使用的 Room 版本一致
}
```

> **依赖说明**：aw-db 通过 `api` 暴露 Room 和 Lifecycle 依赖，宿主项目无需单独声明 `room-runtime`/`room-ktx`。但必须添加 `ksp("androidx.room:room-compiler")` 才能编译 `@Dao`/`@Database` 注解。

## 快速开始

### 1. 定义实体、DAO 和数据库

```kotlin
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int
)

@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>
}

@Database(entities = [User::class], version = 1)
@TypeConverters(AwConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

### 2. 构建数据库

```kotlin
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2)
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
}

// 不再使用时释放
DatabaseManager.release("app.db")
```

### 3. 执行 CRUD 操作

```kotlin
// 插入
val id = db.userDao().insert(User(name = "张三", age = 25))

// 查询
val users = db.userDao().getAll()

// 观察数据变化
db.userDao().observeAll()
    .asDbResultWithLoading()
    .collect { result ->
        result.onLoading { showLoading() }
              .onSuccess { showData(it) }
              .onFailure { showError(it) }
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

    // 自动迁移规范（Room 2.4+ 的 @AutoMigration 所需）
    addAutoMigrationSpec(MyAutoMigrationSpec())

    // 回调
    addCallback(onCreateCallback {
        execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')")
    })
    addCallback(onOpenCallback {
        // 每次打开数据库时执行
    })
    addCallback(onDestructiveMigrationCallback {
        // 销毁式迁移时执行（如记录日志或重新填充数据）
    })

    // 销毁式迁移（⚠️ 会丢失数据）
    fallbackToDestructiveMigration()

    // 从指定版本允许销毁式迁移
    fallbackToDestructiveMigrationFrom(1, 2)

    // WAL 日志模式
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)

    // 预打包数据库（与下面 createFromFile 二选一，不能同时配置）
    createFromAsset("databases/prepopulated.db")
    // createFromFile(File("/path/to/prepopulated.db"))

    // 自定义查询线程池
    setQueryExecutor(Executors.newFixedThreadPool(4))

    // 自定义事务线程池
    setTransactionExecutor(Executors.newSingleThreadExecutor())

    // 多进程应用启用多实例失效通知（单进程无需启用）
    enableMultiInstanceInvalidation()

    // ⚠️ 仅用于测试！生产环境会导致 ANR
    // allowMainThreadQueries()
}
```

### BaseDao — 通用 CRUD

继承 `BaseDao<T>` 即可获得以下操作：

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `insert(entity)` | 插入，冲突替换（⚠️ 会删除旧行，可能触发 CASCADE） | 行 ID |
| `insertAll(entities)` | 批量插入，冲突替换（`@Transaction`） | 行 ID 列表 |
| `insertOrIgnore(entity)` | 插入，冲突忽略 | 行 ID（冲突返回 -1） |
| `insertOrIgnoreAll(entities)` | 批量插入，冲突忽略（`@Transaction`） | 行 ID 列表 |
| `update(entity)` | 更新 | 受影响行数 |
| `updateAll(entities)` | 批量更新（`@Transaction`） | 受影响行数 |
| `delete(entity)` | 删除 | 受影响行数 |
| `deleteAll(entities)` | 批量删除（`@Transaction`） | 受影响行数 |
| `upsert(entity)` | 插入或更新（Room 原生 @Upsert） | 行 ID（更新返回 -1） |
| `upsertAll(entities)` | 批量插入或更新（`@Transaction`） | 行 ID 列表 |

> **注意**：所有方法均为 `suspend`，Room 自动在 IO 线程执行，无需外部包裹 `withContext(Dispatchers.IO)`。批量操作已标注 `@Transaction`，确保原子性。

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
result.getOrElse { default } // 成功返回数据，否则执行 lambda 返回默认值（惰性求值）
result.getOrThrow()          // 成功返回数据，失败/加载中抛异常
```

#### 链式回调

```kotlin
result.onSuccess { data -> showData(data) }
      .onFailure { error -> showError(error) }
      .onLoading { showLoading() }
```

#### 数据转换

```kotlin
// map — 转换成功数据
val names: DbResult<List<String>> = result.map { users -> users.map { it.name } }

// flatMap — 链式操作
val detail: DbResult<UserDetail> = result.flatMap { user -> fetchDetail(user) }

// mapFailure — 转换错误类型
val mapped = result.mapFailure { ApiError.from(it) }

// filter — 条件过滤
val filtered = result.filter { it.isNotEmpty() }

// onEach — 副作用
result.onEach { log("Got: $it") }
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

#### 合并多个 DbResult

```kotlin
val combined = combineDbResults(result1, result2) { users, orders ->
    Pair(users, orders)
}
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

// 转为 LiveData（带超时自动断开上游 Flow）
val liveData = userDao.observeAll()
    .asDbResultWithLoading()
    .asDbResultLiveData() // 默认 5 秒超时

// 或一步到位
val liveData = userDao.observeAll()
    .asDbResultLiveDataWithLoading() // 等价于 asDbResultWithLoading().asDbResultLiveData()
```

#### 一次性操作包装

```kotlin
val result = dbResultOf { userDao.getById(id) }
result.onSuccess { user -> showUser(user) }
      .onFailure { error -> showError(error) }
```

### 事务辅助

#### withTx

在事务中执行操作，任何异常导致回滚：

```kotlin
val (userId, orderId) = db.withTx {
    val userId = userDao.insert(user)
    val orderId = orderDao.insert(order.copy(userId = userId))
    Pair(userId, orderId)
}
```

> **迁移提示**：旧方法 `runInTransaction` 已标记 `@Deprecated`，请迁移到 `withTx` 以避免与 `RoomDatabase.runInTransaction` 的命名冲突。

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

批量执行操作，返回类型安全的 `BatchResult` 密封类：

```kotlin
// SKIP 策略（默认）：跳过失败项，返回详细结果
val result = db.batchExecute(users) { user ->
    userDao.insert(user)
}
when (result) {
    is BatchResult.Skipped -> {
        println("成功 ${result.successCount} 条，失败 ${result.failedCount} 条")
        result.failures.forEach { (index, error) ->
            println("  第 $index 项失败: ${error.message}")
        }
    }
    is BatchResult.AllOrNothing -> {}
}

// FAIL_FAST 策略：任一失败则全部回滚（整批在单事务中，batchSize 须为 0；需分批请用 SKIP 策略）
val result = db.batchExecute(users, BatchFailureStrategy.FAIL_FAST) { user ->
    userDao.insert(user)
}
when (result) {
    is BatchResult.Skipped -> {}
    is BatchResult.AllOrNothing -> {
        result.result.onSuccess { println("全部成功: $it 条") }
              .onFailure { println("全部回滚: ${it.message}") }
    }
}
```

> `safeTransaction`、`dbResultOf` 以及 Flow 的 `asDbResult` / `asDbResultWithLoading` 会**重新抛出**
> `kotlinx.coroutines.CancellationException`，以便协程取消不被当成普通 `Failure`。

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

// 销毁式迁移时（Room 2.4+）
val onDestructive = onDestructiveMigrationCallback {
    // 数据库被销毁重建，可记录日志或重新填充数据
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
| `Instant` ↔ `Long` | 毫秒时间戳 | java.time，需脱糖支持 |
| `LocalDateTime` ↔ `String` | ISO 格式 | java.time，需脱糖支持 |
| `LocalDate` ↔ `String` | ISO 格式 | java.time，需脱糖支持 |
| `List<String>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `List<Long>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `List<Int>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `Set<String>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `Set<Long>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `Set<Int>` ↔ `String` | JSON 数组 | 使用 kotlinx.serialization |
| `Map<String, String>` ↔ `String` | JSON 对象 | 使用 kotlinx.serialization |
| `Map<String, Long>` ↔ `String` | JSON 对象 | 使用 kotlinx.serialization |
| `Map<String, Int>` ↔ `String` | JSON 对象 | 使用 kotlinx.serialization |
| `Boolean` ↔ `Int` | 0/1 | SQLite 原生不支持 Boolean |
| `ByteArray` ↔ `String` | Base64 | 适用于二进制数据 |
| `Enum` ↔ `String` | 枚举名称 | 继承 `EnumConverter` |

> **注意**：JSON 解析失败时会抛出 `IllegalArgumentException`，Room 会将异常传播到查询调用方。这确保了数据损坏可被感知，而非静默返回空集合。

> **java.time 脱糖**：使用 `Instant`/`LocalDateTime`/`LocalDate` 转换器需要启用 `coreLibraryDesugaring`。

#### Enum 转换器

由于 Room 的 TypeConverter 不支持泛型（运行时类型擦除），需要为每个 Enum 类型创建具体的转换器子类：

```kotlin
class StatusConverter : EnumConverter<Status>(Status::class.java)

@Database(entities = [...], version = 1)
@TypeConverters(AwConverters::class, StatusConverter::class)
abstract class AppDatabase : RoomDatabase()
```

### Paging 3 集成

Room 原生支持 `@Query` 返回 `PagingSource<Int, T>`，配合 aw-db 的 Paging 扩展可直接使用：

```kotlin
@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User ORDER BY name ASC")
    abstract fun pagingSource(): PagingSource<Int, User>
}

// 基本分页 Flow（使用方法引用作为 factory，确保刷新时创建新 PagingSource）
val pagingFlow = userDao::pagingSource.asPagingFlow(pageSize = 20)

// 包装为 DbResult 的分页 Flow
val dbResultPagingFlow = userDao::pagingSource.asDbResultPagingFlow(pageSize = 20)

// 转换分页数据
val mappedFlow = dbResultPagingFlow.mapResult { user ->
    UserUiModel(user)
}
```

> **重要**：`asPagingFlow()` 扩展在 `() -> PagingSource<Int, T>`（工厂函数）上调用，而非直接在 `PagingSource` 上调用。使用方法引用（如 `userDao::pagingSource`）确保 Paging 刷新时创建新的 `PagingSource` 实例。

### PagedResult — 手动分页

`PagedResult<T>` 提供手动分页查询结果的封装：

```kotlin
data class PagedResult<T>(
    val items: List<T>,    // 当前页数据
    val total: Int,        // 总记录数
    val page: Int,         // 页码（从 0 开始）
    val pageSize: Int,     // 每页大小
    val hasMore: Boolean   // 是否还有更多数据
)
```

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

### DatabaseManager — 数据库生命周期管理

`DatabaseManager` 提供引用计数的数据库单例管理，防止多实例打开同一数据库文件。

- 首次 `getOrCreate` 时执行 DSL `block` 并完成构建；**同一** `name` 已存在时**不会**再执行 `block`（以首次成功创建时的配置为准）。
- 同一 `name` 必须始终对应**同一** `RoomDatabase` 子类；若已打开 `AppDatabase` 又以另一类型请求，会抛出 `IllegalStateException`（`getOrNull` 在名称已存在但类型不符时亦会抛出，而不再默默返回 `null`）。
- `forceClose(name)`：无视引用计数，立即 `close` 并移除该名称实例。用于备份恢复等**必须在替换数据库文件前**保证连接全部关闭的场景；一般业务请用 `release` 成对管理。
- 使用 `DbBackupHelper.restore` 时，内部会调用 `forceClose`，旧句柄在恢复后**不得再使用**；应重新 `getOrCreate` 取得新库。

```kotlin
// 获取数据库实例（自动引用计数）
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2)
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
}

// 释放引用（计数归零时自动关闭数据库）
DatabaseManager.release("app.db")

// 必须立即关闭并换文件/进程级重建时（慎用，避免持有旧引用）
DatabaseManager.forceClose("app.db")

// 关闭所有数据库实例
DatabaseManager.closeAll()

// 获取已存在的实例（不创建新实例；Java 可 DatabaseManager.getOrNull("app.db", AppDatabase::class.java)）
val existingDb: AppDatabase? = DatabaseManager.getOrNull("app.db")

// 检查数据库是否已初始化
if (DatabaseManager.isManaged("app.db")) { ... }
```

### DbDebugHelper — 调试工具

提供数据库调试辅助功能：

```kotlin
// 获取所有表名
val tables = db.tableList()

// 获取表行数（表名仅允许字母数字和下划线）
val count = db.rowCount("users")

// 获取表结构（返回 TableColumnInfo 列表）
val columns = db.tableSchema("users")
columns.forEach { col ->
    println("${col.name} ${col.type} ${if (col.notNull) "NOT NULL" else ""} ${col.defaultValue ?: ""}")
}
```

## 最佳实践

### 协程调度

所有 `BaseDao` 的方法均为 `suspend`，Room 默认使用 IO 调度器。**无需**外部包裹 `withContext(Dispatchers.IO)`：

```kotlin
// ✅ Room 自动在 IO 线程执行
viewModelScope.launch {
    val users = userDao.getAll()
}

// ❌ 多余的 withContext(Dispatchers.IO)
viewModelScope.launch {
    val users = withContext(Dispatchers.IO) { userDao.getAll() } // 不需要！
}
```

### 数据库生命周期

使用 `DatabaseManager` 管理数据库单例，避免多实例问题：

```kotlin
// ✅ 推荐：使用 DatabaseManager
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db")

// 释放
DatabaseManager.release("app.db")
```

或手动实现单例：

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

### 分页

```kotlin
// ✅ 推荐：使用 Paging 3（数据库级分页，高效）
userDao::pagingSource.asPagingFlow()

// ❌ 不推荐：内存分页（已标记 @Deprecated）
userDao.observeAll().paginate(page = 0, pageSize = 20)
```

### TypeConverter 与性能

`AwConverters` 使用 JSON 存储 List/Map/Set 类型，每次读写都会进行序列化/反序列化：

```kotlin
// ✅ 适合：小规模、低频访问的集合字段
@Entity
data class User(
    @PrimaryKey val id: Long,
    val tags: List<String>  // 几个标签，JSON 存储完全 OK
)

// ❌ 不适合：大规模、高频查询的集合字段
// 对于频繁查询的大集合，建议使用关联表代替 JSON 存储：
@Entity
data class Order(
    @PrimaryKey val id: Long,
    val userId: Long
)

@Entity
data class OrderItem(
    @PrimaryKey val id: Long,
    val orderId: Long,
    val productName: String
    // 而非在 Order 中存储 items: List<OrderItem>
)
```

## 常见问题

### Q: 为什么不用逗号分隔存储 List？

逗号分隔在字符串本身包含逗号时会出错。aw-db 使用 JSON 格式（基于 `kotlinx.serialization`），确保数据完整性。

### Q: upsert 和 insert 有什么区别？

- `insert`：冲突时替换整行（`OnConflictStrategy.REPLACE`），实际是先 DELETE 再 INSERT，会触发外键的 CASCADE 删除
- `upsert`：冲突时仅更新指定列（Room 原生 `@Upsert`），更高效且不会触发级联删除

> 如果你的表有外键约束，推荐使用 `upsert` 或 `insertOrIgnore` 代替 `insert`。

### Q: batchExecute 和 insertAll 有什么区别？

- `insertAll`：Room 原生批量插入，性能最优，但冲突策略固定
- `batchExecute`：逐条执行自定义操作，支持 SKIP/FAIL_FAST 策略，返回类型安全的 `BatchResult`

### Q: 什么时候用 asDbResult() vs asDbResultWithLoading()？

- `asDbResult()`：不需要 Loading 状态的场景（如后台同步）
- `asDbResultWithLoading()`：需要在 UI 展示加载状态的场景（如列表页面）

### Q: TypeConverter 解析失败会怎样？

JSON 解析失败时会抛出 `IllegalArgumentException`，Room 会将异常传播到查询调用方。这确保了数据损坏可被感知，而非静默返回空集合。如果你需要容错行为，请在调用方自行 try-catch。

### Q: 为什么 runInTransaction 被标记为 @Deprecated？

`RoomDatabase` 已有同名方法 `runInTransaction`（非 suspend 版本），扩展函数会造成命名冲突。请迁移到 `withTx`。

### Q: asDbResultLiveData 的超时参数有什么影响？

`timeoutInMs` 控制 LiveData 在没有活跃观察者时等待多久才断开上游 Flow。默认 5 秒。屏幕旋转时 LiveData 可能短暂无观察者，如果超时设置过短，可能导致 Flow 被断开、数据丢失。如果遇到旋转屏幕后数据消失的问题，可以增大超时值：

```kotlin
userDao.observeAll().asDbResultLiveDataWithLoading(timeoutInMs = 10_000L)
```

## 混淆规则

aw-db 已通过 `consumer-rules.pro` 自动配置了必要的混淆规则，宿主项目无需额外配置。如果你的应用开启了 `minifyEnabled`，库中的 `DbResult`、`BatchResult`、TypeConverter 等类会被自动保留。

## 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)。

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

# Last updated: 2026年 4月 21日
