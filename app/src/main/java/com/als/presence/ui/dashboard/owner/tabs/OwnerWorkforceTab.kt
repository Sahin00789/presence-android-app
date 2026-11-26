package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollBreakdownDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.MonthlyEarningLedgerModal
import com.als.presence.ui.dashboard.common.PresenceSearchBar
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.components.PresenceAvatar
import com.als.presence.ui.dashboard.common.components.PresenceBadge
import com.als.presence.ui.dashboard.common.components.PresenceEmptyState
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.common.components.PresenceSectionHeader
import java.time.YearMonth

/**
 * Editorial, 2026 Android Product Redesign for Owner "Employees" Workforce Screen.
 * Fulfills all product hierarchy requirements:
 * 1. Organisation identity & premises context
 * 2. Total workforce summary & present/active overview
 * 3. Employee search & filter controls
 * 4. Refined list directory (NOT large individual cards!)
 * 5. Add employee action affordance
 * 6. Smooth navigation to employee ledger & details
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
    staffLedgerMap: Map<String, List<StaffEarningLedgerDto>> = emptyMap(),
    isRecordingPayment: Boolean = false,
    onRecordPayment: ((itemId: String, amountPaise: Long, paymentMethod: String, reference: String) -> Unit)? = null,
    onLoadStaffLedger: ((staffProfileId: String, month: Int, year: Int) -> Unit)? = null,
    onDownloadPayslip: ((context: android.content.Context, month: Int, year: Int, staffProfileId: String?) -> Unit)? = null,
    onAddEmployeeClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ledgerTarget by remember { mutableStateOf<WorkforceLedgerTarget?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPremisesId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    val isDark = isSystemInDarkTheme()

    val staff = (staffState as? UiState.Success)?.data.orEmpty().filter { it.id.isNotBlank() }.distinctBy { it.id }
    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()

    val queryTrimmed = searchQuery.trim()
    val filteredStaff = staff.filter { member ->
        val matchesPremises = selectedPremisesId == null || member.premisesId == selectedPremisesId
        val matchesSearch = queryTrimmed.isBlank() ||
                member.fullName.contains(queryTrimmed, ignoreCase = true) ||
                member.staffCode.contains(queryTrimmed, ignoreCase = true) ||
                member.email.contains(queryTrimmed, ignoreCase = true) ||
                (member.department?.contains(queryTrimmed, ignoreCase = true) == true) ||
                (member.designation?.contains(queryTrimmed, ignoreCase = true) == true)
        val matchesStatus = when (selectedStatusFilter) {
            "ACTIVE" -> member.isActive
            "INACTIVE" -> !member.isActive
            "DUE" -> (member.earnedBalancePaise - member.paidBalancePaise) > 0L
            else -> true
        }
        matchesPremises && matchesSearch && matchesStatus
    }

    // Workforce KPI Summary Counts
    val totalEmployees = staff.size
    val activeCount = staff.count { it.isActive }
    val inactiveCount = staff.count { !it.isActive }
    val dueCount = staff.count { (it.earnedBalancePaise - it.paidBalancePaise) > 0L }
    val activeRatio = if (totalEmployees > 0) (activeCount.toFloat() / totalEmployees.toFloat()).coerceIn(0f, 1f) else 1f

    val screenInfo = LocalScreenSizeInfo.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (screenInfo.isTabletOrFoldable) 720.dp else Dp.Unspecified)
                .fillMaxWidth()
                .padding(
                    horizontal = screenInfo.horizontalScreenPadding,
                    vertical = if (screenInfo.isVeryCompact) 10.dp else 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. ORGANISATION IDENTITY & HEADER BAR
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Workforce Directory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PresenceBadge(
                            text = "OWNER",
                            tone = PresenceTone.Info
                        )
                    }
                    Text(
                        text = "Real-time roster, staff profiles & earning balances",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. SELECTED PREMISES CONTEXT STRIP
            // -------------------------------------------------------------
            if (premisesList.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedPremisesId == null,
                            onClick = { selectedPremisesId = null },
                            label = { Text("All Premises (${staff.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(premisesList) { premises ->
                        val count = staff.count { it.premisesId == premises.id }
                        FilterChip(
                            selected = premises.id == selectedPremisesId,
                            onClick = {
                                selectedPremisesId = if (selectedPremisesId == premises.id) null else premises.id
                            },
                            label = { Text("${Dates.cleanPremisesName(premises.name)} ($count)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. TOTAL WORKFORCE SUMMARY & ACTIVE RATIO OVERVIEW
            // -------------------------------------------------------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            PresenceMicroLabel(text = "Total Workforce")
                            Text(
                                text = "$totalEmployees Employees",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                PresenceMicroLabel(text = "Active Staff")
                                Text(
                                    text = "$activeCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.Positive
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                PresenceMicroLabel(text = "Pending Dues")
                                Text(
                                    text = "$dueCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dueCount > 0) PresenceDesign.DangerText else PresenceDesign.Positive
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { activeRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = PresenceDesign.Positive,
                        trackColor = PresenceDesign.NeutralSoft
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(activeRatio * 100).toInt()}% Active Workforce",
                            style = MaterialTheme.typography.labelSmall,
                            color = PresenceDesign.Positive,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$inactiveCount Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. SEARCH & STATUS FILTER STRIP
            // -------------------------------------------------------------
            PresenceSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholderText = "Search by employee name, code, designation…"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text("All (${staff.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == "ACTIVE",
                    onClick = { selectedStatusFilter = "ACTIVE" },
                    label = { Text("Active ($activeCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PresenceDesign.Positive
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PresenceDesign.PositiveSoft,
                        selectedLabelColor = PresenceDesign.Positive
                    )
                )
                if (dueCount > 0) {
                    FilterChip(
                        selected = selectedStatusFilter == "DUE",
                        onClick = { selectedStatusFilter = "DUE" },
                        label = { Text("Balance Due ($dueCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PresenceDesign.DangerText
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PresenceDesign.DangerSoft,
                            selectedLabelColor = PresenceDesign.DangerText
                        )
                    )
                }
            }

            // -------------------------------------------------------------
            // 5. REFINED EMPLOYEE DIRECTORY LIST (NOT INDIVIDUAL CARDS!)
            // -------------------------------------------------------------
            if (filteredStaff.isEmpty()) {
                PresenceEmptyState(
                    title = "No Employees Found",
                    description = if (queryTrimmed.isNotBlank()) "No employee matching \"$queryTrimmed\"." else "No employees configured in this premises."
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 1.dp
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredStaff, key = { it.id }) { member ->
                            val ymNow = YearMonth.now()
                            val netDuePaise = (member.earnedBalancePaise - member.paidBalancePaise).coerceAtLeast(0L)

                            WorkforceEmployeeRow(
                                member = member,
                                netDuePaise = netDuePaise,
                                onClick = {
                                    val m = ymNow.monthValue
                                    val y = ymNow.year
                                    onLoadStaffLedger?.invoke(member.id, m, y)
                                    ledgerTarget = WorkforceLedgerTarget(
                                        staffProfileId = member.id,
                                        staffName = member.fullName,
                                        staffCode = member.staffCode,
                                        monthLabel = "${ymNow.month.name.take(3)} $y",
                                        month = m,
                                        year = y,
                                        earnedPaise = member.earnedBalancePaise,
                                        paidPaise = member.paidBalancePaise
                                    )
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Ledger Modal (Employee drill-down)
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
            entries = targetEntries,
            isSubmitting = isRecordingPayment,
            onDismiss = { ledgerTarget = null },
            onRecordPayment = if (!target.payrollItemId.isNullOrBlank() && onRecordPayment != null) {
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

/**
 * Compact, elegant workforce directory row.
 * Displays avatar with status beacon, name, designation, shift/premises, and financial status.
 */
@Composable
private fun WorkforceEmployeeRow(
    member: StaffDto,
    netDuePaise: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar, Name, Designation & Code
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                PresenceAvatar(
                    name = member.fullName,
                    size = 42.dp
                )
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(1.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (member.isActive) PresenceDesign.Positive else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (member.staffCode.isNotBlank()) {
                        Text(
                            text = "#${member.staffCode}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val roleInfo = member.designation?.takeIf { it.isNotBlank() }
                    ?: member.department?.takeIf { it.isNotBlank() }
                    ?: "Employee"
                val premisesClean = Dates.cleanPremisesName(member.premisesName ?: "").ifBlank { "Main Premises" }

                Text(
                    text = "$roleInfo • $premisesClean",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right: Status badge & Earning balance
        Column(horizontalAlignment = Alignment.End) {
            PresenceBadge(
                text = if (member.isActive) "ACTIVE" else "INACTIVE",
                tone = if (member.isActive) PresenceTone.Success else PresenceTone.Neutral
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (netDuePaise > 0) "Due: ${Money.format(netDuePaise)}" else "Settled",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (netDuePaise > 0) PresenceDesign.DangerText else PresenceDesign.Positive
            )
        }
    }
}

private data class WorkforceLedgerTarget(
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
)
