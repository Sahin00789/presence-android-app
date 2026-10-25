package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.als.presence.domain.model.Role

/**
 * Shared leave request and review screen.
 *
 * Staff and Managers can request leave for specific shifts or entire workdays; Owners and Managers review the queue.
 * The staff profile is resolved server-side for Staff, so no client-supplied identity is trusted.
 */
@Composable
fun LeaveScreen(
    role: Role,
    staffProfileId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val leavesState by viewModel.leaves.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<LeaveDto?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    val canRequest = role == Role.STAFF || role == Role.MANAGER
    val canReview = role == Role.OWNER || role == Role.MANAGER

    PresenceSecondaryScaffold(
        crumb = "Leave",
        title = if (canReview) "Leave requests" else "My leave",
        onBack = onBack,
        modifier = modifier,
        actions = {
            PresenceIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh leave requests",
                onClick = viewModel::refresh
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (canReview) {
                    "Approve or reject shift leave requests and review leave history."
                } else {
                    "Request leave for specific assigned shifts or entire workdays."
                },
                fontSize = 11.5.sp,
                color = PresenceDesign.LabelText,
                modifier = Modifier.weight(1f)
            )
            if (canRequest) {
                Spacer(modifier = Modifier.width(10.dp))
                PresenceButton(
                    label = "Request",
                    icon = Icons.Default.EventAvailable,
                    onClick = { showRequestDialog = true },
                    size = PresenceSize.Sm
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
                if (state.data.isEmpty()) {
                    PresenceEmptyState(
                        icon = Icons.Default.EventAvailable,
                        title = "No leave requests yet",
                        description = if (canRequest) {
                            "Tap Request to submit your first shift leave request."
                        } else {
                            "Staff leave requests will appear here for review."
                        }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.data.forEach { leave ->
                            LeaveCard(
                                leave = leave,
                                canReview = canReview && leave.status == "PENDING",
                                isSubmitting = isSubmitting,
                                onApprove = { viewModel.review(leave.id, "APPROVED", null) },
                                onReject = {
                                    rejectTarget = leave
                                    rejectNote = ""
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        if (canReview) {
            PresenceSectionTitle("How approval affects payroll")
            Text(
                text = "Approved paid or sick shift leave is included in the salary calculation. " +
                    "Unpaid leave is recorded but not paid. Rejections never create salary deductions automatically.",
                fontSize = 11.sp,
                color = PresenceDesign.LabelText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
                    staffProfileId = staffProfileId,
                    shiftName = shiftName
                )
                showRequestDialog = false
            }
        )
    }

    rejectTarget?.let { leave ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject leave request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add an optional note for ${leave.staffName ?: "the employee"}. " +
                            "The decision is permanent and audited."
                    )
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.review(leave.id, "REJECTED", rejectNote.trim().ifBlank { null })
                        rejectTarget = null
                    }
                ) { Text("Reject", color = PresenceDesign.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Cancel") }
            }
        )
    }

    MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
}

@Composable
private fun LeaveCard(
    leave: LeaveDto,
    canReview: Boolean,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    PresenceCard {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = leave.staffName ?: "You",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "${leave.staffCode ?: ""} ${leave.type.lowercase().replace('_', ' ')} leave",
                        fontSize = 10.5.sp,
                        color = PresenceDesign.LabelText
                    )
                }
                PresenceBadge(
                    label = leave.status.lowercase().replace('_', ' '),
                    tone = when (leave.status) {
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
                    "Shift" to (leave.shiftName?.ifBlank { null } ?: "All Shifts"),
                    "From" to leave.startDate,
                    "To" to leave.endDate,
                    "Requested" to (leave.createdAt?.take(10) ?: "—")
                )
            )
            leave.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = reason,
                    fontSize = 11.sp,
                    color = PresenceDesign.NeutralText
                )
            }
            leave.reviewNote?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Review note: $note",
                    fontSize = 10.5.sp,
                    color = PresenceDesign.LabelText
                )
            }
            if (canReview) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresenceButton(
                        label = "Approve",
                        icon = Icons.Default.Check,
                        onClick = onApprove,
                        enabled = !isSubmitting,
                        size = PresenceSize.Sm,
                        modifier = Modifier.weight(1f)
                    )
                    PresenceButton(
                        label = "Reject",
                        icon = Icons.Default.Close,
                        onClick = onReject,
                        variant = PresenceVariant.Danger,
                        enabled = !isSubmitting,
                        size = PresenceSize.Sm,
                        modifier = Modifier.weight(1f)
                    )
                }
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
        title = { Text("Request shift leave", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Dates use the format YYYY-MM-DD.", fontSize = 11.sp, color = PresenceDesign.LabelText)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shiftName,
                    onValueChange = { shiftName = it },
                    label = { Text("Shift name (e.g., Morning Shift or All Shifts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PAID", "UNPAID", "SICK", "LATE_CLOCK_IN").forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = {
                                Text(
                                    when (option) {
                                        "LATE_CLOCK_IN" -> "Late In"
                                        else -> option.lowercase().replaceFirstChar { it.uppercase() }
                                    }
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
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
