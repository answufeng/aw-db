package com.answufeng.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * 数据库操作结果包装，统一处理查询的加载/成功/失败状态。
 *
 * ### 配合 Flow 使用
 * ```kotlin
 * fun observeUsers(): Flow<DbResult<List<User>>> {
 *     return userDao.observeAll().asDbResult()
 * }
 * ```
 *
 * ### 在 UI 层消费
 * ```kotlin
 * viewModel.users.collect { result ->
 *     result.onSuccess { users -> showList(users) }
 *           .onFailure { error -> showError(error) }
 *           .onLoading { showLoading() }
 * }
 * ```
 */
sealed class DbResult<out T> {

    /** 加载中 */
    data object Loading : DbResult<Nothing>()

    /** 查询成功 */
    data class Success<T>(val data: T) : DbResult<T>()

    /** 查询失败 */
    data class Failure(val error: Throwable) : DbResult<Nothing>()

    /** 是否为成功状态 */
    val isSuccess: Boolean get() = this is Success

    /** 是否为失败状态 */
    val isFailure: Boolean get() = this is Failure

    /** 是否为加载中 */
    val isLoading: Boolean get() = this is Loading

    /**
     * 获取数据，失败或加载中返回 null。
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 获取数据，失败或加载中返回默认值。
     *
     * @param default 默认值
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * 获取数据，失败时抛出异常，加载中返回 null。
     *
     * @throws Throwable 当状态为 [Failure] 时抛出内部异常
     */
    fun getOrThrow(): T? = when (this) {
        is Success -> data
        is Failure -> throw error
        is Loading -> null
    }

    /**
     * 成功时执行回调。
     *
     * @param action 成功回调
     */
    inline fun onSuccess(action: (T) -> Unit): DbResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * 失败时执行回调。
     *
     * @param action 失败回调
     */
    inline fun onFailure(action: (Throwable) -> Unit): DbResult<T> {
        if (this is Failure) action(error)
        return this
    }

    /**
     * 加载中执行回调。
     *
     * @param action 加载中回调
     */
    inline fun onLoading(action: () -> Unit): DbResult<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * 转换成功数据。
     *
     * @param transform 转换函数
     */
    fun <R> map(transform: (T) -> R): DbResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
        is Loading -> Loading
    }

    /**
     * 对三种状态分别处理，返回统一类型的结果。
     *
     * ```kotlin
     * val text = result.fold(
     *     onLoading = { "Loading..." },
     *     onSuccess = { "${it.size} items" },
     *     onFailure = { "Error: ${it.message}" }
     * )
     * ```
     */
    inline fun <R> fold(
        onLoading: () -> R,
        onSuccess: (T) -> R,
        onFailure: (Throwable) -> R
    ): R = when (this) {
        is Loading -> onLoading()
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    /**
     * 恢复失败状态为默认数据。
     *
     * @param default 失败时返回的默认值
     */
    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * 当失败时，使用恢复函数提供替代数据。
     *
     * @param recover 恢复函数
     */
    fun recover(recover: (Throwable) -> @UnsafeVariance T): DbResult<T> = when (this) {
        is Failure -> Success(recover(error))
        else -> this
    }

    /**
     * 当失败时，使用恢复函数提供替代的 [DbResult]。
     *
     * @param recover 恢复函数
     */
    fun recoverWith(recover: (Throwable) -> DbResult<@UnsafeVariance T>): DbResult<T> = when (this) {
        is Failure -> recover(error)
        else -> this
    }
}

/**
 * 将 Flow<T> 转换为 Flow<DbResult<T>>，自动处理异常，不包含 Loading 状态。
 *
 * 适用于不需要 Loading 状态的简单场景。
 *
 * ```kotlin
 * userDao.observeAll()
 *     .asDbResult()
 *     .collect { result -> handleResult(result) }
 * ```
 */
fun <T> Flow<T>.asDbResult(): Flow<DbResult<T>> {
    return this
        .map<T, DbResult<T>> { DbResult.Success(it) }
        .catch { emit(DbResult.Failure(it)) }
}

/**
 * 将 Flow<T> 转换为 Flow<DbResult<T>>，包含 Loading 状态。
 *
 * 在 Flow 开始发射数据前先发射 [DbResult.Loading]，
 * 适用于需要在 UI 层展示加载状态的场景。
 *
 * ```kotlin
 * userDao.observeAll()
 *     .asDbResultWithLoading()
 *     .collect { result ->
 *         result.onLoading { showLoading() }
 *               .onSuccess { showData(it) }
 *               .onFailure { showError(it) }
 *     }
 * ```
 */
fun <T> Flow<T>.asDbResultWithLoading(): Flow<DbResult<T>> {
    return this
        .map<T, DbResult<T>> { DbResult.Success(it) }
        .onStart { emit(DbResult.Loading) }
        .catch { emit(DbResult.Failure(it)) }
}

/**
 * 将 Flow<DbResult<T>> 转换为 LiveData<DbResult<T>>。
 *
 * ```kotlin
 * val usersLiveData: LiveData<DbResult<List<User>>> =
 *     userDao.observeAll().asDbResultWithLoading().asDbResultLiveData()
 * ```
 */
fun <T> Flow<DbResult<T>>.asDbResultLiveData(): LiveData<DbResult<T>> {
    return this.asLiveData()
}

/**
 * 安全执行数据库操作，自动包装为 [DbResult]。
 *
 * ```kotlin
 * val result = dbResultOf { userDao.getById(1) }
 * result.onSuccess { user -> showUser(user) }
 *       .onFailure { error -> showError(error) }
 * ```
 *
 * @param block 数据库操作
 * @return [DbResult] 包装的结果
 */
suspend fun <T> dbResultOf(block: suspend () -> T): DbResult<T> {
    return try {
        DbResult.Success(block())
    } catch (e: Exception) {
        DbResult.Failure(e)
    }
}
