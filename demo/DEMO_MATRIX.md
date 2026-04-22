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
