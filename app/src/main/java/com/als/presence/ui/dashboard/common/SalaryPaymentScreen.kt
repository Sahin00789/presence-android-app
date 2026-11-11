package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PaymentRecordDto
import com.als.presence.data.model.PayrollBreakdownDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.StaffEarningLedgerDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Dedicated Salary Payment Console screen.
 * Loads workforce employees and managers directly with monthly calculation cards from joining date,
 * month card pay buttons, and monthly date-separated earning ledger view.
 */
@Composable
fun SalaryPaymentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayrollViewModel = hiltViewModel()
) {
    val staffState by viewModel.staff.collectAsStateWithLifecycle()
    val membersState by viewModel.members.collectAsStateWithLifecycle()
    val periodsState by viewModel.periods.collectAsStateWithLifecycle()
    val payrollItemsMap by viewModel.payrollItemsMap.collectAsStateWithLifecycle()
    val staffLedgerMap by viewModel.staffLedgerEntriesMap.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "PAYROLL & FINANCE",
            title = "Salary Payments",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            SalaryPaymentScreenContent(
                staffState = staffState,
                membersState = membersState,
                payrollPeriodsState = periodsState,
                payrollItemsMap = payrollItemsMap,
                staffLedgerMap = staffLedgerMap,
                isSubmitting = isSubmitting,
                onRecordPayment = viewModel::recordPayment,
                onLoadStaffLedger = viewModel::loadStaffEarningLedger,
                onDownloadPayslip = viewModel::downloadPayslip
            )
        }

        MessageSnackbar(
            message = activeMessage,
            onDismiss = { activeMessage = null }
        )
    }
}

