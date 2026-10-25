package com.als.presence.ui.dashboard.staff.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ScheduleEntryDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill

/** Staff "Shifts" tab: personal register, upcoming schedule and overtime. */
@Composable
fun StaffAttendanceTab(
    recordsState: UiState<List<AttendanceRecordDto>>,
    scheduleState: UiState<List<ScheduleEntryDto>>,
    overtimeState: UiState<List<OvertimeDto>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    var section by remember { mutableIntStateOf(1) } // Default to "Schedule" (Shifts)
    val sections = listOf("Register", "Schedule", "Overtime")
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "My Shifts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your upcoming shifts, register, and overtime",
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
                    contentDescription = "Refresh attendance",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(
                label = "Present",
                value = records.count { it.status == "PRESENT" }.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
            MetricTile(
                label = "Late",
                value = records.count { it.status == "LATE" }.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary
            )
            MetricTile(
                label = "Hours",
                value = "%.1f".format(records.sumOf { it.workedMinutes } / 60.0),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEachIndexed { index, label ->
                FilterChip(
                    selected = section == index,
                    onClick = { section = index },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        when (section) {
            0 -> when (recordsState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading your register…")
                is UiState.Error -> SectionError(recordsState.message)
                is UiState.Success -> {
                    if (records.isEmpty()) {
                        SectionEmpty("No attendance in the last 30 days.")
                    } else {
                        records.forEach { record -> RecordRow(record) }
                    }
                }
            }

            1 -> when (scheduleState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading your schedule…")
                is UiState.Error -> SectionError(scheduleState.message)
                is UiState.Success -> {
                    val entries = scheduleState.data
                    if (entries.isEmpty()) {
                        SectionEmpty("No shifts scheduled for the next two weeks.")
                    } else {
                        entries.forEach { entry -> ScheduleRow(entry) }
                    }
                }
            }

            else -> when (overtimeState) {
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
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RecordRow(record: AttendanceRecordDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${Dates.formatDate(record.workDate)} • ${Dates.dayName(record.workDate)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Dates.formatTime(record.clockIn)} → ${Dates.formatTime(record.clockOut)} • ${Dates.formatMinutes(record.workedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(record.status)
        }
    }
}

@Composable
private fun ScheduleRow(entry: ScheduleEntryDto) {
    val isShift = entry.type.equals("SHIFT", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isShift) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isShift) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isShift) {
                        Icons.Default.Schedule
                    } else {
                        Icons.Default.BeachAccess
                    },
                    contentDescription = null,
                    tint = if (isShift) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${Dates.formatDate(entry.date)} • ${Dates.dayName(entry.date)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isShift && entry.shiftName != null ->
                            "${entry.shiftName} • ${Dates.formatWindow(entry.startTime, entry.endTime)}"
                        entry.holidayName != null -> entry.holidayName
                        else -> entry.type.lowercase().replace('_', ' ')
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isShift) {
                Text(
                    text = Dates.formatMinutes(entry.scheduledMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                StatusPill(entry.type)
            }
        }
    }
}

@Composable
private fun OvertimeRow(entry: OvertimeDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Dates.formatDate(entry.workDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(entry.status)
            }
            Text(
                text = "${Dates.formatWindow(entry.startTime, entry.endTime)} • ${Dates.formatMinutes(entry.plannedMinutes)} planned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = entry.reason ?: "Overtime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Money.format(entry.paymentCents),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (entry.workedMinutes > 0) {
                Text(
                    text = "Worked ${Dates.formatMinutes(entry.workedMinutes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
