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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
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
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import com.als.presence.ui.dashboard.owner.screens.DayInfo
import com.als.presence.ui.dashboard.owner.screens.WeekDaysList

private data class AssignedShiftInfo(
    val assignment: ShiftAssignmentDto,
    val template: ShiftDto?
)

/**
 * "My Shifts & Schedule" screen for Employees, Managers, and Owners.
 * Cleanly presents the 7-day weekly assigned shift schedule, working hours,
 * and per-shift payment rates breakdown.
 */
@Composable
fun MyShiftsScreen(
    user: User,
    onBack: () -> Unit,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel()
) {
    val assignmentsState by viewModel.assignments.collectAsStateWithLifecycle()
    val templatesState by viewModel.templates.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

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

    val hasPersonalAssignments = myAssignments.isNotEmpty()

    // 1. My Personal Schedule Stats
    val myAssignedDaysCount = remember(myAssignments, templates) {
        WeekDaysList.count { dayInfo ->
            myAssignments.any { assignment ->
                val template = templates.find { it.id == assignment.shiftId }
                val days = template?.daysOfWeek
                days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
            }
        }
    }
    val myWeeklyOffsCount = 7 - myAssignedDaysCount

    // 2. Premises Schedule Stats
    val premisesWorkingDaysCount = remember(templates) {
        WeekDaysList.count { dayInfo ->
            templates.any { template ->
                val days = template.daysOfWeek
                days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
            }
        }
    }
    val premisesWeeklyOffsCount = 7 - premisesWorkingDaysCount
    val assignedShiftsCount = myAssignments.size

    PresenceSecondaryScaffold(
        crumb = "MY WORKSPACE",
        title = "My Shifts & Schedule",
        onBack = onBack,
        onRefresh = viewModel::refresh
    ) {
        PresencePageIntro(
            eyebrow = "ROSTER",
            title = "My Weekly Schedule",
            description = "Your 7-day shift timetable, working hours, and per-shift earning rates."
        )

        PresenceMetaStrip(
            items = if (hasPersonalAssignments) {
                listOf(
                    "My Assigned Shifts" to "$assignedShiftsCount ${if (assignedShiftsCount == 1) "Shift" else "Shifts"}",
                    "My Working Days" to "$myAssignedDaysCount Days",
                    "My Weekly Offs" to "$myWeeklyOffsCount Days",
                    "Premises Work Days" to "$premisesWorkingDaysCount Days"
                )
            } else {
                listOf(
                    "Assigned Shifts" to "None (Default Premises)",
                    "Premises Work Days" to "$premisesWorkingDaysCount Days",
                    "Premises Weekly Offs" to "$premisesWeeklyOffsCount Days"
                )
            }
        )

        // 7-Day Weekly Shift Schedule Cards
        Personal7DayScheduleSection(
            assignmentsState = assignmentsState,
            templatesState = templatesState,
            myAssignments = myAssignments,
            assignedShiftsCount = assignedShiftsCount,
            assignedDaysCount = myAssignedDaysCount,
            hasPersonalAssignments = hasPersonalAssignments
        )
    }
}

@Composable
private fun Personal7DayScheduleSection(
    assignmentsState: UiState<List<ShiftAssignmentDto>>,
    templatesState: UiState<List<ShiftDto>>,
    myAssignments: List<ShiftAssignmentDto>,
    assignedShiftsCount: Int,
    assignedDaysCount: Int,
    hasPersonalAssignments: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PresenceSectionTitle(
            title = "Weekly 7-Day Timetable",
            description = if (hasPersonalAssignments) {
                "Detailed day-by-day timetable ($assignedShiftsCount assigned shift${if (assignedShiftsCount != 1) "s" else ""} across $assignedDaysCount assigned day${if (assignedDaysCount != 1) "s" else ""})."
            } else {
                "Detailed day-by-day timetable using default premises shift schedule."
            }
        )

        when {
            assignmentsState is UiState.Loading || templatesState is UiState.Loading -> {
                PresenceLoading("Loading 7-day schedule…")
            }
            assignmentsState is UiState.Error -> {
                PresenceErrorState(message = assignmentsState.message, onRetry = { })
            }
            else -> {
                val templates = (templatesState as? UiState.Success)?.data.orEmpty()

                WeekDaysList.forEach { dayInfo ->
                    val dayAssignedShifts = myAssignments.mapNotNull { assignment ->
                        val template = templates.find { it.id == assignment.shiftId }
                        val days = template?.daysOfWeek
                        val applies = days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        if (applies) AssignedShiftInfo(assignment, template) else null
                    }

                    val dayFallbackTemplates = if (dayAssignedShifts.isEmpty() && !hasPersonalAssignments) {
                        templates.filter { template ->
                            val days = template.daysOfWeek
                            days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        }
                    } else emptyList()

                    DayScheduleAndPaymentCard(
                        dayInfo = dayInfo,
                        assignedShifts = dayAssignedShifts,
                        fallbackTemplates = dayFallbackTemplates,
                        hasPersonalAssignments = hasPersonalAssignments
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
    fallbackTemplates: List<ShiftDto>,
    hasPersonalAssignments: Boolean
) {
    val isAssigned = assignedShifts.isNotEmpty()
    val isFallback = !isAssigned && fallbackTemplates.isNotEmpty()
    val isMyWeeklyOff = !isAssigned && hasPersonalAssignments
    val isPremisesWeeklyOff = !isAssigned && !hasPersonalAssignments && fallbackTemplates.isEmpty()

    PresenceCard(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isAssigned -> MaterialTheme.colorScheme.primaryContainer
                                    isFallback -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayInfo.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isAssigned -> MaterialTheme.colorScheme.onPrimaryContainer
                                isFallback -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> PresenceDesign.LabelText
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dayInfo.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMyWeeklyOff || isPremisesWeeklyOff) PresenceDesign.LabelText else PresenceDesign.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isAssigned -> "${assignedShifts.size} Assigned Shift${if (assignedShifts.size > 1) "s" else ""}"
                                isFallback -> "Default Premises Shift"
                                isMyWeeklyOff -> "My Scheduled Rest Day"
                                else -> "Premises Weekly Off"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAssigned) MaterialTheme.colorScheme.primary
                            else PresenceDesign.LabelText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                when {
                    isAssigned -> PresenceBadge(label = "My Shift", tone = PresenceTone.Success, dot = true)
                    isFallback -> PresenceBadge(label = "Premises Shift", tone = PresenceTone.Info)
                    isMyWeeklyOff -> PresenceBadge(label = "My Weekly Off", tone = PresenceTone.Neutral)
                    else -> PresenceBadge(label = "Premises Off", tone = PresenceTone.Neutral)
                }
            }

            // Body Content
            when {
                isAssigned -> {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        assignedShifts.forEach { shiftInfo ->
                            AssignedShiftPaymentDetailRow(shiftInfo = shiftInfo)
                        }
                    }
                }
                isFallback -> {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        fallbackTemplates.forEach { template ->
                            TemplateShiftDetailRow(template = template)
                        }
                    }
                }
                else -> {
                    // Weekly Off Body
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Rest Day",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (isMyWeeklyOff) "My Weekly Off" else "Premises Weekly Off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PresenceDesign.Ink
                                )
                                Text(
                                    text = if (isMyWeeklyOff) {
                                        "Scheduled rest day for your assigned working schedule"
                                    } else {
                                        "No premises shift scheduled for today"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PresenceDesign.LabelText
                                )
                            }
                        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = windowText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = Dates.formatWindow(template.startTime, template.endTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
