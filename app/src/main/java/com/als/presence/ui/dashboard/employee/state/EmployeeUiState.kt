package com.als.presence.ui.dashboard.employee.state

import com.als.presence.core.common.UiState

data class EmployeeUiState(val state: UiState<Any> = UiState.Idle)
typealias StaffUiState = EmployeeUiState
