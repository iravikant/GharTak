package com.ghartak.app.util

/**
 * Wraps a value coming from a repository/use-case boundary so ViewModels never need to catch
 * exceptions from Retrofit/Room directly.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: String? = null) : Result<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, String?) -> Unit): Result<T> {
        if (this is Error) action(message, code)
        return this
    }
}
