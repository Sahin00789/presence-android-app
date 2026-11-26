package com.als.presence.core.common

/**
 * Encapsulates data state with loading, success, and error representations for repository responses.
 */
sealed interface Resource<out T> {
    /** Data operation succeeded with payload [data]. */
    data class Success<out T>(val data: T) : Resource<T>

    /** Data operation failed with [exception] and optional [message] or error [code]. */
    data class Error(
        val exception: Throwable,
        val message: String? = null,
        val code: String? = null
    ) : Resource<Nothing>

    /** Data operation is currently loading or executing. */
    data object Loading : Resource<Nothing>
}
