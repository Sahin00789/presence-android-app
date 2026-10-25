package com.als.presence.ui.dashboard.staff.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.UiState
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.WalletSummaryDto
import com.als.presence.data.model.WalletTransactionDto
import com.als.presence.data.remote.DomainWebSocketEvent
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Staff "Wallet" tab: ledger-derived balance, transactions and payslips. */
@HiltViewModel
class StaffWalletViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _summary = MutableStateFlow<UiState<WalletSummaryDto>>(UiState.Loading)
    val summary: StateFlow<UiState<WalletSummaryDto>> = _summary.asStateFlow()

    private val _transactions = MutableStateFlow<UiState<List<WalletTransactionDto>>>(UiState.Loading)
    val transactions: StateFlow<UiState<List<WalletTransactionDto>>> = _transactions.asStateFlow()

    private val _payslips = MutableStateFlow<UiState<List<PayslipDto>>>(UiState.Loading)
    val payslips: StateFlow<UiState<List<PayslipDto>>> = _payslips.asStateFlow()

    init {
        refresh()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                if (event is DomainWebSocketEvent.WalletUpdate) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        loadSummary()
        loadTransactions()
        loadPayslips()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = UiState.Loading
            _summary.value = repository.getWalletSummary().toUiState()
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = UiState.Loading
            _transactions.value = repository.getWalletTransactions().toUiState()
        }
    }

    fun loadPayslips() {
        viewModelScope.launch {
            _payslips.value = UiState.Loading
            _payslips.value = repository.getMyPayslips().toUiState()
        }
    }
}
