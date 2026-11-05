package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AssignedShiftIdWithDate
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.UpcomingShiftDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State for the shared leave request / review screen. */
@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    private val _leaves = MutableStateFlow<UiState<List<LeaveDto>>>(UiState.Loading)
    val leaves: StateFlow<UiState<List<LeaveDto>>> = _leaves.asStateFlow()

    private val _upcomingShifts = MutableStateFlow<UiState<List<UpcomingShiftDto>>>(UiState.Loading)
    val upcomingShifts: StateFlow<UiState<List<UpcomingShiftDto>>> = _upcomingShifts.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
        loadUpcomingShifts()
    }

    fun refresh() {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.LEAVES)
            _leaves.value = UiState.Loading
            _leaves.value = repository.getLeaves().toUiState()
        }
    }

    fun loadUpcomingShifts() {
        viewModelScope.launch {
            _upcomingShifts.value = UiState.Loading
            _upcomingShifts.value = repository.getUpcomingShifts().toUiState()
        }
    }

    fun requestLeave(
        selectedShifts: List<UpcomingShiftDto>,
        reason: String
    ) {
        if (selectedShifts.isEmpty()) {
            _message.value = "Please select at least one upcoming shift."
            return
        }
        if (reason.isBlank()) {
            _message.value = "Please enter a reason for the leave."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val items = selectedShifts.map {
                AssignedShiftIdWithDate(
                    shiftAssignmentId = it.shiftAssignmentId,
                    workDate = it.workDate
                )
            }
            val request = LeaveRequestCreate(
                assignedShiftIdWithDate = items,
                assignedshiftidwithdate = items,
                reason = reason.trim()
            )
            when (val result = repository.requestLeave(request)) {
                is Resource.Success -> {
                    _message.value = "Leave request(s) submitted for review."
                    refresh()
                    loadUpcomingShifts()
                }
                is Resource.Error -> _message.value = result.message ?: "Could not submit leave request"
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun review(id: String, decision: String, note: String?) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.reviewLeave(id, decision, note)) {
                is Resource.Success -> {
                    _message.value = if (decision == "APPROVED") {
                        "Leave approved."
                    } else {
                        "Leave rejected."
                    }
                    refresh()
                }
                is Resource.Error -> _message.value = result.message ?: "Could not review leave request"
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
