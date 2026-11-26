package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.LocalScreenSizeInfo
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import com.als.presence.ui.dashboard.common.PresenceSearchBar
import com.als.presence.ui.dashboard.common.PresenceSegmentedTabRow
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.common.components.PresenceBadge
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Monthly aggregate summary for a single employee.
 */
data class StaffMonthlySummary(
    val employeeId: String,
    val staffName: String,
    val staffCode: String,
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val lateCount: Int,
    val totalWorkedMinutes: Int,
    val totalLateMinutes: Int,
    val records: List<AttendanceRecordDto>
) {
    val attendanceRate: Int
        get() = if (totalDays > 0) (((presentDays + leaveDays + halfDays).toFloat() / totalDays) * 100).toInt() else 100
}

/**
 * Owner "Reports" tab — Redesigned with an editorial 2026 analytics aesthetic.
 * Features:
 * - Clean temporal navigation (Daily / Monthly) with quick jumps
 * - Atmospheric Gradient Hero with large KPI typography and a custom Canvas Donut Chart
 * - Monthly staff attendance comparison ranking bar visualization
 * - Structured information rows with hairline dividers (no repetitive nested cards)
 * - Detailed Employee Monthly Inspection Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OwnerReportsTab(
    recordsState: UiState<List<AttendanceRecordDto>>,
    premisesState: UiState<List<PremisesDto>> = UiState.Success(emptyList()),
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    user: User? = null,
    initialPremisesId: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val premisesScrollState = rememberScrollState()

    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()
    val defaultPremisesId = initialPremisesId ?: if (user?.role == Role.EMPLOYEE || premisesList.size == 1) premisesList.firstOrNull()?.id else null
    var selectedPremisesId by remember(defaultPremisesId) { mutableStateOf(defaultPremisesId) }

    val records = (recordsState as? UiState.Success)?.data.orEmpty()

    // Sub-tabs for Daily / Monthly
    var reportTab by remember { mutableStateOf("DAILY") }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    val currentMonth = YearMonth.now()
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var statusFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedStaffForDetail by remember { mutableStateOf<StaffMonthlySummary?>(null) }

    fun parseWorkDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val str = raw.trim()
        if (str.length >= 10 && str[4] == '-' && str[7] == '-') {
            return runCatching { LocalDate.parse(str.take(10)) }.getOrNull()
        }
        return runCatching { LocalDate.parse(str.take(10)) }.getOrNull()
            ?: runCatching { java.time.Instant.parse(str).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(str).toLocalDate() }.getOrNull()
    }

    fun isPremisesMatch(recordPremisesId: String?, pId: String?): Boolean {
        if (pId == null || pId == "ALL") {
            if (user?.role == Role.EMPLOYEE || initialPremisesId != null || premisesList.size == 1) {
                val allowedIds = premisesList.map { it.id }.toSet()
                return allowedIds.isEmpty() || recordPremisesId.isNullOrBlank() || allowedIds.contains(recordPremisesId)
            }
            return true
        }
        return recordPremisesId == pId || (recordPremisesId.isNullOrBlank() && premisesList.size == 1)
    }

    fun isStatusPresent(status: String?): Boolean {
        if (status == null) return false
        val s = status.uppercase()
        return s == "PRESENT" || s == "COMPLETED" || s == "IN_PROGRESS" || s == "CLOCKED_IN" || s == "APPROVED"
    }

    val dailyCount = remember(records, selectedPremisesId, selectedDate, premisesList, user, initialPremisesId) {
        records.count { record ->
            isPremisesMatch(record.premisesId, selectedPremisesId) && parseWorkDate(record.workDate) == selectedDate
        }
    }

    val monthlyCount = remember(records, selectedPremisesId, selectedMonth, premisesList, user, initialPremisesId) {
        records.count { record ->
            val date = parseWorkDate(record.workDate)
            isPremisesMatch(record.premisesId, selectedPremisesId) && date != null && date.year == selectedMonth.year && date.month == selectedMonth.month
        }
    }

    val allPremisesCount = remember(records, reportTab, selectedDate, selectedMonth) {
        records.count { record ->
            val date = parseWorkDate(record.workDate)
            if (reportTab == "DAILY") date == selectedDate else date != null && date.year == selectedMonth.year && date.month == selectedMonth.month
        }
    }

    val premisesCounts = remember(records, reportTab, selectedDate, selectedMonth, premisesList) {
        premisesList.associate { p ->
            p.id to records.count { record ->
                val date = parseWorkDate(record.workDate)
                val matchesDate = if (reportTab == "DAILY") date == selectedDate else date != null && date.year == selectedMonth.year && date.month == selectedMonth.month
                record.premisesId == p.id && matchesDate
            }
        }
    }

    // Filter records based on selected Period and Premises
    val filteredRecords = remember(records, selectedPremisesId, reportTab, selectedDate, selectedMonth) {
        records.filter { record ->
            val matchesPremises = isPremisesMatch(record.premisesId, selectedPremisesId)
            val recordDate = parseWorkDate(record.workDate)
            val matchesDate = if (reportTab == "DAILY") {
                recordDate == selectedDate
            } else {
                recordDate != null && recordDate.year == selectedMonth.year && recordDate.month == selectedMonth.month
            }
            matchesPremises && matchesDate
        }
    }

    // Daily & Monthly Metrics calculations
    val totalRecords = filteredRecords.size
    val presentCount = filteredRecords.count { isStatusPresent(it.status) }
    val halfDayCount = filteredRecords.count { it.status == "HALF_DAY" }
    val leaveCount = filteredRecords.count { it.status == "LEAVE" }
    val absentCount = filteredRecords.count { it.status == "ABSENT" }
    val lateCount = filteredRecords.count {
        it.lateMinutes > 0 || Dates.calculateLateMinutes(it.clockIn, it.assignedStartTime) > 0
    }
    val totalWorkedMinutes = filteredRecords.sumOf { it.workedMinutes }
    val totalLateMinutes = filteredRecords.sumOf {
        if (it.lateMinutes > 0) it.lateMinutes else Dates.calculateLateMinutes(it.clockIn, it.assignedStartTime)
    }
    val attendanceRate = if (totalRecords > 0) (((presentCount + leaveCount + halfDayCount).toFloat() / totalRecords) * 100).toInt() else 100

    // Staff monthly aggregates
    val staffMonthlySummaries = remember(filteredRecords, reportTab) {
        if (reportTab != "MONTHLY") emptyList()
        else {
            filteredRecords
                .groupBy { it.employeeId.ifBlank { it.staffProfileId ?: it.staffCode ?: it.staffName ?: "UNKNOWN" } }
                .map { (empId, empRecords) ->
                    val first = empRecords.first()
                    val sName = first.staffName?.ifBlank { null } ?: first.staffCode?.ifBlank { null } ?: "Employee"
                    val sCode = first.staffCode?.ifBlank { null } ?: "—"
                    val pDays = empRecords.count { isStatusPresent(it.status) }
                    val aDays = empRecords.count { it.status == "ABSENT" }
                    val hDays = empRecords.count { it.status == "HALF_DAY" }
                    val lDays = empRecords.count { it.status == "LEAVE" }
                    val lCount = empRecords.count {
                        it.lateMinutes > 0 || Dates.calculateLateMinutes(it.clockIn, it.assignedStartTime) > 0
                    }
                    val wMins = empRecords.sumOf { it.workedMinutes }
                    val lMins = empRecords.sumOf {
                        if (it.lateMinutes > 0) it.lateMinutes else Dates.calculateLateMinutes(it.clockIn, it.assignedStartTime)
                    }

                    StaffMonthlySummary(
                        employeeId = empId,
                        staffName = sName,
                        staffCode = sCode,
                        totalDays = empRecords.size,
                        presentDays = pDays,
                        absentDays = aDays,
                        halfDays = hDays,
                        leaveDays = lDays,
                        lateCount = lCount,
                        totalWorkedMinutes = wMins,
                        totalLateMinutes = lMins,
                        records = empRecords.sortedByDescending { it.workDate }
                    )
                }
                .sortedByDescending { it.presentDays }
        }
    }

    // Filter displayed daily records by status and search text
    val displayedDailyRecords = remember(filteredRecords, statusFilter, searchQuery) {
        filteredRecords.filter { record ->
            val matchesSearch = searchQuery.isBlank() ||
                (record.staffName?.contains(searchQuery, ignoreCase = true) == true) ||
                (record.staffCode?.contains(searchQuery, ignoreCase = true) == true)

            val matchesStatus = when (statusFilter) {
                "PRESENT" -> isStatusPresent(record.status)
                "ABSENT" -> record.status == "ABSENT"
                "HALF_DAY" -> record.status == "HALF_DAY"
                "LEAVE" -> record.status == "LEAVE"
                "LATE" -> record.lateMinutes > 0 || Dates.calculateLateMinutes(record.clockIn, record.assignedStartTime) > 0
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    // Filter displayed monthly staff summaries by status and search text
    val displayedStaffSummaries = remember(staffMonthlySummaries, statusFilter, searchQuery) {
        staffMonthlySummaries.filter { summary ->
            val matchesSearch = searchQuery.isBlank() ||
                summary.staffName.contains(searchQuery, ignoreCase = true) ||
                summary.staffCode.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                "PRESENT" -> summary.presentDays > 0
                "ABSENT" -> summary.absentDays > 0
                "HALF_DAY" -> summary.halfDays > 0
                "LEAVE" -> summary.leaveDays > 0
                "LATE" -> summary.lateCount > 0
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    val screenInfo = LocalScreenSizeInfo.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = screenInfo.horizontalScreenPadding)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Editorial Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Workforce Reports",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (reportTab == "DAILY") {
                            "Real-time attendance logs & compliance • ${Dates.formatDateShort(selectedDate.toString())}"
                        } else {
                            "Monthly roster aggregates • ${selectedMonth.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${selectedMonth.year}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PresenceRefreshButton(
                    onClick = onRefresh,
                    isRefreshing = recordsState is UiState.Loading || premisesState is UiState.Loading,
                    contentDescription = "Refresh reports"
                )
            }

            // 2. Premises Context / Filter Chips
            if (user?.role == Role.EMPLOYEE) {
                val assignedPremisesName = remember(user, premisesList, initialPremisesId) {
                    val matched = premisesList.find { it.id == (initialPremisesId ?: user.premisesId) }
                    matched?.name?.let { Dates.cleanPremisesName(it) }
                        ?: user.premisesName?.let { Dates.cleanPremisesName(it) }
                        ?.takeIf { it.isNotBlank() }
                        ?: premisesList.firstOrNull()?.name?.let { Dates.cleanPremisesName(it) }
                        ?: "Assigned Premises"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Premises: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = assignedPremisesName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (premisesList.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(premisesScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    FilterChip(
                        selected = selectedPremisesId == null,
                        onClick = { selectedPremisesId = null },
                        label = { Text("All ($allPremisesCount)", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    premisesList.forEach { premises ->
                        val shortName = Dates.cleanPremisesName(premises.name)
                        val pCount = premisesCounts[premises.id] ?: 0
                        FilterChip(
                            selected = selectedPremisesId == premises.id,
                            onClick = { selectedPremisesId = premises.id },
                            label = { Text("$shortName ($pCount)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // 3. Temporal Navigation (Segmented Switch & Scrollers)
            val dayOffsetList = remember { (-365..0).toList() }
            val monthOffsetList = remember { (-60..0).toList() }

            val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = dayOffsetList.lastIndex)
            val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = monthOffsetList.lastIndex)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresenceSegmentedTabRow(
                    selectedTabIndex = if (reportTab == "DAILY") 0 else 1,
                    tabs = listOf("Daily ($dailyCount)", "Monthly ($monthlyCount)"),
                    onTabSelected = { reportTab = if (it == 0) "DAILY" else "MONTHLY" },
                    icons = listOf(Icons.Default.Today, Icons.Default.CalendarMonth),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (reportTab == "DAILY") {
                            selectedDate = today
                            coroutineScope.launch { dayListState.animateScrollToItem(dayOffsetList.lastIndex) }
                        } else {
                            selectedMonth = currentMonth
                            coroutineScope.launch { monthListState.animateScrollToItem(monthOffsetList.lastIndex) }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Jump to current",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Temporal Selector Strip
            if (reportTab == "DAILY") {
                LazyRow(
                    state = dayListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dayOffsetList.size, key = { dayOffsetList[it] }) { index ->
                        val offset = dayOffsetList[index]
                        val date = today.plusDays(offset.toLong())
                        val isSelected = date == selectedDate
                        val isFuture = date.isAfter(today)

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = date.dayOfWeek.name.take(3).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = date.month.name.take(3).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(52.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                onClick = {
                                    if (!isFuture) {
                                        selectedDate = date
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .let { if (isFuture) it.background(Color.Black.copy(alpha = 0.04f)) else it },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = date.dayOfWeek.name.take(3).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = date.month.name.take(3).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    state = monthListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(monthOffsetList.size, key = { monthOffsetList[it] }) { index ->
                        val offset = monthOffsetList[index]
                        val month = currentMonth.plusMonths(offset.toLong())
                        val isSelected = month == selectedMonth
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMonth = month },
                            label = {
                                Text(
                                    text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // 4. Atmospheric Gradient Hero Card with Canvas Donut Chart
            EditorialAnalyticsHero(
                reportTab = reportTab,
                attendanceRate = attendanceRate,
                presentCount = presentCount,
                totalRecords = totalRecords,
                staffCount = staffMonthlySummaries.size,
                totalWorkedMinutes = totalWorkedMinutes,
                halfDayCount = halfDayCount,
                absentCount = absentCount,
                leaveCount = leaveCount,
                lateCount = lateCount,
                totalLateMinutes = totalLateMinutes,
                selectedDateStr = Dates.formatDateShort(selectedDate.toString())
            )

            // 5. Monthly Staff Comparison Ranking (Visible only in Monthly mode)
            if (reportTab == "MONTHLY" && staffMonthlySummaries.isNotEmpty()) {
                StaffAttendanceComparisonBarChart(
                    staffList = staffMonthlySummaries.take(5),
                    onSelectStaff = { selectedStaffForDetail = it }
                )
            }

            // 6. Search Bar
            PresenceSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholderText = "Search by employee name or staff code…"
            )

            // 7. Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("All (${if (reportTab == "DAILY") totalRecords else staffMonthlySummaries.size})", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = statusFilter == "PRESENT",
                    onClick = { statusFilter = "PRESENT" },
                    label = { Text("Present ($presentCount)", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryEmerald.copy(alpha = 0.15f),
                        selectedLabelColor = SecondaryEmerald
                    )
                )
                FilterChip(
                    selected = statusFilter == "ABSENT",
                    onClick = { statusFilter = "ABSENT" },
                    label = { Text("Absent ($absentCount)", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFFEF4444)
                    )
                )
                FilterChip(
                    selected = statusFilter == "HALF_DAY",
                    onClick = { statusFilter = "HALF_DAY" },
                    label = { Text("Half-Day ($halfDayCount)", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TertiaryAmber.copy(alpha = 0.15f),
                        selectedLabelColor = TertiaryAmber
                    )
                )
                FilterChip(
                    selected = statusFilter == "LEAVE",
                    onClick = { statusFilter = "LEAVE" },
                    label = { Text("Leave ($leaveCount)", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF0284C7)
                    )
                )
                FilterChip(
                    selected = statusFilter == "LATE",
                    onClick = { statusFilter = "LATE" },
                    label = { Text("Late ($lateCount)", fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEA580C).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFFEA580C)
                    )
                )
            }

            // 8. Attendance Records / Staff Roster Section
            when (recordsState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Aggregating operational analytics…")
                is UiState.Error -> PresenceErrorState(
                    message = recordsState.message,
                    onRetry = onRefresh
                )
                is UiState.Success -> {
                    if (reportTab == "DAILY") {
                        if (displayedDailyRecords.isEmpty()) {
                            EmptyReportState(
                                message = if (searchQuery.isNotBlank()) "No records found matching '$searchQuery'." else "No attendance records logged for ${Dates.formatDate(selectedDate.toString())}."
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Section Sub-header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Attendance Records (${displayedDailyRecords.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = Dates.formatMinutes(displayedDailyRecords.sumOf { it.workedMinutes }),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                    displayedDailyRecords.forEachIndexed { idx, record ->
                                        DailyAttendanceRow(record = record)
                                        if (idx < displayedDailyRecords.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(start = 58.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // MONTHLY VIEW
                        if (displayedStaffSummaries.isEmpty()) {
                            EmptyReportState(
                                message = if (searchQuery.isNotBlank()) "No staff found matching '$searchQuery'." else "No monthly summary data for ${selectedMonth.month.name} ${selectedMonth.year}."
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Section Sub-header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Monthly Staff Roster (${displayedStaffSummaries.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Tap row for breakdown",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                    displayedStaffSummaries.forEachIndexed { idx, summary ->
                                        StaffMonthlyRow(
                                            summary = summary,
                                            onClick = { selectedStaffForDetail = summary }
                                        )
                                        if (idx < displayedStaffSummaries.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(start = 58.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Monthly Attendance Modal Bottom Sheet for selected staff
        selectedStaffForDetail?.let { staffSummary ->
            ModalBottomSheet(
                onDismissRequest = { selectedStaffForDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                EmployeeMonthlyDetailSheet(
                    summary = staffSummary,
                    selectedMonth = selectedMonth,
                    onDismiss = { selectedStaffForDetail = null }
                )
            }
        }
    }
}

/**
 * Editorial Atmospheric Gradient Hero with large KPI typography and a custom Canvas Donut Chart.
 */
