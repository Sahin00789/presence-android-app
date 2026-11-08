package com.als.presence.ui.dashboard.common

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import java.time.LocalDate
import java.time.YearMonth

private data class DayAttendanceGroup(
    val dateIso: String,
    val dateDisplay: String,
    val dayOfWeekName: String,
    val shifts: List<AttendanceRecordDto>,
    val overtimes: List<OvertimeDto>,
    val ledgerEntries: List<StaffEarningLedgerDto>
)

/**
 * Screen: "My Attendance" for Staff & Managers.
 * Shift-based actual shifts attended log with month-year scrollable chip selector
 * and monthly shift attendance statistics.
 */
@Composable
fun MyAttendanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel()
) {
    Box(modifier = modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "PERSONAL WORKSPACE",
            title = "My Attendance",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            MyAttendanceContent(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
                isScrollable = false
            )
        }
    }
}

@Composable
fun MyAttendanceContent(
    modifier: Modifier = Modifier,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel(),
    isScrollable: Boolean = true
) {
    val recordsState by viewModel.records.collectAsStateWithLifecycle()
    val overtimeState by viewModel.overtime.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerEntries.collectAsStateWithLifecycle()

    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentMonth = remember { YearMonth.now() }
    val monthOffsetList = remember { (-60..0).toList() }
    val monthListState = rememberLazyListState()

    LaunchedEffect(selectedMonth) {
        viewModel.loadForMonth(selectedMonth)
    }

    LaunchedEffect(Unit) {
        monthListState.scrollToItem(monthOffsetList.lastIndex.coerceAtLeast(0))
    }

    val monthName = selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthYearLabel = "$monthName ${selectedMonth.year}"

    val allRecords = (recordsState as? UiState.Success)?.data.orEmpty()
    val allOvertimes = (overtimeState as? UiState.Success)?.data.orEmpty()
    val allLedgerEntries = (ledgerState as? UiState.Success)?.data.orEmpty()

    // Filter out un-attended shifts (no clock-in and non-resolved/unattended status)
    val attendedRecords = remember(allRecords) {
        allRecords.filter { r ->
            !r.clockIn.isNullOrBlank() || r.status in listOf("PRESENT", "HALF_DAY", "ABSENT", "LEAVE", "COMPLETED", "RESOLVED")
        }
    }

    val attendedOvertimes = remember(allOvertimes) {
        allOvertimes.filter { ot ->
            ot.status in listOf("COMPLETED", "APPROVED", "IN_PROGRESS") || ot.workedMinutes > 0 || ot.attendanceEventId != null
        }
    }

    val monthShifts = remember(attendedRecords, selectedMonth) {
        attendedRecords.filter { r ->
            val localDate = runCatching { LocalDate.parse(r.workDate.take(10)) }.getOrNull()
            localDate != null && localDate.year == selectedMonth.year && localDate.month == selectedMonth.month
        }
    }

    val dayGroups = remember(attendedRecords, attendedOvertimes, allLedgerEntries, selectedMonth) {
        val dateSet = mutableSetOf<String>()
        attendedRecords.forEach { r ->
            val d = r.workDate.take(10)
            if (d.length == 10) dateSet.add(d)
        }
        attendedOvertimes.forEach { ot ->
            val d = ot.workDate.take(10)
            if (d.length == 10) dateSet.add(d)
        }
        allLedgerEntries.forEach { le ->
            val d = le.earningDate.take(10)
            if (d.length == 10) dateSet.add(d)
        }

        dateSet
            .mapNotNull { dateIso ->
                val localDate = runCatching { LocalDate.parse(dateIso) }.getOrNull()
                if (localDate != null && localDate.year == selectedMonth.year && localDate.month == selectedMonth.month) {
                    val shifts = attendedRecords.filter { it.workDate.take(10) == dateIso }
                    val overtimes = attendedOvertimes.filter { it.workDate.take(10) == dateIso }
                    val ledger = allLedgerEntries.filter { it.earningDate.take(10) == dateIso }

                    val dayOfWeek = localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

                    DayAttendanceGroup(
                        dateIso = dateIso,
                        dateDisplay = Dates.formatDate(dateIso),
                        dayOfWeekName = dayOfWeek,
                        shifts = shifts,
                        overtimes = overtimes,
                        ledgerEntries = ledger
                    )
                } else null
            }
            .sortedByDescending { it.dateIso }
    }

    val monthLedgerEntries = remember(allLedgerEntries, selectedMonth) {
        allLedgerEntries.filter { le ->
            val localDate = runCatching { LocalDate.parse(le.earningDate.take(10)) }.getOrNull()
            localDate != null && localDate.year == selectedMonth.year && localDate.month == selectedMonth.month
        }
    }

    val totalShiftsAttended = monthShifts.size
    val presentCount = monthShifts.count { it.status == "PRESENT" || it.status == "COMPLETED" }
    val halfDayCount = monthShifts.count { it.status == "HALF_DAY" }
    val leaveCount = monthShifts.count { it.status == "LEAVE" }
    val absentCount = monthShifts.count { it.status == "ABSENT" }
    val holidayCount = monthLedgerEntries.count { it.type.equals("HOLIDAY", ignoreCase = true) }
        .coerceAtLeast(monthShifts.count { it.status.equals("HOLIDAY", ignoreCase = true) })
    val offDayCount = monthLedgerEntries.count { it.type.equals("OFF_DAY", ignoreCase = true) }
        .coerceAtLeast(monthShifts.count { it.status.equals("OFF_DAY", ignoreCase = true) })
    val totalOvertimesCount = dayGroups.sumOf { it.overtimes.size }

    val scrollState = rememberScrollState()
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) 14.dp else 0.dp)
            .padding(top = if (isScrollable) 10.dp else 0.dp, bottom = if (isScrollable) 28.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month-Year Scrollable Chip Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            LazyRow(
                state = monthListState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(monthOffsetList.size) { index ->
                    val offset = monthOffsetList[index]
                    val month = currentMonth.plusMonths(offset.toLong())
                    val isSelected = month == selectedMonth
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMonth = month },
                        label = {
                            Text(
                                text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year}"
                            )
                        }
                    )
                }
            }
        }

        // 2. Selected Month Attendance Stats Card (Compact Single Row Chips)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$monthYearLabel Stats".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    StatusPill("$totalShiftsAttended Shifts Attended")
                }

                // Two Row Compact Count Chips (Shift related in Row 1, Overtime/Holiday/Off-day in Row 2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactStatChip(
                            label = "Present",
                            count = presentCount,
                            badgeColor = SecondaryEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatChip(
                            label = "Half Day",
                            count = halfDayCount,
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatChip(
                            label = "Leave",
                            count = leaveCount,
                            badgeColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatChip(
                            label = "Absent",
                            count = absentCount,
                            badgeColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactStatChip(
                            label = "Holiday",
                            count = holidayCount,
                            badgeColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatChip(
                            label = "Off Day",
                            count = offDayCount,
                            badgeColor = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatChip(
                            label = "Overtime",
                            count = totalOvertimesCount,
                            badgeColor = SecondaryEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Date-by-date Attendance Log Cards
        SectionHeader(title = "$monthYearLabel Register Log")
        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance records…")
            is UiState.Error -> SectionError((recordsState as UiState.Error).message)
            is UiState.Success -> {
                if (dayGroups.isEmpty()) {
                    SectionEmpty("No attendance records found for $monthYearLabel.")
                } else {
                    dayGroups.forEach { group -> AttendanceDayCard(group) }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun AttendanceDayCard(group: DayAttendanceGroup) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Date & Shifts Attended Count (NO day status pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${group.dateDisplay} • ${group.dayOfWeekName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val summaryText = buildString {
                            if (group.shifts.isNotEmpty()) append("${group.shifts.size} Actual Shift${if (group.shifts.size > 1) "s" else ""} Attended")
                            if (group.overtimes.isNotEmpty()) {
                                if (isNotEmpty()) append(" • ")
                                append("${group.overtimes.size} Overtime")
                            }
                        }
                        if (summaryText.isNotEmpty()) {
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val headerBadgeLabel = when {
                    group.shifts.isNotEmpty() -> "${group.shifts.size} Shift${if (group.shifts.size > 1) "s" else ""}"
                    group.overtimes.isNotEmpty() -> "${group.overtimes.size} Overtime"
                    else -> "Ledger Log"
                }
                StatusPill(headerBadgeLabel)
            }

            // 1. Actual Shifts Attended Section
            if (group.shifts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Text(
                    text = "ACTUAL SHIFTS ATTENDED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                group.shifts.forEach { shift ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shift.shiftName ?: if (shift.overtimeId != null) "Overtime" else "Shift",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!shift.assignedStartTime.isNullOrBlank() || !shift.assignedEndTime.isNullOrBlank()) {
                                val lateText = Dates.formatLateText(shift.clockIn, shift.assignedStartTime, shift.lateMinutes)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Assigned: ${Dates.formatTime(shift.assignedStartTime)} – ${Dates.formatTime(shift.assignedEndTime)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (!shift.clockIn.isNullOrBlank() || !shift.clockOut.isNullOrBlank()) {
                                        "Clock In/Out: ${Dates.formatTime(shift.clockIn)} – ${Dates.formatTime(shift.clockOut)}"
                                    } else {
                                        "Not Clocked In"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (shift.workedMinutes > 0) {
                                Text(
                                    text = "Worked: ${shift.workedMinutes / 60}h ${shift.workedMinutes % 60}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(shift.status)
                    }
                }
            }

            // 2. Overtime Section (if overtimes exist)
            if (group.overtimes.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Text(
                    text = "OVERTIME SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                group.overtimes.forEach { ot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "${Dates.formatTime(ot.startTime)} – ${Dates.formatTime(ot.endTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Worked: ${ot.workedMinutes} mins / Planned: ${ot.plannedMinutes} mins",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusPill(ot.status)
                            if (ot.paymentInr > 0) {
                                Text(
                                    text = "+${Money.format(ot.paymentInr)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryEmerald
                                )
                            }
                        }
                    }
                }
            }

            // 3. Earning Ledger Accruals (if ledgerEntries exist)
            if (group.ledgerEntries.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Text(
                    text = "ACCRUED EARNINGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = SecondaryEmerald
                )
                group.ledgerEntries.forEach { entry ->
                    val amt = entry.amountCents
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+" + Money.format(amt),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatChip(
    label: String,
    count: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
