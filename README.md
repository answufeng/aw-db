# aw-db

[![](https://jitpack.io/v/answufeng/aw-db.svg)](https://jitpack.io/#answufeng/aw-db)

轻量 **Room** 工具库：DSL 建库、通用 `BaseDao`、事务与批量辅助、`DbResult` 包装、类型转换、Paging 扩展与 `DatabaseManager` 生命周期。面向 **传统 XML** 开发即可使用（不依赖 Compose）。

---

## 目录

| 块 | 说明 |
|----|------|
| [特性](#特性) | 能力一览 |
| [环境](#环境) | minSdk、Kotlin、Room、行为说明 |
| [安装](#安装) | Gradle / JitPack、`ksp` 说明 |
| [快速开始](#快速开始) | 三步可跑通 |
| [演示应用](#演示应用) | `demo` 与 [DEMO_MATRIX](demo/DEMO_MATRIX.md) |
| [参考手册](#参考手册) | 速览 + 可折叠的完整代码与表格 |
| [工程与发版](#工程与发版) | CI、本地命令、上线注意 |
| [SQLCipher（可选）](#sqlcipher可选) | 文件加密接入要点 |
| [最佳实践](#最佳实践) | 协程、单例、迁移、Flow、分页、类型转换性能 |
| [常见问题](#常见问题) | FAQ |
| [混淆](#混淆) | 宿主 R8/ProGuard |
| [许可证](#许可证) | Apache-2.0 |

---

## 特性

- **DSL 建库** — 迁移、回调、日志模式、预打包 DB、自定义 Executor 等
- **BaseDao** — CRUD + Upsert，批量方法带 `@Transaction`
- **事务** — `withTx`、`safeTransaction`、`batchExecute`（`BatchResult` + SKIP / FAIL_FAST）
- **DbResult** — Loading / Success / Failure，含 `map`、`combineDbResults`、Flow / LiveData 扩展
- **Migration DSL** — `migration { }`、onCreate / onOpen / 销毁式迁移回调
- **AwConverters** — Date、java.time、List/Set/Map（JSON）、Boolean、ByteArray、`EnumConverter`
- **Paging 3** — `asPagingFlow`、`asDbResultPagingFlow`、`mapResult`（需宿主 `paging-runtime` 兼容）
- **DatabaseManager** — 按文件名引用计数；`acquireScoped` + `use` 成对 `release`

---

## 环境

| 项 | 要求 |
|----|------|
| minSdk | 24+ |
| 语言 / Room | Kotlin 2.0+，Room 2.6.1+（与库内一致） |
| 构建本仓库 | **JDK 17+**；`demo` 用 compileSdk 35 / targetSdk 35 做验证（库不限定宿主 targetSdk） |

| 能力 | minSdk 24 时 |
|------|----------------|
| Room / SQLite | 标准 API，无额外反射壳 |
| `kotlinx.serialization`（`AwConverters` 内 JSON） | 与库内 BOM 对齐升级 |
| Paging 3 | `room-paging` 经 `api` 传递，宿主自管 `paging-runtime` 版本 |
| 预打包 DB / 迁移 | 在真机**低存储**下至少测一次大迁移 |

---

## 安装

`settings.gradle.kts` 增加 JitPack；`app` 模块：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven { url = uri("https://jitpack.io") } }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-db:1.0.0")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

> aw-db 以 `api` 带上 Room、Lifecycle 等，一般不必再写 `room-runtime` / `room-ktx`；**必须**加 `ksp("androidx.room:room-compiler")` 以处理 `@Database` / `@Dao`。

---

## 快速开始

**1. 实体、DAO、Database**

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

**2. 打开数据库**

```kotlin
val db = DatabaseManager.getOrCreate<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2)
    setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
}
DatabaseManager.release("app.db") // 不再使用时
```

**3. 读写与观察**

```kotlin
val id = db.userDao().insert(User(name = "张三", age = 25))
val users = db.userDao().getAll()
db.userDao().observeAll()
    .asDbResultWithLoading()
    .collect { r ->
        r.onLoading { showLoading() }
            .onSuccess { showData(it) }
            .onFailure { showError(it) }
    }
```

---

## 演示应用

`demo` 含 CRUD、DbResult、Flow、事务与 `batchExecute`、DebugHelper、手动分页、**Paging3 列表**（`PagingDemoActivity`）、备份/恢复、`DatabaseManager` 等；按钮与场景对照 [demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md)。

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

三种状态 + `getOrNull` / `map` / `flatMap` / `fold` / `combineDbResults`；Flow 用 `asDbResult` / `asDbResultWithLoading`；`asDbResultLiveData` 可设超时；单次操作用 `dbResultOf { }`。

### 事务

- **`withTx { }`**：挂起事务 = `withTransaction`；与 Java `runInTransaction(Runnable)` 可并存，命名不冲突即好。
- **`safeTransaction`**：返回 `Result`；`CancellationException` 仍向上抛。
- **`batchExecute`**：FAIL_FAST = 整批**单**事务；SKIP = **每条独立事务**（`batchSize` 在 SKIP 下无效果）。

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

`@TypeConverters(AwConverters::class)`；`java.time` 需 **coreLibraryDesugaring** 并加 `AwJavaTimeConverters`；`Enum` 为 `EnumConverter<T>` 子类。JSON 解析失败会抛异常（见 FAQ）。

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

`@Query` 返回 `PagingSource<Int, T>` 后，用**方法引用**作工厂： `userDao::pagingSource.asPagingFlow(20)`。`asDbResultPagingFlow` 的项目前多为 `Success`；错误与重试看 Paging 的 `LoadState`。

手写 `LIMIT`/`OFFSET` 时，用 `items.toPagedResult(page, pageSize, total)` 封装。

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
```

</details>

---

## 工程与发版

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
- **单例**：优先 `DatabaseManager`；或自管 `synchronized` + `AwDatabase.build`。
- **迁移**：发版前完整迁移链；仅开发可销毁式。
- **Flow UI**：需要加载态用 `asDbResultWithLoading()`。
- **分页**：优先 Paging3 + `LIMIT`/`OFFSET`；避免全表进内存再 slice。
- **JSON 列**：小集合、低频可 `AwConverters`；高频大集合用关联表。

```kotlin
// Flow + Loading 示例
userDao.observeAll()
    .asDbResultWithLoading()
    .collect { r ->
        r.onLoading { binding.progressBar.isVisible = true }
            .onSuccess { binding.progressBar.isVisible = false; showData(it) }
            .onFailure { binding.progressBar.isVisible = false; showError(it) }
    }
```

---

## 常见问题

- **List 存库为何用 JSON 不用逗号分隔？** 逗号会与内容冲突；库内用 `kotlinx.serialization`。
- **insert 与 upsert？** `REPLACE` 会删后插，可触发 CASCADE；有外键时倾向 `upsert` / `insertOrIgnore`。
- **batchExecute 与 insertAll？** `insertAll` 吞吐最好；`batchExecute` 要逐条逻辑或错误收集时用；SKIP=每行一事务，整表原子用 `insertAll` 或 FAIL_FAST。
- **asDbResult 与 asDbResultWithLoading？** 无 UI 加载态用前者；有则用后者。
- **TypeConverter 解析失败？** 抛 `IllegalArgumentException`；要容错在调用方 catch。
- **挂起事务用啥？** `withTx`；Java 用 `runInTransaction(Runnable)`。
- **asDbResultLiveData 超时？** 无观察者时多久断开 Flow；转屏可加大 `timeoutInMs`（如 `10_000L`）。

---

## 混淆

`consumer-rules.pro` 会随 aar 注入宿主；`minifyEnabled true` 时一般无需再抄 `DbResult` / `BatchResult` / Converter 等规则。若 R8 仍报缺失，再按堆栈补 `-keep`（避免整包 `keep`）。

---

## 许可证

Apache License 2.0 — 见 [LICENSE](LICENSE)。
