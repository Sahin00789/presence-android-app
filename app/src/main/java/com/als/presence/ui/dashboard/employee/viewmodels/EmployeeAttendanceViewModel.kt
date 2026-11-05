package com.als.presence.ui.dashboard.employee.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.NextSessionUi
import com.als.presence.ui.dashboard.common.nextSessionFor
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Employee "Attendance" tab: personal register, next session card, and earning ledger. */
@HiltViewModel
class EmployeeAttendanceViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val sessionManager: SessionManager,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    private val _records = MutableStateFlow<UiState<List<AttendanceRecordDto>>>(UiState.Loading)
    val records: StateFlow<UiState<List<AttendanceRecordDto>>> = _records.asStateFlow()

    private val _overtime = MutableStateFlow<UiState<List<OvertimeDto>>>(UiState.Loading)
    val overtime: StateFlow<UiState<List<OvertimeDto>>> = _overtime.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<UiState<List<StaffEarningLedgerDto>>>(UiState.Loading)
    val ledgerEntries: StateFlow<UiState<List<StaffEarningLedgerDto>>> = _ledgerEntries.asStateFlow()

    /** Single upcoming/active session card for the Attendance tab. */
    private val _nextSession = MutableStateFlow<NextSessionUi?>(null)
    val nextSession: StateFlow<NextSessionUi?> = _nextSession.asStateFlow()

    private val _nextSessionLoading = MutableStateFlow(true)
    val nextSessionLoading: StateFlow<Boolean> = _nextSessionLoading.asStateFlow()

    private val _assignments = MutableStateFlow<UiState<List<ShiftAssignmentDto>>>(UiState.Loading)
    val assignments: StateFlow<UiState<List<ShiftAssignmentDto>>> = _assignments.asStateFlow()

    private val _templates = MutableStateFlow<UiState<List<ShiftDto>>>(UiState.Loading)
    val templates: StateFlow<UiState<List<ShiftDto>>> = _templates.asStateFlow()

    val currentUserId: String? get() = sessionManager.userId

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.ATTENDANCE)
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.SHIFTS)
            loadRecords()
            loadNextSession()
            loadLedger()
            loadOvertime()
            loadAssignmentsAndTemplates()
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = UiState.Loading
            _records.value = repository.getAttendanceRecords(
                from = Dates.isoDaysAgo(365),
                to = Dates.todayIso(),
                staffProfileId = sessionManager.userId
            ).toUiState()
        }
    }

    fun loadNextSession() {
        viewModelScope.launch {
            _nextSessionLoading.value = true
            val currentUserId = sessionManager.userId
            if (currentUserId.isNullOrBlank()) {
                _nextSession.value = null
                _nextSessionLoading.value = false
                return@launch
            }

            when (val result = repository.getTodayLocationPresence()) {
                is Resource.Success -> {
                    _nextSession.value = result.data.nextSessionFor(currentUserId)
                }
                is Resource.Error -> _nextSession.value = null
                Resource.Loading -> Unit
            }
            _nextSessionLoading.value = false
        }
    }

    fun loadOvertime() {
        viewModelScope.launch {
            _overtime.value = UiState.Loading
            _overtime.value = repository.getOvertime(
                from = Dates.isoDaysAgo(60),
                to = Dates.todayIso()
            ).toUiState()
        }
    }

    fun loadLedger() {
        viewModelScope.launch {
            _ledgerEntries.value = UiState.Loading
            _ledgerEntries.value = repository.getEarningLedger().toUiState()
        }
    }

    fun loadAssignmentsAndTemplates() {
        viewModelScope.launch {
            _assignments.value = UiState.Loading
            _templates.value = UiState.Loading
            _assignments.value = repository.getShiftAssignments().toUiState()
            _templates.value = repository.getShifts().toUiState()
        }
    }

    fun loadForMonth(month: java.time.YearMonth) {
        val startDate = "${month.year}-${String.format("%02d", month.monthValue)}-01"
        val daysInMonth = month.lengthOfMonth()
        val endDate = "${month.year}-${String.format("%02d", month.monthValue)}-${String.format("%02d", daysInMonth)}"
        viewModelScope.launch {
            _records.value = UiState.Loading
            _overtime.value = UiState.Loading
            _ledgerEntries.value = UiState.Loading
            _records.value = repository.getAttendanceRecords(
                from = startDate,
                to = endDate,
                staffProfileId = sessionManager.userId
            ).toUiState()
            _overtime.value = repository.getOvertime(from = startDate, to = endDate).toUiState()
            _ledgerEntries.value = repository.getEarningLedger(month = month.monthValue, year = month.year).toUiState()
        }
    }
}
