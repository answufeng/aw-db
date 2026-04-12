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
 * 所有方法均为 suspend，应在协程中调用，推荐使用 [Dispatchers.IO][kotlinx.coroutines.Dispatchers.IO]。
 *
 * @param T 实体类型，必须标注 [@Entity][Entity]
 */
abstract class BaseDao<T> {

    /**
     * 插入单条记录。
     * 冲突策略：[OnConflictStrategy.REPLACE]（存在则替换）。
     *
     * @param entity 要插入的实体
     * @return 插入的行 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: T): Long

    /**
     * 批量插入记录。
     * 冲突策略：[OnConflictStrategy.REPLACE]。
     *
     * @param entities 要插入的实体列表
     * @return 插入的行 ID 列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<T>): List<Long>

    /**
     * 插入单条记录，冲突时忽略。
     *
     * @param entity 要插入的实体
     * @return 插入的行 ID，冲突时返回 -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOrIgnore(entity: T): Long

    /**
     * 更新记录。
     *
     * @param entity 要更新的实体（根据主键匹配）
     * @return 受影响的行数
     */
    @Update
    abstract suspend fun update(entity: T): Int

    /**
     * 批量更新记录。
     *
     * @param entities 要更新的实体列表
     * @return 受影响的行数
     */
    @Update
    abstract suspend fun updateAll(entities: List<T>): Int

    /**
     * 删除记录。
     *
     * @param entity 要删除的实体（根据主键匹配）
     * @return 受影响的行数
     */
    @Delete
    abstract suspend fun delete(entity: T): Int

    /**
     * 批量删除记录。
     *
     * @param entities 要删除的实体列表
     * @return 受影响的行数
     */
    @Delete
    abstract suspend fun deleteAll(entities: List<T>): Int

    /**
     * 插入或更新（upsert 语义）。
     *
     * 使用 Room 原生 [@Upsert][Upsert] 注解，由 Room 在数据库层面保证原子性：
     * 主键不存在时执行 INSERT，主键已存在时执行 UPDATE。
     *
     * @param entity 要插入或更新的实体
     * @return 插入的行 ID；如果是更新，返回 -1
     */
    @Upsert
    abstract suspend fun upsert(entity: T): Long

    /**
     * 批量插入或更新。
     *
     * 使用 Room 原生 [@Upsert][Upsert] 注解，由 Room 在数据库层面保证原子性。
     *
     * @param entities 要插入或更新的实体列表
     * @return 插入的行 ID 列表；更新的条目返回 -1
     */
    @Upsert
    abstract suspend fun upsertAll(entities: List<T>): List<Long>
}
