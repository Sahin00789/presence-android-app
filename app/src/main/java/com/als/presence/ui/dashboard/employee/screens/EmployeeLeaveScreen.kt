package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.UpcomingShiftDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.LeaveViewModel
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFilterChip
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone

/**
 * Employee Leave Requests Screen.
 * Allows employees to select upcoming shifts with date toggles and submit leave requests.
 */
@Composable
fun EmployeeLeaveScreen(
    user: User,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val leavesState by viewModel.leaves.collectAsStateWithLifecycle()
    val upcomingShiftsState by viewModel.upcomingShifts.collectAsStateWithLifecycle()
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
        onRefresh = {
            viewModel.refresh()
            viewModel.loadUpcomingShifts()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val myLeaves = (leavesState as? UiState.Success)?.data.orEmpty()
            val pendingCount = myLeaves.count { it.status == "PENDING" }
            val approvedCount = myLeaves.count { it.status == "APPROVED" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL", "APPLIED", "APPROVED", "REJECTED").forEach { status ->
                        val isSelected = statusFilter == status
                        PresenceFilterChip(
                            selected = isSelected,
                            onClick = {
                                statusFilter = if (isSelected && status != "ALL") "ALL" else status
                            },
                            label = when (status) {
                                "ALL" -> "All (${myLeaves.size})"
                                "APPLIED" -> "Applied ($pendingCount)"
                                "APPROVED" -> "Approved ($approvedCount)"
                                "REJECTED" -> "Rejected (${myLeaves.size - pendingCount - approvedCount})"
                                else -> status
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                PresenceButton(
                    label = "Request",
                    icon = Icons.Default.EventAvailable,
                    onClick = {
                        viewModel.loadUpcomingShifts()
                        showRequestDialog = true
                    },
                    size = PresenceSize.Sm
                )
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
        LeaveRequestSheet(
            upcomingShiftsState = upcomingShiftsState,
            isSubmitting = isSubmitting,
            onDismiss = { showRequestDialog = false },
            onSubmit = { selectedShifts, reason ->
                viewModel.requestLeave(
                    selectedShifts = selectedShifts,
                    reason = reason
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
                        text = "Shift: ${leave.shiftName?.ifBlank { null } ?: "Assigned Shift"}",
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
                    "Work Date" to leave.workDate.ifBlank { leave.startDate },
                    "Requested" to (leave.createdAt?.take(10) ?: "—")
                ).filter { it.second.isNotBlank() }
            )
            leave.reason.takeIf { it.isNotBlank() }?.let { reason ->
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

private data class DateTimelineItem(
    val isoDate: String,
    val dayNameShort: String,
    val dateDisplayShort: String,
    val fullDisplay: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveRequestSheet(
    upcomingShiftsState: UiState<List<UpcomingShiftDto>>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (selectedShifts: List<UpcomingShiftDto>, reason: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmSubmodal by remember { mutableStateOf(false) }

    // Next 14 days (up to 2 weeks)
    val dateItems = remember {
        val today = LocalDate.now()
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")
        val dateShortFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")

        (0..13).map { offset ->
            val date = today.plusDays(offset.toLong())
            val iso = date.toString()
            val dayName = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> date.format(dayOfWeekFormatter)
            }
            DateTimelineItem(
                isoDate = iso,
                dayNameShort = dayName,
                dateDisplayShort = date.format(dateShortFormatter),
                fullDisplay = date.format(fullDateFormatter)
            )
        }
    }

    var selectedIsoDate by remember { mutableStateOf<String?>("ALL") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Row with Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEAVE REQUEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Request Shift Leave",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Scroll dates across the next 2 weeks to view and enable assigned shifts for leave.",
                    fontSize = 12.sp,
                    color = PresenceDesign.LabelText
                )

                when (upcomingShiftsState) {
                    is UiState.Loading, is UiState.Idle -> PresenceLoading("Loading upcoming shifts…")
                    is UiState.Error -> PresenceErrorState(
                        message = upcomingShiftsState.message,
                        onRetry = {}
                    )
                    is UiState.Success -> {
                        val upcomingShifts = upcomingShiftsState.data

                        // 14-Day Date Timeline Scroll Row
                        val dateScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(dateScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All 14 Days" chip
                            val isAllSelected = selectedIsoDate == "ALL"

                            Surface(
                                onClick = { selectedIsoDate = "ALL" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAllSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "All 14 Days",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${upcomingShifts.size} Shifts",
                                        fontSize = 9.5.sp,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else PresenceDesign.LabelText
                                    )
                                }
                            }

                            // 14 Date Chips
                            dateItems.forEach { item ->
                                val isSelected = selectedIsoDate == item.isoDate
                                val shiftsOnDate = upcomingShifts.filter { it.workDate == item.isoDate }
                                val enabledOnDate = shiftsOnDate.count {
                                    selectedKeys.contains("${it.shiftAssignmentId}_${it.workDate}")
                                }

                                Surface(
                                    onClick = { selectedIsoDate = item.isoDate },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        enabledOnDate > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        shiftsOnDate.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            enabledOnDate > 0 -> MaterialTheme.colorScheme.primary
                                            shiftsOnDate.isNotEmpty() -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                        }
                                    ),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = item.dayNameShort,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (enabledOnDate > 0) {
                                                Text(
                                                    text = "✓",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.dateDisplayShort,
                                            fontSize = 9.5.sp,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                shiftsOnDate.isNotEmpty() -> MaterialTheme.colorScheme.primary
                                                else -> PresenceDesign.LabelText
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Date Header Label
                        val currentSelectedDateItem = dateItems.find { it.isoDate == selectedIsoDate }
                        val headerLabel = if (selectedIsoDate == "ALL") {
                            "Showing All Shifts (Next 14 Days)"
                        } else {
                            "Shifts for ${currentSelectedDateItem?.fullDisplay ?: selectedIsoDate}"
                        }

                        Text(
                            text = headerLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Visible Shifts List
                        val visibleShifts = if (selectedIsoDate == "ALL") {
                            upcomingShifts
                        } else {
                            upcomingShifts.filter { it.workDate == selectedIsoDate }
                        }

                        if (visibleShifts.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "No Shift Scheduled Today",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink
                                    )
                                    Text(
                                        text = "No shift assigned on ${currentSelectedDateItem?.dateDisplayShort ?: "this date"}. Select highlighted dates in the bar above to view & enable shifts for leave.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PresenceDesign.LabelText,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                visibleShifts.forEach { shift ->
                                    val key = "${shift.shiftAssignmentId}_${shift.workDate}"
                                    val isSelected = selectedKeys.contains(key)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            }
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${shift.dayOfWeek ?: ""} • ${shift.workDate}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PresenceDesign.Ink
                                                )
                                                Text(
                                                    text = "${shift.shiftName}${if (!shift.startTime.isNullOrBlank()) " (${shift.startTime?.take(5)} - ${shift.endTime?.take(5)})" else ""}",
                                                    fontSize = 11.sp,
                                                    color = PresenceDesign.LabelText
                                                )
                                                if (!shift.premisesName.isNullOrBlank()) {
                                                    Text(
                                                        text = shift.premisesName,
                                                        fontSize = 10.sp,
                                                        color = PresenceDesign.NeutralText
                                                    )
                                                }
                                            }

                                            if (shift.alreadyRequested) {
                                                PresenceBadge(
                                                    label = "Already Applied",
                                                    tone = PresenceTone.Neutral
                                                )
                                            } else {
                                                Switch(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedKeys = if (checked) {
                                                            selectedKeys + key
                                                        } else {
                                                            selectedKeys - key
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (error != null) error = null
                    },
                    label = { Text("Reason for leave") },
                    placeholder = { Text("e.g. Personal work, family function, doctor visit") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = PresenceDesign.Danger, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shiftsList = (upcomingShiftsState as? UiState.Success)?.data.orEmpty()
                    val selectedShifts = shiftsList.filter { selectedKeys.contains("${it.shiftAssignmentId}_${it.workDate}") }

                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSubmitting && selectedShifts.isNotEmpty() && reason.isNotBlank(),
                        onClick = {
                            if (selectedShifts.isEmpty()) {
                                error = "Please toggle at least one shift to enable."
                            } else if (reason.isBlank()) {
                                error = "Please enter a reason for the leave."
                            } else {
                                showConfirmSubmodal = true
                            }
                        }
                    ) { Text(if (isSubmitting) "Submitting…" else "Review & Submit (${selectedShifts.size})") }
                }
            }
        }
    }

    // Minor Pop Confirmation Submodal over Bottom Sheet
    if (showConfirmSubmodal) {
        val shiftsList = (upcomingShiftsState as? UiState.Success)?.data.orEmpty()
        val selectedShifts = shiftsList.filter { selectedKeys.contains("${it.shiftAssignmentId}_${it.workDate}") }

        AlertDialog(
            onDismissRequest = { showConfirmSubmodal = false },
            title = { Text("Confirm Leave Request", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to submit a leave request for ${selectedShifts.size} shift(s)?",
                        fontSize = 12.5.sp,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "Reason: ${reason.trim()}",
                        fontSize = 11.5.sp,
                        color = PresenceDesign.LabelText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSubmodal = false
                        onSubmit(selectedShifts, reason)
                    }
                ) { Text("Confirm & Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSubmodal = false }) { Text("Back") }
            }
        )
    }
}
