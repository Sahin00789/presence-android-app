package com.als.presence.ui.dashboard.owner.tabs

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollBreakdownDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.MonthlyEarningLedgerModal
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Owner "Workforce" tab — every manager and every staff member of the
 * organization with live salary, wallet summary and monthly cards from joining date.
 */
@Composable
fun OwnerWorkforceTab(
    staffState: UiState<List<StaffDto>>,
    membersState: UiState<List<OrganizationMemberDto>>,
    premisesState: UiState<List<PremisesDto>>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    payrollPeriodsState: UiState<List<PayrollPeriodDto>> = UiState.Success(emptyList()),
    payrollItemsMap: Map<String, List<PayrollItemDto>> = emptyMap(),
    isRecordingPayment: Boolean = false,
    onRecordPayment: ((itemId: String, amountCents: Long, paymentMethod: String, reference: String) -> Unit)? = null,
    onEditOrgClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    var ledgerTarget by remember { mutableStateOf<WorkforceLedgerTarget?>(null) }

    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val periods = (payrollPeriodsState as? UiState.Success)?.data.orEmpty()
    val managers = members.filter { it.role == "MANAGER" }
    val managerUserIds = managers.map { it.userId }.toSet()

    var selectedPremisesId by remember { mutableStateOf<String?>(null) } // null = All Premises

    val filteredStaff = staff.filter { member ->
        !managerUserIds.contains(member.userId) &&
        (selectedPremisesId == null || member.premisesId == null || member.premisesId == selectedPremisesId)
    }

    val filteredManagers = managers.filter { manager ->
        selectedPremisesId == null || manager.premisesId == null || manager.premisesId == selectedPremisesId
    }

    val failure = listOfNotNull(
        (staffState as? UiState.Error)?.message,
        (membersState as? UiState.Error)?.message,
        (premisesState as? UiState.Error)?.message
    ).firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your organization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Everyone who works here, live from the roster",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEditOrgClick != null) {
                    IconButton(
                        onClick = onEditOrgClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Organization",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh workforce",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top Premises Selector ("All Premises" + list of premises chips)
        if (premises.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Premises",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPremisesId == null,
                        onClick = { selectedPremisesId = null },
                        label = { Text("All Premises", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    premises.forEach { p ->
                        FilterChip(
                            selected = selectedPremisesId == p.id,
                            onClick = { selectedPremisesId = p.id },
                            label = { Text(Dates.cleanPremisesName(p.name), style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        if (failure != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = failure,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile(
                label = "Employees",
                value = filteredStaff.size.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary
            )
            MetricTile(
                label = "Managers",
                value = filteredManagers.size.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary
            )
            MetricTile(
                label = "Premises",
                value = premises.size.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        // Workforce Salary Summary Card
        val totalEarnedCents = filteredStaff.sumOf { it.earnedBalanceCents }
        val totalPaidCents = filteredStaff.sumOf { it.paidBalanceCents }
        val totalDueCents = (totalEarnedCents - totalPaidCents).coerceAtLeast(0L)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workforce Salary Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${filteredStaff.size} Employees",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Earned",
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
                            text = "Paid",
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
                            text = "Due Salary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(totalDueCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDueCents > 0) TertiaryAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- Staff
        SectionHeader(
            title = "Staff Roster",
            trailing = {
                Text(
                    text = "${filteredStaff.size} staff members",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        when (staffState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading roster…")
            is UiState.Error -> SectionError(staffState.message)
            is UiState.Success -> {
                if (filteredStaff.isEmpty()) {
                    SectionEmpty("No employees onboarded for selected premises.")
                } else {
                    filteredStaff.forEach { member ->
                        StaffCard(
                            staff = member,
                            payrollPeriods = periods,
                            payrollItemsMap = payrollItemsMap,
                            onOpenLedger = { cardData ->
                                ledgerTarget = WorkforceLedgerTarget(
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

        Spacer(modifier = Modifier.height(4.dp))

        // ------------------------------------------------------------- Managers
        SectionHeader(title = "Managers")
        when (membersState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading managers…")
            is UiState.Error -> SectionError(membersState.message)
            is UiState.Success -> {
                if (filteredManagers.isEmpty()) {
                    SectionEmpty("No managers for selected premises.")
                } else {
                    filteredManagers.forEach { manager ->
                        ManagerRow(
                            member = manager,
                            payrollPeriods = periods,
                            payrollItemsMap = payrollItemsMap,
                            onOpenLedger = { cardData ->
                                ledgerTarget = WorkforceLedgerTarget(
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

        Spacer(modifier = Modifier.height(4.dp))

        // ------------------------------------------------------------- Premises
        SectionHeader(title = "Geofenced premises")
        if (premises.isEmpty()) {
            SectionEmpty("No premises configured. Add one from the menu.")
        } else {
            premises.forEach { site ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    val nameOnly = Dates.cleanPremisesName(site.name)

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                nameOnly,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusPill("${site.radiusMeters}m radius")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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

private data class WorkforceLedgerTarget(
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val earnedCents: Long,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto()
)

@Composable
private fun StaffCard(
    staff: StaffDto,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onOpenLedger: (MonthlySalaryCardData) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

            // Salary Summary Banner
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

            // Monthly Salary History with Attendance Counts
            val monthlyCards = remember(staff, payrollPeriods, payrollItemsMap) {
                computeMonthlySalaryCards(
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
                            MonthlySalaryCard(cardData = cardData, onOpenLedger = { onOpenLedger(cardData) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerRow(
    member: OrganizationMemberDto,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onOpenLedger: (MonthlySalaryCardData) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // Monthly Salary History with Attendance Counts
            val monthlyCards = remember(member, payrollPeriods, payrollItemsMap) {
                computeMonthlySalaryCards(
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
                            MonthlySalaryCard(cardData = cardData, onOpenLedger = { onOpenLedger(cardData) })
                        }
                    }
                }
            }
        }
    }
}

private data class MonthlySalaryCardData(
    val monthYearLabel: String,
    val earnedCents: Long,
    val paidCents: Long,
    val statusLabel: String,
    val payrollItemId: String? = null,
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val presentShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0
)

private fun computeMonthlySalaryCards(
    joiningDateIso: String?,
    employeeId: String?,
    email: String?,
    staffCode: String?,
    earnedBalanceCents: Long,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>
): List<MonthlySalaryCardData> {
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
            MonthlySalaryCardData(
                monthYearLabel = monthLabel,
                earnedCents = foundItem.grossSalaryCents,
                paidCents = paidAmount,
                statusLabel = if (isPaid) "PAID" else "UNPAID",
                payrollItemId = foundItem.id,
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
                MonthlySalaryCardData(
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
                MonthlySalaryCardData(
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
private fun MonthlySalaryCard(
    cardData: MonthlySalaryCardData,
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

            FilledTonalButton(
                onClick = onOpenLedger,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Earning Ledger",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
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
                    AttendanceCountBadge(label = "Present", count = cardData.presentShiftsCount, color = SecondaryEmerald)
                    AttendanceCountBadge(label = "Half-Day", count = cardData.halfDayShiftsCount, color = TertiaryAmber)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceCountBadge(label = "Leave", count = cardData.leaveShiftsCount, color = Color(0xFF3B82F6))
                    AttendanceCountBadge(label = "Absent", count = cardData.absentShiftsCount, color = Color(0xFFEF4444))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceCountBadge(label = "Holidays", count = cardData.holidayCount, color = Color(0xFF8B5CF6))
                    AttendanceCountBadge(label = "Off-Days", count = cardData.offDayCount, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun AttendanceCountBadge(
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
