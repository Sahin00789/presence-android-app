@file:OptIn(ExperimentalLayoutApi::class)

package com.als.presence.ui.dashboard.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.als.presence.ui.dashboard.common.components.PresenceBadge
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayAttendanceGroup(
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
 * Editorial, 2026 Redesign for "My Attendance" Screen.
 * Contains:
 * - Month/Year quick selector
 * - Monthly Attendance Summary Surface (KPIs: Present, Absent, Half Day, Leave, Worked Hours, Total Month Earnings)
 * - Calendar-style Attendance Visualization Grid (M, T, W, T, F, S, S) with semantic indicators
 * - Interactive Day Selection Filter
 * - Structured Daily Attendance List with shift details & earnings breakdown (NOT dozens of cards!)
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
    var selectedDayFilter by remember { mutableStateOf<String?>(null) }
    val currentMonth = remember { YearMonth.now() }
    val monthOffsetList = remember { (-60..0).toList() }
    val monthListState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(selectedMonth) {
        viewModel.loadForMonth(selectedMonth)
        selectedDayFilter = null
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

    val earliestAssignmentDateIso = remember(assignments, allRecords, allLedgerEntries) {
        val assignmentEarliest = assignments.mapNotNull { it.createdAt?.take(10) }.minOrNull()
        val recordEarliest = allRecords.mapNotNull { it.workDate.take(10) }.minOrNull()
        val ledgerEarliest = allLedgerEntries.mapNotNull { it.earningDate.take(10) }.minOrNull()
        listOfNotNull(assignmentEarliest, recordEarliest, ledgerEarliest).minOrNull() ?: Dates.todayIso()
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
            .filter { dateIso ->
                dateIso <= Dates.todayIso() && (
                    dateIso >= earliestAssignmentDateIso ||
                    attendedRecords.any { it.workDate.take(10) == dateIso } ||
                    allLedgerEntries.any { it.earningDate.take(10) == dateIso }
                )
            }
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

    // Monthly aggregates
    val presentCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "PRESENT" || s.status == "COMPLETED" } }
    val halfDayCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "HALF_DAY" } }
    val leaveCount = dayGroups.count { g -> g.shifts.any { s -> s.status == "LEAVE" } }
    val absentCount = dayGroups.count { g -> (g.shifts.isNotEmpty() && g.shifts.all { s -> s.status == "ABSENT" }) || (!g.isHoliday && !g.isWeeklyOff && g.shifts.isEmpty() && g.overtimes.isEmpty() && !g.isToday) }
    val totalWorkedMinutes = dayGroups.sumOf { g -> g.shifts.sumOf { it.workedMinutes } + g.overtimes.sumOf { it.workedMinutes } }

    val totalMonthEarnedInr = remember(dayGroups, assignments, templates, unworkedRules) {
        dayGroups.sumOf { group ->
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
                        sum += r?.paymentInr ?: (assignments.firstOrNull()?.presentPay?.toLong() ?: 600L)
                    } else if (group.isWeeklyOff) {
                        val r = unworkedRules.find { it.employeeId != null && it.dayType == "WEEKLY_OFF" }
                            ?: unworkedRules.find { it.dayType == "WEEKLY_OFF" }
                        sum += r?.paymentInr ?: (assignments.firstOrNull()?.presentPay?.toLong() ?: 600L)
                    }
                }
                sum
            }
        }
    }

    val filteredDayGroups = remember(dayGroups, selectedDayFilter) {
        if (selectedDayFilter == null) dayGroups else dayGroups.filter { it.dateIso == selectedDayFilter }
    }

    val scrollState = rememberScrollState()
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier
    val screenInfo = rememberScreenSizeInfo()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) screenInfo.horizontalScreenPadding else 0.dp)
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
                        text = "Attendance Records",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Calendar visualization, shift logs & daily earnings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 1. MONTH SELECTOR CHIP ROW
        // -------------------------------------------------------------
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
                                text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
        // 2. MONTHLY ATTENDANCE SUMMARY SURFACE (FINTECH / EDITORIAL)
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        PresenceMicroLabel(text = "$monthYearLabel Total Earned")
                        Text(
                            text = Money.format(totalMonthEarnedInr),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PresenceDesign.Positive
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        PresenceMicroLabel(text = "Total Worked")
                        Text(
                            text = Dates.formatMinutes(totalWorkedMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(12.dp))

                // 4 Semantic KPI tiles in a clean row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthlyStatPill(
                        label = "Present",
                        count = presentCount,
                        color = PresenceDesign.Positive,
                        bg = PresenceDesign.PositiveSoft,
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyStatPill(
                        label = "Half-Day",
                        count = halfDayCount,
                        color = PresenceDesign.CautionText,
                        bg = PresenceDesign.CautionSoft,
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyStatPill(
                        label = "Leave",
                        count = leaveCount,
                        color = Color(0xFF7C3AED),
                        bg = Color(0xFF7C3AED).copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyStatPill(
                        label = "Absent",
                        count = absentCount,
                        color = PresenceDesign.DangerText,
                        bg = PresenceDesign.DangerSoft,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 3. CALENDAR-STYLE ATTENDANCE VISUALIZATION GRID
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedDayFilter != null) {
                        Text(
                            text = "Show All Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .clickable { selectedDayFilter = null }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weekday Headers (Mon - Sun)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Days Grid
                val firstOfMonth = selectedMonth.atDay(1)
                val daysInMonth = selectedMonth.lengthOfMonth()
                val startDayOfWeek = firstOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val totalCells = ((startDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

                val dayMap = remember(dayGroups) {
                    dayGroups.associateBy { it.dateIso }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - (startDayOfWeek - 1) + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = selectedMonth.atDay(dayNum)
                                    val dateIso = date.toString()
                                    val group = dayMap[dateIso]
                                    val isSelected = selectedDayFilter == dateIso
                                    val isToday = dateIso == Dates.todayIso()

                                    // Determine day status color
                                    val statusColor = when {
                                        group?.shifts?.any { it.status == "PRESENT" || it.status == "COMPLETED" } == true -> PresenceDesign.Positive
                                        group?.shifts?.any { it.status == "HALF_DAY" } == true -> PresenceDesign.Caution
                                        group?.shifts?.any { it.status == "LEAVE" } == true -> Color(0xFF7C3AED)
                                        group?.isHoliday == true -> Color(0xFF0D9488)
                                        group?.isWeeklyOff == true -> MaterialTheme.colorScheme.outlineVariant
                                        group != null && (group.shifts.all { it.status == "ABSENT" } || date.isBefore(LocalDate.now())) -> PresenceDesign.Danger
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else if (isToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                BorderStroke(
                                                    if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent
                                                ),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDayFilter = if (selectedDayFilter == dateIso) null else dateIso
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (statusColor != Color.Transparent) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // Calendar Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Present", PresenceDesign.Positive)
                    LegendItem("Half-Day", PresenceDesign.Caution)
                    LegendItem("Leave", Color(0xFF7C3AED))
                    LegendItem("Absent", PresenceDesign.Danger)
                    LegendItem("Holiday", Color(0xFF0D9488))
                    LegendItem("Off", MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        // -------------------------------------------------------------
        // 4. STRUCTURED DAILY ATTENDANCE LIST (NOT CARDS!)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedDayFilter != null) "Attendance Log (${Dates.formatDate(selectedDayFilter!!)})" else "$monthYearLabel Daily Roster Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${filteredDayGroups.size} record${if (filteredDayGroups.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance records…")
            is UiState.Error -> SectionError((recordsState as UiState.Error).message)
            is UiState.Success -> {
                if (filteredDayGroups.isEmpty()) {
                    SectionEmpty("No attendance entries found for the selected period.")
                } else {
                    // Unified Surface containing structured rows
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            filteredDayGroups.forEachIndexed { index, group ->
                                AttendanceStructuredRow(
                                    group = group,
                                    assignments = assignments,
                                    templates = templates,
                                    unworkedRules = unworkedRules
                                )
                                if (index < filteredDayGroups.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MonthlyStatPill(
    label: String,
    count: Int,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
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
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttendanceStructuredRow(
    group: DayAttendanceGroup,
    assignments: List<ShiftAssignmentDto>,
    templates: List<ShiftDto>,
    unworkedRules: List<UnworkedPaymentRuleDto>
) {
    val primaryAssignment = assignments.firstOrNull()
    val presentPayInr = (primaryAssignment?.presentPay ?: 600.0).toLong()

    // Calculate Total Day Earnings
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

    val primaryShift = group.shifts.firstOrNull()
    val isPresent = primaryShift?.status in listOf("PRESENT", "COMPLETED")
    val isHalfDay = primaryShift?.status == "HALF_DAY"
    val isLeave = primaryShift?.status == "LEAVE"
    val isAbsent = primaryShift?.status == "ABSENT"

    val statusLabel = when {
        isPresent -> "PRESENT"
        isHalfDay -> "HALF_DAY"
        isLeave -> "LEAVE"
        isAbsent -> "ABSENT"
        group.isHoliday -> "HOLIDAY"
        group.isWeeklyOff -> "OFF_DAY"
        group.isToday -> "TODAY"
        else -> "NO_SHIFT"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date & Shift Context
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Date Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (group.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, if (group.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dateParsed = runCatching { LocalDate.parse(group.dateIso) }.getOrNull()
                    Text(
                        text = dateParsed?.month?.name?.take(3)?.uppercase() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (group.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${dateParsed?.dayOfMonth ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (group.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Shift Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = primaryShift?.shiftName ?: if (group.isHoliday) "Public Holiday" else if (group.isWeeklyOff) "Weekly Off" else primaryAssignment?.shiftName ?: "Assigned Shift",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (group.isToday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // In/Out time & worked duration
                val clockInStr = primaryShift?.clockIn?.let { Dates.formatTime(it) }
                val clockOutStr = primaryShift?.clockOut?.let { Dates.formatTime(it) }
                val timeDetail = when {
                    clockInStr != null && clockOutStr != null -> "In: $clockInStr • Out: $clockOutStr"
                    clockInStr != null -> "Clock In: $clockInStr"
                    group.isHoliday -> "Gazetted Holiday"
                    group.isWeeklyOff -> "Scheduled Weekly Off"
                    else -> "No Clock-In Recorded"
                }
                Text(
                    text = timeDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Status & Earning Breakdown
        Column(horizontalAlignment = Alignment.End) {
            StatusPill(statusLabel)
            if (totalEarningInr > 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${Money.format(totalEarningInr)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PresenceDesign.Positive
                )
            }
        }
    }
}

fun calculateEarlyDepartureMinutes(clockOut: String?, assignedEndTime: String?): Int {
    if (clockOut.isNullOrBlank() || assignedEndTime.isNullOrBlank()) return 0
    return try {
        val clockOutTime = Dates.parseLocalTime(clockOut) ?: return 0
        val endTime = Dates.parseLocalTime(assignedEndTime) ?: return 0
        val diffSeconds = endTime.toSecondOfDay() - clockOutTime.toSecondOfDay()
        if (diffSeconds > 60) diffSeconds / 60 else 0
    } catch (_: Exception) {
        0
    }
}

fun calculateShiftEarning(
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
