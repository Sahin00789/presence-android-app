package com.als.presence.ui.dashboard.common

import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState

/** Maps a repository [Resource] onto the UI [UiState] contract. */
fun <T> Resource<T>.toUiState(): UiState<T> = when (this) {
    is Resource.Success -> UiState.Success(data)
    is Resource.Error -> UiState.Error(message ?: exception.message ?: "Unexpected error")
    Resource.Loading -> UiState.Loading
}

/** Human-readable text for an error resource, used in submit handlers. */
fun <T> Resource<T>.errorText(): String =
    (this as? Resource.Error)?.message ?: "Something went wrong"
