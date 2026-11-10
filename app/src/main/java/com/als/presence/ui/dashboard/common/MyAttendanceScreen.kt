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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.data.model.UnworkedPaymentRuleDto
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import java.time.LocalDate
import java.time.YearMonth

private data class DayAttendanceGroup(
    val dateIso: String,
    val dateDisplay: String,
    val dayOfWeekName: String,
    val isToday: Boolean,
    val shifts: List<AttendanceRecordDto>,
    val overtimes: List<OvertimeDto>,
    val ledgerEntries: List<StaffEarningLedgerDto>,
    val isWeeklyOff: Boolean = false,
    val isHoliday: Boolean = false
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
    isScrollable: Boolean = true,
    showHeader: Boolean = false,
    onRefresh: () -> Unit = { viewModel.refresh() }
) {
    val recordsState by viewModel.records.collectAsStateWithLifecycle()
    val overtimeState by viewModel.overtime.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val assignmentsState by viewModel.assignments.collectAsStateWithLifecycle()
    val templatesState by viewModel.templates.collectAsStateWithLifecycle()
    val unworkedRulesState by viewModel.unworkedRules.collectAsStateWithLifecycle()

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
    val assignments = (assignmentsState as? UiState.Success)?.data.orEmpty()
    val templates = (templatesState as? UiState.Success)?.data.orEmpty()
    val unworkedRules = (unworkedRulesState as? UiState.Success)?.data.orEmpty()

    val earliestAssignmentDateIso = remember(assignments) {
        assignments.mapNotNull { it.createdAt?.take(10) }.minOrNull() ?: Dates.todayIso()
    }
    val earliestYearMonth = remember(earliestAssignmentDateIso) {
        runCatching {
            val ld = LocalDate.parse(earliestAssignmentDateIso)
            YearMonth.of(ld.year, ld.month)
        }.getOrDefault(currentMonth)
    }

    val displayMonths = remember(earliestYearMonth, currentMonth) {
        val list = mutableListOf<YearMonth>()
        var ym = earliestYearMonth
        while (!ym.isAfter(currentMonth)) {
            list.add(ym)
            ym = ym.plusMonths(1)
        }
        if (list.isEmpty()) list.add(currentMonth)
        list
    }

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



    val dayGroups = remember(attendedRecords, attendedOvertimes, allLedgerEntries, assignments, templates, selectedMonth, earliestAssignmentDateIso) {
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
        allRecords.forEach { r ->
            val d = r.workDate.take(10)
            if (d.length == 10) dateSet.add(d)
        }
        if (selectedMonth == currentMonth) {
            dateSet.add(Dates.todayIso())
        }

        dateSet
            .filter { dateIso -> dateIso >= earliestAssignmentDateIso && dateIso <= Dates.todayIso() }
            .mapNotNull { dateIso ->
                val localDate = runCatching { LocalDate.parse(dateIso) }.getOrNull()
                if (localDate != null && localDate.year == selectedMonth.year && localDate.month == selectedMonth.month) {
                    val shifts = attendedRecords.filter { it.workDate.take(10) == dateIso }
                    val overtimes = attendedOvertimes.filter { it.workDate.take(10) == dateIso }
                    val ledger = allLedgerEntries.filter { it.earningDate.take(10) == dateIso }

                    val dayOfWeek = localDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                    val isToday = dateIso == Dates.todayIso()

                    val hasShiftOnDay = assignments.any { a ->
                        val t = templates.firstOrNull { it.id == a.shiftId }
                        t != null && Dates.isShiftMatchingDay(t.dayName, dateIso, t.daysOfWeek)
                    }
                    val isWeeklyOff = !hasShiftOnDay && (
                        ledger.any { it.type.equals("WEEKLY_OFF", ignoreCase = true) || it.type.equals("OFF_DAY", ignoreCase = true) } ||
                        shifts.any { it.status.equals("OFF_DAY", ignoreCase = true) || it.status.equals("WEEKLY_OFF", ignoreCase = true) } ||
                        (shifts.isEmpty() && overtimes.isEmpty() && !isToday)
                    )

                    val isHoliday = ledger.any { it.type.equals("HOLIDAY", ignoreCase = true) } ||
                        shifts.any { it.status.equals("HOLIDAY", ignoreCase = true) }

                    DayAttendanceGroup(
                        dateIso = dateIso,
                        dateDisplay = Dates.formatDate(dateIso),
                        dayOfWeekName = dayOfWeek,
                        isToday = isToday,
                        shifts = shifts,
                        overtimes = overtimes,
                        ledgerEntries = ledger,
                        isWeeklyOff = isWeeklyOff,
                        isHoliday = isHoliday
                    )
                } else null
            }
            .sortedByDescending { it.dateIso }
    }



    val totalShiftsAttended = dayGroups.count { g -> g.shifts.any { s -> s.status in listOf("PRESENT", "HALF_DAY", "COMPLETED") } }
    val presentCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "PRESENT" || s.status == "COMPLETED" } }
    val halfDayCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "HALF_DAY" } }
    val leaveCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "LEAVE" } }
    val absentCount = dayGroups.count { g -> (g.shifts.isNotEmpty() && g.shifts.all { s -> s.status == "ABSENT" }) || (!g.isHoliday && !g.isWeeklyOff && g.shifts.isEmpty() && g.overtimes.isEmpty() && !g.isToday) }
    val holidayCount = dayGroups.count { it.isHoliday }
    val offDayCount = dayGroups.count { it.isWeeklyOff }
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
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance Register",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "Shift presence, overtime logs & work calendar",
                        style = MaterialTheme.typography.bodySmall,
                        color = PresenceDesign.Muted
                    )
                }
                PresenceRefreshButton(
                    onClick = onRefresh,
                    isRefreshing = recordsState is UiState.Loading,
                    contentDescription = "Refresh attendance"
                )
            }
        }

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
                items(displayMonths.size) { index ->
                    val month = displayMonths[index]
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

        // 3. Date-by-date Attendance Log Cards
        SectionHeader(title = "$monthYearLabel Register Log")
        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance records…")
            is UiState.Error -> SectionError((recordsState as UiState.Error).message)
            is UiState.Success -> {
                if (dayGroups.isEmpty()) {
                    SectionEmpty("No attendance records found for $monthYearLabel.")
                } else {
                    dayGroups.forEach { group ->
                        AttendanceDayCard(
                            group = group,
                            assignments = assignments,
                            templates = templates,
                            unworkedRules = unworkedRules
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun AttendanceDayCard(
    group: DayAttendanceGroup,
    assignments: List<ShiftAssignmentDto>,
    templates: List<ShiftDto>,
    unworkedRules: List<UnworkedPaymentRuleDto>
) {
    val primaryAssignment = assignments.firstOrNull()
    val presentPayInr = (primaryAssignment?.presentPay ?: 600.0).toLong()
    val halfDayPayInr = (primaryAssignment?.halfDayPay ?: (presentPayInr / 2.0)).toLong()
    val leavePayInr = (primaryAssignment?.leavePay ?: presentPayInr).toLong()
    val absentPayInr = (primaryAssignment?.absentPay ?: 0.0).toLong()

    val todayIso = Dates.todayIso()
    val hasMatchingShiftToday = assignments.any { a ->
        val template = templates.firstOrNull { it.id == a.shiftId }
        template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
    }

    val startTimeRaw = primaryAssignment?.let { a -> templates.find { it.id == a.shiftId }?.startTime }
    val endTimeRaw = primaryAssignment?.let { a -> templates.find { it.id == a.shiftId }?.endTime }
    val isSessionRunningNow = group.isToday && hasMatchingShiftToday && Dates.isWithinTimeWindow(startTimeRaw, endTimeRaw)

    // Calculate Total Day Earnings for Top Corner
    val totalEarningInr = remember(group, assignments, templates, unworkedRules) {
        val ledgerSum = group.ledgerEntries.sumOf { it.amountCents }
        if (group.ledgerEntries.isNotEmpty()) {
            ledgerSum
        } else {
            var sum = 0L
            group.shifts.forEach { s ->
                sum += calculateShiftEarning(s, assignments, unworkedRules, group.ledgerEntries)
            }
            group.overtimes.forEach { ot ->
                if (ot.status in listOf("COMPLETED", "APPROVED", "IN_PROGRESS")) {
                    sum += ot.paymentInr
                }
            }
            if (group.shifts.isEmpty() && group.overtimes.isEmpty()) {
                if (group.isHoliday) {
                    val r = unworkedRules.find { it.employeeId != null && it.dayType == "HOLIDAY" }
                        ?: unworkedRules.find { it.dayType == "HOLIDAY" }
                    sum += r?.paymentInr ?: presentPayInr
                } else if (group.isWeeklyOff) {
                    val r = unworkedRules.find { it.employeeId != null && it.dayType == "WEEKLY_OFF" }
                        ?: unworkedRules.find { it.dayType == "WEEKLY_OFF" }
                    sum += r?.paymentInr ?: presentPayInr
                }
            }
            sum
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (group.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        border = if (group.isToday) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Date info on Left, Total Earning at Top Corner
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (group.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${group.dateDisplay} • ${group.dayOfWeekName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (group.isToday) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "TODAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val summaryText = buildString {
                            if (group.shifts.isNotEmpty()) append("${group.shifts.size} Shift${if (group.shifts.size > 1) "s" else ""}")
                            if (group.overtimes.isNotEmpty()) {
                                if (isNotEmpty()) append(" • ")
                                append("${group.overtimes.size} Overtime")
                            }
                            if (isEmpty()) {
                                if (group.isHoliday) append("Public Holiday")
                                else if (group.isWeeklyOff) append("Weekly Off")
                                else if (group.isToday && hasMatchingShiftToday) append("Scheduled Shift")
                                else append("No Shifts Scheduled")
                            }
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Top Right Corner: Small subtle earnings text
                if (totalEarningInr > 0) {
                    Text(
                        text = "Earned: ${Money.format(totalEarningInr)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF747895)
                    )
                }
            }

            // 1. Shift Sub-cards (Attended or Actual)
            if (group.shifts.isNotEmpty()) {
                group.shifts.forEach { shift ->
                    ShiftSubCard(
                        shift = shift,
                        assignments = assignments,
                        unworkedRules = unworkedRules,
                        ledgerEntries = group.ledgerEntries
                    )
                }
            } else if (group.isToday && hasMatchingShiftToday) {
                // Today's Unclocked Assigned Shift Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = primaryAssignment?.shiftName ?: "Assigned Shift",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Assigned: ${Dates.formatTime(startTimeRaw)} – ${Dates.formatTime(endTimeRaw)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        StatusPill(if (isSessionRunningNow) "RUNNING" else "UPCOMING")
                    }
                }
            }

            // 2. Overtime Sub-cards
            if (group.overtimes.isNotEmpty()) {
                group.overtimes.forEach { ot ->
                    OvertimeSubCard(ot = ot)
                }
            }

            // 3. Weekly Off Sub-card
            if (group.isWeeklyOff && group.shifts.isEmpty() && group.overtimes.isEmpty()) {
                WeeklyOffSubCard(
                    unworkedRules = unworkedRules,
                    assignments = assignments
                )
            }

            // 4. Holiday Sub-card
            if (group.isHoliday && group.shifts.isEmpty() && group.overtimes.isEmpty()) {
                HolidaySubCard(
                    unworkedRules = unworkedRules,
                    assignments = assignments
                )
            }

            // 5. Accrued Ledger Entries (if any standalone entries exist)
            val standaloneLedger = group.ledgerEntries.filter { le ->
                le.type !in listOf("SHIFT", "OVERTIME", "HOLIDAY", "OFF_DAY", "WEEKLY_OFF")
            }
            if (standaloneLedger.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                standaloneLedger.forEach { entry ->
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
                            text = "+" + Money.format(entry.amountCents),
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
private fun ShiftSubCard(
    shift: AttendanceRecordDto,
    assignments: List<ShiftAssignmentDto>,
    unworkedRules: List<UnworkedPaymentRuleDto>,
    ledgerEntries: List<StaffEarningLedgerDto>
) {
    val shiftEarning = calculateShiftEarning(shift, assignments, unworkedRules, ledgerEntries)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shift.shiftName ?: if (shift.overtimeId != null) "Overtime Shift" else "Shift",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!shift.assignedStartTime.isNullOrBlank() || !shift.assignedEndTime.isNullOrBlank()) {
                        Text(
                            text = "Assigned: ${Dates.formatTime(shift.assignedStartTime)} – ${Dates.formatTime(shift.assignedEndTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(shift.status)
                    Text(
                        text = Money.format(shiftEarning),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (shift.status == "ABSENT" && shiftEarning == 0L) MaterialTheme.colorScheme.error else SecondaryEmerald
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (!shift.clockIn.isNullOrBlank()) {
                            "Clock In: ${Dates.formatTime(shift.clockIn)} • Out: ${if (!shift.clockOut.isNullOrBlank()) Dates.formatTime(shift.clockOut) else "Pending"}"
                        } else {
                            "Not Clocked In"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
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

            val lateMins = if (shift.lateMinutes > 0) shift.lateMinutes else Dates.calculateLateMinutes(shift.clockIn, shift.assignedStartTime)
            val earlyMins = calculateEarlyDepartureMinutes(shift.clockOut, shift.assignedEndTime)

            if (lateMins > 0 || earlyMins > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lateMins > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "⏰ Late: ${Dates.formatMinutes(lateMins)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (earlyMins > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TertiaryAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, TertiaryAmber.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🏃 Early Dep: ${Dates.formatMinutes(earlyMins)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (!shift.clockIn.isNullOrBlank()) {
                val outsideCount = shift.outsidePingCount
                val workedMins = shift.workedMinutes
                val expectedPings = if (workedMins > 0) (workedMins / 10).coerceAtLeast(outsideCount + 1) else if (shift.clockOut == null) 1 else 0
                val insideCount = (expectedPings - outsideCount).coerceAtLeast(0)
                val missedCount = if (shift.scheduledMinutes > 0 && workedMins > 0 && workedMins < shift.scheduledMinutes) {
                    ((shift.scheduledMinutes - workedMins) / 10).coerceAtLeast(0)
                } else 0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    PingBadge(label = "Inside", count = insideCount, color = SecondaryEmerald)
                    PingBadge(label = "Outside", count = outsideCount, color = if (outsideCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                    PingBadge(label = "Missed", count = missedCount, color = if (missedCount > 0) TertiaryAmber else MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun OvertimeSubCard(ot: OvertimeDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = ot.reason?.ifBlank { "Overtime Session" } ?: "Overtime Session",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(ot.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Window: ${Dates.formatTime(ot.startTime)} – ${Dates.formatTime(ot.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Worked: ${ot.workedMinutes}m / Planned: ${ot.plannedMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ot.workedMinutes > 0 || ot.status == "IN_PROGRESS") {
                val expectedPings = (ot.workedMinutes / 10).coerceAtLeast(1)
                val otOutside = 0
                val otInside = (expectedPings - otOutside).coerceAtLeast(0)
                val otMissed = if (ot.plannedMinutes > 0 && ot.workedMinutes < ot.plannedMinutes) {
                    ((ot.plannedMinutes - ot.workedMinutes) / 10).coerceAtLeast(0)
                } else 0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PingBadge(label = "Inside", count = otInside, color = SecondaryEmerald)
                    PingBadge(label = "Outside", count = otOutside, color = MaterialTheme.colorScheme.outline)
                    PingBadge(label = "Missed", count = otMissed, color = if (otMissed > 0) TertiaryAmber else MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun WeeklyOffSubCard(
    unworkedRules: List<UnworkedPaymentRuleDto>,
    assignments: List<ShiftAssignmentDto>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Weekly Off",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scheduled Weekly Rest Day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill("OFF_DAY")
                val offDayPay = (unworkedRules.find { it.employeeId != null && it.dayType == "WEEKLY_OFF" }
                    ?: unworkedRules.find { it.dayType == "WEEKLY_OFF" })?.paymentInr ?: (assignments.firstOrNull()?.presentPay ?: 600.0).toLong()
                if (offDayPay > 0) {
                    Text(
                        text = "+${Money.format(offDayPay)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun HolidaySubCard(
    unworkedRules: List<UnworkedPaymentRuleDto>,
    assignments: List<ShiftAssignmentDto>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Public Holiday",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paid Holiday",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill("HOLIDAY")
                val holidayPay = (unworkedRules.find { it.employeeId != null && it.dayType == "HOLIDAY" }
                    ?: unworkedRules.find { it.dayType == "HOLIDAY" })?.paymentInr ?: (assignments.firstOrNull()?.presentPay ?: 600.0).toLong()
                if (holidayPay > 0) {
                    Text(
                        text = "+${Money.format(holidayPay)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun PingBadge(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
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

@Composable
private fun AttendanceOutcomeChip(
    label: String,
    value: String,
    isSelected: Boolean,
    tone: PresenceTone,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) when (tone) {
            PresenceTone.Success -> Color(0xFFE6F4EA)
            PresenceTone.Warning -> Color(0xFFFEF3C7)
            PresenceTone.Info -> Color(0xFFEEF2FF)
            PresenceTone.Danger -> Color(0xFFFCE8E6)
            else -> MaterialTheme.colorScheme.surfaceVariant
        } else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) when (tone) {
                PresenceTone.Success -> Color(0xFF34D399)
                PresenceTone.Warning -> Color(0xFFFBBF24)
                PresenceTone.Info -> Color(0xFF818CF8)
                PresenceTone.Danger -> Color(0xFFF87171)
                else -> MaterialTheme.colorScheme.outline
            } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) when (tone) {
                    PresenceTone.Success -> Color(0xFF137333)
                    PresenceTone.Warning -> Color(0xFFD97706)
                    PresenceTone.Info -> Color(0xFF4338CA)
                    PresenceTone.Danger -> Color(0xFFC5221F)
                    else -> PresenceDesign.Ink
                } else PresenceDesign.LabelText,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PresenceDesign.Ink,
                maxLines = 1
            )
        }
    }
}

private fun calculateEarlyDepartureMinutes(clockOut: String?, assignedEndTime: String?): Int {
    if (clockOut.isNullOrBlank() || assignedEndTime.isNullOrBlank()) return 0
    return try {
        val clockOutTime = if (clockOut.contains('T')) {
            val parsed = java.time.OffsetDateTime.parse(clockOut).toInstant()
            parsed.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        } else {
            java.time.LocalTime.parse(clockOut.take(8))
        }
        val endTime = java.time.LocalTime.parse(assignedEndTime.take(8))
        val diffSeconds = endTime.toSecondOfDay() - clockOutTime.toSecondOfDay()
        if (diffSeconds > 60) diffSeconds / 60 else 0
    } catch (_: Exception) {
        0
    }
}

private fun calculateShiftEarning(
    shift: AttendanceRecordDto,
    assignments: List<ShiftAssignmentDto>,
    unworkedRules: List<UnworkedPaymentRuleDto>,
    ledgerEntries: List<StaffEarningLedgerDto>
): Long {
    val ledgerMatch = ledgerEntries.firstOrNull { it.type.equals("SHIFT", ignoreCase = true) && it.earningDate.take(10) == shift.workDate.take(10) }
    if (ledgerMatch != null) return ledgerMatch.amountCents

    val primaryAssignment = assignments.firstOrNull()
    val presentPay = (primaryAssignment?.presentPay ?: 600.0).toLong()
    val halfDayPay = (primaryAssignment?.halfDayPay ?: (presentPay / 2.0)).toLong()
    val leavePay = (primaryAssignment?.leavePay ?: presentPay).toLong()
    val absentPay = (primaryAssignment?.absentPay ?: 0.0).toLong()

    return when (shift.status.uppercase()) {
        "PRESENT", "COMPLETED" -> presentPay
        "HALF_DAY" -> halfDayPay
        "LEAVE" -> leavePay
        "ABSENT" -> absentPay
        "HOLIDAY" -> (unworkedRules.find { it.employeeId != null && it.dayType == "HOLIDAY" }
            ?: unworkedRules.find { it.dayType == "HOLIDAY" })?.paymentInr ?: presentPay
        "OFF_DAY", "WEEKLY_OFF" -> (unworkedRules.find { it.employeeId != null && it.dayType == "WEEKLY_OFF" }
            ?: unworkedRules.find { it.dayType == "WEEKLY_OFF" })?.paymentInr ?: presentPay
        else -> 0L
    }
}
