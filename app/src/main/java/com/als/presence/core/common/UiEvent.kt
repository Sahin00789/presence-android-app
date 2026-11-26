package com.als.presence.core.common

/**
 * One-time UI events triggered by ViewModels for navigation or transient user feedback.
 */
sealed interface UiEvent {
    /** Displays transient toast/snackbar notification with [message]. */
    data class ShowSnackbar(val message: String) : UiEvent

    /** Navigates back to previous screen in backstack. */
    data object NavigateBack : UiEvent
}
