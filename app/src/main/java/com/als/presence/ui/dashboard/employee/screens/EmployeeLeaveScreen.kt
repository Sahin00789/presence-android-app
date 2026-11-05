package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.LeaveDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.LeaveViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFilterChip
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone

/**
 * Employee Leave Requests Screen.
 * Allows employees to request shift leave and view their request status and review notes.
 */
@Composable
fun EmployeeLeaveScreen(
    user: User,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val leavesState by viewModel.leaves.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    PresenceSecondaryScaffold(
        crumb = "PERSONAL WORKSPACE",
        title = "Apply Leave",
        onBack = onBack,
        modifier = modifier,
        onRefresh = viewModel::refresh
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Overview Card
            val myLeaves = (leavesState as? UiState.Success)?.data.orEmpty()
            val pendingCount = myLeaves.count { it.status == "PENDING" }
            val approvedCount = myLeaves.count { it.status == "APPROVED" }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Leave Request Center",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${myLeaves.size} Total Submitted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        PresenceButton(
                            label = "Request Leave",
                            icon = Icons.Default.EventAvailable,
                            onClick = { showRequestDialog = true },
                            size = PresenceSize.Sm
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresenceStatTile(
                            label = "Applied Leaves",
                            value = pendingCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        PresenceStatTile(
                            label = "Approved Leaves",
                            value = approvedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "APPLIED", "APPROVED", "REJECTED").forEach { status ->
                    val isSelected = statusFilter == status
                    PresenceFilterChip(
                        selected = isSelected,
                        onClick = {
                            statusFilter = if (isSelected && status != "ALL") "ALL" else status
                        },
                        label = when (status) {
                            "ALL" -> "All"
                            "APPLIED" -> "Applied"
                            "APPROVED" -> "Approved"
                            "REJECTED" -> "Rejected"
                            else -> status
                        }
                    )
                }
            }

            when (val state = leavesState) {
                is UiState.Loading, is UiState.Idle -> PresenceLoading("Loading leave requests…")
                is UiState.Error -> PresenceErrorState(
                    message = state.message,
                    onRetry = viewModel::refresh
                )
                is UiState.Success -> {
                    val allLeaves = state.data
                    val filteredLeaves = remember(allLeaves, statusFilter) {
                        when (statusFilter) {
                            "ALL" -> allLeaves
                            "APPLIED" -> allLeaves.filter { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("APPLIED", ignoreCase = true) }
                            "APPROVED" -> allLeaves.filter { it.status.equals("APPROVED", ignoreCase = true) }
                            "REJECTED" -> allLeaves.filter { it.status.equals("REJECTED", ignoreCase = true) }
                            else -> allLeaves
                        }
                    }

                    if (filteredLeaves.isEmpty()) {
                        PresenceEmptyState(
                            icon = Icons.Default.EventAvailable,
                            title = "No leave requests match filter",
                            description = "Submitted shift leave applications will appear here."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredLeaves.forEach { leave ->
                                EmployeeLeaveCard(leave = leave)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRequestDialog) {
        LeaveRequestDialog(
            isSubmitting = isSubmitting,
            onDismiss = { showRequestDialog = false },
            onSubmit = { startDate, endDate, type, shiftName, reason ->
                viewModel.requestLeave(
                    startDate = startDate,
                    endDate = endDate,
                    type = type,
                    reason = reason,
                    employeeId = user.id,
                    shiftName = shiftName
                )
                showRequestDialog = false
            }
        )
    }

    MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
}

@Composable
private fun EmployeeLeaveCard(leave: LeaveDto) {
    PresenceCard {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${leave.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} Leave",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "Shift: ${leave.shiftName?.ifBlank { null } ?: "All Shifts"}",
                        fontSize = 10.5.sp,
                        color = PresenceDesign.LabelText
                    )
                }
                PresenceBadge(
                    label = when (leave.status.uppercase()) {
                        "PENDING" -> "applied"
                        else -> leave.status.lowercase().replace('_', ' ')
                    },
                    tone = when (leave.status.uppercase()) {
                        "APPROVED" -> PresenceTone.Success
                        "REJECTED" -> PresenceTone.Danger
                        "CANCELLED" -> PresenceTone.Neutral
                        else -> PresenceTone.Warning
                    },
                    dot = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            PresenceMetaStrip(
                items = listOf(
                    "From" to (leave.startDate),
                    "To" to (leave.endDate),
                    "Requested" to (leave.createdAt?.take(10) ?: "—")
                ).filter { it.second != null } as List<Pair<String, String>>
            )
            leave.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "Reason: $reason",
                    fontSize = 11.sp,
                    color = PresenceDesign.NeutralText
                )
            }
            leave.reviewNote?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manager review note: $note",
                    fontSize = 10.5.sp,
                    color = PresenceDesign.LabelText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LeaveRequestDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (startDate: String, endDate: String, type: String, shiftName: String?, reason: String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PAID") }
    var shiftName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Shift Leave", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Dates use the format YYYY-MM-DD.", fontSize = 11.sp, color = PresenceDesign.LabelText)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shiftName,
                    onValueChange = { shiftName = it },
                    label = { Text("Shift name (e.g. Morning Shift or All Shifts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("PAID", "UNPAID", "SICK").forEach { option ->
                        PresenceFilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = option.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for leave") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = PresenceDesign.Danger, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(startDate) ||
                        !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(endDate)
                    ) {
                        error = "Enter both dates as YYYY-MM-DD."
                    } else if (endDate < startDate) {
                        error = "The end date cannot be before the start date."
                    } else if (reason.isBlank()) {
                        error = "Add a reason for the leave."
                    } else {
                        onSubmit(startDate, endDate, type, shiftName.trim().ifBlank { null }, reason)
                    }
                }
            ) { Text(if (isSubmitting) "Submitting…" else "Submit request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
