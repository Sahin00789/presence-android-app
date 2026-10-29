package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Payroll console shared by the Owner "Salary" tab, Manager payroll screen,
 * and the dedicated Salary Payment Console screen.
 */
@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val repository: OperationsRepository
) : ViewModel() {

    private val _periods = MutableStateFlow<UiState<List<PayrollPeriodDto>>>(UiState.Loading)
    val periods: StateFlow<UiState<List<PayrollPeriodDto>>> = _periods.asStateFlow()

    private val _items = MutableStateFlow<UiState<List<PayrollItemDto>>>(UiState.Idle)
    val items: StateFlow<UiState<List<PayrollItemDto>>> = _items.asStateFlow()

    private val _payrollItemsMap = MutableStateFlow<Map<String, List<PayrollItemDto>>>(emptyMap())
    val payrollItemsMap: StateFlow<Map<String, List<PayrollItemDto>>> = _payrollItemsMap.asStateFlow()

    private val _employee = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _employee.asStateFlow()

    private val _members = MutableStateFlow<UiState<List<OrganizationMemberDto>>>(UiState.Loading)
    val members: StateFlow<UiState<List<OrganizationMemberDto>>> = _members.asStateFlow()

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
        loadPeriods()
        loadStaff()
        loadMembers()
    }

    fun loadStaff() {
        viewModelScope.launch {
            _employee.value = UiState.Loading
            _employee.value = repository.getEmployees().toUiState()
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _members.value = UiState.Loading
            _members.value = repository.getMembers().toUiState()
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

            val itemsMap = mutableMapOf<String, List<PayrollItemDto>>()
            generatedPeriods.forEach { period ->
                val y = period.periodStartDate.take(4).toIntOrNull() ?: now.year
                val m = period.periodStartDate.substring(5, 7).toIntOrNull() ?: now.monthValue
                val itemsRes = repository.getMonthlyPayrollSummary(m, y)
                if (itemsRes is Resource.Success) {
                    itemsMap[period.id] = itemsRes.data
                }
            }
            _payrollItemsMap.value = itemsMap

            val current = _selectedPeriodId.value
            val next = generatedPeriods.firstOrNull { it.id == current } ?: generatedPeriods.firstOrNull()
            if (next != null) {
                selectPeriod(next.id)
            } else {
                _items.value = UiState.Success(emptyList())
            }
        }
    }

    fun selectPeriod(periodId: String) {
        _selectedPeriodId.value = periodId
        viewModelScope.launch {
            _items.value = UiState.Loading
            val y = periodId.take(4).toIntOrNull() ?: java.time.LocalDate.now().year
            val m = periodId.substring(5, 7).toIntOrNull() ?: java.time.LocalDate.now().monthValue
            _items.value = repository.getMonthlyPayrollSummary(m, y).toUiState()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
