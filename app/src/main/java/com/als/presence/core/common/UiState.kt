package com.als.presence.core.common

/**
 * Generic state wrapper for Jetpack Compose UI rendering.
 */
sealed interface UiState<out T> {
    /** Screen or component is idle with no data fetched yet. */
    data object Idle : UiState<Nothing>

    /** Screen or component is loading data. */
    data object Loading : UiState<Nothing>

    /** Screen or component loaded data payload [data] successfully. */
    data class Success<out T>(val data: T) : UiState<T>

    /** Screen or component encountered an error with descriptive [message]. */
    data class Error(val message: String, val code: String? = null) : UiState<Nothing>
}
