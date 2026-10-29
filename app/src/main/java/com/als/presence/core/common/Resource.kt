package com.als.presence.core.common

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(
        val exception: Throwable,
        val message: String? = null,
        val code: String? = null
    ) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
