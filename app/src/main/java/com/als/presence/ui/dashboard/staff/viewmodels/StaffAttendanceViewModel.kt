package com.als.presence.ui.dashboard.staff.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ScheduleEntryDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Staff "Attendance" tab: personal register, schedule and overtime history. */
@HiltViewModel
class StaffAttendanceViewModel @Inject constructor(
    private val repository: OperationsRepository
) : ViewModel() {

    private val _records = MutableStateFlow<UiState<List<AttendanceRecordDto>>>(UiState.Loading)
    val records: StateFlow<UiState<List<AttendanceRecordDto>>> = _records.asStateFlow()

    private val _schedule = MutableStateFlow<UiState<List<ScheduleEntryDto>>>(UiState.Loading)
    val schedule: StateFlow<UiState<List<ScheduleEntryDto>>> = _schedule.asStateFlow()

    private val _overtime = MutableStateFlow<UiState<List<OvertimeDto>>>(UiState.Loading)
    val overtime: StateFlow<UiState<List<OvertimeDto>>> = _overtime.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadRecords()
        loadSchedule()
        loadOvertime()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = UiState.Loading
            _records.value = repository.getAttendanceRecords(
                from = Dates.isoDaysAgo(30),
                to = Dates.todayIso()
            ).toUiState()
        }
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _schedule.value = UiState.Loading
            _schedule.value = repository.getMySchedule(
                from = Dates.todayIso(),
                to = Dates.isoDaysAhead(14)
            ).toUiState()
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
}
