package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.owner.screens.DayInfo
import com.als.presence.ui.dashboard.owner.screens.WeekDaysList
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel

private data class AssignedShiftInfo(
    val assignment: ShiftAssignmentDto,
    val template: ShiftDto?
)

/**
 * "My Shifts & Schedule" screen for Staff, Managers, and Owners.
 * Cleanly presents the 7-day weekly assigned shift schedule, working hours,
 * and per-shift payment rates breakdown.
 */
@Composable
fun MyShiftsScreen(
    user: User,
    onBack: () -> Unit,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel()
) {
    val nextSession by viewModel.nextSession.collectAsStateWithLifecycle()
    val assignmentsState by viewModel.assignments.collectAsStateWithLifecycle()
    val templatesState by viewModel.templates.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PresenceSecondaryScaffold(
        crumb = "MY WORKSPACE",
        title = "My Shifts & Schedule",
        onBack = onBack,
        onRefresh = viewModel::refresh
    ) {
        // 1. Shift Schedule Overview Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Current Schedule Window",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val rawTitle = nextSession?.title ?: "Standard Shift Schedule"
                            val displayTitle = remember(rawTitle) {
                                formatTitle(rawTitle)
                            }
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = user.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sessionTime = nextSession?.window ?: "Regular Working Hours"
                    val premisesName = remember(user) {
                        user.premisesName?.let { Dates.cleanPremisesName(it) }
                            ?.takeIf { it.isNotBlank() }
                            ?: user.organizationName.orEmpty().ifBlank { "Assigned Premises" }
                    }

                    OverviewStatBox(
                        label = "Shift Window",
                        value = sessionTime,
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatBox(
                        label = "Premises",
                        value = premisesName,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. 7-Day Weekly Shift Schedule & Payments Cards
        Personal7DayScheduleSection(
            assignmentsState = assignmentsState,
            templatesState = templatesState,
            currentUserId = currentUserId
        )
    }
}

@Composable
private fun Personal7DayScheduleSection(
    assignmentsState: UiState<List<ShiftAssignmentDto>>,
    templatesState: UiState<List<ShiftDto>>,
    currentUserId: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PresenceSectionTitle(
            title = "Weekly 7-Day Shift Schedule & Payments",
            description = "Your assigned shift schedule and earned payment rates for each day of the week."
        )

        when {
            assignmentsState is UiState.Loading || templatesState is UiState.Loading -> {
                PresenceLoading("Loading 7-day schedule…")
            }
            assignmentsState is UiState.Error -> {
                PresenceErrorState(message = assignmentsState.message, onRetry = { })
            }
            else -> {
                val assignments = (assignmentsState as? UiState.Success)?.data.orEmpty()
                val templates = (templatesState as? UiState.Success)?.data.orEmpty()

                val myAssignments = remember(assignments, currentUserId) {
                    if (currentUserId.isNullOrBlank()) {
                        assignments
                    } else {
                        val filtered = assignments.filter {
                            it.employeeId == currentUserId || it.staffId == currentUserId
                        }
                        filtered.ifEmpty { assignments }
                    }
                }

                WeekDaysList.forEach { dayInfo ->
                    val dayAssignedShifts = myAssignments.mapNotNull { assignment ->
                        val template = templates.find { it.id == assignment.shiftId }
                        val days = template?.daysOfWeek
                        val applies = days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        if (applies) AssignedShiftInfo(assignment, template) else null
                    }

                    val dayFallbackTemplates = if (dayAssignedShifts.isEmpty()) {
                        templates.filter { template ->
                            val days = template.daysOfWeek
                            days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        }
                    } else emptyList()

                    DayScheduleAndPaymentCard(
                        dayInfo = dayInfo,
                        assignedShifts = dayAssignedShifts,
                        fallbackTemplates = dayFallbackTemplates
                    )
                }
            }
        }
    }
}

@Composable
private fun DayScheduleAndPaymentCard(
    dayInfo: DayInfo,
    assignedShifts: List<AssignedShiftInfo>,
    fallbackTemplates: List<ShiftDto>
) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Day Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayInfo.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = dayInfo.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PresenceDesign.Ink
                        )
                        Text(
                            text = when {
                                assignedShifts.isNotEmpty() -> "${assignedShifts.size} Assigned Shift${if (assignedShifts.size > 1) "s" else ""}"
                                fallbackTemplates.isNotEmpty() -> "Premises Shift Available"
                                else -> "Weekly Off / Rest Day"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (assignedShifts.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else PresenceDesign.LabelText
                        )
                    }
                }

                if (assignedShifts.isNotEmpty()) {
                    PresenceBadge(
                        label = "Active Schedule",
                        tone = PresenceTone.Success,
                        dot = true
                    )
                } else if (fallbackTemplates.isNotEmpty()) {
                    PresenceBadge(
                        label = "Premises Shift",
                        tone = PresenceTone.Info
                    )
                } else {
                    PresenceBadge(
                        label = "Weekly Off",
                        tone = PresenceTone.Neutral
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Card Body Content
            when {
                assignedShifts.isNotEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        assignedShifts.forEach { shiftInfo ->
                            AssignedShiftPaymentDetailRow(shiftInfo = shiftInfo)
                        }
                    }
                }
                fallbackTemplates.isNotEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        fallbackTemplates.forEach { template ->
                            TemplateShiftDetailRow(template = template)
                        }
                    }
                }
                else -> {
                    // Weekly Off / No Shift Assigned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Scheduled Weekly Off",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PresenceDesign.Ink
                            )
                            Text(
                                text = "Rest day • No active attendance required",
                                style = MaterialTheme.typography.bodySmall,
                                color = PresenceDesign.LabelText
                            )
                        }
                        PresenceBadge(
                            label = "Weekly Off",
                            tone = PresenceTone.Info
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignedShiftPaymentDetailRow(shiftInfo: AssignedShiftInfo) {
    val assignment = shiftInfo.assignment
    val template = shiftInfo.template
    val rawShiftName = template?.shiftName ?: assignment.shiftName ?: "Assigned Shift"
    val shiftName = remember(rawShiftName) { formatTitle(rawShiftName) }
    val windowText = Dates.formatWindow(template?.startTime, template?.endTime)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shift Title & Window
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shiftName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "⏰ $windowText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PresenceBadge(
                    label = "Assigned",
                    tone = PresenceTone.Success
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Payment Rates Grid (2x2 Grid)
            Text(
                text = "Per-Shift Payment Rates",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PresenceDesign.LabelText
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentChip(
                        label = "Present Rate",
                        value = formatRate(assignment.presentPay),
                        tone = PresenceTone.Success,
                        modifier = Modifier.weight(1f)
                    )
                    PaymentChip(
                        label = "Half-Day Rate",
                        value = formatRate(assignment.halfDayPay),
                        tone = PresenceTone.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentChip(
                        label = "Leave Pay",
                        value = formatRate(assignment.leavePay),
                        tone = PresenceTone.Info,
                        modifier = Modifier.weight(1f)
                    )
                    PaymentChip(
                        label = "Absent Pay",
                        value = formatRate(assignment.absentPay),
                        tone = PresenceTone.Danger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateShiftDetailRow(template: ShiftDto) {
    val rawShiftName = template.shiftName
    val shiftName = remember(rawShiftName) { formatTitle(rawShiftName) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
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
                    text = shiftName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "⏰ ${Dates.formatWindow(template.startTime, template.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            PresenceBadge(
                label = "Premises Shift",
                tone = PresenceTone.Info
            )
        }
    }
}

@Composable
private fun PaymentChip(
    label: String,
    value: String,
    tone: PresenceTone,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PresenceDesign.LabelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = when (tone) {
                    PresenceTone.Success -> MaterialTheme.colorScheme.primary
                    PresenceTone.Warning -> MaterialTheme.colorScheme.tertiary
                    PresenceTone.Info -> MaterialTheme.colorScheme.secondary
                    PresenceTone.Danger -> MaterialTheme.colorScheme.error
                    else -> PresenceDesign.Ink
                },
                maxLines = 1
            )
        }
    }
}

private fun formatRate(amount: Double): String {
    val integerPart = amount.toInt()
    return if (amount == integerPart.toDouble()) "₹$integerPart" else "₹${String.format(java.util.Locale.US, "%.2f", amount)}"
}

private fun formatTitle(title: String): String {
    if (title.isBlank()) return "Standard Shift Schedule"
    if (title.equals("day", ignoreCase = true)) return "Day Shift"
    return title.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Composable
private fun OverviewStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

