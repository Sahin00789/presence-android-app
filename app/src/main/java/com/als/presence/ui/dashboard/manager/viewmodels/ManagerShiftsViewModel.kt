package com.als.presence.ui.dashboard.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.CreateHolidayRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.CreateShiftRequest
import com.als.presence.data.model.DomainWebSocketEvent
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UpdateShiftRequest
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shifts domain: shifts, roster assignments and holiday calendar. */
@HiltViewModel
class ManagerShiftsViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _templates = MutableStateFlow<UiState<List<ShiftDto>>>(UiState.Loading)
    val templates: StateFlow<UiState<List<ShiftDto>>> = _templates.asStateFlow()

    private val _assignments = MutableStateFlow<UiState<List<ShiftAssignmentDto>>>(UiState.Loading)
    val assignments: StateFlow<UiState<List<ShiftAssignmentDto>>> = _assignments.asStateFlow()

    private val _holidays = MutableStateFlow<UiState<List<HolidayDto>>>(UiState.Loading)
    val holidays: StateFlow<UiState<List<HolidayDto>>> = _holidays.asStateFlow()

    private val _employee = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _employee.asStateFlow()

    private val _premises = MutableStateFlow<UiState<List<PremisesDto>>>(UiState.Loading)
    val premises: StateFlow<UiState<List<PremisesDto>>> = _premises.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                if (event is DomainWebSocketEvent.ShiftUpdate ||
                    event is DomainWebSocketEvent.PremisesUpdate ||
                    event is DomainWebSocketEvent.StaffUpdate
                ) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        loadTemplates()
        loadAssignments()
        loadHolidays()
        loadStaff()
        loadPremises()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = UiState.Loading
            _templates.value = repository.getShifts().toUiState()
        }
    }

    fun loadAssignments() {
        viewModelScope.launch {
            _assignments.value = UiState.Loading
            _assignments.value = repository.getShiftAssignments().toUiState()
        }
    }

    fun loadHolidays() {
        viewModelScope.launch {
            _holidays.value = UiState.Loading
            _holidays.value = repository.getHolidays().toUiState()
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            _employee.value = UiState.Loading
            _employee.value = repository.getEmployees().toUiState()
        }
    }

    fun loadPremises() {
        viewModelScope.launch {
            _premises.value = UiState.Loading
            _premises.value = repository.getPremises().toUiState()
        }
    }

    fun assignShift(
        employeeId: String,
        shiftTemplateId: String,
        premisesId: String? = null,
        presentPay: Double = 0.0,
        halfDayPay: Double = 0.0,
        leavePay: Double = 0.0,
        absentPay: Double = 0.0
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShiftAssignment(
                    CreateShiftAssignmentRequest(
                        employeeId = employeeId,
                        staffId = employeeId,
                        shiftId = shiftTemplateId,
                        premisesId = premisesId.orEmpty(),
                        presentPay = presentPay,
                        halfDayPay = halfDayPay,
                        leavePay = leavePay,
                        absentPay = absentPay
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift assigned successfully."
                    loadAssignments()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun assignShifts(
        employeeId: String,
        assignmentsList: List<CreateShiftAssignmentRequest>,
        startDate: String? = null,
        premisesId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            var successCount = 0
            var lastError: String? = null

            for (item in assignmentsList) {
                val result = repository.createShiftAssignment(
                    item.copy(
                        employeeId = employeeId,
                        staffId = employeeId,
                        premisesId = premisesId ?: item.premisesId
                    )
                )
                when (result) {
                    is Resource.Success -> successCount++
                    is Resource.Error -> lastError = result.message
                    Resource.Loading -> Unit
                }
            }

            if (successCount > 0) {
                _message.value = "Assigned $successCount shift schedule(s)."
                loadAssignments()
            } else if (lastError != null) {
                _message.value = lastError
            }
            _isSubmitting.value = false
        }
    }

    fun createTemplate(
        name: String,
        startTime: String,
        endTime: String,
        premisesId: String? = null,
        dayName: String = "ALL",
        halfDayThreshold: String = "11:00:00"
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShift(
                    CreateShiftRequest(
                        shiftName = name.trim(),
                        premisesId = premisesId.orEmpty(),
                        dayName = dayName.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        halfDayThreshold = halfDayThreshold.trim()
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift “${result.data.shiftName}” created."
                    loadTemplates()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun updateTemplate(
        templateId: String,
        name: String,
        startTime: String,
        endTime: String,
        dayName: String? = null,
        halfDayThreshold: String? = null,
        premisesId: String? = null,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updateShift(
                    templateId,
                    UpdateShiftRequest(
                        shiftName = name.trim(),
                        dayName = dayName?.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        halfDayThreshold = halfDayThreshold?.trim(),
                        isActive = isActive
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift “${result.data.shiftName}” updated."
                    loadTemplates()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.deleteShift(templateId)) {
                is Resource.Success -> {
                    _message.value = "Shift removed."
                    loadTemplates()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun createHoliday(name: String, date: String, isPaid: Boolean) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createHoliday(
                    CreateHolidayRequest(
                        name = name.trim(),
                        holidayDate = date.trim(),
                        isPaid = isPaid
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Holiday “${result.data.name}” added for ${Dates.formatDate(result.data.holidayDate)}."
                    loadHolidays()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun deleteHoliday(holidayId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.deleteHoliday(holidayId)) {
                is Resource.Success -> {
                    _message.value = "Holiday removed from the calendar."
                    loadHolidays()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
