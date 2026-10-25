package com.als.presence.ui.dashboard.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.staff.tabs.StaffAttendanceTab
import com.als.presence.ui.dashboard.staff.viewmodels.StaffAttendanceViewModel

/**
 * "My Shifts" secondary screen for Managers and Staff.
 * Displays personal attendance register, upcoming shift schedule, and overtime history.
 */
@Composable
fun MyShiftsScreen(
    user: User,
    onBack: () -> Unit,
    viewModel: StaffAttendanceViewModel = hiltViewModel()
) {
    val recordsState by viewModel.records.collectAsStateWithLifecycle()
    val scheduleState by viewModel.schedule.collectAsStateWithLifecycle()
    val overtimeState by viewModel.overtime.collectAsStateWithLifecycle()

    PresenceSecondaryScaffold(
        crumb = "PERSONAL WORKSPACE",
        title = "My Shifts & Schedule",
        onBack = onBack
    ) {
        StaffAttendanceTab(
            recordsState = recordsState,
            scheduleState = scheduleState,
            overtimeState = overtimeState,
            onRefresh = viewModel::refresh,
            isScrollable = false
        )
    }
}
