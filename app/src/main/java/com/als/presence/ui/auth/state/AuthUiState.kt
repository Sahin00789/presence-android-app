package com.als.presence.ui.auth.state

import com.als.presence.core.common.UiState
import com.als.presence.domain.model.User

data class AuthUiState(val auth: UiState<User> = UiState.Idle)
