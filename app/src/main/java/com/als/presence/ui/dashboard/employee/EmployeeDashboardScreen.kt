package com.als.presence.ui.dashboard.employee

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
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.employee.tabs.EmployeeAttendanceTab
import com.als.presence.ui.dashboard.employee.tabs.EmployeeClockInTab
import com.als.presence.ui.dashboard.employee.tabs.EmployeeLedgerTab
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeClockInViewModel
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeLedgerViewModel

/** Employee dashboard: Clock-In (0), Shifts/Attendance (1), Ledger (2). */
@Composable
fun EmployeeDashboardScreen(
    user: User,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    clockInViewModel: EmployeeClockInViewModel = hiltViewModel(),
    attendanceViewModel: EmployeeAttendanceViewModel = hiltViewModel(),
    ledgerViewModel: EmployeeLedgerViewModel = hiltViewModel()
) {
    val clockState by clockInViewModel.state.collectAsStateWithLifecycle()
    val overallSummaryState by ledgerViewModel.overallSummary.collectAsStateWithLifecycle()
    val monthlyLedgerState by ledgerViewModel.monthlyLedger.collectAsStateWithLifecycle()
    val ledgerState by ledgerViewModel.ledgerEntries.collectAsStateWithLifecycle()
    val selectedMonth by ledgerViewModel.selectedMonth.collectAsStateWithLifecycle()
    val ledgerMessage by ledgerViewModel.message.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(clockState.message) {
        if (clockState.message != null) {
            activeMessage = clockState.message
            clockInViewModel.consumeMessage()
            attendanceViewModel.refresh()
            ledgerViewModel.refresh()
        }
    }
    LaunchedEffect(ledgerMessage) {
        if (ledgerMessage != null) {
            activeMessage = ledgerMessage
            ledgerViewModel.consumeMessage()
        }
    }
    LaunchedEffect(clockState.error) {
        if (clockState.error != null) {
            activeMessage = clockState.error
            clockInViewModel.consumeError()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) ledgerViewModel.refresh()
        if (selectedTab == 1) attendanceViewModel.refresh()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> EmployeeClockInTab(
                user = user,
                state = clockState,
                onRefresh = { clockInViewModel.refresh() },
                onLocate = clockInViewModel::locate,
                onUpdateLocation = clockInViewModel::updateLocation,
                onClockIn = { overtimeId, embedding ->
                    clockInViewModel.clockIn(overtimeId, embedding?.vector?.toList())
                },
                onClockOut = clockInViewModel::clockOut,
                engine = clockInViewModel.faceEmbeddingEngine,
                onFaceCaptured = clockInViewModel::setFaceEmbedding,
                onSilentRefresh = { clockInViewModel.refresh(silent = true) },
                onClaimUnhandledEvent = clockInViewModel::claimUnhandledEvent
            )
            1 -> EmployeeAttendanceTab(
                viewModel = attendanceViewModel,
                modifier = Modifier.fillMaxSize(),
                onRefresh = attendanceViewModel::refresh
            )
            else -> EmployeeLedgerTab(
                overallSummaryState = overallSummaryState,
                monthlyLedgerState = monthlyLedgerState,
                ledgerState = ledgerState,
                selectedMonth = selectedMonth,
                onMonthSelected = ledgerViewModel::selectMonth,
                onDownloadPayslip = { ym ->
                    ledgerViewModel.downloadPayslip(context, ym.monthValue, ym.year)
                },
                onRefresh = ledgerViewModel::refresh
            )
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }
}
