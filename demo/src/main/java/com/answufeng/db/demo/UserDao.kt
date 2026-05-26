package com.answufeng.db.demo

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.answufeng.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao : BaseDao<User>() {

    @Query("SELECT * FROM User ORDER BY id ASC")
    abstract suspend fun getAll(): List<User>

    @Query("SELECT * FROM User WHERE id = :id")
    abstract suspend fun getById(id: Long): User?

    @Query("SELECT * FROM User")
    abstract fun observeAll(): Flow<List<User>>

    @Query("SELECT * FROM User ORDER BY name ASC")
    abstract fun pagingSource(): PagingSource<Int, User>

    @Query("SELECT COUNT(*) FROM User")
    abstract suspend fun count(): Int

    @Query("DELETE FROM User")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM User ORDER BY id ASC LIMIT :limit OFFSET :offset")
    abstract suspend fun getPage(limit: Int, offset: Int): List<User>

    @Transaction
    @Query("SELECT * FROM User WHERE id = :userId")
    abstract suspend fun getUserWithHistories(userId: Long): UserWithHistories?

    @Transaction
    @Query("SELECT * FROM User ORDER BY id ASC")
    abstract suspend fun getAllUsersWithHistories(): List<UserWithHistories>
}
