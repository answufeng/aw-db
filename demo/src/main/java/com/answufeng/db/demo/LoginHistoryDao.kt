package com.answufeng.db.demo

import androidx.room.Dao
import androidx.room.Query
import com.answufeng.db.BaseDao

@Dao
abstract class LoginHistoryDao : BaseDao<LoginHistory>() {

    @Query("SELECT * FROM login_history WHERE userId = :userId ORDER BY createdAt DESC")
    abstract suspend fun getByUserId(userId: Long): List<LoginHistory>

    @Query("SELECT COUNT(*) FROM login_history")
    abstract suspend fun count(): Int

    @Query("DELETE FROM login_history")
    abstract suspend fun deleteAll()
}
