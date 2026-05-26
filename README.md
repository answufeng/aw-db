# aw-db

[![](https://jitpack.io/v/answufeng/aw-db.svg)](https://jitpack.io/#answufeng/aw-db)

轻量 **Room** 工具库（`com.answufeng.db`）：DSL 建库、通用 `BaseDao`、事务与批量辅助、`DbResult` 包装、类型转换、Paging 扩展与 `DatabaseManager` 生命周期。面向 **传统 XML / View 体系**（不依赖 Compose）。

### 建议阅读顺序

| 步骤 | 章节 | 适合 |
|------|------|------|
| 1 | [5 分钟上手](#5-分钟上手) | 依赖、建库、第一次读写 |
| 2 | [使用示例 → 单表增删改查](#单表增删改查) | `BaseDao` 增删改查 |
| 3 | [使用示例 → 多表关联](#多表关联-user-与-loginhistory) | 外键、`userId`、`@Relation` |
| 4 | [API 选型](#api-选型核心) · [参考手册](#参考手册) | 查 API、进阶配置 |
| 5 | [演示应用](#演示应用) | 安装 Demo 手测 |

---

## 目录

| 想做什么 | 跳转到 |
|----------|--------|
| 接入依赖与第一次读写 | [5 分钟上手](#5-分钟上手) |
| 单表 CRUD | [单表增删改查](#单表增删改查) |
| 多表外键与 `@Relation` | [多表关联](#多表关联-user-与-loginhistory) |
| 能力列表 / API 怎么选 | [功能概览](#功能概览) · [API 选型](#api-选型核心) |
| 进阶 API 说明 | [参考手册](#参考手册) |
| 本地构建与发版检查 | [本仓库与工程检查](#本仓库与工程检查) |
| Demo 手测 | [演示应用](#演示应用) |
| SQLCipher（可选） | [SQLCipher（可选）](#sqlcipher可选) |
| FAQ / 混淆 | [常见问题](#常见问题) · [混淆配置](#混淆配置) |

---

## 环境要求

| 项 | 最低版本 |
|----|----------|
| Android minSdk | 24+ |
| Kotlin | 2.0+ |
| Room | 2.6.1+（与库内一致） |
| 构建本仓库 | **JDK 17+**；`demo` 用 compileSdk 35 验证（库不限定宿主 targetSdk） |

---

## 5 分钟上手

### 1) 添加依赖（JitPack）

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-db:1.1.0")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

> `implementation` 的版本号与 Git / JitPack 的 **tag 一致**（当前为 `1.1.0`）。  
> aw-db 以 `api` 传递 Room、Lifecycle、`room-paging` 等，一般不必再显式依赖 `room-runtime` / `room-ktx`；但**必须**加 `ksp("androidx.room:room-compiler")` 以处理 `@Database` / `@Dao`。

<details>
<summary><b>传递依赖（api）</b></summary>

| 依赖 | 用途 |
|------|------|
| `room-runtime` / `room-ktx` | Room 核心 |
| `room-paging` | Paging3 与 Room 集成（含 Paging 运行时传递） |
| `lifecycle-livedata-ktx` | `asDbResultLiveData*` |
| `kotlinx-serialization-json` | `AwConverters` 的 JSON 列 |

若与宿主其它库发生 Paging 版本冲突，可在 app 里用 `resolutionStrategy` 或显式声明 `androidx.paging:paging-runtime` 对齐版本。

</details>

### 2) 定义 Entity / Dao / Database

以下为**单表**最小示例；带外键的子表（如登录历史）见 [多表关联](#多表关联-user-与-loginhistory)。

```kotlin
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int
)

@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User ORDER BY id ASC")
    abstract suspend fun getAll(): List<User>

    @Query("SELECT * FROM User WHERE id = :id")
    abstract suspend fun getById(id: Long): User?

    @Query("SELECT * FROM User")
    abstract fun observeAll(): kotlinx.coroutines.flow.Flow<List<User>>
}

@Database(entities = [User::class], version = 1)
@TypeConverters(AwConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

### 3) 打开数据库并读写

```kotlin
// 打开（推荐：DatabaseManager 管生命周期）
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") {
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
}

// 写
val id = db.userDao().insert(User(name = "张三", age = 25))

// 读
val users = db.userDao().getAll()

// 观察（Flow -> DbResult：带 Loading/Success/Failure）
db.userDao().observeAll()
    .asDbResultWithLoading()
    .collect { r ->
        r.onLoading { showLoading() }
            .onSuccess { showData(it) }
            .onFailure { showError(it) }
    }

// 不再使用时（如进程结束、或你明确需要提前 close）
DatabaseManager.release("app.db")
```

---

## 使用示例

以下均在 `lifecycleScope` / `viewModelScope` 中调用。Room 的 `suspend` 已调度到 IO 线程，**无需**再包 `withContext(Dispatchers.IO)`。

### 单表增删改查

继承 `BaseDao<T>` 即可获得 `insert` / `update` / `delete` / `upsert` 等；**列表、条件查询**需在 Dao 里自行写 `@Query`。

```kotlin
class UserRepository(context: Context) {
    private val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db")
    private val dao = db.userDao()

    /** 增：插入一条，返回 rowId */
    suspend fun addUser(name: String, age: Int): Long {
        return dao.insert(User(name = name, age = age))
    }

    /** 查：全部 / 按主键 */
    suspend fun listUsers(): List<User> = dao.getAll()

    suspend fun findUser(id: Long): User? = dao.getById(id)

    /** 改：先查再 update（也可对 id 已知的实体直接 upsert） */
    suspend fun renameUser(id: Long, newName: String): Boolean {
        val user = dao.getById(id) ?: return false
        return dao.update(user.copy(name = newName)) > 0
    }

    /** 删：按实体删除 / 清空表（清空需在 Dao 里自定义 @Query） */
    suspend fun removeUser(id: Long): Boolean {
        val user = dao.getById(id) ?: return false
        return dao.delete(user) > 0
    }

    fun release() = DatabaseManager.release("app.db")
}
```

在 Activity 中调用示例：

```kotlin
lifecycleScope.launch {
  val repo = UserRepository(applicationContext)
  val newId = repo.addUser("李四", 28)           // 增
  val all = repo.listUsers()                      // 查（列表）
  val one = repo.findUser(newId)                  // 查（单条）
  repo.renameUser(newId, "李四-已改名")          // 改
  repo.removeUser(newId)                          // 删
}
```

| 操作 | BaseDao 方法 | 说明 |
|------|--------------|------|
| **增** | `insert` / `insertAll` | 默认 `REPLACE` 冲突策略，有外键时注意 CASCADE |
| **查** | 自定义 `@Query` | `BaseDao` 不提供通用查询，列表/条件查询在 Dao 里声明 |
| **改** | `update` / `upsert` | `upsert` 适合「有则更新、无则插入」 |
| **删** | `delete` | 传实体实例；批量删可用 `deleteAll(entities)` |

<details>
<summary><b>批量与冲突策略（可选）</b></summary>

```kotlin
// 批量增
dao.insertAll(listOf(User(name = "A", age = 1), User(name = "B", age = 2)))

// 冲突时忽略（不覆盖已有行）
dao.insertOrIgnore(User(id = 1, name = "重复", age = 0))  // 冲突返回 -1

// 有外键时更安全的「改」
dao.upsert(user)
```

</details>

### 多表关联（User 与 LoginHistory）

典型场景：**一个用户对应多条登录/操作历史**（1:N）。aw-db 不替代 Room 的关联能力，但与 `BaseDao`、`withTx`、`DatabaseManager` 可配合使用。Demo 模块有完整可运行代码，App 内 **「关联」** Tab 可手测。

#### 关系示意

```mermaid
erDiagram
    User ||--o{ LoginHistory : "userId → id"
    User {
        long id PK
        string name
        int age
    }
    LoginHistory {
        long id PK
        long userId FK
        string action
        string note
    }
```

| 层次 | 怎么做 |
|------|--------|
| **建表** | 子表字段 `userId` + `@Entity(foreignKeys = …)` 指向 `User.id` |
| **插入** | 先 `insert User` 得到 `id`，再 `LoginHistory(userId = id, …)`；推荐 `withTx` |
| **查「用户+全部历史」** | POJO + `@Relation`（见下） |
| **只查历史** | `loginHistoryDao.getByUserId(userId)`，不必用 `@Relation` |
| **删用户** | `onDelete = CASCADE` 时，SQLite 自动删该用户全部历史 |

#### 1. 定义实体与外键

```kotlin
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int
)

@Entity(
    tableName = "login_history",
    foreignKeys = [
        ForeignKey(
            entity = User::class,           // 父表实体
            parentColumns = ["id"],         // 父表主键列
            childColumns = ["userId"],      // 子表外键列（见下方字段）
            onDelete = ForeignKey.CASCADE,  // 删用户 → 自动删其历史
            onUpdate = ForeignKey.CASCADE   // 改用户 id 时同步（少见）
        )
    ],
    indices = [Index("userId")]             // 按 userId 查询时建议加索引
)
data class LoginHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 必须对应某条 User.id
    val action: String,
    val note: String = "",
    val createdAt: Date = Date()
)
```

| 注解/字段 | 含义 |
|-----------|------|
| `ForeignKey.entity` | 父表是哪个 `@Entity` |
| `parentColumns` / `childColumns` | 父主键列名 ↔ 子表外键列名，**名字不必相同**，Demo 为 `id` ↔ `userId` |
| `userId` | 普通 `Long` 字段；Room **不会**自动填，插入 `LoginHistory` 前你要先有用户的 `id` |
| `indices` | 常查 `WHERE userId = ?` 时建议索引 |

`@Database` 注册**两个**实体，并各提供一个 Dao：

```kotlin
@Database(entities = [User::class, LoginHistory::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun loginHistoryDao(): LoginHistoryDao
}
```

```kotlin
@Dao
abstract class LoginHistoryDao : BaseDao<LoginHistory>() {
    @Query("SELECT * FROM login_history WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getByUserId(userId: Long): List<LoginHistory>
}
```

#### 2. 插入：如何把 LoginHistory 关联到 User

**核心规则：`userId` 必须等于已存在用户的 `User.id`；Room 不会自动填写。**

```kotlin
// 方式 A：分两步（适合简单场景）
val userId = userDao.insert(User(name = "张三", age = 25))
loginHistoryDao.insert(
    LoginHistory(userId = userId, action = "LOGIN", note = "首次登录")
)

// 方式 B：同一事务（推荐，避免只插入一半）
db.withTx {
    val userId = userDao().insert(User(name = "李四", age = 30))
    loginHistoryDao().insert(
        LoginHistory(userId = userId, action = "LOGIN")
    )
    userId
}
```

> `userId` 不能为 `0` 或不存在的主键，否则外键失败。  
> `UserDao.insert` 为 `REPLACE` 时可能 **CASCADE 清空**该用户下历史，有子表时优先 `upsert` / `insertOrIgnore`（见 [FAQ](#常见问题)）。

#### 3. 查询：一次取出「用户 + 历史列表」

用 **`@Embedded` + `@Relation`**（Room 自动生成子表查询，无需手写 JOIN）：

```kotlin
data class UserWithHistories(
    @Embedded val user: User,
    @Relation(
        parentColumn = "id",       // User 表的主键列
        entityColumn = "userId"    // LoginHistory 表里指向 User 的列
    )
    val histories: List<LoginHistory>
)
```

```kotlin
@Dao
abstract class UserDao : BaseDao<User>() {
    @Transaction
    @Query("SELECT * FROM User WHERE id = :userId")
    suspend fun getUserWithHistories(userId: Long): UserWithHistories?

    @Transaction
    @Query("SELECT * FROM User ORDER BY id ASC")
    suspend fun getAllUsersWithHistories(): List<UserWithHistories>
}
```

| 要点 | 说明 |
|------|------|
| `@Transaction` | 读用户 + 读多条历史要在同一事务里，**必须**加在方法上 |
| `@Query` 只查父表 | 写 `SELECT * FROM User WHERE id = ?` 即可；子表由 `@Relation` 按 `userId` 自动加载 |
| `parentColumn` / `entityColumn` | 必须与实体字段名一致（Demo：`id` / `userId`） |

使用：

```kotlin
val uw = userDao.getUserWithHistories(1)
if (uw != null) {
    val name = uw.user.name
    uw.histories.forEach { history ->
        // history.userId 应等于 uw.user.id
    }
}
```

#### 4. 删除

```kotlin
userDao.delete(user)           // CASCADE：该用户全部历史一并删除
loginHistoryDao.delete(row)    // 只删一条历史，保留用户
```

#### 场景速查

| 你想… | 代码要点 |
|--------|----------|
| 注册并记首条登录 | `withTx { insert User → insert LoginHistory(userId) }` |
| 给老用户追加历史 | `loginHistoryDao.insert(LoginHistory(userId = 已有Id, …))` |
| 看用户+全部历史 | `userDao.getUserWithHistories(userId)` |
| 只看某用户历史 | `loginHistoryDao.getByUserId(userId)` |

Demo 源码：`demo/.../User.kt`、`LoginHistory.kt`、`UserWithHistories.kt`、`UserDao.kt`、`LoginHistoryDao.kt`。

---

## 功能概览

- **DSL 建库**：迁移、回调、WAL、预打包 DB、自定义 Executor、多进程 invalidation 等
- **BaseDao**：CRUD + Upsert，批量方法带 `@Transaction`
- **事务**：`withTx`、`safeTransaction`、`batchExecute`（`BatchResult` + SKIP / FAIL_FAST）
- **DbResult**：Loading / Success / Failure，含 `map`、`combineDbResults`、Flow / LiveData 扩展
- **Migration DSL**：`migration(…) { }`、onCreate / onOpen / 销毁式迁移回调
- **AwConverters**：Date、java.time、List/Set/Map（JSON）、Boolean、ByteArray、`EnumConverter`
- **Paging 3**：`asPagingFlow`；手写分页用 `PagedResult` / `toPagedResult`
- **DatabaseManager**：按文件名引用计数；`acquireScoped` + `use` 成对 `release`
- **DbBackupHelper**：备份/恢复、备份验证（SQLite 文件头校验）、元数据（时间戳/版本号）
- **SqlQueryLogger**：SQL 执行日志（`RoomDatabase.QueryCallback`，无可靠单次耗时）

---

## 演示应用

安装 `demo` 模块对应 APK，按 Tab 手测（与上文 [使用示例](#使用示例) 对应）：

| Tab | 内容 |
|-----|------|
| 单表 | `BaseDao` 增删改查 |
| **关联** | `userId` 关联、`@Relation`、CASCADE 删除 |
| DbResult / 事务 / 分页 / 运维 | 库内其它能力 |

推荐：单表插入用户 → **关联**「注册并记登录」→「列出全部关联」→「删用户验 CASCADE」。

---

## API 选型（核心）

| 场景 | 推荐 API |
|------|----------|
| 打开数据库 | `DatabaseManager.getOrCreate` + `release` |
| 单表 CRUD | 继承 `BaseDao` + 自定义 `@Query` |
| 多表 1:N | `ForeignKey` + 子表 `userId` + `@Relation`（见 [多表关联](#多表关联-user-与-loginhistory)） |
| 单次查询/写入 | Dao `suspend` 或 `dbResultOf { }` |
| UI 观察列表 | `Flow.asDbResultWithLoading()` |
| 分页列表 | `dao::pagingSource.asPagingFlow` + Paging `LoadState` |
| 多步原子操作 | `withTx` 或 `safeTransaction` |
| 逐条错误收集 | `batchExecute(SKIP)`；吞吐优先用 `insertAll` |
| 合并多个 DbResult | `combineDbResults(vararg …)` |
| 备份/还原 | `backupToWithMetadata` / `DbBackupHelper.restore` |
| 短生命周期持有 DB | `DatabaseManager.acquireScoped` + `use { }` |

### 结果包装怎么选？

| 场景 | 类型 | 典型 API |
|------|------|----------|
| UI 三态（加载/成功/失败） | `DbResult` | `asDbResultWithLoading`、`dbResultOf` |
| 单次事务成功/失败 | `Result` | `safeTransaction` |
| 批量逐条成败统计 | `BatchResult` | `batchExecute` + `Skipped` |
| 分页列表加载/错误 | Paging `LoadState` | `asPagingFlow` + `loadStateFlow` |

<details>
<summary><b>进阶 API</b>（按需）</summary>

- `AwJavaTimeConverters`（需 coreLibraryDesugaring）
- `AwDatabase.buildInMemory`（仅测试）
- `PagedResult` / `toPagedResult`（手动 LIMIT/OFFSET，无 Paging3 时）
- `SqlQueryLogger`（SQL 执行日志）

</details>

---

## 参考手册

**速览**：先给最常用的一行/一段；长表与多段代码放在下文的 **可折叠块** 里，默认收起（GitHub 支持 `<details>`）。

### AwDatabase

```kotlin
val db = AwDatabase.build<AppDatabase>(context, "app.db")
val inMem = AwDatabase.buildInMemory<AppDatabase>(context) // 仅测试
```

<details>
<summary><b>完整 DatabaseConfig 示例</b>（迁移、AutoMigration、回调、WAL、预打包、Executor、多进程等）</summary>

```kotlin
import java.util.concurrent.Executors
import androidx.room.RoomDatabase

val db = AwDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(
        migration(1, 2) { execSQL("ALTER TABLE User ADD COLUMN email TEXT") }
    )
    addAutoMigrationSpec(MyAutoMigrationSpec())
    addCallback(onCreateCallback {
        execSQL("INSERT INTO config (key, value) VALUES ('version', '1.0')")
    })
    addCallback(onOpenCallback { /* 每次打开 */ })
    addCallback(onDestructiveMigrationCallback { /* 仅销毁式重建时 */ })
    // fallbackToDestructiveMigration()          // 生产慎用
    // fallbackToDestructiveMigrationFrom(1, 2)
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    // createFromAsset("databases/prepopulated.db")  // 与 createFromFile 二选一
    // createFromFile(File("/path/to/prepopulated.db"))
    setQueryExecutor(Executors.newFixedThreadPool(4))
    setTransactionExecutor(Executors.newSingleThreadExecutor())
    // enableMultiInstanceInvalidation()  // 多进程同库时
    // allowMainThreadQueries()           // 仅调试用
    setQueryCallback(SqlQueryLogger, Executors.newSingleThreadExecutor()) // 第二参可传共享 Executor，避免多次 build 建池
}
```

</details>

### BaseDao

继承 `BaseDao<T>` 即得 `insert` / `insertAll`、`insertOrIgnore` / `All`、`update` / `All`、`delete` / `All`、`upsert` / `All`。`insert` 为 `OnConflictStrategy.REPLACE`（可能影响外键 **CASCADE**）。

<details>
<summary><b>方法一览表</b></summary>

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `insert` / `insertAll` | 冲突 **REPLACE**（先删后插，可触发 CASCADE） | 行 ID / 列表 |
| `insertOrIgnore` / `All` | 冲突忽略 | 行 ID，冲突为 `-1` |
| `update` / `All` | 更新 | 影响行数 |
| `delete` / `All` | 删除 | 影响行数 |
| `upsert` / `All` | Room `@Upsert` | 行 ID（部分场景更新为 `-1`） |

所有方法均为 `suspend`；Room 在 IO 上执行。批量带 `@Transaction`。

</details>

### DbResult

三种状态 + `getOrNull` / `map` / `flatMap` / `fold`；Flow 用 `asDbResult` / `asDbResultWithLoading`；`asDbResultLiveData*` 可设超时；单次操作用 `dbResultOf { }`。

合并多个 `DbResult`：两路用双参重载；三路及以上用 **vararg**：

```kotlin
combineDbResults(userResult, orderResult) { users, orders ->
    Dashboard(users, orders)
}

combineDbResults(r1, r2, r3) { values ->
    Triple(values[0], values[1], values[2])
}
```

### 事务

- **`withTx { }`**：挂起事务 = `withTransaction`；与 Java `runInTransaction(Runnable)` 可并存，命名不冲突即好。
- **`safeTransaction`**：返回 `Result`；`CancellationException` 仍向上抛。
- **`batchExecute`**：FAIL_FAST = 整批**单**事务；SKIP 默认 = **每条独立事务**；SKIP 且 `batchSize > 0` = **每 N 条一块、块内单事务**（块内首条约束失败后同事务后续语句可能不可用，见 KDoc）。

<details>
<summary><b>batchExecute 示例</b></summary>

```kotlin
// SKIP：逐条成功/失败统计
val r1 = db.batchExecute(users) { user -> userDao.insert(user) }
if (r1 is BatchResult.Skipped) { /* successCount, failedCount, failures */ }

// FAIL_FAST：整批单事务
val r2 = db.batchExecute(users, BatchFailureStrategy.FAIL_FAST) { u -> userDao.insert(u) }
if (r2 is BatchResult.AllOrNothing) { r2.result.getOrNull() }
```

</details>

### Migration DSL

`migration(1, 2) { execSQL("…") }`；`onCreateCallback`、`onOpenCallback`、`onDestructiveMigrationCallback`。

### AwConverters

`@TypeConverters(AwConverters::class)`；`java.time` 需 **coreLibraryDesugaring** 并加 `AwJavaTimeConverters`；`Enum` 为 `EnumConverter<T>` 子类（可选 `strict = true`，未知枚举名抛异常而非返回 null）。JSON 解析失败会抛异常（见 FAQ）。

<details>
<summary><b>支持类型与存储格式</b></summary>

| 类型 | 存储 |
|------|------|
| `Date` / `Instant` | `Long`（毫秒） |
| `LocalDateTime` / `LocalDate` | ISO 字符串 |
| `List` / `Set` / `Map`（表列类型） | JSON 字符串 |
| `Boolean` | `0` / `1` |
| `ByteArray` | Base64 字符串 |
| `Enum` | 名字符串，配合 `EnumConverter` |

</details>

### Paging 3 与 PagedResult

`@Query` 返回 `PagingSource<Int, T>` 后，用**方法引用**作工厂：`userDao::pagingSource.asPagingFlow(20)`。错误与重试看 Paging 的 `LoadState`。

手写 `LIMIT`/`OFFSET` 时，用 `items.toPagedResult(page, pageSize, total)` 封装。

<details>
<summary><b>Paging 列表示例</b></summary>

```kotlin
// Dao
@Query("SELECT * FROM User ORDER BY name ASC")
abstract fun pagingSource(): PagingSource<Int, User>

// ViewModel / Activity
val flow = userDao::pagingSource.asPagingFlow(pageSize = 20)
// 收集 PagingData，错误与重试观察 adapter.loadStateFlow 或 combinedLoadStates
```

</details>

### DatabaseManager 与调试

- **DatabaseManager**：`getOrCreate` / `release` / `forceClose` / `closeAll` / `getOrNull` / `acquireScoped`；`restore` 会 `forceClose`，旧 `RoomDatabase` 不可再用。
- **DbDebugHelper**：`db.tableList()`、`db.rowCount("t")`、`db.tableSchema("t")`（表名仅字母数字下划线）。

<details>
<summary><b>DatabaseManager 代码片段</b></summary>

```kotlin
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") { /* 仅首次 */ }
DatabaseManager.release("app.db")
// DatabaseManager.forceClose("app.db")
// val x = DatabaseManager.getOrNull<AppDatabase>("app.db")

// 短生命周期：use 结束自动 release
DatabaseManager.acquireScoped<AppDatabase>(context, "app.db").use { handle ->
    handle.database.userDao().getAll()
}
```

</details>

### 备份与恢复

```kotlin
// 备份（带元数据）
val backupFile = File(exportDir, "app.db.bak")
DbBackupHelper.backupWithMetadata(db, backupFile)

// 验证备份文件
if (DbBackupHelper.verifyBackup(backupFile)) {
    // 读取元数据
    val meta = DbBackupHelper.readBackupMetadata(backupFile)
    println("备份时间: ${meta?.timestamp}, 版本: ${meta?.version}")
}

// 恢复（还原前会删除主库及 -wal/-shm；还原后 refCount=1，须重新管理 release）
val restoredDb = DbBackupHelper.restore<AppDatabase>(context, "app.db", backupFile)

// 可选：要求备份 .meta 中的 version 与还原后库一致
DbBackupHelper.restore<AppDatabase>(context, "app.db", backupFile, requireVersionMatch = true)
```

**注意**：`restore` 会 `forceClose` 并替换文件；此前持有的 `RoomDatabase` 引用不可再使用，须丢弃后重新 `getOrCreate`。

### SQL 查询日志

Room 的 `QueryCallback` 在查询**即将执行**时回调，**无法**在此获得可靠耗时。

```kotlin
SqlQueryLogger.enable()
SqlQueryLogger.addListener(SqlQueryListener { sql, bindArgs, durationMs ->
    Log.d("DB", "SQL: $sql  args=$bindArgs")
})

AwDatabase.build<AppDatabase>(context, "app.db") {
    setQueryCallback(SqlQueryLogger) // 可选第二参：共享 Executor
}
```

---

## 本仓库与工程检查

| 项 | 内容 |
|----|------|
| CI | [`.github/workflows/ci.yml`](.github/workflows/ci.yml)：`assembleRelease`、单测、ktlint、lint、`:demo:assembleRelease` |
| 本地 | `./gradlew :aw-db:assembleRelease :aw-db:testDebugUnitTest :aw-db:ktlintCheck :aw-db:lintRelease :demo:assembleRelease` |
| 上线前 | 每个 DB 版本有正式 `Migration`；**勿**依赖生产用 `fallbackToDestructiveMigration()`；大表在低存储/后台限制下各验一次 |

---

## SQLCipher（可选）

不内置。宿主自行引入 SQLCipher 与 `SupportFactory` / 加密 `SupportSQLiteOpenHelper.Factory`；**密钥**勿硬编码；`DatabaseManager` 多路径与多进程一并测。DSL / `DbResult` / 事务仍照常用；加密接在 `openHelperFactory` 上。

---

## 最佳实践

- **协程**：`BaseDao` 的 `suspend` 由 Room 调度到 IO，一般**不要**再包一层 `withContext(Dispatchers.IO)`。
- **单例**：始终 `DatabaseManager.getOrCreate(context, "明确库名")`，多库用不同文件名。
- **迁移**：发版前完整迁移链；仅开发可销毁式。
- **Flow UI**：需要加载态用 `asDbResultWithLoading()`（示例见上文「5 分钟上手」）。
- **分页**：列表用 Paging3；仅简单页码用 `PagedResult`；避免全表进内存再 slice。
- **JSON 列**：小集合、低频可 `AwConverters`；高频或需关联查询用**子表 + 外键**（见 [多表关联](#多表关联-user-与-loginhistory)）。
- **多表**：插入子表前必须有父表 `id`；联查用 `@Transaction` + `@Relation`。
- **备份还原**：还原后丢弃旧 `RoomDatabase` 引用，按新实例重新 `getOrCreate` / `release`。

---

## 常见问题

- **List 存库为何用 JSON 不用逗号分隔？** 逗号会与内容冲突；库内用 `kotlinx.serialization`。
- **insert 与 upsert？** `REPLACE` 会删后插，可触发 CASCADE；有外键时倾向 `upsert` / `insertOrIgnore`。
- **batchExecute 与 insertAll？** `insertAll` 吞吐最好；`batchExecute` 要逐条逻辑或错误收集时用；SKIP=每行一事务，整表原子用 `insertAll` 或 FAIL_FAST。
- **asDbResult 与 asDbResultWithLoading？** 无 UI 加载态用前者；有则用后者。
- **TypeConverter 解析失败？** 抛 `IllegalArgumentException`；要容错在调用方 catch。
- **挂起事务用啥？** `withTx`；Java 用 `runInTransaction(Runnable)`。
- **asDbResultLiveData 超时？** 无观察者时多久断开 Flow；转屏可加大 `timeoutInMs`（如 `10_000L`）。
- **dbResultOf 会捕获 Error 吗？** 不会，仅捕获 `Exception`；`Error` 仍会向上抛。
- **还原后还要 release 吗？** `restore` 内部 `getOrCreate` 后引用计数为 1，与首次打开相同，退出时仍需 `release`。
- **LoginHistory 如何关联 User？** 子表 `userId` 存 `User.id`；插入：`insert User` 得 `id` 再 `LoginHistory(userId=id)`；查询：`UserWithHistories` + `@Relation`。详见 [多表关联](#多表关联-user-与-loginhistory)。
- **safeTransaction 与 dbResultOf？** 前者返回 Kotlin `Result`（事务边界）；后者返回 `DbResult`（适合直接驱动 UI 三态）。
- **Enum 列出现未知值？** 默认 `EnumConverter` 打日志并返回 null；生产可 `strict = true` 尽早失败。
- **Java 项目能用吗？** 可以；`DatabaseManager.getOrNull(name, AppDatabase.class)` 等非 inline API 可从 Java 调用，DSL 建库建议 Kotlin。

---

## 混淆配置

`consumer-rules.pro` 会随 aar 注入宿主；`minifyEnabled true` 时一般无需再抄 `DbResult` / `BatchResult` / Converter 等规则。若 R8 仍报缺失，再按堆栈补 `-keep`（避免整包 `keep`）。

---

## 许可证

Apache License 2.0 — 见 [LICENSE](LICENSE)。
