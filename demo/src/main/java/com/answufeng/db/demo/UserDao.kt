package com.answufeng.db.demo

import androidx.room.Dao
import com.answufeng.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao : BaseDao<User>() {
    @androidx.room.Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>

    @androidx.room.Query("SELECT * FROM User")
    abstract fun observeAll(): Flow<List<User>>

    @androidx.room.Query("DELETE FROM User")
    abstract suspend fun deleteAll()
}
