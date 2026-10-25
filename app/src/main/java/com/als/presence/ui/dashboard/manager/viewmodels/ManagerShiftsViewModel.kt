package com.als.presence.ui.dashboard.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.CreateHolidayRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.CreateShiftTemplateRequest
import com.als.presence.data.model.DayShiftAssignmentRequest
import com.als.presence.data.model.UpdateShiftTemplateRequest
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.als.presence.data.remote.DomainWebSocketEvent
import com.als.presence.data.remote.EventsWebSocketService

/**
 * Shifts domain: templates, roster assignments and holiday calendar.
 */
@HiltViewModel
class ManagerShiftsViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _templates = MutableStateFlow<UiState<List<ShiftTemplateDto>>>(UiState.Loading)
    val templates: StateFlow<UiState<List<ShiftTemplateDto>>> = _templates.asStateFlow()

    private val _assignments = MutableStateFlow<UiState<List<ShiftAssignmentDto>>>(UiState.Loading)
    val assignments: StateFlow<UiState<List<ShiftAssignmentDto>>> = _assignments.asStateFlow()

    private val _holidays = MutableStateFlow<UiState<List<HolidayDto>>>(UiState.Loading)
    val holidays: StateFlow<UiState<List<HolidayDto>>> = _holidays.asStateFlow()

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

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
        loadStaff()
        loadHolidays()
        loadPremises()
    }

    fun loadPremises() {
        viewModelScope.launch {
            _premises.value = UiState.Loading
            _premises.value = repository.getPremises().toUiState()
        }
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = UiState.Loading
            _templates.value = repository.getShiftTemplates().toUiState()
        }
    }

    fun loadAssignments() {
        viewModelScope.launch {
            _assignments.value = UiState.Loading
            _assignments.value = repository.getShiftAssignments().toUiState()
        }
    }

    fun loadHolidays(from: String? = null, to: String? = null) {
        viewModelScope.launch {
            _holidays.value = UiState.Loading
            _holidays.value = repository.getHolidays(from, to).toUiState()
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            _staff.value = UiState.Loading
            _staff.value = repository.getStaff().toUiState()
        }
    }

    fun assignShift(
        staffProfileId: String,
        shiftTemplateId: String,
        startDate: String,
        premisesId: String? = null,
        daysOfWeek: List<Int>? = null,
        paymentAmount: String? = null
    ) {
        val paymentCents = paymentAmount?.let { Money.parseToCents(it) }
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShiftAssignment(
                    CreateShiftAssignmentRequest(
                        staffProfileId = staffProfileId,
                        shiftTemplateId = shiftTemplateId,
                        premisesId = premisesId,
                        startDate = startDate,
                        paymentCents = paymentCents,
                        daysOfWeek = daysOfWeek
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift assigned from ${Dates.formatDate(startDate)}."
                    loadAssignments()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun assignShifts(
        staffProfileId: String,
        assignmentsList: List<DayShiftAssignmentRequest>,
        startDate: String,
        premisesId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            var successCount = 0
            var lastError: String? = null

            for (item in assignmentsList) {
                val paymentCents = item.presentPay?.let { Money.parseToCents(it) }
                val result = repository.createShiftAssignment(
                    CreateShiftAssignmentRequest(
                        staffProfileId = staffProfileId,
                        shiftTemplateId = item.shiftTemplateId,
                        premisesId = premisesId,
                        startDate = startDate,
                        paymentCents = paymentCents,
                        daysOfWeek = item.daysOfWeek
                    )
                )
                when (result) {
                    is Resource.Success -> successCount++
                    is Resource.Error -> lastError = result.message
                    Resource.Loading -> Unit
                }
            }

            if (successCount > 0) {
                _message.value = "Assigned $successCount shift schedule(s) from ${Dates.formatDate(startDate)}."
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
        breakMinutes: Int = 0,
        lateGraceMinutes: Int = 15,
        halfDayThresholdMinutes: Int = 240,
        premisesId: String? = null,
        daysOfWeek: List<Int>? = null,
        lateClockInAllowed: Boolean = true
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShiftTemplate(
                    CreateShiftTemplateRequest(
                        name = name.trim(),
                        premisesId = premisesId,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        breakMinutes = breakMinutes,
                        lateGraceMinutes = lateGraceMinutes,
                        halfDayThresholdMinutes = halfDayThresholdMinutes,
                        daysOfWeek = daysOfWeek,
                        lateClockInAllowed = lateClockInAllowed
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift template “${result.data.name}” created."
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
        breakMinutes: Int = 0,
        lateGraceMinutes: Int = 15,
        halfDayThresholdMinutes: Int = 240,
        premisesId: String? = null,
        daysOfWeek: List<Int>? = null,
        lateClockInAllowed: Boolean = true,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updateShiftTemplate(
                    templateId,
                    UpdateShiftTemplateRequest(
                        name = name.trim(),
                        premisesId = premisesId,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        breakMinutes = breakMinutes,
                        lateGraceMinutes = lateGraceMinutes,
                        halfDayThresholdMinutes = halfDayThresholdMinutes,
                        daysOfWeek = daysOfWeek,
                        lateClockInAllowed = lateClockInAllowed,
                        isActive = isActive
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift template “${result.data.name}” updated."
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
            when (val result = repository.deleteShiftTemplate(templateId)) {
                is Resource.Success -> {
                    _message.value = "Shift template removed."
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
                        date = date.trim(),
                        isPaid = isPaid
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Holiday “${result.data.name}” added for ${Dates.formatDate(result.data.date)}."
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
