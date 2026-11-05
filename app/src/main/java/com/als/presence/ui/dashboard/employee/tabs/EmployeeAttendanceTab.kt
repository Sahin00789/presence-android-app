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
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.MyAttendanceContent
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
 * The month/year selector chips sit at the very top and the attendance register
 * below is grouped date-by-date. Clock-in lives in its own primary tab.
 */
@Composable
fun EmployeeAttendanceTab(
    viewModel: EmployeeAttendanceViewModel,
    modifier: Modifier = Modifier
) {
    MyAttendanceContent(modifier = modifier, viewModel = viewModel)
}

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
    ledgerState: UiState<List<StaffEarningLedgerDto>> = UiState.Success(emptyList())
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

            else -> EarningsSection(ledgerState)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EarningsSection(ledgerState: UiState<List<StaffEarningLedgerDto>>) {
    when (ledgerState) {
        is UiState.Loading, is UiState.Idle -> SectionLoading("Loading earning ledger…")
        is UiState.Error -> SectionError(ledgerState.message)
        is UiState.Success -> {
            val entries = ledgerState.data
            if (entries.isEmpty()) {
                SectionEmpty("No earning ledger entries recorded yet.")
            } else {
                val groupedByMonth = remember(entries) {
                    entries.sortedByDescending { it.earningDate }
                        .groupBy { formatMonthHeader(it.earningDate) }
                }
                groupedByMonth.forEach { (monthHeader, monthEntries) ->
                    val totalMonthPaise = monthEntries.sumOf { it.amountCents }
                    MonthlySeparatorHeader(
                        monthTitle = monthHeader,
                        monthTotalPaise = totalMonthPaise,
                        entryCount = monthEntries.size
                    )
                    monthEntries.forEach { entry -> LedgerRow(entry) }
                }
            }
        }
    }
}

private fun formatMonthHeader(iso: String?): String {
    if (iso.isNullOrBlank()) return "Other"
    return try {
        val date = LocalDate.parse(iso.take(10))
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (_: Exception) {
        "Other"
    }
}

@Composable
private fun MonthlySeparatorHeader(
    monthTitle: String,
    monthTotalPaise: Long,
    entryCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
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
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(monthTotalPaise),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

@Composable
private fun LedgerRow(entry: StaffEarningLedgerDto) {
    val amountPaise = entry.amountCents
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Dates.formatDate(entry.earningDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+" + Money.format(amountPaise),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
