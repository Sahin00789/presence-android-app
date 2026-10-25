package com.als.presence.ui.dashboard.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.OvertimeDto
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

/** Manager Overtime creation and management. */
@HiltViewModel
class ManagerOvertimeViewModel @Inject constructor(
    private val repository: OperationsRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<UiState<List<OvertimeDto>>>(UiState.Loading)
    val overtime: StateFlow<UiState<List<OvertimeDto>>> = _entries.asStateFlow()

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadEntries()
        loadStaff()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _entries.value = UiState.Loading
            _entries.value = repository.getOvertime().toUiState()
        }
    }

    private fun loadStaff() {
        viewModelScope.launch {
            _staff.value = UiState.Loading
            _staff.value = repository.getStaff().toUiState()
        }
    }

    fun create(
        staffProfileId: String?,
        workDate: String,
        startTime: String,
        endTime: String,
        paymentAmount: String,
        reason: String
    ) {
        val cents = Money.parseToCents(paymentAmount) ?: 0L
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createOvertime(
                    CreateOvertimeRequest(
                        staffProfileId = staffProfileId,
                        workDate = workDate,
                        startTime = startTime,
                        endTime = endTime,
                        paymentCents = cents,
                        reason = reason.trim().ifBlank { "Overtime" }
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Overtime booked for ${Dates.formatDate(workDate)}."
                    loadEntries()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun bookOvertime(
        staffProfileId: String,
        workDate: String,
        startTime: String,
        endTime: String,
        reason: String
    ) {
        create(
            staffProfileId = staffProfileId,
            workDate = workDate,
            startTime = startTime,
            endTime = endTime,
            paymentAmount = "0",
            reason = reason
        )
    }

    fun approveOvertime(overtimeId: String) {
        _message.value = "Overtime approval handled by backend."
    }

    fun rejectOvertime(overtimeId: String, reason: String) {
        _message.value = "Overtime rejection handled by backend."
    }

    fun markOvertimeWorked(overtimeId: String) {
        _message.value = "Overtime is completed on staff clock-out."
    }

    fun cancel(overtimeId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.cancelOvertime(overtimeId)) {
                is Resource.Success -> {
                    _message.value = "Overtime entry cancelled."
                    loadEntries()
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
