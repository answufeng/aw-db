package com.answufeng.db

import androidx.room.*

/**
 * Room Dao 基类，提供通用 CRUD 操作。
 *
 * 业务 Dao 只需继承并指定实体类型，即可获得完整的增删改查能力：
 *
 * ```kotlin
 * @Dao
 * abstract class UserDao : BaseDao<UserEntity>() {
 *
 *     @Query("SELECT * FROM users WHERE id = :id")
 *     abstract suspend fun getById(id: Long): UserEntity?
 *
 *     @Query("SELECT * FROM users ORDER BY name ASC")
 *     abstract fun observeAll(): Flow<List<UserEntity>>
 *
 *     @Query("DELETE FROM users")
 *     abstract suspend fun deleteAll()
 * }
 * ```
 *
 * 所有方法均为 suspend，应在协程中调用。Room 的 suspend 函数内部已切换到 IO 线程，
 * 无需外部包裹 `withContext(Dispatchers.IO)`。
 *
 * 批量操作（insertAll / updateAll / deleteAll / upsertAll）已标注 [@Transaction][Transaction]，
 * 确保在单个数据库事务中执行，避免中途崩溃导致部分写入。
 *
 * **注意**：[insert] 使用 [OnConflictStrategy.REPLACE]，主键冲突时会先删除旧行再插入新行，
 * 这可能触发外键的 CASCADE 删除。如果需要保留旧行，请使用 [insertOrIgnore] 或 [upsert]。
 *
 * @param T 实体类型，必须标注 [@Entity][Entity]
 */
abstract class BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract suspend fun insertAll(entities: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOrIgnore(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Transaction
    abstract suspend fun insertOrIgnoreAll(entities: List<T>): List<Long>

    @Update
    abstract suspend fun update(entity: T): Int

    @Update
    @Transaction
    abstract suspend fun updateAll(entities: List<T>): Int

    @Delete
    abstract suspend fun delete(entity: T): Int

    @Delete
    @Transaction
    abstract suspend fun deleteAll(entities: List<T>): Int

    @Upsert
    abstract suspend fun upsert(entity: T): Long

    @Upsert
    @Transaction
    abstract suspend fun upsertAll(entities: List<T>): List<Long>
}
