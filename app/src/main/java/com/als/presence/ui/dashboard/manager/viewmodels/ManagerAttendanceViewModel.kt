package com.als.presence.ui.dashboard.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.ResolveExceptionRequest
import com.als.presence.data.model.DomainWebSocketEvent
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

/** Manager "Attendance" tab: register and exception resolution. */
@HiltViewModel
class ManagerAttendanceViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _records = MutableStateFlow<UiState<List<AttendanceRecordDto>>>(UiState.Loading)
    val records: StateFlow<UiState<List<AttendanceRecordDto>>> = _records.asStateFlow()

    private val _exceptions = MutableStateFlow<UiState<List<AttendanceExceptionDto>>>(UiState.Loading)
    val exceptions: StateFlow<UiState<List<AttendanceExceptionDto>>> = _exceptions.asStateFlow()

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
                if (event is DomainWebSocketEvent.AttendanceUpdate || event is DomainWebSocketEvent.Generic) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        loadRecords()
        loadExceptions()
    }

    fun loadRecords(from: String = Dates.isoDaysAgo(30), to: String = Dates.todayIso()) {
        viewModelScope.launch {
            _records.value = UiState.Loading
            _records.value = repository.getAttendanceRecords(from = from, to = to).toUiState()
        }
    }

    fun loadExceptions() {
        viewModelScope.launch {
            _exceptions.value = UiState.Loading
            _exceptions.value = repository.getExceptions().toUiState()
        }
    }

    fun resolve(exceptionId: String, correctedStatus: String, reason: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.resolveException(
                    exceptionId,
                    correctedStatus,
                    reason
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Exception resolved as ${correctedStatus.lowercase().replace('_', ' ')}."
                    refresh()
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
