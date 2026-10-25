package com.als.presence.ui.dashboard.manager.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.data.model.OvertimeDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceKicker
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.manager.ManagerOvertimeViewModel
import com.als.presence.ui.dashboard.manager.modals.BookOvertimeModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeScreen(
    onBack: () -> Unit,
    viewModel: ManagerOvertimeViewModel = hiltViewModel()
) {
    val overtimeState by viewModel.overtime.collectAsStateWithLifecycle()
    val staffState by viewModel.staff.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    var showBook by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "SCHEDULING",
            title = "Overtime",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showBook = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Book overtime",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresenceKicker("ACTIVE REQUESTS")
                    val count = (overtimeState as? UiState.Success)?.data?.size ?: 0
                    Text(
                        text = "$count items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (val state = overtimeState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading overtime…")
                    is UiState.Error -> SectionError(state.message)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            SectionEmpty("No overtime has been booked for the organization yet.")
                        } else {
                            state.data.forEach { entry ->
                                OvertimeCard(
                                    entry = entry,
                                    onApprove = { viewModel.approveOvertime(entry.id) },
                                    onReject = { viewModel.rejectOvertime(entry.id, "Not approved") },
                                    onMarkWorked = { viewModel.markOvertimeWorked(entry.id) },
                                    isSubmitting = isSubmitting
                                )
                            }
                        }
                    }
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showBook) {
        val staffList = (staffState as? UiState.Success)?.data.orEmpty()
        BookOvertimeModal(
            staff = staffList,
            onDismiss = { showBook = false },
            onBook = { staffProfileId, workDate, startTime, endTime, reason ->
                viewModel.bookOvertime(staffProfileId, workDate, startTime, endTime, reason)
                showBook = false
            }
        )
    }
}

@Composable
private fun OvertimeCard(
    entry: OvertimeDto,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkWorked: () -> Unit,
    isSubmitting: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(name = entry.staffName ?: entry.staffCode ?: "?")
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.staffName ?: entry.staffCode ?: "Staff",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.staffCode ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(entry.status)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = Dates.formatDate(entry.workDate),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• ${Dates.formatWindow(entry.startTime, entry.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            KeyValueRow(label = "Planned", value = Dates.formatMinutes(entry.plannedMinutes))
            KeyValueRow(label = "Rate", value = Money.format(entry.paymentCents))
            KeyValueRow(
                label = "Worked",
                value = if (entry.workedMinutes > 0) Dates.formatMinutes(entry.workedMinutes) else "Not recorded"
            )

            if (entry.reason?.isNotBlank() == true) {
                Text(
                    text = entry.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (entry.status) {
                    "PENDING" -> {
                        ActionPill(label = "Approve", onClick = onApprove, enabled = !isSubmitting, primary = true)
                        ActionPill(label = "Reject", onClick = onReject, enabled = !isSubmitting)
                    }
                    "APPROVED" -> {
                        ActionPill(label = "Mark worked", onClick = onMarkWorked, enabled = !isSubmitting, primary = true)
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val container = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val content = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(1.dp, content.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        color = container,
        onClick = { if (enabled) onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
