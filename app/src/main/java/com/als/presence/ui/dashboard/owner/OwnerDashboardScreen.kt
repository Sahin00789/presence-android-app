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
import com.als.presence.ui.dashboard.common.LocationsTab
import com.als.presence.ui.dashboard.common.LocationsViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.owner.tabs.OwnerApprovalsTab
import com.als.presence.ui.dashboard.owner.tabs.OwnerReportsTab
import com.als.presence.ui.dashboard.owner.tabs.OwnerWorkforceTab
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerAttendanceViewModel
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/** Owner dashboard: Employees (0), Locations (1), Reports (2), Approvals (3). */
@Composable
fun OwnerDashboardScreen(
    user: User,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    onEditOrgClick: (() -> Unit)? = null,
    workforceViewModel: OwnerWorkforceViewModel = hiltViewModel(),
    attendanceViewModel: OwnerAttendanceViewModel = hiltViewModel(),
    locationsViewModel: LocationsViewModel = hiltViewModel()
) {
    val staffState by workforceViewModel.staff.collectAsStateWithLifecycle()
    val membersState by workforceViewModel.members.collectAsStateWithLifecycle()
    val premisesState by workforceViewModel.premises.collectAsStateWithLifecycle()
    val payrollPeriodsState by workforceViewModel.payrollPeriods.collectAsStateWithLifecycle()
    val payrollItemsMap by workforceViewModel.payrollItemsMap.collectAsStateWithLifecycle()
    val workforceMessage by workforceViewModel.message.collectAsStateWithLifecycle()

    val recordsState by attendanceViewModel.records.collectAsStateWithLifecycle()
    val exceptionsState by attendanceViewModel.exceptions.collectAsStateWithLifecycle()
    val attendanceMessage by attendanceViewModel.message.collectAsStateWithLifecycle()
    val attendanceSubmitting by attendanceViewModel.isSubmitting.collectAsStateWithLifecycle()

    val presenceState by locationsViewModel.presence.collectAsStateWithLifecycle()
    val selectedPremisesId by locationsViewModel.selectedPremisesId.collectAsStateWithLifecycle()
    val locationsSubmitting by locationsViewModel.isSubmitting.collectAsStateWithLifecycle()
    val locationsMessage by locationsViewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }

    val candidateMessage = workforceMessage ?: attendanceMessage ?: locationsMessage
    LaunchedEffect(candidateMessage) {
        if (candidateMessage != null) {
            activeMessage = candidateMessage
            workforceViewModel.consumeMessage()
            attendanceViewModel.consumeMessage()
            locationsViewModel.consumeMessage()
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
                isRecordingPayment = workforceViewModel.isSubmitting.collectAsStateWithLifecycle().value,
                onRetry = workforceViewModel::refresh,
                onSetSalary = workforceViewModel::setSalary,
                onRecordPayment = workforceViewModel::recordPayment,
                onEditOrgClick = onEditOrgClick
            )
            1 -> LocationsTab(
                user = user,
                premisesState = premisesState,
                presenceState = presenceState,
                staffState = staffState,
                selectedPremisesId = selectedPremisesId,
                isSubmitting = locationsSubmitting,
                onSelectPremises = locationsViewModel::selectPremises,
                onRefresh = {
                    locationsViewModel.refresh()
                    workforceViewModel.refresh()
                },
                onResolveGpsException = locationsViewModel::resolveGpsException
            )
            2 -> OwnerReportsTab(
                recordsState = recordsState,
                premisesState = premisesState,
                onRefresh = {
                    attendanceViewModel.refresh()
                    workforceViewModel.refresh()
                }
            )
            else -> OwnerApprovalsTab(
                exceptionsState = exceptionsState,
                isSubmitting = attendanceSubmitting,
                onRefresh = attendanceViewModel::refresh,
                onResolve = attendanceViewModel::resolve
            )
        }

        MessageSnackbar(
            message = activeMessage,
            onDismiss = { activeMessage = null }
        )
    }
}
