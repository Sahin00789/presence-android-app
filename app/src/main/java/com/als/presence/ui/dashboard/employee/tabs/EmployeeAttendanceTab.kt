package com.als.presence.ui.dashboard.employee.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.MyAttendanceContent
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Employee "Attendance" tab.
 *
 * Displays a unified header with PresenceRefreshButton, followed by month selector
 * and daily attendance logs.
 */
@Composable
fun EmployeeAttendanceTab(
    viewModel: EmployeeAttendanceViewModel,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = { viewModel.refresh() }
) {
    MyAttendanceContent(
        modifier = modifier,
        viewModel = viewModel,
        isScrollable = true,
        showHeader = true,
        onRefresh = onRefresh
    )
}

private data class LedgerEntryWithBalance(
    val entry: StaffEarningLedgerDto,
    val runningBalanceInr: Long
)

/* -------------------------------------------------------------------------
 * Personal history (used by "My Shifts & Schedule")
 * ---------------------------------------------------------------------- */

/** Register + overtime history + ledger, shown on the secondary My Shifts screen. */
@Composable
fun personalAttendanceHistoryTab(
    recordsState: UiState<List<AttendanceRecordDto>>,
    overtimeState: UiState<List<OvertimeDto>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    ledgerState: UiState<List<StaffEarningLedgerDto>> = UiState.Success(emptyList()),
    assignmentsState: UiState<List<ShiftAssignmentDto>> = UiState.Success(emptyList())
) {
    var section by remember { mutableIntStateOf(0) }
    val sections = listOf("Register", "Overtime", "My Earning Ledger")
    val scrollState = rememberScrollState()

    val records = (recordsState as? UiState.Success)?.data.orEmpty()
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) 16.dp else 0.dp)
            .padding(top = if (isScrollable) 16.dp else 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Personal Workspace",
            trailing = {
                PresenceRefreshButton(
                    onClick = onRefresh,
                    contentDescription = "Refresh personal workspace"
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "Days Present",
                value = records.count { it.status == "PRESENT" }.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Worked Hours",
                value = Dates.formatMinutes(records.sumOf { it.workedMinutes }),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            sections.forEachIndexed { index, label ->
                FilterChip(
                    selected = section == index,
                    onClick = { section = index },
                    label = { Text(label) }
                )
            }
        }

        when (section) {
            0 -> when (recordsState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance register…")
                is UiState.Error -> SectionError(recordsState.message)
                is UiState.Success -> {
                    val list = recordsState.data
                    if (list.isEmpty()) {
                        SectionEmpty("No attendance records for the past 30 days.")
                    } else {
                        list.forEach { record -> RecordRow(record) }
                    }
                }
            }

            1 -> when (overtimeState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading overtime…")
                is UiState.Error -> SectionError(overtimeState.message)
                is UiState.Success -> {
                    val entries = overtimeState.data
                    if (entries.isEmpty()) {
                        SectionEmpty("No overtime has been booked for you.")
                    } else {
                        entries.forEach { entry -> OvertimeRow(entry) }
                    }
                }
            }

            else -> EarningsSection(
                ledgerState = ledgerState,
                assignmentsState = assignmentsState
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EarningsSection(
    ledgerState: UiState<List<StaffEarningLedgerDto>>,
    assignmentsState: UiState<List<ShiftAssignmentDto>> = UiState.Success(emptyList())
) {
    when (ledgerState) {
        is UiState.Loading, is UiState.Idle -> SectionLoading("Loading earning ledger…")
        is UiState.Error -> SectionError(ledgerState.message)
        is UiState.Success -> {
            val rawEntries = ledgerState.data
            val entries = remember(rawEntries) {
                rawEntries
                    .filter { e ->
                        val dateIso = e.earningDate.take(10)
                        dateIso <= Dates.todayIso()
                    }
                    .distinctBy { "${it.earningDate.take(10)}::${it.type}" }
            }

            val entriesWithBalance = remember(entries) {
                val sorted = entries.sortedBy { it.earningDate }
                var accum = 0L
                sorted.map { item ->
                    accum += item.amountCents
                    LedgerEntryWithBalance(item, accum)
                }
            }

            if (entriesWithBalance.isEmpty()) {
                SectionEmpty("No earning ledger entries recorded yet.")
            } else {
                val totalOverallBalanceInr = remember(entriesWithBalance) { entriesWithBalance.lastOrNull()?.runningBalanceInr ?: 0L }

                // Calculate Month-wise grouping with running monthly balance
                val monthGroups = remember(entriesWithBalance) {
                    val grouped = entriesWithBalance.groupBy { formatMonthYear(it.entry.earningDate) }
                    var runningSum = 0L
                    grouped.entries
                        .sortedBy { (monthKey, _) -> monthKey }
                        .map { (monthKey, monthEntries) ->
                            val monthTotal = monthEntries.sumOf { it.entry.amountCents }
                            runningSum += monthTotal
                            Triple(monthKey, monthEntries.sortedByDescending { it.entry.earningDate }, runningSum)
                        }
                        .sortedByDescending { (monthKey, _, _) -> monthKey }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Overall Running Balance Summary Card
                    OverallRunningBalanceCard(
                        totalBalanceInr = totalOverallBalanceInr,
                        totalEntriesCount = entriesWithBalance.size
                    )

                    monthGroups.forEach { (monthKey, monthEntries, monthlyRunningBalance) ->
                        val monthTotal = monthEntries.sumOf { it.entry.amountCents }
                        val monthTitle = formatMonthHeader(monthKey)

                        // 2. Monthly Running Balance Header
                        MonthlyRunningBalanceHeader(
                            monthTitle = monthTitle,
                            monthTotalInr = monthTotal,
                            monthlyRunningBalanceInr = monthlyRunningBalance,
                            entryCount = monthEntries.size
                        )

                        // 3. Group day-by-day within the month
                        val dayGroups = monthEntries.groupBy { it.entry.earningDate.take(10) }
                        dayGroups.forEach { (dateIso, dayEntries) ->
                            DayLedgerCard(
                                dateIso = dateIso,
                                dayEntries = dayEntries
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallRunningBalanceCard(
    totalBalanceInr: Long,
    totalEntriesCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "OVERALL RUNNING BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                Text(
                    text = Money.format(totalBalanceInr),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "$totalEntriesCount ${if (totalEntriesCount == 1) "Entry" else "Entries"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthlyRunningBalanceHeader(
    monthTitle: String,
    monthTotalInr: Long,
    monthlyRunningBalanceInr: Long,
    entryCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$entryCount ${if (entryCount == 1) "entry" else "entries"} • Month Total: ${Money.format(monthTotalInr)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Running Balance",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Money.format(monthlyRunningBalanceInr),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DayLedgerCard(
    dateIso: String,
    dayEntries: List<LedgerEntryWithBalance>
) {
    val dayTotalInr = remember(dayEntries) { dayEntries.sumOf { it.entry.amountCents } }
    val dateDisplay = remember(dateIso) { Dates.formatDate(dateIso) }
    val dayOfWeek = remember(dateIso) {
        runCatching {
            val ld = LocalDate.parse(dateIso.take(10))
            ld.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }.getOrDefault("")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateDisplay • $dayOfWeek",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+${Money.format(dayTotalInr)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SecondaryEmerald
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Day Individual Accrual Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dayEntries.forEach { item ->
                    val entry = item.entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (entry.type.uppercase()) {
                                            "SHIFT" -> MaterialTheme.colorScheme.primaryContainer
                                            "OVERTIME" -> MaterialTheme.colorScheme.tertiaryContainer
                                            "HOLIDAY" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (entry.type.uppercase()) {
                                        "SHIFT" -> Icons.Default.Schedule
                                        "OVERTIME" -> Icons.Default.MoreTime
                                        "HOLIDAY" -> Icons.Default.CalendarMonth
                                        else -> Icons.AutoMirrored.Filled.TrendingUp
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = when (entry.type.uppercase()) {
                                    "SHIFT" -> "Shift"
                                    "OVERTIME" -> "Overtime"
                                    "HOLIDAY" -> "Holiday Pay"
                                    "OFF_DAY", "WEEKLY_OFF" -> "Weekly Off Pay"
                                    else -> entry.type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${Money.format(entry.amountCents)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryEmerald
                            )
                            if (item.runningBalanceInr > 0) {
                                Text(
                                    text = "Bal: ${Money.format(item.runningBalanceInr)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMonthYear(iso: String?): String {
    if (iso.isNullOrBlank()) return "0000-00"
    return try {
        val date = LocalDate.parse(iso.take(10))
        "${date.year}-${String.format("%02d", date.monthValue)}"
    } catch (_: Exception) {
        "0000-00"
    }
}

private fun formatMonthHeader(iso: String?): String {
    if (iso.isNullOrBlank()) return "Other"
    return try {
        val date = if (iso.length == 7) LocalDate.parse("$iso-01") else LocalDate.parse(iso.take(10))
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (_: Exception) {
        "Other"
    }
}



@Composable
private fun RecordRow(record: AttendanceRecordDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${Dates.formatDate(record.workDate)} • ${Dates.dayName(record.workDate)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.shiftName ?: if (record.overtimeId != null) "Overtime" else "Shift",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusPill(record.status)
            }

            if (!record.assignedStartTime.isNullOrBlank() || !record.assignedEndTime.isNullOrBlank()) {
                val lateText = Dates.formatLateText(record.clockIn, record.assignedStartTime, record.lateMinutes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned Shift: ${Dates.formatTime(record.assignedStartTime)} – ${Dates.formatTime(record.assignedEndTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lateText.isNotEmpty()) {
                        Text(
                            text = "⏰ $lateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Clock In: ${Dates.formatTime(record.clockIn)} • Clock Out: ${Dates.formatTime(record.clockOut)} (${Dates.formatMinutes(record.workedMinutes)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OvertimeRow(entry: OvertimeDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${Dates.formatDate(entry.workDate)} • ${Dates.formatTime(entry.startTime)} – ${Dates.formatTime(entry.endTime)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Dates.formatMinutes(entry.plannedMinutes)} planned • Pay: ${Money.format(entry.paymentInr)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(entry.status)
        }
    }
}


