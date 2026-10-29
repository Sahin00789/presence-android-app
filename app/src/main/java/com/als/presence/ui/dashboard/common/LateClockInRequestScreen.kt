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
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated screen for filing and reviewing Late Clock-In Requests as Leave Requests.
 *
 * Allows staff members to request approval for late arrival on scheduled shifts,
 * and allows Managers/Owners to review and approve/reject them.
 */
@Composable
fun LateClockInRequestScreen(
    role: Role,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val leavesState by viewModel.leaves.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    var showLateDialog by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<LeaveDto?>(null) }
    var rejectNote by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }

    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    val canRequest = role == Role.STAFF || role == Role.MANAGER
    val canReview = role == Role.OWNER || role == Role.MANAGER

    PresenceSecondaryScaffold(
        crumb = "Shift Requests",
        title = "Late Clock-In Requests",
        onBack = onBack,
        modifier = modifier,
        actions = {
            PresenceIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh late clock-in requests",
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
                    "Review staff late clock-in leave requests and resolve attendance grace exceptions."
                } else {
                    "Request advance permission for late shift clock-in to avoid penalty or absence."
                },
                fontSize = 11.5.sp,
                color = PresenceDesign.LabelText,
                modifier = Modifier.weight(1f)
            )
            if (canRequest) {
                Spacer(modifier = Modifier.width(10.dp))
                PresenceButton(
                    label = "Request Late In",
                    icon = Icons.Default.MoreTime,
                    onClick = { showLateDialog = true },
                    size = PresenceSize.Sm
                )
            }
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL" to "All Requests", "PENDING" to "Pending", "APPROVED" to "Approved", "LATE_ONLY" to "Late In Only").forEach { (key, label) ->
                FilterChip(
                    selected = filterStatus == key,
                    onClick = { filterStatus = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        when (val state = leavesState) {
            is UiState.Loading, is UiState.Idle -> PresenceLoading("Loading late clock-in requests…")
            is UiState.Error -> PresenceErrorState(
                message = state.message,
                onRetry = viewModel::refresh
            )
            is UiState.Success -> {
                val filtered = state.data.filter { leave ->
                    when (filterStatus) {
                        "PENDING" -> leave.status == "PENDING"
                        "APPROVED" -> leave.status == "APPROVED"
                        "LATE_ONLY" -> leave.type == "LATE_CLOCK_IN"
                        else -> true
                    }
                }

                if (filtered.isEmpty()) {
                    PresenceEmptyState(
                        icon = Icons.Default.Schedule,
                        title = "No late clock-in requests found",
                        description = if (canRequest) {
                            "Tap Request Late In to file an advance request for arriving late."
                        } else {
                            "Staff late clock-in requests will appear here for management approval."
                        }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filtered.forEach { leave ->
                            LateClockInCard(
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
            PresenceSectionTitle("Late Clock-in & Leave Policy")
            Text(
                text = "Approved late clock-in leave requests mark the shift attendance as authorized late arrival. " +
                    "Penalties are waived when the manager approves the request before or after shift start.",
                fontSize = 11.sp,
                color = PresenceDesign.LabelText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    if (showLateDialog) {
        LateClockInDialog(
            isSubmitting = isSubmitting,
            onDismiss = { showLateDialog = false },
            onSubmit = { date, shiftName, expectedTime, reason ->
                viewModel.requestLeave(
                    startDate = date,
                    endDate = date,
                    type = "LATE_CLOCK_IN",
                    reason = "Expected Clock-In: $expectedTime. Reason: $reason",
                    shiftName = shiftName
                )
                showLateDialog = false
            }
        )
    }

    rejectTarget?.let { leave ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject late clock-in request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add an optional note for ${leave.staffName ?: "the employee"}. " +
                            "Rejection enforces standard late arrival penalties."
                    )
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text("Rejection Note") },
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
private fun LateClockInCard(
    leave: LeaveDto,
    canReview: Boolean,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isLateType = leave.type == "LATE_CLOCK_IN"
    PresenceCard {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = leave.staffName ?: "Staff Member",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = if (isLateType) "Late Clock-In Request" else "${leave.type.lowercase().replace('_', ' ')} leave",
                        fontSize = 10.5.sp,
                        color = if (isLateType) PresenceDesign.CautionText else PresenceDesign.LabelText,
                        fontWeight = if (isLateType) FontWeight.SemiBold else FontWeight.Normal
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
                    "Shift" to (leave.shiftName?.ifBlank { null } ?: "Scheduled Shift"),
                    "Date" to (leave.startDate),
                    "Requested" to (leave.createdAt?.take(10) ?: "—")
                ).filter { it.second != null } as List<Pair<String, String>>
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
                    text = "Manager note: $note",
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
private fun LateClockInDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (date: String, shiftName: String, expectedTime: String, reason: String) -> Unit
) {
    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(todayIso) }
    var shiftName by remember { mutableStateOf("") }
    var expectedTime by remember { mutableStateOf("09:30 AM") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MoreTime, contentDescription = null, tint = PresenceDesign.Caution)
                Text("Request Late Clock-In", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Submit a late clock-in request as a leave exception for manager review.",
                    fontSize = 11.sp,
                    color = PresenceDesign.LabelText
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Shift Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shiftName,
                    onValueChange = { shiftName = it },
                    label = { Text("Shift Name (e.g., Morning Shift)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expectedTime,
                    onValueChange = { expectedTime = it },
                    label = { Text("Expected Clock-In Time (e.g., 09:30 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for late clock-in") },
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
                    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) {
                        error = "Enter shift date as YYYY-MM-DD."
                    } else if (expectedTime.isBlank()) {
                        error = "Specify expected clock-in time."
                    } else if (reason.isBlank()) {
                        error = "Provide a reason for arriving late."
                    } else {
                        onSubmit(date, shiftName.trim().ifBlank { "Scheduled Shift" }, expectedTime.trim(), reason.trim())
                    }
                }
            ) { Text(if (isSubmitting) "Submitting…" else "Submit Late Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
