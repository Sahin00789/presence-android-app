package com.als.presence.ui.dashboard.manager

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
import com.als.presence.core.common.UiState
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.LocationsTab
import com.als.presence.ui.dashboard.common.LocationsViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.manager.modals.AddStaffModal
import com.als.presence.ui.dashboard.manager.modals.AssignShiftWithRulesModal
import com.als.presence.ui.dashboard.manager.tabs.ManagerAttendanceTab
import com.als.presence.ui.dashboard.manager.tabs.ManageremployeeTab
import com.als.presence.ui.dashboard.owner.tabs.OwnerApprovalsTab
import com.als.presence.ui.dashboard.staff.StaffClockInViewModel

/** Manager dashboard: Attendance (0), Staff (1), Locations (2), Approvals (3). */
@Composable
fun ManagerDashboardScreen(
    user: User,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    staffViewModel: ManagerStaffViewModel = hiltViewModel(),
    shiftsViewModel: ManagerShiftsViewModel = hiltViewModel(),
    attendanceViewModel: ManagerAttendanceViewModel = hiltViewModel(),
    clockInViewModel: StaffClockInViewModel = hiltViewModel(),
    locationsViewModel: LocationsViewModel = hiltViewModel()
) {
    val staffState by staffViewModel.staff.collectAsStateWithLifecycle()
    val staffMessage by staffViewModel.message.collectAsStateWithLifecycle()
    val staffSubmitting by staffViewModel.isSubmitting.collectAsStateWithLifecycle()
    val faceStatuses by staffViewModel.faceStatuses.collectAsStateWithLifecycle()
    val faceSubmittingFor by staffViewModel.faceSubmittingFor.collectAsStateWithLifecycle()
    val payrollPeriodsState by staffViewModel.payrollPeriods.collectAsStateWithLifecycle()
    val payrollItemsMap by staffViewModel.payrollItemsMap.collectAsStateWithLifecycle()

    val templatesState by shiftsViewModel.templates.collectAsStateWithLifecycle()
    val assignmentsState by shiftsViewModel.assignments.collectAsStateWithLifecycle()
    val shiftStaffState by shiftsViewModel.staff.collectAsStateWithLifecycle()
    val premisesState by shiftsViewModel.premises.collectAsStateWithLifecycle()
    val shiftsMessage by shiftsViewModel.message.collectAsStateWithLifecycle()

    val recordsState by attendanceViewModel.records.collectAsStateWithLifecycle()
    val exceptionsState by attendanceViewModel.exceptions.collectAsStateWithLifecycle()
    val attendanceMessage by attendanceViewModel.message.collectAsStateWithLifecycle()
    val attendanceSubmitting by attendanceViewModel.isSubmitting.collectAsStateWithLifecycle()

    val clockState by clockInViewModel.state.collectAsStateWithLifecycle()

    val presenceState by locationsViewModel.presence.collectAsStateWithLifecycle()
    val selectedPremisesId by locationsViewModel.selectedPremisesId.collectAsStateWithLifecycle()
    val locationsSubmitting by locationsViewModel.isSubmitting.collectAsStateWithLifecycle()
    val locationsMessage by locationsViewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    val candidateMessage = staffMessage ?: shiftsMessage ?: attendanceMessage ?: clockState.message ?: locationsMessage
    LaunchedEffect(candidateMessage) {
        if (candidateMessage != null) {
            activeMessage = candidateMessage
            staffViewModel.consumeMessage()
            shiftsViewModel.consumeMessage()
            attendanceViewModel.consumeMessage()
            clockInViewModel.consumeMessage()
            locationsViewModel.consumeMessage()
        }
    }

    var ownAssignDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForShift by remember { mutableStateOf<StaffDto?>(null) }
    var selectedPremisesForShift by remember { mutableStateOf<String?>(null) }
    var localAddEmployee by remember { mutableStateOf(false) }

    val allStaff = (shiftStaffState as? UiState.Success)?.data.orEmpty().ifEmpty {
        (staffState as? UiState.Success)?.data.orEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> ManagerAttendanceTab(
                user = user,
                recordsState = recordsState,
                exceptionsState = exceptionsState,
                isSubmitting = attendanceSubmitting,
                clockState = clockState,
                onRefresh = {
                    attendanceViewModel.refresh()
                    clockInViewModel.refresh()
                },
                onResolve = attendanceViewModel::resolve,
                onClockIn = clockInViewModel::clockIn,
                onClockOut = clockInViewModel::clockOut,
                onLocate = clockInViewModel::locate,
                engine = clockInViewModel.faceEmbeddingEngine,
                onFaceCaptured = clockInViewModel::setFaceEmbedding
            )
            1 -> ManageremployeeTab(
                user = user,
                staffState = staffState,
                faceStatuses = faceStatuses,
                faceSubmittingFor = faceSubmittingFor,
                isSubmitting = staffSubmitting,
                engine = staffViewModel.faceEmbeddingEngine,
                onRefresh = staffViewModel::refresh,
                onAddStaff = { localAddEmployee = true },
                onEnrollFace = staffViewModel::enrollFace,
                onRevokeFace = staffViewModel::revokeFace,
                onDeactivateStaff = staffViewModel::deactivateMember,
                onAssignShift = { staff -> selectedEmployeeForShift = staff },
                premisesState = premisesState,
                payrollPeriodsState = payrollPeriodsState,
                payrollItemsMap = payrollItemsMap,
                onRecordPayment = staffViewModel::recordPayment
            )
            else -> LocationsTab(
                user = user,
                premisesState = premisesState,
                presenceState = presenceState,
                staffState = staffState,
                selectedPremisesId = selectedPremisesId,
                isSubmitting = locationsSubmitting,
                onSelectPremises = locationsViewModel::selectPremises,
                onRefresh = {
                    locationsViewModel.refresh()
                    shiftsViewModel.refresh()
                },
                onResolveGpsException = locationsViewModel::resolveGpsException,
                onCancelOvertime = locationsViewModel::cancelOvertime
            )
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    val managerPremisesId = (staffState as? UiState.Success)?.data?.find {
        it.userId == user.id || it.email.equals(user.email, ignoreCase = true)
    }?.premisesId

    val availablePremisesForAdd = if (user.role == Role.MANAGER && managerPremisesId != null) {
        (premisesState as? UiState.Success)?.data.orEmpty().filter { it.id == managerPremisesId }
    } else {
        (premisesState as? UiState.Success)?.data.orEmpty()
    }

    if (localAddEmployee) {
        AddStaffModal(
            premises = availablePremisesForAdd,
            initialPremisesId = managerPremisesId ?: selectedPremisesForShift,
            onDismiss = { localAddEmployee = false },
            onAddStaff = { name, email, department, address, premisesId ->
                staffViewModel.addStaff(name, email, department, address, premisesId)
                localAddEmployee = false
            }
        )
    }

    val activeStaffForShift = selectedEmployeeForShift
    if (ownAssignDialog || activeStaffForShift != null) {
        val staffList = if (activeStaffForShift != null) listOf(activeStaffForShift) else allStaff
        AssignShiftWithRulesModal(
            staff = staffList,
            templates = (templatesState as? UiState.Success)?.data.orEmpty(),
            initialStaffId = activeStaffForShift?.id,
            initialPremisesId = selectedPremisesForShift,
            onDismiss = {
                ownAssignDialog = false
                selectedEmployeeForShift = null
                selectedPremisesForShift = null
            },
            onAssignWithRules = { employeeId, assignments, startDate, premisesId, _, _ ->
                shiftsViewModel.assignShifts(
                    employeeId = employeeId,
                    assignmentsList = assignments,
                    startDate = startDate,
                    premisesId = premisesId
                )
                ownAssignDialog = false
                selectedEmployeeForShift = null
                selectedPremisesForShift = null
            }
        )
    }
}
