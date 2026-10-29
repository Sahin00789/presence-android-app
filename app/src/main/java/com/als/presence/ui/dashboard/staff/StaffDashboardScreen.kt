package com.als.presence.ui.dashboard.staff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.LocationsTab
import com.als.presence.ui.dashboard.common.LocationsViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.staff.tabs.employeeAttendanceTab
import com.als.presence.ui.dashboard.staff.tabs.employeeClockInTab
import com.als.presence.ui.dashboard.staff.tabs.staffLedgerTab
import com.als.presence.ui.dashboard.staff.viewmodels.StaffAttendanceViewModel
import com.als.presence.ui.dashboard.staff.viewmodels.StaffClockInViewModel
import com.als.presence.ui.dashboard.staff.viewmodels.StaffLedgerViewModel

/** Staff dashboard: Clock-In (0), Shifts/Attendance (1), Locations (2), Ledger (3). */
@Composable
fun EmployeeDashboardScreen(
    user: User,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    clockInViewModel: StaffClockInViewModel = hiltViewModel(),
    attendanceViewModel: StaffAttendanceViewModel = hiltViewModel(),
    ledgerViewModel: StaffLedgerViewModel = hiltViewModel(),
    locationsViewModel: LocationsViewModel = hiltViewModel()
) {
    val clockState by clockInViewModel.state.collectAsStateWithLifecycle()
    val recordsState by attendanceViewModel.records.collectAsStateWithLifecycle()
    val overtimeState by attendanceViewModel.overtime.collectAsStateWithLifecycle()
    val summaryState by ledgerViewModel.summary.collectAsStateWithLifecycle()
    val ledgerState by ledgerViewModel.ledgerEntries.collectAsStateWithLifecycle()
    val payslipsState by ledgerViewModel.payslips.collectAsStateWithLifecycle()

    val presenceState by locationsViewModel.presence.collectAsStateWithLifecycle()
    val premisesState by locationsViewModel.premises.collectAsStateWithLifecycle()
    val selectedPremisesId by locationsViewModel.selectedPremisesId.collectAsStateWithLifecycle()
    val staffState by locationsViewModel.staff.collectAsStateWithLifecycle()
    val locationsSubmitting by locationsViewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(clockState.message) {
        if (clockState.message != null) {
            activeMessage = clockState.message
            clockInViewModel.consumeMessage()
            attendanceViewModel.refresh()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) ledgerViewModel.refresh()
        if (selectedTab == 1) attendanceViewModel.refresh()
        if (selectedTab == 2) locationsViewModel.refresh()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> employeeClockInTab(
                user = user,
                state = clockState,
                onRefresh = clockInViewModel::refresh,
                onLocate = clockInViewModel::locate,
                onClockIn = clockInViewModel::clockIn,
                onClockOut = clockInViewModel::clockOut,
                engine = clockInViewModel.faceEmbeddingEngine,
                onFaceCaptured = clockInViewModel::setFaceEmbedding
            )
            1 -> employeeAttendanceTab(
                recordsState = recordsState,
                overtimeState = overtimeState,
                onRefresh = attendanceViewModel::refresh
            )
            2 -> LocationsTab(
                user = user,
                premisesState = premisesState,
                presenceState = presenceState,
                staffState = staffState,
                selectedPremisesId = selectedPremisesId,
                isSubmitting = locationsSubmitting,
                onSelectPremises = locationsViewModel::selectPremises,
                onRefresh = locationsViewModel::refresh,
                onResolveGpsException = locationsViewModel::resolveGpsException
            )
            else -> staffLedgerTab(
                summaryState = summaryState,
                ledgerState = ledgerState,
                payslipsState = payslipsState,
                onRefresh = ledgerViewModel::refresh
            )
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }
}
