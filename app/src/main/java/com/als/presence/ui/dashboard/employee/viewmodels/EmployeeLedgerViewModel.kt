package com.als.presence.ui.dashboard.employee.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.DomainWebSocketEvent
import com.als.presence.data.model.EmployeeDto
import com.als.presence.data.model.MonthlyEarningLedgerResponseDto
import com.als.presence.data.model.MyOverallPayrollSummaryDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Employee "Ledger" tab: append-only ledger entries, overall summary, and monthly ledger breakdown. */
@HiltViewModel
class EmployeeLedgerViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val webSocketService: EventsWebSocketService,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _summary = MutableStateFlow<UiState<EmployeeDto>>(UiState.Loading)
    val summary: StateFlow<UiState<EmployeeDto>> = _summary.asStateFlow()

    private val _overallSummary = MutableStateFlow<UiState<MyOverallPayrollSummaryDto>>(UiState.Loading)
    val overallSummary: StateFlow<UiState<MyOverallPayrollSummaryDto>> = _overallSummary.asStateFlow()

    private val _monthlyLedger = MutableStateFlow<UiState<MonthlyEarningLedgerResponseDto>>(UiState.Loading)
    val monthlyLedger: StateFlow<UiState<MonthlyEarningLedgerResponseDto>> = _monthlyLedger.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<UiState<List<StaffEarningLedgerDto>>>(UiState.Loading)
    val ledgerEntries: StateFlow<UiState<List<StaffEarningLedgerDto>>> = _ledgerEntries.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isDownloadingPayslip = MutableStateFlow(false)
    val isDownloadingPayslip: StateFlow<Boolean> = _isDownloadingPayslip.asStateFlow()

    init {
        refresh()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                if (event is DomainWebSocketEvent.WalletUpdate || event is DomainWebSocketEvent.AttendanceUpdate || event is DomainWebSocketEvent.Generic) {
                    refresh()
                }
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
        loadMonthlyLedger(month.monthValue, month.year)
    }

    fun refresh() {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.PAYROLL)
            loadSummary()
            loadOverallSummary()
            val ym = _selectedMonth.value
            loadMonthlyLedger(ym.monthValue, ym.year)
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summary.value = UiState.Loading
            when (val res = repository.getEmployees()) {
                is Resource.Success -> {
                    val me = res.data.firstOrNull()
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

    fun loadOverallSummary() {
        viewModelScope.launch {
            _overallSummary.value = UiState.Loading
            _overallSummary.value = repository.getMyOverallPayrollSummary().toUiState()
        }
    }

    fun loadMonthlyLedger(month: Int = _selectedMonth.value.monthValue, year: Int = _selectedMonth.value.year) {
        viewModelScope.launch {
            _monthlyLedger.value = UiState.Loading
            _ledgerEntries.value = UiState.Loading
            val res = repository.getMyMonthlyEarningLedger(month, year)
            _monthlyLedger.value = res.toUiState()
            if (res is Resource.Success) {
                _ledgerEntries.value = UiState.Success(res.data.entries)
            } else if (res is Resource.Error) {
                _ledgerEntries.value = UiState.Error(res.message ?: "Could not load ledger entries")
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun downloadPayslip(context: android.content.Context, month: Int, year: Int, staffProfileId: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isDownloadingPayslip.value = true
            when (val res = repository.downloadPayslipPdf(month, year, staffProfileId)) {
                is Resource.Success -> {
                    try {
                        val body = res.data
                        val monthNames = arrayOf(
                            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                        )
                        val mName = monthNames.getOrNull(month - 1) ?: "Month$month"
                        val fileName = "Payslip_${mName}_${year}.pdf"

                        val targetFile = java.io.File(
                            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        body.byteStream().use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        val authority = "${context.packageName}.fileprovider"
                        val contentUri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, targetFile)

                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, "Open Payslip PDF"))
                            } catch (_: Exception) {
                                _message.value = "Payslip saved to downloads: $fileName"
                            }
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _message.value = "Error saving PDF: ${e.message}"
                        }
                    }
                }
                is Resource.Error -> {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _message.value = res.message ?: "Failed to download payslip"
                    }
                }
                else -> {}
            }
            _isDownloadingPayslip.value = false
        }
    }
}
