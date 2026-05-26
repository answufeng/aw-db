package com.answufeng.db.demo

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 查询结果 POJO：**一个用户 + 其全部 [LoginHistory]**（Room 一对多）。
 *
 * - [@Embedded] [user]：来自 `SELECT * FROM User …` 的列。
 * - [@Relation] [histories]：Room 根据 [User.id] 与 [LoginHistory.userId] 自动再查子表。
 *
 * Dao 示例：
 * ```kotlin
 * @Transaction
 * @Query("SELECT * FROM User WHERE id = :userId")
 * suspend fun getUserWithHistories(userId: Long): UserWithHistories?
 * ```
 *
 * [parentColumn] = `"id"`（User 主键字段名），[entityColumn] = `"userId"`（子表外键字段名）。
 */
data class UserWithHistories(
    @Embedded val user: User,
    @Relation(parentColumn = "id", entityColumn = "userId")
    val histories: List<LoginHistory>
)