@Composable
fun SalaryPaymentScreenContent(
    staffState: UiState<List<StaffDto>>,
    membersState: UiState<List<OrganizationMemberDto>>,
    payrollPeriodsState: UiState<List<PayrollPeriodDto>>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    isSubmitting: Boolean,
    onRecordPayment: (itemId: String, amountPaise: Long, paymentMethod: String, reference: String) -> Unit,
    staffLedgerMap: Map<String, List<StaffEarningLedgerDto>> = emptyMap(),
    onLoadStaffLedger: ((staffProfileId: String, month: Int, year: Int) -> Unit)? = null,
    onDownloadPayslip: ((context: android.content.Context, month: Int, year: Int, staffProfileId: String?) -> Unit)? = null
) {
    val staff = (staffState as? UiState.Success)?.data.orEmpty().filter { it.id.isNotBlank() }.distinctBy { it.id }
    val periods = (payrollPeriodsState as? UiState.Success)?.data.orEmpty()

    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var monthPayTarget by remember { mutableStateOf<SalaryMonthPayTarget?>(null) }
    var ledgerTarget by remember { mutableStateOf<SalaryLedgerTarget?>(null) }

    val filteredStaff = staff.filter { member ->
        searchQuery.isBlank() ||
            member.fullName.contains(searchQuery, ignoreCase = true) ||
            member.staffCode.contains(searchQuery, ignoreCase = true) ||
            (member.department?.contains(searchQuery, ignoreCase = true) == true) ||
            member.email.contains(searchQuery, ignoreCase = true)
    }

    val totalEarnedPaise = filteredStaff.sumOf { it.earnedBalancePaise }
    val totalPaidPaise = filteredStaff.sumOf { it.paidBalancePaise }
    val totalDuePaise = (totalEarnedPaise - totalPaidPaise).coerceAtLeast(0L)

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy") }
    val givenPaymentTransactions = remember(periods, payrollItemsMap) {
        val list = mutableListOf<GivenPaymentTransaction>()
        payrollItemsMap.forEach { (periodId, items) ->
            items.forEach { item ->
                val payment = item.payment
                if (payment != null && payment.amountPaise > 0) {
                    val periodYm = try { YearMonth.parse(periodId.take(7)) } catch (_: Exception) { YearMonth.now() }
                    val label = periodYm.format(monthFormatter)
                    list.add(
                        GivenPaymentTransaction(
                            id = payment.id.ifBlank { item.id },
                            staffName = item.staffName ?: "Staff Member",
                            staffCode = item.staffCode,
                            monthLabel = label,
                            amountPaise = payment.amountPaise,
                            paymentMethod = payment.paymentMethod.ifBlank { "BANK_TRANSFER" },
                            referenceNumber = payment.referenceNumber,
                            paymentDate = payment.paymentDate
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.paymentDate }
    }
    val totalGivenPaise = givenPaymentTransactions.sumOf { it.amountPaise }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Payments Given This Month / Disbursement Transactions Section
        SectionHeader(
            title = "Payments Given This Month",
            trailing = {
                Text(
                    text = "${givenPaymentTransactions.size} payments • ${Money.format(totalGivenPaise)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryEmerald
                )
            }
        )

        if (givenPaymentTransactions.isEmpty()) {
            SectionEmpty("No salary payment transactions recorded yet.")
        } else {
            givenPaymentTransactions.forEach { transaction ->
                PaymentTransactionRow(transaction = transaction)
            }
        }

        // Compact Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search name, code or email...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionHeader(
            title = "Employee Salary Roster",
            trailing = {
                Text(
                    text = "${filteredStaff.size} employees",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        when (staffState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading employee roster…")
            is UiState.Error -> SectionError(staffState.message)
            is UiState.Success -> {
                if (filteredStaff.isEmpty()) {
                    SectionEmpty(
                        if (searchQuery.isNotBlank()) "No employees found matching “$searchQuery”."
                        else "No employees onboarded."
                    )
                } else {
                    filteredStaff.forEach { member ->
                        SalaryEmployeeCard(
                            staff = member,
                            payrollPeriods = periods,
                            payrollItemsMap = payrollItemsMap,
                            onPayMonth = { cardData ->
                                val targetPeriodId = "${cardData.year}-${cardData.month.toString().padStart(2, '0')}"
                                val itemId = cardData.payrollItemId?.ifBlank { null }
                                    ?: payrollItemsMap[targetPeriodId]?.firstOrNull { item ->
                                        item.employeeId == member.id || item.staffProfileId == member.id || item.resolvedEmployeeId == member.id ||
                                        (!member.email.isBlank() && item.staffEmail.equals(member.email, ignoreCase = true)) ||
                                        (!member.staffCode.isNullOrBlank() && item.resolvedStaffCode.equals(member.staffCode, ignoreCase = true))
                                    }?.let { item -> item.monthlyPayrollId.ifBlank { item.id } }
                                    ?: periods.firstNotNullOfOrNull { period ->
                                        payrollItemsMap[period.id]?.firstOrNull { item ->
                                            item.employeeId == member.id || item.staffProfileId == member.id || item.resolvedEmployeeId == member.id ||
                                            (!member.email.isBlank() && item.staffEmail.equals(member.email, ignoreCase = true)) ||
                                            (!member.staffCode.isNullOrBlank() && item.resolvedStaffCode.equals(member.staffCode, ignoreCase = true))
                                        }
                                    }?.let { item -> item.monthlyPayrollId.ifBlank { item.id } }

                                if (!itemId.isNullOrBlank()) {
                                    monthPayTarget = SalaryMonthPayTarget(
                                        payrollItemId = itemId,
                                        staffName = member.fullName,
                                        staffCode = member.staffCode,
                                        monthLabel = cardData.monthYearLabel,
                                        earnedPaise = cardData.earnedPaise,
                                        paidPaise = cardData.paidPaise,
                                        breakdown = cardData.breakdown,
                                        attendanceSummary = cardData.attendanceSummary,
                                        paymentHistory = cardData.paymentHistory
                                    )
                                }
                            },
                            onOpenLedger = { cardData ->
                                val targetPeriodId = "${cardData.year}-${cardData.month.toString().padStart(2, '0')}"
                                val itemId = cardData.payrollItemId?.ifBlank { null }
                                    ?: payrollItemsMap[targetPeriodId]?.firstOrNull { item ->
                                        item.employeeId == member.id || item.staffProfileId == member.id || item.resolvedEmployeeId == member.id ||
                                        (!member.email.isBlank() && item.staffEmail.equals(member.email, ignoreCase = true)) ||
                                        (!member.staffCode.isNullOrBlank() && item.resolvedStaffCode.equals(member.staffCode, ignoreCase = true))
                                    }?.let { item -> item.monthlyPayrollId.ifBlank { item.id } }
                                    ?: periods.firstNotNullOfOrNull { period ->
                                        payrollItemsMap[period.id]?.firstOrNull { item ->
                                            item.employeeId == member.id || item.staffProfileId == member.id || item.resolvedEmployeeId == member.id ||
                                            (!member.email.isBlank() && item.staffEmail.equals(member.email, ignoreCase = true)) ||
                                            (!member.staffCode.isNullOrBlank() && item.resolvedStaffCode.equals(member.staffCode, ignoreCase = true))
                                        }
                                    }?.let { item -> item.monthlyPayrollId.ifBlank { item.id } }

                                val m = cardData.month
                                val y = cardData.year
                                onLoadStaffLedger?.invoke(member.id, m, y)

                                ledgerTarget = SalaryLedgerTarget(
                                    payrollItemId = if (!itemId.isNullOrBlank()) itemId else null,
                                    staffProfileId = member.id,
                                    staffName = member.fullName,
                                    staffCode = member.staffCode,
                                    monthLabel = cardData.monthYearLabel,
                                    month = m,
                                    year = y,
                                    earnedPaise = cardData.earnedPaise,
                                    paidPaise = cardData.paidPaise,
                                    attendanceSummary = cardData.attendanceSummary,
                                    breakdown = cardData.breakdown,
                                    paymentHistory = cardData.paymentHistory
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    monthPayTarget?.let { target ->
        PayStaffModal(
            staffName = target.staffName,
            staffCode = target.staffCode,
            monthLabel = target.monthLabel,
            earnedPaise = target.earnedPaise,
            paidPaise = target.paidPaise,
            breakdown = target.breakdown,
            attendanceSummary = target.attendanceSummary,
            paymentHistory = target.paymentHistory,
            isSubmitting = isSubmitting,
            onDismiss = { if (!isSubmitting) monthPayTarget = null },
            onConfirmPay = { amountPaise, method, reference ->
                onRecordPayment(target.payrollItemId, amountPaise, method, reference)
                monthPayTarget = null
            }
        )
    }

    ledgerTarget?.let { target ->
        val key = "${target.staffProfileId}-${target.year}-${target.month}"
        val targetEntries = staffLedgerMap[key].orEmpty()

        MonthlyEarningLedgerModal(
            staffName = target.staffName,
            staffCode = target.staffCode,
            monthLabel = target.monthLabel,
            earnedPaise = target.earnedPaise,
            paidPaise = target.paidPaise,
            attendanceSummary = target.attendanceSummary,
            breakdown = target.breakdown,
            paymentHistory = target.paymentHistory,
            entries = targetEntries,
            isSubmitting = isSubmitting,
            onDismiss = { ledgerTarget = null },
            onRecordPayment = if (!target.payrollItemId.isNullOrBlank()) {
                { amount, method, ref ->
                    onRecordPayment(target.payrollItemId, amount, method, ref)
                    ledgerTarget = null
                }
            } else null,
            onDownloadPayslip = if (onDownloadPayslip != null) {
                { onDownloadPayslip(context, target.month, target.year, target.staffProfileId) }
            } else null
        )
    }
}

private data class SalaryMonthPayTarget(
    val payrollItemId: String,
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val earnedPaise: Long,
    val paidPaise: Long,
    val paymentHistory: List<PaymentRecordDto> = emptyList(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto()
)

private data class SalaryLedgerTarget(
    val payrollItemId: String? = null,
    val staffProfileId: String? = null,
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val month: Int = 1,
    val year: Int = 2025,
    val earnedPaise: Long,
    val paidPaise: Long = 0L,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val paymentHistory: List<PaymentRecordDto> = emptyList()
)

@Composable
private fun SalaryEmployeeCard(
    staff: StaffDto,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onPayMonth: (SalaryMonthlyCardData) -> Unit,
    onOpenLedger: (SalaryMonthlyCardData) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(name = staff.fullName)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        staff.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(staff.staffCode)
                            staff.department?.let { append(" • ").append(it) }
                            staff.premisesName?.let { append(" • ").append(Dates.cleanPremisesName(it)) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(if (staff.isActive) "ACTIVE" else "CANCELLED")
            }

            val totalEarned = staff.earnedBalancePaise
            val totalPaid = staff.paidBalancePaise
            val dueUnpaid = (totalEarned - totalPaid).coerceAtLeast(0L)

            // Salary Summary Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(totalEarned), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(totalPaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(dueUnpaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                    }
                }
            }

            // Monthly Salary Cards from joining date
            val monthlyCards = remember(staff, payrollPeriods, payrollItemsMap) {
                computeSalaryMonthlyCards(
                    joiningDateIso = staff.hireDate,
                    employeeId = staff.id,
                    email = staff.email,
                    staffCode = staff.staffCode,
                    earnedBalancePaise = staff.earnedBalancePaise,
                    payrollPeriods = payrollPeriods,
                    payrollItemsMap = payrollItemsMap
                )
            }

            if (monthlyCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Monthly Salary History",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthlyCards.size, key = { monthlyCards[it].monthYearLabel }) { index ->
                            val cardData = monthlyCards[index]
                            SalaryMonthlyCardItem(
                                cardData = cardData,
                                onPay = { onPayMonth(cardData) },
                                onOpenLedger = { onOpenLedger(cardData) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryManagerCard(
    member: OrganizationMemberDto,
    matchingStaff: StaffDto?,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onPayMonth: (SalaryMonthlyCardData) -> Unit,
    onOpenLedger: (SalaryMonthlyCardData) -> Unit
) {
    val totalEarned = matchingStaff?.earnedBalancePaise ?: member.earnedBalancePaise
    val totalPaid = matchingStaff?.paidBalancePaise ?: member.paidBalancePaise
    val dueUnpaid = (totalEarned - totalPaid).coerceAtLeast(0L)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(name = member.fullName, accent = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            val code = matchingStaff?.staffCode?.ifBlank { null } ?: member.staffCode
                            if (code != null) {
                                append(code).append(" • ")
                            } else {
                                append("Manager • ")
                            }
                            append(member.email)
                            val prem = matchingStaff?.premisesName ?: member.premisesName
                            prem?.let { append(" • ").append(Dates.cleanPremisesName(it)) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(if (member.isActive) "ACTIVE" else "CANCELLED")
            }

            // Salary Summary Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(totalEarned), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(totalPaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(dueUnpaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                    }
                }
            }

            // Monthly Salary Cards from joining date
            val monthlyCards = remember(member, matchingStaff, payrollPeriods, payrollItemsMap) {
                computeSalaryMonthlyCards(
                    joiningDateIso = matchingStaff?.startDate ?: member.joinedAt,
                    employeeId = matchingStaff?.id ?: member.employeeId,
                    email = member.email,
                    staffCode = matchingStaff?.staffCode ?: member.staffCode,
                    earnedBalancePaise = totalEarned,
                    payrollPeriods = payrollPeriods,
                    payrollItemsMap = payrollItemsMap
                )
            }

            if (monthlyCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Monthly Salary History",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthlyCards.size, key = { monthlyCards[it].monthYearLabel }) { index ->
                            val cardData = monthlyCards[index]
                            SalaryMonthlyCardItem(
                                cardData = cardData,
                                onPay = { onPayMonth(cardData) },
                                onOpenLedger = { onOpenLedger(cardData) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SalaryMonthlyCardData(
    val monthYearLabel: String,
    val month: Int = 1,
    val year: Int = 2025,
    val earnedPaise: Long,
    val paidPaise: Long,
    val statusLabel: String,
    val payrollItemId: String? = null,
    val paymentHistory: List<PaymentRecordDto> = emptyList(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val presentShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0
)

private fun computeSalaryMonthlyCards(
    joiningDateIso: String?,
    employeeId: String?,
    email: String?,
    staffCode: String?,
    earnedBalancePaise: Long,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>
): List<SalaryMonthlyCardData> {
    val currentYm = YearMonth.now()
    val minAllowedYm = currentYm.minusMonths(24)
    val rawStartYm = try {
        if (!joiningDateIso.isNullOrBlank()) {
            YearMonth.from(LocalDate.parse(joiningDateIso.take(10)))
        } else {
            currentYm
        }
    } catch (_: Exception) {
        currentYm
    }

    val effectiveStartYm = when {
        rawStartYm.isAfter(currentYm) -> currentYm
        rawStartYm.isBefore(minAllowedYm) -> minAllowedYm
        else -> rawStartYm
    }

    val monthList = mutableListOf<YearMonth>()
    var temp = effectiveStartYm
    while (!temp.isAfter(currentYm)) {
        monthList.add(temp)
        temp = temp.plusMonths(1)
    }

    val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    return monthList.reversed().map { ym ->
        val monthLabel = ym.format(monthFormatter)

        var foundItem: PayrollItemDto? = null
        var periodStatus: String? = null

        for (period in payrollPeriods) {
            val periodYm = try {
                YearMonth.from(LocalDate.parse(period.startDate.take(10)))
            } catch (_: Exception) {
                null
            }
            if (periodYm == ym) {
                periodStatus = period.status
                val items = payrollItemsMap[period.id].orEmpty()
                foundItem = items.firstOrNull { item ->
                    (employeeId != null && (item.employeeId == employeeId || item.staffProfileId == employeeId || item.resolvedEmployeeId == employeeId)) ||
                    (!email.isNullOrBlank() && item.staffEmail != null && item.staffEmail.equals(email, ignoreCase = true)) ||
                    (!staffCode.isNullOrBlank() && (item.staffCode?.equals(staffCode, ignoreCase = true) == true || item.resolvedStaffCode.equals(staffCode, ignoreCase = true)))
                }
                if (foundItem != null) break
            }
        }

        val isCurrentMonth = (ym == currentYm)

        if (foundItem != null) {
            val summary = foundItem.attendanceSummary
            val parsedPaid = Money.parseToInr(foundItem.paidAmount) ?: 0L
            val paidAmount = if (foundItem.payment != null) foundItem.payment.amountPaise else if (parsedPaid > 0) parsedPaid else if (periodStatus == "LOCKED") foundItem.resolvedNetSalaryPaise else 0L
            val isPaid = paidAmount > 0 || foundItem.payment != null || periodStatus == "LOCKED"
            val history = if (foundItem.payment != null) listOf(foundItem.payment) else emptyList()
            SalaryMonthlyCardData(
                monthYearLabel = monthLabel,
                month = ym.monthValue,
                year = ym.year,
                earnedPaise = foundItem.resolvedGrossSalaryPaise,
                paidPaise = paidAmount,
                statusLabel = if (isPaid) "PAID" else "UNPAID",
                payrollItemId = foundItem.monthlyPayrollId.ifBlank { foundItem.id },
                paymentHistory = history,
                breakdown = foundItem.breakdown,
                attendanceSummary = summary,
                presentShiftsCount = summary.presentShiftsCount,
                halfDayShiftsCount = summary.halfDayShiftsCount,
                leaveShiftsCount = summary.leaveShiftsCount,
                absentShiftsCount = summary.absentShiftsCount,
                holidayCount = summary.holidayCount,
                offDayCount = summary.offDayCount
            )
        } else {
            if (isCurrentMonth) {
                SalaryMonthlyCardData(
                    monthYearLabel = monthLabel,
                    month = ym.monthValue,
                    year = ym.year,
                    earnedPaise = earnedBalancePaise,
                    paidPaise = 0L,
                    statusLabel = "UNPAID",
                    presentShiftsCount = 0,
                    halfDayShiftsCount = 0,
                    leaveShiftsCount = 0,
                    absentShiftsCount = 0,
                    holidayCount = 0,
                    offDayCount = 0
                )
            } else {
                SalaryMonthlyCardData(
                    monthYearLabel = monthLabel,
                    month = ym.monthValue,
                    year = ym.year,
                    earnedPaise = 0L,
                    paidPaise = 0L,
                    statusLabel = "UNPAID",
                    presentShiftsCount = 0,
                    halfDayShiftsCount = 0,
                    leaveShiftsCount = 0,
                    absentShiftsCount = 0,
                    holidayCount = 0,
                    offDayCount = 0
                )
            }
        }
    }
}

@Composable
private fun SalaryMonthlyCardItem(
    cardData: SalaryMonthlyCardData,
    onPay: () -> Unit,
    onOpenLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaid = cardData.statusLabel.uppercase() == "PAID"
    val statusColor = if (isPaid) SecondaryEmerald else TertiaryAmber
    val pendingPaise = (cardData.earnedPaise - cardData.paidPaise).coerceAtLeast(0L)

    Surface(
        modifier = modifier.width(165.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Month & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardData.monthYearLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "DUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Financial Breakdown: Earned, Paid, Due
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Earned",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(cardData.earnedPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(cardData.paidPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (cardData.paidPaise > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Due",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(pendingPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = if (pendingPaise > 0) TertiaryAmber else SecondaryEmerald
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons
            if (!isPaid && pendingPaise > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Pay",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }

                    TextButton(
                        onClick = onOpenLedger,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ledger",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenLedger,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

private data class GivenPaymentTransaction(
    val id: String,
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val amountPaise: Long,
    val paymentMethod: String,
    val referenceNumber: String?,
    val paymentDate: String
)

@Composable
private fun PaymentTransactionRow(
    transaction: GivenPaymentTransaction,
    modifier: Modifier = Modifier
) {
    val methodLabel = when (transaction.paymentMethod.uppercase()) {
        "BANK_TRANSFER" -> "Bank Transfer"
        "UPI" -> "UPI / GPay"
        "CASH" -> "Cash"
        "CHEQUE" -> "Cheque"
        else -> transaction.paymentMethod.replace('_', ' ')
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SecondaryEmerald.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = SecondaryEmerald,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.staffName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        if (!transaction.staffCode.isNullOrBlank()) append(transaction.staffCode).append(" • ")
                        append(transaction.monthLabel).append(" Salary")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = methodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (!transaction.referenceNumber.isNullOrBlank()) {
                        Text(
                            text = "Ref: ${transaction.referenceNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+" + Money.format(transaction.amountPaise),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryEmerald
                )
                if (transaction.paymentDate.isNotBlank()) {
                    Text(
                        text = Dates.formatDateShort(transaction.paymentDate),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
