package com.als.presence.ui.dashboard.owner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Money
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
    private val repository: OperationsRepository
) : ViewModel() {

    private val _periods = MutableStateFlow<UiState<List<PayrollPeriodDto>>>(UiState.Loading)
    val periods: StateFlow<UiState<List<PayrollPeriodDto>>> = _periods.asStateFlow()

    private val _items = MutableStateFlow<UiState<List<PayrollItemDto>>>(UiState.Idle)
    val items: StateFlow<UiState<List<PayrollItemDto>>> = _items.asStateFlow()

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

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
    }

    fun loadPeriods() {
        viewModelScope.launch {
            _periods.value = UiState.Loading
            _periods.value = repository.getPayrollPeriods().toUiState()
            val periods = (_periods.value as? UiState.Success)?.data.orEmpty()
            val current = _selectedPeriodId.value
            _selectedPeriodId.value = when {
                periods.isEmpty() -> null
                current != null && periods.any { it.id == current } -> current
                else -> periods.first().id
            }
            _selectedPeriodId.value?.let { loadItems(it) }
        }
    }

    fun loadItems(periodId: String) {
        viewModelScope.launch {
            _items.value = UiState.Loading
            _items.value = repository.getPayrollItems(periodId).toUiState()
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            _staff.value = UiState.Loading
            _staff.value = repository.getStaff().toUiState()
        }
    }

    fun selectPeriod(periodId: String) {
        _selectedPeriodId.value = periodId
        loadItems(periodId)
    }

    fun calculatePayroll(periodId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.calculatePayroll(periodId)) {
                is Resource.Success -> {
                    _message.value = "Payroll recalculated for the selected period."
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
                    _selectedPeriodId.value?.let { loadItems(it) }
                    loadStaff()
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
