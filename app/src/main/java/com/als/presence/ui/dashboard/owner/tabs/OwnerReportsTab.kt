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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val premisesScrollState = rememberScrollState()
    var selectedRoleFilter by remember { mutableStateOf("ALL") } // "ALL", "MANAGER", "STAFF"
    var selectedPremisesId by remember { mutableStateOf<String?>(null) } // null = All Premises

    val records = (recordsState as? UiState.Success)?.data.orEmpty()
    // Sub-tabs for Daily / Monthly
    var reportTab by remember { mutableStateOf("DAILY") }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    
    val currentMonth = YearMonth.now()
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()

    // Filter records based on selected Period, Role, and Premises
    val filteredRecords = records.filter { record ->
        val matchesPremises = selectedPremisesId == null || record.premisesId == selectedPremisesId
        val matchesRole = when (selectedRoleFilter) {
            "MANAGER" -> record.staffCode?.startsWith("M", ignoreCase = true) == true || record.staffName?.contains("Manager", ignoreCase = true) == true
            "STAFF" -> !(record.staffCode?.startsWith("M", ignoreCase = true) == true || record.staffName?.contains("Manager", ignoreCase = true) == true)
            else -> true
        }
        
        val matchesDate = if (record.workDate.isNullOrBlank()) {
            false
        } else {
            val recordDate = runCatching { LocalDate.parse(record.workDate) }.getOrNull()
            if (reportTab == "DAILY") {
                recordDate == selectedDate
            } else {
                recordDate != null && recordDate.year == selectedMonth.year && recordDate.month == selectedMonth.month
            }
        }
        
        matchesPremises && matchesRole && matchesDate
    }

    val totalRecords = filteredRecords.size
    val presentCount = filteredRecords.count { it.status == "PRESENT" || it.status == "COMPLETED" }
    val lateCount = filteredRecords.count { it.status == "LATE" }
    val halfDayCount = filteredRecords.count { it.status == "HALF_DAY" }
    val absentCount = filteredRecords.count { it.status == "ABSENT" }
    val attendanceRate = if (totalRecords > 0) ((presentCount + lateCount).toFloat() / totalRecords * 100).toInt() else 100

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
                Text(
                    text = "Attendance metrics & premises analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh reports",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Premises-wise Filter chips (Scrollable)
        if (premisesList.isNotEmpty()) {
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
                    label = { Text("All Premises") }
                )
                premisesList.forEach { premises ->
                    val parts = premises.name.split(",", limit = 2)
                    val shortName = parts[0].trim()
                    FilterChip(
                        selected = selectedPremisesId == premises.id,
                        onClick = { selectedPremisesId = premises.id },
                        label = { Text(shortName) }
                    )
                }
            }
        }

        // Sub-tabs for Daily / Monthly
        val coroutineScope = rememberCoroutineScope()
        val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = 5000 - 2)
        val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = 5000 - 2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = reportTab == "DAILY",
                    onClick = { reportTab = "DAILY" },
                    label = { Text("Daily") }
                )
                FilterChip(
                    selected = reportTab == "MONTHLY",
                    onClick = { reportTab = "MONTHLY" },
                    label = { Text("Monthly") }
                )
            }
            IconButton(
                onClick = {
                    if (reportTab == "DAILY") {
                        selectedDate = today
                        coroutineScope.launch { dayListState.animateScrollToItem(5000 - 2) }
                    } else {
                        selectedMonth = currentMonth
                        coroutineScope.launch { monthListState.animateScrollToItem(5000 - 2) }
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
                items(10000) { index ->
                    val date = today.plusDays((index - 5000).toLong())
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
                items(10000) { index ->
                    val month = currentMonth.plusMonths((index - 5000).toLong())
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

        // Summary Metric Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Attendance Rate",
                value = "$attendanceRate%",
                icon = Icons.Default.CheckCircle,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Sessions",
                value = "$totalRecords",
                icon = Icons.Default.BarChart,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // Attendance Status Breakdown
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Attendance Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusCountItem(label = "Present", count = presentCount, color = MaterialTheme.colorScheme.primary)
                    StatusCountItem(label = "Late", count = lateCount, color = MaterialTheme.colorScheme.tertiary)
                    StatusCountItem(label = "Half-Day", count = halfDayCount, color = MaterialTheme.colorScheme.secondary)
                    StatusCountItem(label = "Absent", count = absentCount, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        SectionHeader(title = "Filtered Activity Log")
        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance reports…")
            is UiState.Error -> PresenceErrorState(
                message = recordsState.message,
                onRetry = onRefresh
            )
            is UiState.Success -> {
                if (filteredRecords.isEmpty()) {
                    Text(
                        text = "No records found matching the selected filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    filteredRecords.take(15).forEach { record ->
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
                                        text = record.staffName ?: record.staffCode ?: "Staff",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${record.workDate ?: ""} • ${Dates.formatMinutes(record.workedMinutes)} worked",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
