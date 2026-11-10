package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Owner "Reports" tab — Review attendance metrics, workforce costs, and separate overtime expenses.
 * Supports Period, Role (All/Managers/Staff), and Premises-wise filters.
 */
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
    val filteredRecords = records.filter { record ->
        val matchesPremises = isPremisesMatch(record.premisesId, selectedPremisesId)
        val recordDate = parseWorkDate(record.workDate)
        val matchesDate = if (reportTab == "DAILY") {
            recordDate == selectedDate
        } else {
            recordDate != null && recordDate.year == selectedMonth.year && recordDate.month == selectedMonth.month
        }
        
        matchesPremises && matchesDate
    }

    val displayedRecords = remember(filteredRecords, statusFilter) {
        when (statusFilter) {
            "PRESENT" -> filteredRecords.filter { it.status == "PRESENT" || it.status == "COMPLETED" }
            "ABSENT" -> filteredRecords.filter { it.status == "ABSENT" }
            "HALF_DAY" -> filteredRecords.filter { it.status == "HALF_DAY" }
            "LEAVE" -> filteredRecords.filter { it.status == "LEAVE" }
            else -> filteredRecords
        }
    }

    val totalRecords = filteredRecords.size
    val presentCount = filteredRecords.count { it.status == "PRESENT" || it.status == "COMPLETED" }
    val halfDayCount = filteredRecords.count { it.status == "HALF_DAY" }
    val leaveCount = filteredRecords.count { it.status == "LEAVE" }
    val absentCount = filteredRecords.count { it.status == "ABSENT" }
    val attendanceRate = if (totalRecords > 0) ((presentCount + leaveCount + halfDayCount).toFloat() / totalRecords * 100).toInt() else 100

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
                    text = "Workforce Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
                val matched = premisesList.find { it.id == (initialPremisesId ?: user?.premisesId) }
                matched?.name?.let { Dates.cleanPremisesName(it) }
                    ?: user?.premisesName?.let { Dates.cleanPremisesName(it) }
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

        // Sub-tabs for Daily / Monthly
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
                    label = { Text("Daily ($dailyCount)") }
                )
                FilterChip(
                    selected = reportTab == "MONTHLY",
                    onClick = { reportTab = "MONTHLY" },
                    label = { Text("Monthly ($monthlyCount)") }
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
                                text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year % 100}"
                            )
                        }
                    )
                }
            }
        }

        SectionHeader(title = "Filtered Activity Log")

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
                label = { Text("All ($totalRecords)") }
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
        }

        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance reports…")
            is UiState.Error -> PresenceErrorState(
                message = recordsState.message,
                onRetry = onRefresh
            )
            is UiState.Success -> {
                if (displayedRecords.isEmpty()) {
                    Text(
                        text = "No records found matching the selected filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    displayedRecords.take(15).forEach { record ->
                        val lateText = Dates.formatLateText(record.clockIn, record.assignedStartTime, record.lateMinutes)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
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
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${record.workDate ?: ""} • ${Dates.formatMinutes(record.workedMinutes)} worked",
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
                                StatusPill(record.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun StatusCountItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
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
