package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto

/**
 * Owner "Attendance" tab — the organization-wide register plus every open
 * exception that needs a decision.
 */
@Composable
fun AttendanceRegisterTab(
    recordsState: UiState<List<AttendanceRecordDto>>,
    exceptionsState: UiState<List<AttendanceExceptionDto>>,
    isSubmitting: Boolean,
    onRefresh: () -> Unit,
    onResolve: (exceptionId: String, status: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var resolving by remember { mutableStateOf<AttendanceExceptionDto?>(null) }

    val rawRecords = (recordsState as? UiState.Success)?.data.orEmpty()
    val records = remember(rawRecords) {
        rawRecords.filter { r ->
            !r.clockIn.isNullOrBlank() || r.status in listOf("PRESENT", "HALF_DAY", "ABSENT", "LEAVE")
        }
    }
    val exceptions = (exceptionsState as? UiState.Success)?.data.orEmpty()
    val open = exceptions.filter { it.status == "OPEN" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Last 30 days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Register and every exception that needs a decision",
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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile(
                label = "Records",
                value = records.size.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Open exceptions",
                value = open.size.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.error
            )
            MetricTile(
                label = "Present",
                value = records.count { it.status == "PRESENT" || it.status == "COMPLETED" }.toString(),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        SectionHeader(title = "Exceptions needing review")
        when (exceptionsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading exceptions…")
            is UiState.Error -> SectionError(exceptionsState.message)
            is UiState.Success -> {
                if (open.isEmpty()) {
                    SectionEmpty("No open exceptions. Nice and clean.")
                } else {
                    open.forEach { exception ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InitialAvatar(name = exception.staffName ?: "?")
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exception.staffName ?: exception.staffCode ?: "Employee",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = exception.reason?.replace('_', ' ').orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(exception.status)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                KeyValueRow(
                                    label = "Raised",
                                    value = Dates.formatTimestamp(exception.createdAt)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                AssistChip(
                                    onClick = { resolving = exception },
                                    label = { Text("Resolve") },
                                    enabled = !isSubmitting,
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        labelColor = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        SectionHeader(title = "Attendance register")
        when (recordsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading attendance…")
            is UiState.Error -> SectionError(recordsState.message)
            is UiState.Success -> {
                if (records.isEmpty()) {
                    SectionEmpty("No attendance recorded yet.")
                } else {
                    records.take(60).forEach { record -> AttendanceRow(record) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    resolving?.let { exception ->
        ResolveExceptionDialog(
            exception = exception,
            onDismiss = { resolving = null },
            onConfirm = { status, reason ->
                onResolve(exception.id, status, reason)
                resolving = null
            }
        )
    }
}

@Composable
private fun AttendanceRow(record: AttendanceRecordDto) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.staffName ?: record.staffCode ?: "Employee",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${Dates.formatDate(record.workDate)} • ${record.shiftName ?: if (record.overtimeId != null) "Overtime" else "Shift"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "Assigned Time: ${Dates.formatTime(record.assignedStartTime)} – ${Dates.formatTime(record.assignedEndTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (!record.clockIn.isNullOrBlank()) "In: ${Dates.formatTime(record.clockIn)}" else "In: --",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (!record.clockOut.isNullOrBlank()) "Out: ${Dates.formatTime(record.clockOut)}" else "Out: --",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Worked: ${Dates.formatMinutes(record.workedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.overtimeId != null) {
                    Text(
                        text = "Overtime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolveExceptionDialog(
    exception: AttendanceExceptionDto,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val statuses = listOf("PRESENT", "HALF_DAY", "ABSENT")
    var status by remember { mutableStateOf(statuses.first()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolve exception", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${exception.staffName ?: "Employee"} • ${exception.reason?.replace('_', ' ').orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = {
                                Text(
                                    text = option.lowercase().replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for correction") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "The correction will update the employee's attendance record.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(status, reason) },
                enabled = reason.trim().length >= 2
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

