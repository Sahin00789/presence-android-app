package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
            title = "Salary Payment Console",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh salary payments",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            SalaryPaymentScreenContent(
                staffState = staffState,
                membersState = membersState,
                payrollPeriodsState = periodsState,
                payrollItemsMap = payrollItemsMap,
                isSubmitting = isSubmitting,
                onRecordPayment = { _, _, _, _ -> }
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
    onRecordPayment: (itemId: String, amountCents: Long, paymentMethod: String, reference: String) -> Unit
) {
    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val periods = (payrollPeriodsState as? UiState.Success)?.data.orEmpty()
    val managers = members.filter { it.role == "MANAGER" }

    var searchQuery by remember { mutableStateOf("") }
    var monthPayTarget by remember { mutableStateOf<SalaryMonthPayTarget?>(null) }
    var ledgerTarget by remember { mutableStateOf<SalaryLedgerTarget?>(null) }

    val filteredStaff = staff.filter { member ->
        searchQuery.isBlank() ||
            member.fullName.contains(searchQuery, ignoreCase = true) ||
            member.staffCode.contains(searchQuery, ignoreCase = true) ||
            (member.department?.contains(searchQuery, ignoreCase = true) == true)
    }

    val filteredManagers = managers.filter { manager ->
        searchQuery.isBlank() ||
            manager.fullName.contains(searchQuery, ignoreCase = true) ||
            manager.email.contains(searchQuery, ignoreCase = true) ||
            (manager.staffCode?.contains(searchQuery, ignoreCase = true) == true)
    }

    val totalEarnedCents = filteredStaff.sumOf { it.earnedBalanceCents }
    val totalPaidCents = filteredStaff.sumOf { it.paidBalanceCents }
    val totalDueCents = (totalEarnedCents - totalPaidCents).coerceAtLeast(0L)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Workforce Salary Payment Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Salary Payment Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatusPill("${filteredStaff.size + filteredManagers.size} People")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Earned Salary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(totalEarnedCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Disbursed / Paid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(totalPaidCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Pending Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(totalDueCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDueCents > 0) TertiaryAmber else SecondaryEmerald
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search employee or manager name, code, or email") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

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
                                val itemId = cardData.payrollItemId
                                    ?: periods.firstNotNullOfOrNull { period ->
                                        payrollItemsMap[period.id]?.firstOrNull {
                                            it.employeeId == member.id || it.staffEmail.equals(member.email, ignoreCase = true)
                                        }
                                    }?.id
                                    ?: periods.firstOrNull()?.id

                                if (itemId != null) {
                                    monthPayTarget = SalaryMonthPayTarget(
                                        payrollItemId = itemId,
                                        staffName = member.fullName,
                                        staffCode = member.staffCode,
                                        monthLabel = cardData.monthYearLabel,
                                        earnedCents = cardData.earnedCents,
                                        paidCents = cardData.paidCents,
                                        breakdown = cardData.breakdown,
                                        attendanceSummary = cardData.attendanceSummary,
                                        paymentHistory = cardData.paymentHistory
                                    )
                                }
                            },
                            onOpenLedger = { cardData ->
                                ledgerTarget = SalaryLedgerTarget(
                                    staffName = member.fullName,
                                    staffCode = member.staffCode,
                                    monthLabel = cardData.monthYearLabel,
                                    earnedCents = cardData.earnedCents,
                                    attendanceSummary = cardData.attendanceSummary,
                                    breakdown = cardData.breakdown
                                )
                            }
                        )
                    }
                }
            }
        }

        // Managers Section
        if (filteredManagers.isNotEmpty() || searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Manager Salary Roster",
                trailing = {
                    Text(
                        text = "${filteredManagers.size} managers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            when (membersState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading manager roster…")
                is UiState.Error -> SectionError(membersState.message)
                is UiState.Success -> {
                    if (filteredManagers.isEmpty()) {
                        SectionEmpty("No managers found matching “$searchQuery”.")
                    } else {
                        filteredManagers.forEach { manager ->
                            SalaryManagerCard(
                                member = manager,
                                payrollPeriods = periods,
                                payrollItemsMap = payrollItemsMap,
                                onPayMonth = { cardData ->
                                    val itemId = cardData.payrollItemId
                                        ?: periods.firstNotNullOfOrNull { period ->
                                            payrollItemsMap[period.id]?.firstOrNull {
                                                it.employeeId == manager.employeeId ||
                                                it.staffEmail.equals(manager.email, ignoreCase = true) ||
                                                (!manager.staffCode.isNullOrBlank() && it.staffCode.equals(manager.staffCode, ignoreCase = true))
                                            }
                                        }?.id
                                        ?: periods.firstOrNull()?.id

                                    if (itemId != null) {
                                        monthPayTarget = SalaryMonthPayTarget(
                                            payrollItemId = itemId,
                                            staffName = manager.fullName,
                                            staffCode = manager.staffCode,
                                            monthLabel = cardData.monthYearLabel,
                                            earnedCents = cardData.earnedCents,
                                            paidCents = cardData.paidCents,
                                            breakdown = cardData.breakdown,
                                            attendanceSummary = cardData.attendanceSummary,
                                            paymentHistory = cardData.paymentHistory
                                        )
                                    }
                                },
                                onOpenLedger = { cardData ->
                                    ledgerTarget = SalaryLedgerTarget(
                                        staffName = manager.fullName,
                                        staffCode = manager.staffCode,
                                        monthLabel = cardData.monthYearLabel,
                                        earnedCents = cardData.earnedCents,
                                        attendanceSummary = cardData.attendanceSummary,
                                        breakdown = cardData.breakdown
                                    )
                                }
                            )
                        }
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
            earnedCents = target.earnedCents,
            paidCents = target.paidCents,
            breakdown = target.breakdown,
            attendanceSummary = target.attendanceSummary,
            paymentHistory = target.paymentHistory,
            isSubmitting = isSubmitting,
            onDismiss = { if (!isSubmitting) monthPayTarget = null },
            onConfirmPay = { amountCents, method, reference ->
                onRecordPayment(target.payrollItemId, amountCents, method, reference)
                monthPayTarget = null
            }
        )
    }

    ledgerTarget?.let { target ->
        MonthlyEarningLedgerModal(
            staffName = target.staffName,
            staffCode = target.staffCode,
            monthLabel = target.monthLabel,
            earnedCents = target.earnedCents,
            attendanceSummary = target.attendanceSummary,
            breakdown = target.breakdown,
            onDismiss = { ledgerTarget = null }
        )
    }
}

private data class SalaryMonthPayTarget(
    val payrollItemId: String,
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val earnedCents: Long,
    val paidCents: Long,
    val paymentHistory: List<PaymentRecordDto> = emptyList(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto()
)

private data class SalaryLedgerTarget(
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val earnedCents: Long,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto()
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
                            staff.premisesName?.let { append(" • 📍 ").append(Dates.cleanPremisesName(it)) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(if (staff.isActive) "ACTIVE" else "CANCELLED")
            }

            val totalEarned = staff.earnedBalanceCents
            val totalPaid = staff.paidBalanceCents
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
                    earnedBalanceCents = staff.earnedBalanceCents,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthlyCards.forEach { cardData ->
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
                            member.staffCode?.let { append(it).append(" • ") } ?: append("Manager • ")
                            append(member.email)
                            member.premisesName?.let { append(" • 📍 ").append(Dates.cleanPremisesName(it)) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(if (member.isActive) "ACTIVE" else "CANCELLED")
            }

            // Monthly Salary Cards from joining date
            val monthlyCards = remember(member, payrollPeriods, payrollItemsMap) {
                computeSalaryMonthlyCards(
                    joiningDateIso = member.joinedAt,
                    employeeId = member.employeeId,
                    email = member.email,
                    staffCode = member.staffCode,
                    earnedBalanceCents = 0L,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthlyCards.forEach { cardData ->
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
    val earnedCents: Long,
    val paidCents: Long,
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
    earnedBalanceCents: Long,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>
): List<SalaryMonthlyCardData> {
    val currentYm = YearMonth.now()
    val startYm = try {
        if (!joiningDateIso.isNullOrBlank()) {
            YearMonth.from(LocalDate.parse(joiningDateIso.take(10)))
        } else {
            currentYm
        }
    } catch (_: Exception) {
        currentYm
    }

    val effectiveStartYm = if (startYm.isAfter(currentYm)) currentYm else startYm

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
                    (employeeId != null && item.employeeId == employeeId) ||
                    (!email.isNullOrBlank() && item.staffEmail.equals(email, ignoreCase = true)) ||
                    (!staffCode.isNullOrBlank() && item.staffCode.equals(staffCode, ignoreCase = true))
                }
                if (foundItem != null) break
            }
        }

        val isCurrentMonth = (ym == currentYm)

        if (foundItem != null) {
            val summary = foundItem.attendanceSummary
            val paidAmount = if (foundItem.payment != null) foundItem.payment.amountCents else if (periodStatus == "LOCKED") foundItem.netSalaryCents else 0L
            val isPaid = paidAmount > 0 || foundItem.payment != null || periodStatus == "LOCKED"
            val history = if (foundItem.payment != null) listOf(foundItem.payment) else emptyList()
            SalaryMonthlyCardData(
                monthYearLabel = monthLabel,
                earnedCents = foundItem.grossSalaryCents,
                paidCents = paidAmount,
                statusLabel = if (isPaid) "PAID" else "UNPAID",
                payrollItemId = foundItem.id,
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
                    earnedCents = earnedBalanceCents,
                    paidCents = 0L,
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
                    earnedCents = 0L,
                    paidCents = 0L,
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
    val pendingCents = (cardData.earnedCents - cardData.paidCents).coerceAtLeast(0L)

    Surface(
        modifier = modifier.width(170.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardData.monthYearLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "UNPAID",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Earned",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(cardData.earnedCents),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
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
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(cardData.paidCents),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (cardData.paidCents > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Money.format(pendingCents),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (pendingCents > 0) TertiaryAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = onPay,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isPaid) "Pay" else "Pay",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }

                FilledTonalButton(
                    onClick = onOpenLedger,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }

            Text(
                text = "Shift Breakdown",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SalaryAttendanceBadge(label = "Present", count = cardData.presentShiftsCount, color = SecondaryEmerald)
                    SalaryAttendanceBadge(label = "Half-Day", count = cardData.halfDayShiftsCount, color = TertiaryAmber)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SalaryAttendanceBadge(label = "Leave", count = cardData.leaveShiftsCount, color = Color(0xFF3B82F6))
                    SalaryAttendanceBadge(label = "Absent", count = cardData.absentShiftsCount, color = Color(0xFFEF4444))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SalaryAttendanceBadge(label = "Holidays", count = cardData.holidayCount, color = Color(0xFF8B5CF6))
                    SalaryAttendanceBadge(label = "Off-Days", count = cardData.offDayCount, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun SalaryAttendanceBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
