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
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill

/** Staff "Attendance" tab: personal register and overtime history. */
@Composable
fun employeeAttendanceTab(
    recordsState: UiState<List<AttendanceRecordDto>>,
    overtimeState: UiState<List<OvertimeDto>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    var section by remember { mutableIntStateOf(0) }
    val sections = listOf("Register", "Overtime")
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
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Dates.formatMinutes(entry.plannedMinutes)} planned • Pay: ${Money.format(entry.paymentInr)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(entry.status)
        }
    }
}
