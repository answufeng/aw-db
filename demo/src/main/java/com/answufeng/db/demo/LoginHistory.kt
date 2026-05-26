package com.answufeng.db.demo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 用户登录/操作历史（**子表**），通过 [userId] 关联父表 [User]。
 *
 * ## 和 User 怎么关联？
 *
 * 1. **库表结构**：本表字段 [userId] 存的是 [User.id]（父表主键），并在 [@Entity] 的
 *    [ForeignKey] 里声明 `parentColumns = ["id"]`、`childColumns = ["userId"]`。
 * 2. **插入时**：必须先有用户，再把 `LoginHistory(userId = 那个用户的 id, …)` 插入；
 *    Room 不会自动填 userId。
 * 3. **查询「用户+全部历史」**：用 [UserWithHistories]（@Relation），不要手写 JOIN。
 * 4. **只查历史**：用 [LoginHistoryDao.getByUserId]。
 * 5. **删用户**：若 [onDelete] 为 CASCADE，该用户下的历史行会一并删除。
 *
 * 详细说明见仓库 README「多表关联示例」章节。
 */
@Entity(
    tableName = "login_history",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class LoginHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 外键：必须等于某条已存在 [User.id]。 */
    val userId: Long,
    val action: String,
    val note: String = "",
    val createdAt: Date = Date()
) {
    companion object {
        const val ACTION_LOGIN = "LOGIN"
        const val ACTION_LOGOUT = "LOGOUT"
        const val ACTION_UPDATE = "UPDATE_PROFILE"
    }
}
