package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ResolveExceptionRequest
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.als.presence.data.remote.DomainWebSocketEvent
import com.als.presence.data.remote.EventsWebSocketService

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _premises = MutableStateFlow<UiState<List<PremisesDto>>>(UiState.Loading)
    val premises: StateFlow<UiState<List<PremisesDto>>> = _premises.asStateFlow()

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

    private val _selectedPremisesId = MutableStateFlow<String?>(null)
    val selectedPremisesId: StateFlow<String?> = _selectedPremisesId.asStateFlow()

    private val _presence = MutableStateFlow<UiState<LocationTodayPresenceResponseDto>>(UiState.Loading)
    val presence: StateFlow<UiState<LocationTodayPresenceResponseDto>> = _presence.asStateFlow()

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
                if (event is DomainWebSocketEvent.PremisesUpdate ||
                    event is DomainWebSocketEvent.AttendanceUpdate ||
                    event is DomainWebSocketEvent.StaffUpdate ||
                    event is DomainWebSocketEvent.Generic
                ) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _premises.value = UiState.Loading
            when (val res = repository.getPremises()) {
                is Resource.Success -> {
                    _premises.value = UiState.Success(res.data)
                    if (_selectedPremisesId.value == null && res.data.isNotEmpty()) {
                        _selectedPremisesId.value = res.data.first().id
                    }
                    loadPresence(_selectedPremisesId.value)
                }
                is Resource.Error -> {
                    _premises.value = UiState.Error(res.message ?: "Failed to load premises")
                }
                is Resource.Loading -> {
                    _premises.value = UiState.Loading
                }
            }

            when (val staffRes = repository.getStaff()) {
                is Resource.Success -> _staff.value = UiState.Success(staffRes.data)
                is Resource.Error -> _staff.value = UiState.Error(staffRes.message ?: "Failed to load staff")
                is Resource.Loading -> _staff.value = UiState.Loading
            }
        }
    }

    fun selectPremises(id: String) {
        _selectedPremisesId.value = id
        loadPresence(id)
    }

    fun loadPresence(premisesId: String? = _selectedPremisesId.value) {
        viewModelScope.launch {
            _presence.value = UiState.Loading
            when (val res = repository.getTodayLocationPresence(premisesId = premisesId)) {
                is Resource.Success -> {
                    _presence.value = UiState.Success(res.data)
                    if (res.data.premisesId != null) {
                        _selectedPremisesId.value = res.data.premisesId
                    }
                }
                is Resource.Error -> {
                    _presence.value = UiState.Error(res.message ?: "Failed to load location presence")
                }
                is Resource.Loading -> {
                    _presence.value = UiState.Loading
                }
            }
        }
    }

    fun resolveGpsException(exceptionId: String, status: String, reason: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.resolveException(exceptionId, ResolveExceptionRequest(correctedStatus = status, reason = reason))) {
                is Resource.Success -> {
                    _message.value = "GPS Exception resolved ($status)."
                    loadPresence()
                }
                is Resource.Error -> {
                    _message.value = res.message ?: "Failed to resolve exception"
                }
                is Resource.Loading -> {}
            }
            _isSubmitting.value = false
        }
    }

    fun assignStaffPremises(staffProfileId: String, premisesId: String?) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.assignStaffPremises(staffProfileId, premisesId)) {
                is Resource.Success -> {
                    _message.value = "Assigned location updated successfully."
                    refresh()
                }
                is Resource.Error -> {
                    _message.value = res.message ?: "Failed to assign premises"
                }
                is Resource.Loading -> {}
            }
            _isSubmitting.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
