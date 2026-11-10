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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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
    onRecordPayment: ((itemId: String, amountPaise: Long, paymentMethod: String, reference: String) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    var ledgerTarget by remember { mutableStateOf<WorkforceLedgerTarget?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val staff = (staffState as? UiState.Success)?.data.orEmpty().filter { it.id.isNotBlank() }.distinctBy { it.id }
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val periods = (payrollPeriodsState as? UiState.Success)?.data.orEmpty()

    val filteredStaff = staff

    val queryTrimmed = searchQuery.trim()

    val searchedStaff = filteredStaff.filter { member ->
        queryTrimmed.isBlank() ||
                member.fullName.contains(queryTrimmed, ignoreCase = true) ||
                member.staffCode.contains(queryTrimmed, ignoreCase = true) ||
                member.email.contains(queryTrimmed, ignoreCase = true) ||
                (member.department?.contains(queryTrimmed, ignoreCase = true) == true) ||
                (member.designation?.contains(queryTrimmed, ignoreCase = true) == true)
    }

    val failure = listOfNotNull(
        (staffState as? UiState.Error)?.message,
        (membersState as? UiState.Error)?.message,
        (premisesState as? UiState.Error)?.message,
    ).firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workforce Roster",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            PresenceRefreshButton(
                onClick = onRetry,
                isRefreshing = staffState is UiState.Loading || membersState is UiState.Loading,
                contentDescription = "Refresh workforce"
            )
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
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
                            text = "Search name, code, role or dept…",
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

        // Top Premises Selector Chips


        if (failure != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = failure,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Workforce Salary Summary Card
        val totalEarnedPaise = filteredStaff.sumOf { member: StaffDto -> member.earnedBalancePaise }
        val totalPaidPaise = filteredStaff.sumOf { member: StaffDto -> member.paidBalancePaise }
        val totalDuePaise = (totalEarnedPaise - totalPaidPaise).coerceAtLeast(0L)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Workforce Salary Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusPill("${filteredStaff.size} Employees")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Total Earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.format(totalEarnedPaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.format(totalPaidPaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Due Salary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Money.format(totalDuePaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDuePaise > 0) TertiaryAmber else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- Employee Roster
        SectionHeader(
            title = "Employee Roster",
            trailing = {
                Text(
                    text = "${searchedStaff.size} employees",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        when (staffState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading roster…")
            is UiState.Error -> SectionError(staffState.message)
            is UiState.Success -> {
                if (searchedStaff.isEmpty()) {
                    SectionEmpty(
                        if (queryTrimmed.isNotBlank()) "No employees match \"$queryTrimmed\"."
                        else "No employees onboarded for selected premises."
                    )
                } else {
                    searchedStaff.forEach { member ->
                        StaffCard(
                            staff = member,
                            payrollPeriods = periods,
                            payrollItemsMap = payrollItemsMap,
                            onOpenLedger = { cardData ->
                                val itemId = cardData.payrollItemId
                                    ?: periods.firstNotNullOfOrNull { period ->
                                        payrollItemsMap[period.id]?.firstOrNull { item ->
                                            item.employeeId == member.id || item.staffProfileId == member.id || item.resolvedEmployeeId == member.id || item.staffEmail.equals(member.email, ignoreCase = true)
                                        }
                                    }?.let { item -> item.id.ifBlank { item.monthlyPayrollId } }

                                ledgerTarget = WorkforceLedgerTarget(
                                    payrollItemId = if (!itemId.isNullOrBlank() && itemId.length > 20 && itemId.contains("-")) itemId else null,
                                    staffName = member.fullName,
                                    staffCode = member.staffCode,
                                    monthLabel = cardData.monthYearLabel,
                                    earnedPaise = cardData.earnedPaise,
                                    paidPaise = cardData.paidPaise,
                                    attendanceSummary = cardData.attendanceSummary,
                                    breakdown = cardData.breakdown,
                                )
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Spacer(modifier = Modifier.height(4.dp))

        // ------------------------------------------------------------- Premises
        SectionHeader(title = "Geofenced Premises")
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
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    val nameOnly = Dates.cleanPremisesName(site.name)

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                nameOnly,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gps Geofence active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            earnedPaise = target.earnedPaise,
            paidPaise = target.paidPaise,
            attendanceSummary = target.attendanceSummary,
            breakdown = target.breakdown,
            isSubmitting = isRecordingPayment,
            onDismiss = { ledgerTarget = null },
            onRecordPayment = if (!target.payrollItemId.isNullOrBlank() && onRecordPayment != null) {
                { amount, method, ref ->
                    onRecordPayment(target.payrollItemId, amount, method, ref)
                    ledgerTarget = null
                }
            } else null
        )
    }
}

private data class WorkforceLedgerTarget(
    val payrollItemId: String? = null,
    val staffName: String,
    val staffCode: String?,
    val monthLabel: String,
    val earnedPaise: Long,
    val paidPaise: Long = 0L,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
)

@Composable
private fun StaffCard(
    staff: StaffDto,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onOpenLedger: (MonthlySalaryCardData) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile & Role Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                InitialAvatar(
                    name = staff.fullName,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = staff.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (staff.staffCode.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "#${staff.staffCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (staff.email.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = staff.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "EMPLOYEE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    StatusPill(if (staff.isActive) "ACTIVE" else "CANCELLED")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Quick Metadata Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Premises Chip
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = staff.premisesName?.let { Dates.cleanPremisesName(it) } ?: "No Premises",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Department Chip
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = staff.department?.takeIf { it.isNotBlank() } ?: "Operations",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Designation Chip (if present)
                if (!staff.designation.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = staff.designation,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                // Joining Date Chip
                if (!staff.hireDate.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Joined ${Dates.formatDate(staff.hireDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            val totalEarned = staff.earnedBalancePaise
            val totalPaid = staff.paidBalancePaise
            val dueUnpaid = (totalEarned - totalPaid).coerceAtLeast(0L)

            // Salary Summary Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(totalEarned),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Paid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(totalPaid),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Due Salary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(dueUnpaid),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dueUnpaid > 0) TertiaryAmber else SecondaryEmerald,
                        )
                    }
                }
            }

            // Monthly Salary History Carousel
            val monthlyCards = remember(staff, payrollPeriods, payrollItemsMap) {
                computeMonthlySalaryCards(
                    joiningDateIso = staff.hireDate,
                    employeeId = staff.id,
                    email = staff.email,
                    staffCode = staff.staffCode,
                    earnedBalancePaise = staff.earnedBalancePaise,
                    payrollPeriods = payrollPeriods,
                    payrollItemsMap = payrollItemsMap,
                )
            }

            if (monthlyCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Monthly Salary & Attendance History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${monthlyCards.size} months",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        monthlyCards.forEach { cardData ->
                            MonthlySalaryCard(
                                cardData = cardData,
                                onOpenLedger = { onOpenLedger(cardData) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerCard(
    member: OrganizationMemberDto,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
    onOpenLedger: (MonthlySalaryCardData) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile & Role Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                InitialAvatar(
                    name = member.fullName,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!member.staffCode.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "#${member.staffCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "MANAGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    StatusPill(if (member.isActive) "ACTIVE" else "CANCELLED")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Quick Metadata Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Premises Chip
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = member.premisesName?.let { Dates.cleanPremisesName(it) } ?: "All Premises",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Role/Dept Chip
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Management",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Joining Date Chip
                if (!member.joinedAt.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Joined ${Dates.formatDate(member.joinedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Monthly Salary History
            val monthlyCards = remember(member, payrollPeriods, payrollItemsMap) {
                computeMonthlySalaryCards(
                    joiningDateIso = member.joinedAt,
                    employeeId = member.employeeId,
                    email = member.email,
                    staffCode = member.staffCode,
                    earnedBalancePaise = 0L,
                    payrollPeriods = payrollPeriods,
                    payrollItemsMap = payrollItemsMap,
                )
            }

            val totalEarned = monthlyCards.sumOf { it.earnedPaise }
            val totalPaid = monthlyCards.sumOf { it.paidPaise }
            val dueUnpaid = (totalEarned - totalPaid).coerceAtLeast(0L)

            // Salary Summary Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(totalEarned),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Paid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(totalPaid),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Due Salary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Money.format(dueUnpaid),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dueUnpaid > 0) TertiaryAmber else SecondaryEmerald,
                        )
                    }
                }
            }

            if (monthlyCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Monthly Salary & Attendance History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${monthlyCards.size} months",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        monthlyCards.forEach { cardData ->
                            MonthlySalaryCard(
                                cardData = cardData,
                                onOpenLedger = { onOpenLedger(cardData) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MonthlySalaryCardData(
    val monthYearLabel: String,
    val earnedPaise: Long,
    val paidPaise: Long,
    val statusLabel: String,
    val payrollItemId: String? = null,
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val presentShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0,
)

private fun computeMonthlySalaryCards(
    joiningDateIso: String?,
    employeeId: String?,
    email: String?,
    staffCode: String?,
    earnedBalancePaise: Long,
    payrollPeriods: List<PayrollPeriodDto>,
    payrollItemsMap: Map<String, List<PayrollItemDto>>,
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
            val paidAmount = if (foundItem.payment != null) foundItem.payment.amountPaise else if (periodStatus == "LOCKED") foundItem.netSalaryPaise else 0L
            val isPaid = paidAmount > 0 || foundItem.payment != null || periodStatus == "LOCKED"
            MonthlySalaryCardData(
                monthYearLabel = monthLabel,
                earnedPaise = foundItem.grossSalaryPaise,
                paidPaise = paidAmount,
                statusLabel = if (isPaid) "PAID" else "UNPAID",
                payrollItemId = foundItem.id,
                breakdown = foundItem.breakdown,
                attendanceSummary = summary,
                presentShiftsCount = summary.presentShiftsCount,
                halfDayShiftsCount = summary.halfDayShiftsCount,
                leaveShiftsCount = summary.leaveShiftsCount,
                absentShiftsCount = summary.absentShiftsCount,
                holidayCount = summary.holidayCount,
                offDayCount = summary.offDayCount,
            )
        } else {
            if (isCurrentMonth) {
                MonthlySalaryCardData(
                    monthYearLabel = monthLabel,
                    earnedPaise = earnedBalancePaise,
                    paidPaise = 0L,
                    statusLabel = "UNPAID",
                    presentShiftsCount = 0,
                    halfDayShiftsCount = 0,
                    leaveShiftsCount = 0,
                    absentShiftsCount = 0,
                    holidayCount = 0,
                    offDayCount = 0,
                )
            } else {
                MonthlySalaryCardData(
                    monthYearLabel = monthLabel,
                    earnedPaise = 0L,
                    paidPaise = 0L,
                    statusLabel = "UNPAID",
                    presentShiftsCount = 0,
                    halfDayShiftsCount = 0,
                    leaveShiftsCount = 0,
                    absentShiftsCount = 0,
                    holidayCount = 0,
                    offDayCount = 0,
                )
            }
        }
    }
}

@Composable
private fun MonthlySalaryCard(
    cardData: MonthlySalaryCardData,
    onOpenLedger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPaid = cardData.statusLabel.uppercase() == "PAID"
    val statusColor = if (isPaid) SecondaryEmerald else TertiaryAmber
    val pendingPaise = (cardData.earnedPaise - cardData.paidPaise).coerceAtLeast(0L)

    Surface(
        modifier = modifier.width(165.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Month & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cardData.monthYearLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "DUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Financial Breakdown: Earned, Paid, Due
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Earned",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(cardData.earnedPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(cardData.paidPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (cardData.paidPaise > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Net Due",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(pendingPaise),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = if (pendingPaise > 0) TertiaryAmber else SecondaryEmerald,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Button
            OutlinedButton(
                onClick = onOpenLedger,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "View Ledger",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                )
            }
        }
    }
}
