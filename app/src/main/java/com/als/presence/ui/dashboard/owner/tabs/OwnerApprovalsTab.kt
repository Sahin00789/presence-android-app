package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill

/**
 * Owner "Approvals" tab — Authorization queue for exceptions, leave, and financial adjustments.
 */
@Composable
fun OwnerApprovalsTab(
    exceptionsState: UiState<List<AttendanceExceptionDto>>,
    isSubmitting: Boolean,
    onRefresh: () -> Unit,
    onResolve: (exceptionId: String, status: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var filterStatus by remember { mutableStateOf("PENDING") }

    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReasonText by remember { mutableStateOf("") }
    var rejectReasonError by remember { mutableStateOf<String?>(null) }

    val exceptions = (exceptionsState as? UiState.Success)?.data.orEmpty()
    val filteredExceptions = exceptions.filter {
        when (filterStatus) {
            "PENDING" -> it.status == "OPEN" || it.status == "PENDING"
            "APPROVED" -> it.status == "RESOLVED" || it.status == "APPROVED"
            "REJECTED" -> it.status == "REJECTED"
            else -> true
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Authorization Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${exceptions.count { it.status == "OPEN" || it.status == "PENDING" }} pending approval requests",
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
                    contentDescription = "Refresh approvals",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterStatus == "PENDING",
                onClick = { filterStatus = "PENDING" },
                label = { Text("Pending") }
            )
            FilterChip(
                selected = filterStatus == "APPROVED",
                onClick = { filterStatus = "APPROVED" },
                label = { Text("Approved") }
            )
            FilterChip(
                selected = filterStatus == "REJECTED",
                onClick = { filterStatus = "REJECTED" },
                label = { Text("Rejected") }
            )
        }

        SectionHeader(title = "Requests")
        when (exceptionsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading approval requests…")
            is UiState.Error -> Text(
                text = exceptionsState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            is UiState.Success -> {
                if (filteredExceptions.isEmpty()) {
                    Text(
                        text = "No requests found matching the '$filterStatus' filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    filteredExceptions.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InitialAvatar(name = item.staffName ?: item.staffCode ?: "S")
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.staffName ?: item.staffCode ?: "Staff",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.createdAt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(item.status)
                                }

                                KeyValueRow(label = "Reason", value = item.reason)

                                if (item.status == "OPEN" || item.status == "PENDING") {
                                    if (item.reason.contains("GPS", ignoreCase = true) || item.reason == "THREE_GPS_FAILURES") {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onResolve(item.id, "RESUME", "Resumed by manager") },
                                                    enabled = !isSubmitting,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Resume")
                                                }

                                                OutlinedButton(
                                                    onClick = { onResolve(item.id, "HALF_DAY", "Marked half-day after GPS exception") },
                                                    enabled = !isSubmitting,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Half Day")
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        rejectTargetId = item.id
                                                        rejectReasonText = "Marked absent due to GPS failure"
                                                        rejectReasonError = null
                                                    },
                                                    enabled = !isSubmitting,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Absent")
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { onResolve(item.id, "PRESENT", "Approved by Manager/Owner") },
                                                enabled = !isSubmitting,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Approve")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    rejectTargetId = item.id
                                                    rejectReasonText = ""
                                                    rejectReasonError = null
                                                },
                                                enabled = !isSubmitting,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mandatory Rejection Reason Dialog
    if (rejectTargetId != null) {
        AlertDialog(
            onDismissRequest = { rejectTargetId = null },
            title = { Text("Reject Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A mandatory rejection reason is required for audit history.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rejectReasonText,
                        onValueChange = {
                            rejectReasonText = it
                            if (it.isNotBlank()) rejectReasonError = null
                        },
                        label = { Text("Rejection Reason *") },
                        isError = rejectReasonError != null,
                        supportingText = rejectReasonError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReasonText.isBlank()) {
                            rejectReasonError = "Reason cannot be empty"
                        } else {
                            rejectTargetId?.let { id ->
                                onResolve(id, "ABSENT", rejectReasonText.trim())
                            }
                            rejectTargetId = null
                        }
                    }
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
