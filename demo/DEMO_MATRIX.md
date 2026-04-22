# aw-db Demo 功能矩阵

| 按钮 | 能力 |
|------|------|
| 插入 / 批量 / 查询 / 按 ID | 基础 CRUD |
| Upsert / insertOrIgnore / 更新 / 删除 | 冲突与单行操作 |
| DbResult / 事务 / 批量执行 | `DbResult`、`safeTransaction`、`batchExecute` |
| Flow 观察 | `asDbResultWithLoading` |
| DebugHelper | 表结构、行数 |
| PagedResult | 手动分页模型 |
| 备份 / 恢复 | `backupTo`、`DbBackupHelper.restore` |
| getOrNull / 路径 | `DatabaseManager` 生命周期 |

日志区可复制分享，便于贴 issue。工具栏菜单 **「演示清单」** 可查看本摘要。

## 推荐手测（边界与极端场景）

| 场景 | 建议操作 |
|------|----------|
| 迁移 | 升级 DB version 走 Migration（勿依赖 demo 的 destructive 配置到生产） |
| 事务 | `batchExecute` FAIL_FAST / SKIP 与 `safeTransaction` 失败路径 |
| 并发 | 多协程同时写同一 Dao，观察是否按 Room 规则抛异常 |
| 低存储 | 备份到大文件前后杀进程，恢复路径是否完整 |
