package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant

/**
 * Owner Staff Leave Requests & Approvals Screen.
 * Allows Owners to review, approve, and reject workforce leave requests.
 */
@Composable
fun OwnerLeaveScreen(
    user: User? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val leavesState by viewModel.leaves.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf("PENDING") }
    var rejectTarget by remember { mutableStateOf<LeaveDto?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    PresenceSecondaryScaffold(
        crumb = "Workforce",
        title = "Leave Desk",
        onBack = onBack,
        modifier = modifier,
        onRefresh = viewModel::refresh
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PENDING", "APPROVED", "REJECTED", "ALL").forEach { status ->
                    val isSelected = statusFilter == status
                    PresenceFilterChip(
                        selected = isSelected,
                        onClick = {
                            statusFilter = if (isSelected && status != "ALL") "ALL" else status
                        },
                        label = when (status) {
                            "PENDING" -> "Pending Review"
                            "APPROVED" -> "Approved"
                            "REJECTED" -> "Rejected"
                            else -> "All Requests"
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
                        if (statusFilter == "ALL") allLeaves
                        else allLeaves.filter { it.status.equals(statusFilter, ignoreCase = true) }
                    }

                    if (filteredLeaves.isEmpty()) {
                        PresenceEmptyState(
                            icon = Icons.Default.EventAvailable,
                            title = "No leave requests match filter",
                            description = "Employee leave requests will appear here for review."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredLeaves.forEach { leave ->
                                OwnerLeaveCard(
                                    leave = leave,
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

            Spacer(modifier = Modifier.height(12.dp))
            PresenceSectionTitle("Payroll Impact Notice")
            Text(
                text = "Approved paid or sick shift leave is included in monthly salary calculations. " +
                    "Unpaid leave is logged without disbursement.",
                fontSize = 11.sp,
                color = PresenceDesign.LabelText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    rejectTarget?.let { leave ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject leave request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add an optional note for ${leave.staffName ?: "the employee"}. " +
                            "This decision will update their workspace record."
                    )
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text("Reason / Note") },
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
private fun OwnerLeaveCard(
    leave: LeaveDto,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    PresenceCard {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = leave.staffName ?: "Employee",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "${leave.staffCode ?: ""} • ${leave.type.lowercase().replace('_', ' ')} leave",
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
                    text = "Review note: $note",
                    fontSize = 10.5.sp,
                    color = PresenceDesign.LabelText
                )
            }
            if (leave.status == "PENDING") {
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
