package com.als.presence.ui.dashboard.owner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Owner "Salary" tab state management. */
@HiltViewModel
class OwnerSalaryViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    private val _periods = MutableStateFlow<UiState<List<PayrollPeriodDto>>>(UiState.Loading)
    val periods: StateFlow<UiState<List<PayrollPeriodDto>>> = _periods.asStateFlow()

    private val _items = MutableStateFlow<UiState<List<PayrollItemDto>>>(UiState.Idle)
    val items: StateFlow<UiState<List<PayrollItemDto>>> = _items.asStateFlow()

    private val _employee = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _employee.asStateFlow()

    private val _selectedPeriodId = MutableStateFlow<String?>(null)
    val selectedPeriodId: StateFlow<String?> = _selectedPeriodId.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.PAYROLL)
            loadPeriods()
            loadStaff()
        }
    }

    fun loadPeriods() {
        viewModelScope.launch {
            _periods.value = UiState.Loading
            val now = java.time.LocalDate.now()
            val generatedPeriods = (0..5).map { i ->
                val date = now.minusMonths(i.toLong())
                val y = date.year
                val m = date.monthValue
                val start = "$y-${m.toString().padStart(2, '0')}-01"
                val end = "$y-${m.toString().padStart(2, '0')}-${date.lengthOfMonth()}"
                PayrollPeriodDto(
                    id = "$y-${m.toString().padStart(2, '0')}",
                    organizationId = "",
                    periodStartDate = start,
                    periodEndDate = end,
                    status = if (i == 0) "OPEN" else "PAID"
                )
            }
            _periods.value = UiState.Success(generatedPeriods)
            val current = _selectedPeriodId.value
            val next = generatedPeriods.firstOrNull { it.id == current } ?: generatedPeriods.firstOrNull()
            if (next != null) {
                selectPeriod(next.id)
            } else {
                _items.value = UiState.Success(emptyList())
            }
        }
    }

    fun loadItems(periodId: String) {
        viewModelScope.launch {
            _items.value = UiState.Loading
            val y = periodId.take(4).toIntOrNull() ?: java.time.LocalDate.now().year
            val m = periodId.substring(5, 7).toIntOrNull() ?: java.time.LocalDate.now().monthValue
            _items.value = repository.getMonthlyPayrollSummary(m, y).toUiState()
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            _employee.value = UiState.Loading
            _employee.value = repository.getEmployees().toUiState()
        }
    }

    fun selectPeriod(periodId: String) {
        _selectedPeriodId.value = periodId
        loadItems(periodId)
    }

    fun consumeMessage() {
        _message.value = null
    }
}
