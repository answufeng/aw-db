package com.answufeng.db.demo

import androidx.paging.PagingSource
import androidx.room.Dao
import com.answufeng.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao : BaseDao<User>() {

    @androidx.room.Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>

    @androidx.room.Query("SELECT * FROM User WHERE id = :id")
    abstract suspend fun getById(id: Long): User?

    @androidx.room.Query("SELECT * FROM User")
    abstract fun observeAll(): Flow<List<User>>

    @androidx.room.Query("SELECT * FROM User ORDER BY name ASC")
    abstract fun pagingSource(): PagingSource<Int, User>

    @androidx.room.Query("SELECT COUNT(*) FROM User")
    abstract suspend fun count(): Int

    @androidx.room.Query("DELETE FROM User")
    abstract suspend fun deleteAll()
}
