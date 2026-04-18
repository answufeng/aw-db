package com.answufeng.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed class DbResult<out T> {

    data object Loading : DbResult<Nothing>()

    data class Success<T>(val data: T) : DbResult<T>()

    data class Failure(val error: Throwable) : DbResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    val isFailure: Boolean get() = this is Failure

    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
        is Loading -> throw IllegalStateException("DbResult is still Loading")
    }

    inline fun onSuccess(action: (T) -> Unit): DbResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Throwable) -> Unit): DbResult<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun onLoading(action: () -> Unit): DbResult<T> {
        if (this is Loading) action()
        return this
    }

    inline fun <R> map(crossinline transform: (T) -> R): DbResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
        is Loading -> Loading
    }

    inline fun <R> flatMap(crossinline transform: (T) -> DbResult<R>): DbResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
        is Loading -> Loading
    }

    inline fun mapFailure(crossinline transform: (Throwable) -> Throwable): DbResult<T> = when (this) {
        is Failure -> Failure(transform(error))
        else -> this
    }

    inline fun filter(predicate: (T) -> Boolean): DbResult<T> = when (this) {
        is Success -> if (predicate(data)) this else Failure(NoSuchElementException("Predicate not satisfied"))
        else -> this
    }

    inline fun onEach(action: (T) -> Unit): DbResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun <R> fold(
        crossinline onLoading: () -> R,
        crossinline onSuccess: (T) -> R,
        crossinline onFailure: (Throwable) -> R
    ): R = when (this) {
        is Loading -> onLoading()
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun recover(recover: (Throwable) -> @UnsafeVariance T): DbResult<T> = when (this) {
        is Failure -> Success(recover(error))
        else -> this
    }

    fun recoverWith(recover: (Throwable) -> DbResult<@UnsafeVariance T>): DbResult<T> = when (this) {
        is Failure -> recover(error)
        else -> this
    }
}

fun <T1, T2, R> combineDbResults(
    result1: DbResult<T1>,
    result2: DbResult<T2>,
    transform: (T1, T2) -> R
): DbResult<R> = when {
    result1 is DbResult.Failure -> DbResult.Failure(result1.error)
    result2 is DbResult.Failure -> DbResult.Failure(result2.error)
    result1 is DbResult.Loading || result2 is DbResult.Loading -> DbResult.Loading
    result1 is DbResult.Success && result2 is DbResult.Success -> DbResult.Success(transform(result1.data, result2.data))
    else -> DbResult.Loading
}

fun <T1, T2, T3, R> combineDbResults(
    result1: DbResult<T1>,
    result2: DbResult<T2>,
    result3: DbResult<T3>,
    transform: (T1, T2, T3) -> R
): DbResult<R> = when {
    result1 is DbResult.Failure -> DbResult.Failure(result1.error)
    result2 is DbResult.Failure -> DbResult.Failure(result2.error)
    result3 is DbResult.Failure -> DbResult.Failure(result3.error)
    result1 is DbResult.Loading || result2 is DbResult.Loading || result3 is DbResult.Loading -> DbResult.Loading
    result1 is DbResult.Success && result2 is DbResult.Success && result3 is DbResult.Success ->
        DbResult.Success(transform(result1.data, result2.data, result3.data))
    else -> DbResult.Loading
}

fun <T> Flow<T>.asDbResult(): Flow<DbResult<T>> {
    return this
        .map<T, DbResult<T>> { DbResult.Success(it) }
        .catch { emit(DbResult.Failure(it)) }
}

fun <T> Flow<T>.asDbResultWithLoading(): Flow<DbResult<T>> {
    return this
        .map<T, DbResult<T>> { DbResult.Success(it) }
        .onStart { emit(DbResult.Loading) }
        .catch { emit(DbResult.Failure(it)) }
}

fun <T> Flow<DbResult<T>>.asDbResultLiveData(timeoutInMs: Long = 5000L): LiveData<DbResult<T>> {
    return this.asLiveData(timeoutInMs = timeoutInMs)
}

suspend fun <T> dbResultOf(block: suspend () -> T): DbResult<T> {
    return try {
        DbResult.Success(block())
    } catch (e: Exception) {
        DbResult.Failure(e)
    }
}
