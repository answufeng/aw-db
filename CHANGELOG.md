# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- `DbBackupHelper.restore`：改为通过 `DatabaseManager.forceClose` 在恢复前强制关闭数据库，避免引用计数大于 1 时仍覆盖文件导致损坏。
- `DatabaseManager`：同一数据库名混用不同 `RoomDatabase` 子类时抛出 `IllegalStateException`；`getOrNull` 在类型不符时同样抛出，避免静默 `null`。
- `DatabaseConfig`：`createFromAsset` 与 `createFromFile` 互斥，防止 Room Builder 上重复配置预置库来源。
- `batchExecute`：`FAIL_FAST` 与 `batchSize > 0` 组合非法（此前后者被静默忽略），现显式 `require` 失败并提示使用 `SKIP` 或 `batchSize = 0`。
- 协程取消：`safeTransaction`、`dbResultOf`、带逐条 `catch` 的 `batchExecute`（SKIP 路径与 FAIL_FAST）及 `asDbResult` / `asDbResultWithLoading` 对 `CancellationException` 选择重新抛出，避免取消被误当作业务失败或跳过项。

### Changed

- 新增 `DatabaseManager.forceClose(name)` 供备份恢复等需无视引用计数的场景。

## [1.0.0] - 2024-01-01

### Added

- DSL 风格的 Room 数据库构建器 `AwDatabase`
- 通用 `BaseDao<T>`，提供 CRUD + Upsert 操作
- `DbResult<T>` 密封类，支持 Loading/Success/Failure 状态
- `asDbResult()` 和 `asDbResultWithLoading()` Flow 扩展
- `asDbResultLiveData()` LiveData 扩展
- `dbResultOf()` 一次性操作包装
- 事务辅助：`runInTransaction`、`safeTransaction`、`batchExecute`
- `BatchFailureStrategy` 枚举（SKIP / FAIL_FAST）
- Migration DSL：`migration()`、`onCreateCallback()`、`onOpenCallback()`
- `AwConverters` 类型转换器：Date、List<String>、List<Long>、Map<String, String>、Boolean
- `DatabaseConfig` DSL：addMigrations、addCallback、fallbackToDestructiveMigration、setJournalMode、createFromAsset、createFromFile
- `PagedResult<T>` 分页查询结果封装
- `DbDebugHelper` 调试工具：tableList、rowCount、tableSchema
- 单元测试覆盖 AwConverters、DbResult、MigrationHelper、DatabaseConfig
- GitHub Actions CI 配置

### Changed

- `List<String>` 和 `List<Long>` 转换器改用 JSON 数组格式（替代逗号分隔），确保数据完整性
- `BaseDao.upsert()` 和 `upsertAll()` 改用 Room 原生 `@Upsert` 注解，保证原子性
- `batchInsert` 重命名为 `batchExecute`，新增 `BatchFailureStrategy` 支持
- `DbResult` 新增 `getOrThrow()`、`getOrElse()`、`recover()`、`recoverWith()` 方法
- `BrickDatabase` 重命名为 `AwDatabase`，`BrickConverters` 重命名为 `AwConverters`
- 清理 `libs.versions.toml`，移除未使用的依赖项
- 移除根 `build.gradle.kts` 中未使用的 Hilt 插件声明

### Fixed

- 修复 demo AndroidManifest 中 Activity 名称多了一个点号的问题
- 移除 demo 中的 `allowMainThreadQueries()` 反模式
- 修复 `publish.gradle` 中 groupId 与 README 不一致的问题
- 修复 `User` 实体使用 `var id` 违反不可变原则的问题
