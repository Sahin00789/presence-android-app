package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
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
 * Owner "Reports" tab — Review attendance metrics, workforce costs, and separate overtime expenses.
 * Supports Period (Daily / Monthly), Status, and Premises-wise filters with staff monthly breakdowns.
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

    fun parseWorkDate(workDate: String?): LocalDate? {
        if (workDate.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(workDate.take(10)) }.getOrNull()
    }

    fun isPremisesMatch(recordPremisesId: String?, pId: String?): Boolean {
        return if (pId != null) {
            recordPremisesId == pId
        } else if (user?.role == Role.EMPLOYEE || initialPremisesId != null || premisesList.size == 1) {
            val allowedIds = premisesList.map { it.id }.toSet()
            allowedIds.isEmpty() || allowedIds.contains(recordPremisesId)
        } else {
            true
        }
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
    val presentCount = filteredRecords.count { it.status == "PRESENT" || it.status == "COMPLETED" }
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
    val attendanceRate = if (totalRecords > 0) ((presentCount + leaveCount + halfDayCount).toFloat() / totalRecords * 100).toInt() else 100

    // Staff monthly aggregates
    val staffMonthlySummaries = remember(filteredRecords, reportTab) {
        if (reportTab != "MONTHLY") emptyList()
        else {
            filteredRecords
                .groupBy { it.employeeId.ifBlank { it.staffCode ?: it.staffName ?: "UNKNOWN" } }
                .map { (empId, empRecords) ->
                    val first = empRecords.first()
                    val sName = first.staffName ?: first.staffCode ?: "Employee"
                    val sCode = first.staffCode ?: "—"
                    val pDays = empRecords.count { it.status == "PRESENT" || it.status == "COMPLETED" }
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
                "PRESENT" -> record.status == "PRESENT" || record.status == "COMPLETED"
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workforce Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (reportTab == "DAILY") "Daily attendance & late tracking" else "Monthly summary & staff roster",
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

        // Assigned Premises Info Card for Manager or Premises Filter Chips for Owner
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
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ASSIGNED PREMISES",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = assignedPremisesName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else if (premisesList.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(premisesScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Premises:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = selectedPremisesId == null,
                    onClick = { selectedPremisesId = null },
                    label = { Text("All Premises ($allPremisesCount)") }
                )
                premisesList.forEach { premises ->
                    val shortName = Dates.cleanPremisesName(premises.name)
                    val pCount = premisesCounts[premises.id] ?: 0
                    FilterChip(
                        selected = selectedPremisesId == premises.id,
                        onClick = { selectedPremisesId = premises.id },
                        label = { Text("$shortName ($pCount)") }
                    )
                }
            }
        }

        // Sub-tabs for Daily / Monthly & Quick Date Selectors
        val dayOffsetList = remember { (-365..0).toList() }
        val monthOffsetList = remember { (-60..0).toList() }

        val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = dayOffsetList.lastIndex)
        val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = monthOffsetList.lastIndex)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = reportTab == "DAILY",
                    onClick = { reportTab = "DAILY" },
                    label = { Text("Daily ($dailyCount)") },
                    leadingIcon = {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                FilterChip(
                    selected = reportTab == "MONTHLY",
                    onClick = { reportTab = "MONTHLY" },
                    label = { Text("Monthly ($monthlyCount)") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = "Go to current",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (reportTab == "DAILY") {
            LazyRow(
                state = dayListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayOffsetList.size, key = { dayOffsetList[it] }) { index ->
                    val offset = dayOffsetList[index]
                    val date = today.plusDays(offset.toLong())
                    val isSelected = date == selectedDate
                    val isFuture = date.isAfter(today)
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = {
                            if (!isFuture) {
                                selectedDate = date
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().let { if (isFuture) it.background(Color.Black.copy(alpha = 0.05f)) else it },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyRow(
                state = monthListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year}"
                            )
                        }
                    )
                }
            }
        }

        // Summary KPI Metrics Cards
        SectionHeader(
            title = if (reportTab == "DAILY") "Daily Overview (${Dates.formatDateShort(selectedDate.toString())})" else "Monthly Overview (${selectedMonth.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${selectedMonth.year})"
        )

        // KPI Metrics Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Main Gauge Card: Attendance Rate %
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Attendance Rate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "$attendanceRate%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (attendanceRate.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (reportTab == "DAILY") "$presentCount Present out of $totalRecords scheduled" else "$presentCount Present entries across ${staffMonthlySummaries.size} staff",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Worked: ${Dates.formatMinutes(totalWorkedMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Secondary Stats Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Present",
                    value = "$presentCount",
                    subtext = "Half-day: $halfDayCount",
                    icon = Icons.Default.CheckCircle,
                    accentColor = SecondaryEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Absent",
                    value = "$absentCount",
                    subtext = "Leave: $leaveCount",
                    icon = Icons.Default.EventBusy,
                    accentColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Late Arrivals",
                    value = "$lateCount",
                    subtext = if (totalLateMinutes > 0) "${Dates.formatMinutes(totalLateMinutes)} total late" else "On time",
                    icon = Icons.Default.Alarm,
                    accentColor = TertiaryAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (reportTab == "DAILY") "Worked Hours" else "Total Active Staff",
                    value = if (reportTab == "DAILY") Dates.formatMinutes(totalWorkedMinutes) else "${staffMonthlySummaries.size}",
                    subtext = if (reportTab == "DAILY") "Avg shifts logged" else "${filteredRecords.size} records",
                    icon = if (reportTab == "DAILY") Icons.Default.AccessTime else Icons.Default.People,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search & Filter Section Header
        SectionHeader(
            title = if (reportTab == "DAILY") "Daily Attendance Log" else "Staff Monthly Roster (${staffMonthlySummaries.size} Staff)"
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by employee name or staff code…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        )

        // Status Filter Chips
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
                label = { Text("All (${if (reportTab == "DAILY") totalRecords else staffMonthlySummaries.size})") }
            )
            FilterChip(
                selected = statusFilter == "PRESENT",
                onClick = { statusFilter = "PRESENT" },
                label = { Text("Present ($presentCount)") }
            )
            FilterChip(
                selected = statusFilter == "ABSENT",
                onClick = { statusFilter = "ABSENT" },
                label = { Text("Absent ($absentCount)") }
            )
            FilterChip(
                selected = statusFilter == "HALF_DAY",
                onClick = { statusFilter = "HALF_DAY" },
                label = { Text("Half-Day ($halfDayCount)") }
            )
            FilterChip(
                selected = statusFilter == "LEAVE",
                onClick = { statusFilter = "LEAVE" },
                label = { Text("Leave ($leaveCount)") }
            )
            FilterChip(
                selected = statusFilter == "LATE",
                onClick = { statusFilter = "LATE" },
                label = { Text("Late ($lateCount)") }
            )
        }

        // Attendance List / Staff Roster Content
        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance reports…")
            is UiState.Error -> PresenceErrorState(
                message = recordsState.message,
                onRetry = onRefresh
            )
            is UiState.Success -> {
                if (reportTab == "DAILY") {
                    if (displayedDailyRecords.isEmpty()) {
                        EmptyReportState(
                            message = if (searchQuery.isNotBlank()) "No records found matching '$searchQuery'." else "No attendance records found for ${Dates.formatDate(selectedDate.toString())}."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayedDailyRecords.forEach { record ->
                                DailyAttendanceCard(record = record)
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
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            displayedStaffSummaries.forEach { summary ->
                                StaffMonthlyCard(
                                    summary = summary,
                                    onClick = { selectedStaffForDetail = summary }
                                )
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            EmployeeMonthlyDetailSheet(
                summary = staffSummary,
                selectedMonth = selectedMonth,
                onDismiss = { selectedStaffForDetail = null }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyAttendanceCard(record: AttendanceRecordDto) {
    val lateText = Dates.formatLateText(record.clockIn, record.assignedStartTime, record.lateMinutes)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialAvatar(name = record.staffName ?: record.staffCode ?: "S")
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.staffName ?: record.staffCode ?: "Employee",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    record.staffCode?.let { code ->
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "• ${Dates.formatMinutes(record.workedMinutes)} worked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!record.clockIn.isNullOrBlank() || !record.clockOut.isNullOrBlank()) {
                    Text(
                        text = "In: ${Dates.formatTime(record.clockIn)}  Out: ${Dates.formatTime(record.clockOut)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (lateText.isNotEmpty()) {
                    Text(
                        text = "⏰ $lateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            StatusPill(record.status)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffMonthlyCard(
    summary: StaffMonthlySummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialAvatar(name = summary.staffName)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.staffName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${summary.staffCode} • ${summary.totalDays} Total Days Logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Attendance Rate Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        summary.attendanceRate >= 90 -> SecondaryEmerald.copy(alpha = 0.15f)
                        summary.attendanceRate >= 75 -> TertiaryAmber.copy(alpha = 0.15f)
                        else -> Color(0xFFDC2626).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = "${summary.attendanceRate}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            summary.attendanceRate >= 90 -> SecondaryEmerald
                            summary.attendanceRate >= 75 -> TertiaryAmber
                            else -> Color(0xFFDC2626)
                        }
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Summary Pills Flow Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPillCount(label = "Present", count = summary.presentDays, color = SecondaryEmerald)
                if (summary.absentDays > 0) {
                    StatusPillCount(label = "Absent", count = summary.absentDays, color = Color(0xFFDC2626))
                }
                if (summary.halfDays > 0) {
                    StatusPillCount(label = "Half-Day", count = summary.halfDays, color = TertiaryAmber)
                }
                if (summary.leaveDays > 0) {
                    StatusPillCount(label = "Leave", count = summary.leaveDays, color = Color(0xFF0284C7))
                }
                if (summary.lateCount > 0) {
                    StatusPillCount(label = "Late", count = summary.lateCount, color = Color(0xFFEA580C))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Worked: ${Dates.formatMinutes(summary.totalWorkedMinutes)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.totalLateMinutes > 0) {
                    Text(
                        text = "Total Late: ${Dates.formatMinutes(summary.totalLateMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPillCount(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                style = MaterialTheme.typography.labelSmall,
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
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassBottom,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = summary.staffName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Code: ${summary.staffCode} • ${selectedMonth.month.name} ${selectedMonth.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close sheet")
            }
        }

        // Summary Breakdown Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
            text = "Daily Log Breakdown (${summary.records.size} Records)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summary.records) { record ->
                DailyAttendanceCard(record = record)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailStat(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
