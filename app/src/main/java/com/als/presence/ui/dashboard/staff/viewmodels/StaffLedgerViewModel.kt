package com.als.presence.ui.dashboard.staff.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.EmployeeDto
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.data.model.DomainWebSocketEvent
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Staff "Ledger" tab: append-only ledger entries, balances and payslips. */
@HiltViewModel
class StaffLedgerViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _summary = MutableStateFlow<UiState<EmployeeDto>>(UiState.Loading)
    val summary: StateFlow<UiState<EmployeeDto>> = _summary.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<UiState<List<StaffEarningLedgerDto>>>(UiState.Loading)
    val ledgerEntries: StateFlow<UiState<List<StaffEarningLedgerDto>>> = _ledgerEntries.asStateFlow()

    private val _payslips = MutableStateFlow<UiState<List<PayslipDto>>>(UiState.Loading)
    val payslips: StateFlow<UiState<List<PayslipDto>>> = _payslips.asStateFlow()

    init {
        refresh()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                if (event is DomainWebSocketEvent.WalletUpdate || event is DomainWebSocketEvent.Generic) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        loadSummary()
        loadLedger()
        loadPayslips()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = UiState.Loading
            when (val res = repository.getEmployees()) {
                is Resource.Success<*> -> {
                    val list = res.data as? List<EmployeeDto>
                    val me = list?.firstOrNull()
                    if (me != null) {
                        _summary.value = UiState.Success(me)
                    } else {
                        _summary.value = UiState.Error("Profile not found")
                    }
                }
                is Resource.Error -> _summary.value = UiState.Error(res.message ?: "Could not load profile")
                Resource.Loading -> Unit
            }
        }
    }

    fun loadLedger() {
        viewModelScope.launch {
            _ledgerEntries.value = UiState.Loading
            _ledgerEntries.value = repository.getEarningLedger().toUiState()
        }
    }

    fun loadPayslips() {
        viewModelScope.launch {
            _payslips.value = UiState.Loading
            _payslips.value = repository.getMyPayslips().toUiState()
        }
    }
}
