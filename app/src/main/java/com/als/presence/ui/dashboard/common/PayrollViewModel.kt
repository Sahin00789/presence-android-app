package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.StaffEarningLedgerDto
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
    private val repository: OperationsRepository,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
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

    private val _staffLedgerEntriesMap = MutableStateFlow<Map<String, List<StaffEarningLedgerDto>>>(emptyMap())
    val staffLedgerEntriesMap: StateFlow<Map<String, List<StaffEarningLedgerDto>>> = _staffLedgerEntriesMap.asStateFlow()

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
            loadMembers()
        }
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

    fun loadStaffEarningLedger(staffProfileId: String, month: Int, year: Int) {
        if (staffProfileId.isBlank()) return
        val key = "$staffProfileId-$year-$month"
        viewModelScope.launch {
            val res = repository.getStaffMonthlyEarningLedger(staffProfileId, month, year)
            if (res is Resource.Success) {
                _staffLedgerEntriesMap.value = _staffLedgerEntriesMap.value + (key to res.data.entries)
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun recordPayment(
        itemId: String,
        amountPaise: Long,
        paymentMethod: String = "BANK_TRANSFER",
        reference: String = ""
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.recordPayrollTransaction(
                com.als.presence.data.model.RecordPayrollTransactionRequest(
                    monthlyPayrollId = itemId,
                    amount = amountPaise.toDouble(),
                    paymentDate = java.time.LocalDate.now().toString(),
                    paymentMethod = paymentMethod,
                    reference = reference.ifBlank { null }
                )
            )
            _isSubmitting.value = false
            when (result) {
                is Resource.Success -> {
                    _message.value = "Payment recorded successfully!"
                    refresh()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to record payment"
                }
                else -> {}
            }
        }
    }

    fun downloadPayslip(context: android.content.Context, month: Int, year: Int, staffProfileId: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isSubmitting.value = true
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
            _isSubmitting.value = false
        }
    }
}
