package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.als.presence.ui.dashboard.owner.modals.CreateShiftModal
import com.als.presence.ui.dashboard.owner.modals.EditShiftModal
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

data class DayInfo(
    val dayOfWeek: Int,
    val shortName: String,
    val fullName: String,
    val isWeekend: Boolean = false
)

private data class EditingShiftState(
    val template: ShiftTemplateDto,
    val dayOfWeek: Int
)

val WeekDaysList = listOf(
    DayInfo(1, "Mon", "Monday"),
    DayInfo(2, "Tue", "Tuesday"),
    DayInfo(3, "Wed", "Wednesday"),
    DayInfo(4, "Thu", "Thursday"),
    DayInfo(5, "Fri", "Friday"),
    DayInfo(6, "Sat", "Saturday"),
    DayInfo(0, "Sun", "Sunday")
)

/**
 * Converts half-day threshold duration in minutes to HH:MM:SS wall clock time string based on shift startTime.
 */
private fun convertMinutesToThresholdTime(startTime: String, thresholdMinutes: Int): String {
    val parts = startTime.split(":")
    val startH = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val totalM = (startH * 60 + startM + thresholdMinutes) % (24 * 60)
    val h = totalM / 60
    val m = totalM % 60
    return String.format(java.util.Locale.US, "%02d:%02d:00", h, m)
}

/**
 * "Manage Shifts" management screen. 7-Day cards shift schedule with Add button on each day card and Edit on shifts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementScreen(
    onBack: () -> Unit,
    canManage: Boolean = true,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val templatesState by viewModel.shiftTemplatesState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val premisesState by viewModel.premisesState.collectAsStateWithLifecycle()
    val message by viewModel.actionMessage.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.clearActionMessage()
        }
    }

    var createForDayOfWeek by remember { mutableStateOf<Int?>(null) }
    var showCreateTemplate by remember { mutableStateOf(false) }
    var editingShiftState by remember { mutableStateOf<EditingShiftState?>(null) }

    val templates = (templatesState as? UiState.Success)?.data.orEmpty()
    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()

    Box {
        PresenceSecondaryScaffold(
            crumb = "Scheduling",
            title = "Shift Schedule",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            PresencePageIntro(
                eyebrow = "Timetable",
                title = "Shift Management",
                description = "Define reusable shift templates and timing rules for your workforce across the 7-day week."
            )

            PresenceMetaStrip(
                items = listOf(
                    "Templates" to templates.size.toString(),
                    "Employees Covered" to staff.size.toString()
                )
            )

            PresenceSectionTitle(
                title = "Weekly Shift Schedule",
                description = "7-day view of shifts across the week."
            )

            when (val state = templatesState) {
                is UiState.Idle, is UiState.Loading -> PresenceLoading("Loading shift schedule…")
                is UiState.Error -> PresenceErrorState(message = state.message, onRetry = viewModel::refresh)
                is UiState.Success -> {
                    WeekDaysList.forEach { dayInfo ->
                        val dayShifts = templates.filter { template ->
                            val days = template.daysOfWeek
                            days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        }

                        DayShiftCard(
                            dayInfo = dayInfo,
                            shifts = dayShifts,
                            premisesList = premises,
                            canManage = canManage,
                            onAddShift = { dayOfWeek ->
                                createForDayOfWeek = dayOfWeek
                                showCreateTemplate = true
                            },
                            onEditShift = { template, dayOfWeek ->
                                editingShiftState = EditingShiftState(template, dayOfWeek)
                            }
                        )
                    }
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showCreateTemplate) {
        val targetDay = WeekDaysList.find { it.dayOfWeek == (createForDayOfWeek ?: 1) } ?: WeekDaysList.first()
        val targetPremises = premises.firstOrNull()

        CreateShiftModal(
            selectedPremises = targetPremises,
            selectedDay = targetDay,
            onDismiss = {
                showCreateTemplate = false
                createForDayOfWeek = null
            },
            onConfirm = { name, start, end, dayName, halfDayThresholdMinutes, premisesId ->
                showCreateTemplate = false
                createForDayOfWeek = null
                val halfDayThresholdTime = convertMinutesToThresholdTime(start, halfDayThresholdMinutes)
                viewModel.createShift(
                    premisesId = premisesId,
                    shiftName = name,
                    startTime = start,
                    endTime = end,
                    dayName = dayName,
                    halfDayThreshold = halfDayThresholdTime
                )
            }
        )
    }

    val activeEditing = editingShiftState
    if (activeEditing != null) {
        EditShiftModal(
            template = activeEditing.template,
            targetDayOfWeek = activeEditing.dayOfWeek,
            premisesList = premises,
            onDismiss = { editingShiftState = null },
            onConfirm = { name, start, end, dayName, halfDayThresholdMinutes, _, isActive ->
                val id = activeEditing.template.id
                editingShiftState = null
                val halfDayThresholdTime = convertMinutesToThresholdTime(start, halfDayThresholdMinutes)
                viewModel.updateShift(
                    id = id,
                    shiftName = name,
                    startTime = start,
                    endTime = end,
                    dayName = dayName,
                    halfDayThreshold = halfDayThresholdTime,
                    isActive = isActive
                )
            },
            onDelete = {
                val id = activeEditing.template.id
                editingShiftState = null
                viewModel.deleteShift(id)
            }
        )
    }
}

@Composable
private fun DayShiftCard(
    dayInfo: DayInfo,
    shifts: List<ShiftTemplateDto>,
    premisesList: List<PremisesDto>,
    canManage: Boolean,
    onAddShift: (dayOfWeek: Int) -> Unit,
    onEditShift: (template: ShiftTemplateDto, dayOfWeek: Int) -> Unit
) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayInfo.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
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
                            text = if (shifts.isEmpty()) "No shifts assigned"
                            else "${shifts.size} ${if (shifts.size == 1) "shift" else "shifts"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PresenceDesign.LabelText
                        )
                    }
                }

                if (canManage) {
                    PresenceButton(
                        label = "Add Shift",
                        onClick = { onAddShift(dayInfo.dayOfWeek) },
                        variant = PresenceVariant.Secondary,
                        size = PresenceSize.Sm,
                        icon = Icons.Default.Add
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (shifts.isEmpty()) {
                Text(
                    text = "No shifts scheduled for ${dayInfo.fullName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.LabelText,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                shifts.forEach { template ->
                    val premisesName = premisesList.find { it.id == template.premisesId }?.name
                    ShiftItemRow(
                        template = template,
                        premisesName = premisesName,
                        canManage = canManage,
                        onEditShift = { onEditShift(template, dayInfo.dayOfWeek) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftItemRow(
    template: ShiftTemplateDto,
    premisesName: String?,
    canManage: Boolean,
    onEditShift: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    PresenceBadge(
                        label = if (template.isActive) "Active" else "Inactive",
                        tone = if (template.isActive) PresenceTone.Success else PresenceTone.Warning,
                        dot = true
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
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
                        text = "${Dates.formatTime(template.startTime)} – ${Dates.formatTime(template.endTime)}" +
                                if (!premisesName.isNullOrBlank()) " • ${Dates.cleanPremisesName(premisesName)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresenceBadge(
                        label = "Day: ${template.dayName}",
                        tone = PresenceTone.Info
                    )
                    PresenceBadge(
                        label = "Half-Day: ${template.halfDayThresholdMinutes} mins",
                        tone = PresenceTone.Neutral
                    )
                }
            }

            if (canManage) {
                Spacer(modifier = Modifier.width(8.dp))
                PresenceIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit shift ${template.name}",
                    onClick = onEditShift
                )
            }
        }
    }
}