@Composable
private fun EditorialAnalyticsHero(
    reportTab: String,
    attendanceRate: Int,
    presentCount: Int,
    totalRecords: Int,
    staffCount: Int,
    totalWorkedMinutes: Int,
    halfDayCount: Int,
    absentCount: Int,
    leaveCount: Int,
    lateCount: Int,
    totalLateMinutes: Int,
    selectedDateStr: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6).copy(alpha = 0.6f),
                        Color(0xFF8B5CF6).copy(alpha = 0.4f),
                        Color(0xFF3B82F6).copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Section: Donut Chart + Main KPIs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart
                AttendanceDonutChart(
                    presentCount = presentCount,
                    halfDayCount = halfDayCount,
                    leaveCount = leaveCount,
                    absentCount = absentCount
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Primary Editorial KPIs
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$attendanceRate%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                attendanceRate >= 90 -> Color(0xFF34D399)
                                attendanceRate >= 75 -> Color(0xFFFBBF24)
                                else -> Color(0xFFF87171)
                            },
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        attendanceRate >= 90 -> Color(0xFF059669).copy(alpha = 0.35f)
                                        attendanceRate >= 75 -> Color(0xFFD97706).copy(alpha = 0.35f)
                                        else -> Color(0xFFDC2626).copy(alpha = 0.35f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    attendanceRate >= 90 -> "Optimal"
                                    attendanceRate >= 75 -> "Fair"
                                    else -> "Critical"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = when {
                                    attendanceRate >= 90 -> Color(0xFF6EE7B7)
                                    attendanceRate >= 75 -> Color(0xFFFDE68A)
                                    else -> Color(0xFFFCA5A5)
                                }
                            )
                        }
                    }

                    Text(
                        text = if (reportTab == "DAILY") {
                            "$presentCount Present • $totalRecords Total Logged ($selectedDateStr)"
                        } else {
                            "$presentCount Present entries across $staffCount active staff"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${Dates.formatMinutes(totalWorkedMinutes)} total logged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }
            }

            // Proportional Distribution Indicator Bar
            val totalForBar = (presentCount + halfDayCount + leaveCount + absentCount).coerceAtLeast(1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            ) {
                if (presentCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(presentCount.toFloat() / totalForBar)
                            .fillMaxSize()
                            .background(Color(0xFF10B981))
                    )
                }
                if (halfDayCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(halfDayCount.toFloat() / totalForBar)
                            .fillMaxSize()
                            .background(Color(0xFFF59E0B))
                    )
                }
                if (leaveCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(leaveCount.toFloat() / totalForBar)
                            .fillMaxSize()
                            .background(Color(0xFF0284C7))
                    )
                }
                if (absentCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(absentCount.toFloat() / totalForBar)
                            .fillMaxSize()
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            // 4 High-Contrast Telemetry Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkStatTile(
                    label = "Present",
                    value = "$presentCount",
                    subtext = "Half: $halfDayCount",
                    bgColor = Color(0xFF064E3B).copy(alpha = 0.45f),
                    borderColor = Color(0xFF059669).copy(alpha = 0.5f),
                    textColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                DarkStatTile(
                    label = "Absent",
                    value = "$absentCount",
                    subtext = "Leave: $leaveCount",
                    bgColor = Color(0xFF7F1D1D).copy(alpha = 0.45f),
                    borderColor = Color(0xFFDC2626).copy(alpha = 0.5f),
                    textColor = Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
                DarkStatTile(
                    label = "Late",
                    value = "$lateCount",
                    subtext = if (totalLateMinutes > 0) Dates.formatMinutes(totalLateMinutes) else "On time",
                    bgColor = Color(0xFF78350F).copy(alpha = 0.45f),
                    borderColor = Color(0xFFD97706).copy(alpha = 0.5f),
                    textColor = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                DarkStatTile(
                    label = if (reportTab == "DAILY") "Records" else "Staff",
                    value = if (reportTab == "DAILY") "$totalRecords" else "$staffCount",
                    subtext = if (reportTab == "DAILY") "Entries" else "Roster",
                    bgColor = Color(0xFF312E81).copy(alpha = 0.45f),
                    borderColor = Color(0xFF6366F1).copy(alpha = 0.5f),
                    textColor = Color(0xFFA5B4FC),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Custom Canvas Donut Chart with center compliance rate.
 */
@Composable
private fun AttendanceDonutChart(
    presentCount: Int,
    halfDayCount: Int,
    leaveCount: Int,
    absentCount: Int,
    modifier: Modifier = Modifier
) {
    val total = presentCount + halfDayCount + leaveCount + absentCount
    val presentAngle = if (total > 0) (presentCount.toFloat() / total) * 360f else 0f
    val halfDayAngle = if (total > 0) (halfDayCount.toFloat() / total) * 360f else 0f
    val leaveAngle = if (total > 0) (leaveCount.toFloat() / total) * 360f else 0f
    val absentAngle = if (total > 0) (absentCount.toFloat() / total) * 360f else 0f

    val presentColor = Color(0xFF10B981)
    val halfDayColor = Color(0xFFF59E0B)
    val leaveColor = Color(0xFF0284C7)
    val absentColor = Color(0xFFEF4444)

    Box(
        modifier = modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            if (total == 0) {
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                var currentAngle = -90f
                val activeSegments = listOf(presentCount, halfDayCount, leaveCount, absentCount).count { it > 0 }
                val gap = if (activeSegments > 1) 4f else 0f

                if (presentAngle > 0) {
                    val sweep = (presentAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = presentColor,
                        startAngle = currentAngle + (gap / 2),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += presentAngle
                }
                if (halfDayAngle > 0) {
                    val sweep = (halfDayAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = halfDayColor,
                        startAngle = currentAngle + (gap / 2),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += halfDayAngle
                }
                if (leaveAngle > 0) {
                    val sweep = (leaveAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = leaveColor,
                        startAngle = currentAngle + (gap / 2),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += leaveAngle
                }
                if (absentAngle > 0) {
                    val sweep = (absentAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = absentColor,
                        startAngle = currentAngle + (gap / 2),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        val rate = if (total > 0) (((presentCount + leaveCount + halfDayCount).toFloat() / total) * 100).toInt() else 100
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$rate%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 15.sp
            )
            Text(
                text = "Compliant",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

/**
 * Monthly Staff Attendance Comparison Ranking Bar Visualization.
 */
@Composable
private fun StaffAttendanceComparisonBarChart(
    staffList: List<StaffMonthlySummary>,
    onSelectStaff: (StaffMonthlySummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Top Roster Attendance Rates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Top ${staffList.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            staffList.forEach { staff ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectStaff(staff) }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = staff.staffName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "• ${staff.staffCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${staff.attendanceRate}% (${staff.presentDays}d)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                staff.attendanceRate >= 90 -> SecondaryEmerald
                                staff.attendanceRate >= 75 -> TertiaryAmber
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    // Proportional horizontal bar
                    LinearProgressIndicator(
                        progress = { (staff.attendanceRate.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = when {
                            staff.attendanceRate >= 90 -> SecondaryEmerald
                            staff.attendanceRate >= 75 -> TertiaryAmber
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkStatTile(
    label: String,
    value: String,
    subtext: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = textColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GradientAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().take(1).ifBlank { "?" }.uppercase()
    val gradient = remember(initial) {
        val hash = kotlin.math.abs(initial.hashCode())
        when (hash % 4) {
            0 -> listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
            1 -> listOf(Color(0xFF059669), Color(0xFF0D9488))
            2 -> listOf(Color(0xFFD97706), Color(0xFFEA580C))
            else -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

/**
 * Structured Daily Attendance Information Row (hairline divider layout).
 */
@Composable
private fun DailyAttendanceRow(record: AttendanceRecordDto) {
    val lateText = Dates.formatLateText(record.clockIn, record.assignedStartTime, record.lateMinutes)
    val isPresent = record.status == "PRESENT" || record.status == "COMPLETED"

    val statusAccentColor = when (record.status) {
        "PRESENT", "COMPLETED" -> SecondaryEmerald
        "ABSENT" -> Color(0xFFEF4444)
        "HALF_DAY" -> TertiaryAmber
        "LEAVE" -> Color(0xFF0284C7)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status accent indicator bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(statusAccentColor)
        )

        GradientAvatar(name = record.staffName ?: record.staffCode ?: "S")

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = record.staffName ?: record.staffCode ?: "Employee",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                record.staffCode?.let { code ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!record.clockIn.isNullOrBlank()) {
                    Text(
                        text = "In: ${Dates.formatTime(record.clockIn)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${Dates.formatMinutes(record.workedMinutes)} logged",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusPill(record.status)

            if (lateText.isNotEmpty()) {
                PresenceBadge(
                    text = lateText,
                    tone = PresenceTone.Danger
                )
            } else if (isPresent && !record.clockIn.isNullOrBlank()) {
                PresenceBadge(
                    text = "On Time",
                    tone = PresenceTone.Success
                )
            }
        }
    }
}

/**
 * Structured Staff Monthly Roster Row (hairline divider layout).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffMonthlyRow(
    summary: StaffMonthlySummary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientAvatar(name = summary.staffName)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = summary.staffName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = summary.staffCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }
                }
                Text(
                    text = "${summary.totalDays} days logged • Worked: ${Dates.formatMinutes(summary.totalWorkedMinutes)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PresenceBadge(
                text = "${summary.attendanceRate}%",
                tone = when {
                    summary.attendanceRate >= 90 -> PresenceTone.Success
                    summary.attendanceRate >= 75 -> PresenceTone.Warning
                    else -> PresenceTone.Danger
                }
            )

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View detail",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Inline status breakdown pill counts
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusPillCount(label = "Present", count = summary.presentDays, color = SecondaryEmerald)
            if (summary.absentDays > 0) {
                StatusPillCount(label = "Absent", count = summary.absentDays, color = Color(0xFFDC2626))
            }
            if (summary.halfDays > 0) {
                StatusPillCount(label = "Half", count = summary.halfDays, color = TertiaryAmber)
            }
            if (summary.leaveDays > 0) {
                StatusPillCount(label = "Leave", count = summary.leaveDays, color = Color(0xFF0284C7))
            }
            if (summary.lateCount > 0) {
                StatusPillCount(label = "Late", count = summary.lateCount, color = Color(0xFFEA580C))
            }
            if (summary.totalLateMinutes > 0) {
                Text(
                    text = "(${Dates.formatMinutes(summary.totalLateMinutes)} late)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusPillCount(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyReportState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassBottom,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmployeeMonthlyDetailSheet(
    summary: StaffMonthlySummary,
    selectedMonth: YearMonth,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientAvatar(name = summary.staffName)
                Column {
                    Text(
                        text = summary.staffName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Code: ${summary.staffCode} • ${selectedMonth.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${selectedMonth.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close sheet")
            }
        }

        // Summary Breakdown Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DetailStat(label = "Present", count = "${summary.presentDays}", color = SecondaryEmerald)
                DetailStat(label = "Absent", count = "${summary.absentDays}", color = Color(0xFFDC2626))
                DetailStat(label = "Half-Day", count = "${summary.halfDays}", color = TertiaryAmber)
                DetailStat(label = "Leave", count = "${summary.leaveDays}", color = Color(0xFF0284C7))
                DetailStat(label = "Worked", count = Dates.formatMinutes(summary.totalWorkedMinutes), color = MaterialTheme.colorScheme.primary)
            }
        }

        Text(
            text = "Daily Attendance Breakdown (${summary.records.size} Records)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                items(summary.records) { record ->
                    DailyAttendanceRow(record = record)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DetailStat(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
