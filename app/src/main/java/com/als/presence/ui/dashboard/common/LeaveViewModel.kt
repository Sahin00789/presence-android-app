package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
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
    private val repository: OperationsRepository
) : ViewModel() {

    private val _leaves = MutableStateFlow<UiState<List<LeaveDto>>>(UiState.Loading)
    val leaves: StateFlow<UiState<List<LeaveDto>>> = _leaves.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _leaves.value = UiState.Loading
            _leaves.value = repository.getLeaves().toUiState()
        }
    }

    fun requestLeave(
        startDate: String,
        endDate: String,
        type: String,
        reason: String,
        employeeId: String? = null,
        shiftName: String? = null,
        shiftAssignmentId: String? = null,
        shiftTemplateId: String? = null
    ) {
        if (startDate.isBlank() || endDate.isBlank()) {
            _message.value = "Choose a start and end date."
            return
        }
        if (reason.isBlank()) {
            _message.value = "Add a short reason for the leave."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.requestLeave(
                    LeaveRequestCreate(
                        shiftAssignmentId = shiftAssignmentId.orEmpty(),
                        workDate = startDate,
                        reason = buildString {
                            if (!shiftName.isNullOrBlank()) append("Shift: ").append(shiftName).append(". ")
                            append(reason.trim())
                        }
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Leave request submitted for review."
                    refresh()
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
