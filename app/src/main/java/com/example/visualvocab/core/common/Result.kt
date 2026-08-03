package com.example.visualvocab.core.common

// we use this to wrap around data so we know if it loaded okay or if something went wrong.
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    object Loading : Result<Nothing>
}
