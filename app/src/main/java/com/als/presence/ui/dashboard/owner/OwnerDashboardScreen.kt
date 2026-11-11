package com.als.presence.ui.dashboard.owner

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
import com.als.presence.ui.dashboard.common.LiveMonitorTab
import com.als.presence.ui.dashboard.common.LiveMonitorViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.owner.tabs.OwnerApprovalsTab
import com.als.presence.ui.dashboard.owner.tabs.OwnerReportsTab
import com.als.presence.ui.dashboard.owner.tabs.OwnerWorkforceTab
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerAttendanceViewModel
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/** Owner dashboard: Employees (0), Live Monitor (1), Reports (2), Approvals (3). */
@Composable
fun OwnerDashboardScreen(
    user: User,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    workforceViewModel: OwnerWorkforceViewModel = hiltViewModel(),
    attendanceViewModel: OwnerAttendanceViewModel = hiltViewModel(),
    liveMonitorViewModel: LiveMonitorViewModel = hiltViewModel()
) {
    val staffState by workforceViewModel.staff.collectAsStateWithLifecycle()
    val membersState by workforceViewModel.members.collectAsStateWithLifecycle()
    val premisesState by workforceViewModel.premises.collectAsStateWithLifecycle()
    val payrollPeriodsState by workforceViewModel.payrollPeriods.collectAsStateWithLifecycle()
    val payrollItemsMap by workforceViewModel.payrollItemsMap.collectAsStateWithLifecycle()
    val staffLedgerMap by workforceViewModel.staffLedgerEntriesMap.collectAsStateWithLifecycle()
    val workforceMessage by workforceViewModel.message.collectAsStateWithLifecycle()

    val recordsState by attendanceViewModel.records.collectAsStateWithLifecycle()
    val exceptionsState by attendanceViewModel.exceptions.collectAsStateWithLifecycle()
    val attendanceMessage by attendanceViewModel.message.collectAsStateWithLifecycle()
    val attendanceSubmitting by attendanceViewModel.isSubmitting.collectAsStateWithLifecycle()

    val presenceState by liveMonitorViewModel.presence.collectAsStateWithLifecycle()
    val selectedPremisesId by liveMonitorViewModel.selectedPremisesId.collectAsStateWithLifecycle()
    val locationsSubmitting by liveMonitorViewModel.isSubmitting.collectAsStateWithLifecycle()
    val locationsMessage by liveMonitorViewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }

    val candidateMessage = workforceMessage ?: attendanceMessage ?: locationsMessage
    LaunchedEffect(candidateMessage) {
        if (candidateMessage != null) {
            activeMessage = candidateMessage
            workforceViewModel.consumeMessage()
            attendanceViewModel.consumeMessage()
            liveMonitorViewModel.consumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> OwnerWorkforceTab(
                staffState = staffState,
                membersState = membersState,
                premisesState = premisesState,
                payrollPeriodsState = payrollPeriodsState,
                payrollItemsMap = payrollItemsMap,
                staffLedgerMap = staffLedgerMap,
                isRecordingPayment = workforceViewModel.isSubmitting.collectAsStateWithLifecycle().value,
                onRetry = workforceViewModel::refresh,
                onRecordPayment = workforceViewModel::recordPayment,
                onLoadStaffLedger = workforceViewModel::loadStaffEarningLedger,
                onDownloadPayslip = workforceViewModel::downloadPayslip
            )
            1 -> LiveMonitorTab(
                user = user,
                premisesState = premisesState,
                presenceState = presenceState,
                staffState = staffState,
                selectedPremisesId = selectedPremisesId,
                isSubmitting = locationsSubmitting,
                onSelectPremises = liveMonitorViewModel::selectPremises,
                onRefresh = {
                    liveMonitorViewModel.refresh()
                    workforceViewModel.refresh()
                },
                onResolveGpsException = liveMonitorViewModel::resolveGpsException,
                onCancelOvertime = liveMonitorViewModel::cancelOvertime
            )
            else -> OwnerReportsTab(
                user = user,
                recordsState = recordsState,
                premisesState = premisesState,
                onRefresh = {
                    attendanceViewModel.refresh()
                    workforceViewModel.refresh()
                }
            )
        }

        MessageSnackbar(
            message = activeMessage,
            onDismiss = { activeMessage = null }
        )
    }
}
