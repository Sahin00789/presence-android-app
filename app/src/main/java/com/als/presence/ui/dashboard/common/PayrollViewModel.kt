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

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

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
            _staff.value = UiState.Loading
            _staff.value = repository.getStaff().toUiState()
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
            when (val result = repository.getPayrollPeriods()) {
                is Resource.Success -> {
                    _periods.value = UiState.Success(result.data)
                    val itemsMap = mutableMapOf<String, List<PayrollItemDto>>()
                    result.data.forEach { period ->
                        val itemsRes = repository.getPayrollItems(period.id)
                        if (itemsRes is Resource.Success) {
                            itemsMap[period.id] = itemsRes.data
                        }
                    }
                    _payrollItemsMap.value = itemsMap

                    val current = _selectedPeriodId.value
                    val next = result.data.firstOrNull { it.id == current } ?: result.data.firstOrNull()
                    if (next != null) {
                        selectPeriod(next.id)
                    } else {
                        _items.value = UiState.Success(emptyList())
                    }
                }
                is Resource.Error ->
                    _periods.value = UiState.Error(result.message ?: "Could not load payroll periods")
                Resource.Loading -> Unit
            }
        }
    }

    fun createPeriod(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.createPayrollPeriod(startDate, endDate)) {
                is Resource.Success -> {
                    _message.value = "Payroll period generated."
                    _selectedPeriodId.value = result.data.id
                    selectPeriod(result.data.id)
                    loadPeriods()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun selectPeriod(periodId: String) {
        _selectedPeriodId.value = periodId
        viewModelScope.launch {
            _items.value = UiState.Loading
            _items.value = repository.getPayrollItems(periodId).toUiState()
        }
    }

    fun calculatePayroll(periodId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.calculatePayroll(periodId)) {
                is Resource.Success -> {
                    _message.value = "Payroll calculated."
                    selectPeriod(periodId)
                    loadPeriods()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun approvePayroll(periodId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.approvePayroll(periodId)) {
                is Resource.Success -> {
                    _message.value = "Payroll approved and locked."
                    loadPeriods()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun recordPayment(itemId: String, amountCents: Long, paymentMethod: String = "BANK_TRANSFER", reference: String = "") {
        val method = paymentMethod.ifBlank { "BANK_TRANSFER" }
        val ref = reference.trim().ifEmpty { "Salary Payment via $method" }
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.recordPayment(
                    itemId = itemId,
                    amountCents = amountCents,
                    paymentMethod = method,
                    referenceNumber = ref
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Payment of ${Money.format(amountCents)} recorded."
                    loadPeriods()
                    loadStaff()
                    loadMembers()
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
