package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.als.presence.ui.dashboard.common.PresenceDetailRow
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
    val isWeekend: Boolean
)

private data class EditingShiftState(
    val template: ShiftTemplateDto,
    val dayOfWeek: Int
)

val WeekDaysList = listOf(
    DayInfo(1, "Mon", "Monday", false),
    DayInfo(2, "Tue", "Tuesday", false),
    DayInfo(3, "Wed", "Wednesday", false),
    DayInfo(4, "Thu", "Thursday", false),
    DayInfo(5, "Fri", "Friday", false),
    DayInfo(6, "Sat", "Saturday", true),
    DayInfo(0, "Sun", "Sunday", true)
)

/**
 * "Manage Shifts" management screen. 7-Day cards shift schedule with Add button on each day card and Edit on shifts.
 */
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

    val filteredTemplates = templates

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
                description = "7-day view of shifts for the selected premises."
            )

            when (val state = templatesState) {
                is UiState.Idle, is UiState.Loading -> PresenceLoading("Loading shift schedule…")
                is UiState.Error -> PresenceErrorState(message = state.message, onRetry = viewModel::refresh)
                is UiState.Success -> {
                    WeekDaysList.forEach { dayInfo ->
                        val dayShifts = filteredTemplates.filter { template ->
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
        CreateShiftModal(
            initialPremisesId = premises.firstOrNull()?.id,
            initialDayOfWeek = createForDayOfWeek ?: 1,
            premisesList = premises,
            onDismiss = {
                showCreateTemplate = false
                createForDayOfWeek = null
            },
            onConfirm = { name, start, end, dayName, halfDayThreshold, premisesId ->
                showCreateTemplate = false
                createForDayOfWeek = null
                viewModel.createShift(
                    premisesId = premisesId,
                    shiftName = name,
                    startTime = start,
                    endTime = end,
                    dayName = dayName,
                    halfDayThreshold = halfDayThreshold
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
            onConfirm = { name, start, end, dayName, halfDayThreshold, premisesId, isActive ->
                val id = activeEditing.template.id
                editingShiftState = null
                viewModel.updateShift(
                    id = id,
                    shiftName = name,
                    startTime = start,
                    endTime = end,
                    dayName = dayName,
                    halfDayThreshold = halfDayThreshold,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
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
                        label = "Add",
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
                    text = "No shifts active on ${dayInfo.fullName}.",
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
                Text(
                    text = "${Dates.formatTime(template.startTime)} – ${Dates.formatTime(template.endTime)}" +
                            if (!premisesName.isNullOrBlank()) " • ${Dates.cleanPremisesName(premisesName)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresenceBadge(
                        label = "Day: ${template.dayName}",
                        tone = PresenceTone.Info
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayNameSelector(
    selectedDayName: String,
    onDayNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDaysSet = remember { setOf(1, 2, 3, 4, 5, 6, 0) }
    val weekdaysSet = remember { setOf(1, 2, 3, 4, 5) }
    val weekendsSet = remember { setOf(6, 0) }

    fun parseDays(dayNameStr: String): Set<Int> {
        val dn = dayNameStr.trim()
        if (dn.isEmpty() || dn.equals("ALL", ignoreCase = true) || dn.equals("Everyday", ignoreCase = true) || dn.equals("Every Day", ignoreCase = true)) {
            return allDaysSet
        }
        if (dn.equals("Weekdays", ignoreCase = true)) return weekdaysSet
        if (dn.equals("Weekends", ignoreCase = true)) return weekendsSet

        val set = mutableSetOf<Int>()
        if (dn.contains("Monday", ignoreCase = true) || dn.contains("Mon", ignoreCase = true)) set.add(1)
        if (dn.contains("Tuesday", ignoreCase = true) || dn.contains("Tue", ignoreCase = true)) set.add(2)
        if (dn.contains("Wednesday", ignoreCase = true) || dn.contains("Wed", ignoreCase = true)) set.add(3)
        if (dn.contains("Thursday", ignoreCase = true) || dn.contains("Thu", ignoreCase = true)) set.add(4)
        if (dn.contains("Friday", ignoreCase = true) || dn.contains("Fri", ignoreCase = true)) set.add(5)
        if (dn.contains("Saturday", ignoreCase = true) || dn.contains("Sat", ignoreCase = true)) set.add(6)
        if (dn.contains("Sunday", ignoreCase = true) || dn.contains("Sun", ignoreCase = true)) set.add(0)
        return if (set.isEmpty()) allDaysSet else set
    }

    fun formatDays(set: Set<Int>): String {
        return when {
            set.size == 7 -> "Everyday"
            set == weekdaysSet -> "Weekdays"
            set == weekendsSet -> "Weekends"
            set.isEmpty() -> "Everyday"
            else -> {
                val dayOrder = listOf(1, 2, 3, 4, 5, 6, 0)
                set.sortedBy { dayOrder.indexOf(it) }
                    .mapNotNull { dow -> WeekDaysList.find { it.dayOfWeek == dow }?.fullName }
                    .joinToString(", ")
            }
        }
    }

    var currentSelectedDays by remember(selectedDayName) {
        mutableStateOf(parseDays(selectedDayName))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Applicable Days",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isEveryday = currentSelectedDays.size == 7
            FilterChip(
                selected = isEveryday,
                onClick = {
                    val newSet = allDaysSet
                    currentSelectedDays = newSet
                    onDayNameChanged(formatDays(newSet))
                },
                label = { Text("Everyday") }
            )

            val isWeekdays = currentSelectedDays == weekdaysSet
            FilterChip(
                selected = isWeekdays,
                onClick = {
                    val newSet = weekdaysSet
                    currentSelectedDays = newSet
                    onDayNameChanged(formatDays(newSet))
                },
                label = { Text("Weekdays") }
            )

            val isWeekends = currentSelectedDays == weekendsSet
            FilterChip(
                selected = isWeekends,
                onClick = {
                    val newSet = weekendsSet
                    currentSelectedDays = newSet
                    onDayNameChanged(formatDays(newSet))
                },
                label = { Text("Weekends") }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WeekDaysList.forEach { dayInfo ->
                val isSelected = currentSelectedDays.contains(dayInfo.dayOfWeek)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) {
                            val updated = currentSelectedDays - dayInfo.dayOfWeek
                            if (updated.isEmpty()) allDaysSet else updated
                        } else {
                            currentSelectedDays + dayInfo.dayOfWeek
                        }
                        currentSelectedDays = newSet
                        onDayNameChanged(formatDays(newSet))
                    },
                    label = { Text(dayInfo.shortName) }
                )
            }
        }

        Text(
            text = "Selected: ${formatDays(currentSelectedDays)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateShiftModal(
    initialPremisesId: String? = null,
    initialDayOfWeek: Int = 1,
    premisesList: List<PremisesDto> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        start: String,
        end: String,
        dayName: String,
        halfDayThreshold: String,
        premisesId: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("18:00") }
    var halfDayThreshold by remember { mutableStateOf("11:00") }

    val dayObj = WeekDaysList.find { it.dayOfWeek == initialDayOfWeek }
    var dayName by remember(initialDayOfWeek) { mutableStateOf(dayObj?.fullName ?: "Monday") }

    val targetPremisesId = initialPremisesId ?: premisesList.firstOrNull()?.id.orEmpty()

    val valid = name.trim().length >= 2 &&
            targetPremisesId.isNotBlank() &&
            start.matches(Regex("^\\d{2}:\\d{2}$")) &&
            end.matches(Regex("^\\d{2}:\\d{2}$")) &&
            halfDayThreshold.matches(Regex("^\\d{2}:\\d{2}$"))

    val premiseObj = premisesList.find { it.id == targetPremisesId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "New Shift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            PresenceFieldGroup(title = "Scope Details") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (premiseObj != null) {
                        PresenceDetailRow(label = "Premises", value = Dates.cleanPremisesName(premiseObj.name))
                    }
                    PresenceDetailRow(label = "Selected Day", value = dayName)
                }
            }

            PresenceFieldGroup(title = "Details & Days") {
                PresenceTextField(value = name, onValueChange = { name = it }, label = "Shift name")
                DayNameSelector(
                    selectedDayName = dayName,
                    onDayNameChanged = { dayName = it }
                )
            }

            PresenceFieldGroup(title = "Timing & Thresholds") {
                PresenceTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = "Start time (HH:MM)"
                )
                PresenceTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = "End time (HH:MM)"
                )
                PresenceTextField(
                    value = halfDayThreshold,
                    onValueChange = { halfDayThreshold = it },
                    label = "Half-day threshold (HH:MM)"
                )
                Text(
                    text = "Cutoff time (e.g. 11:00 or 13:00) when shift counts as Half-Day if clock-out is before this threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.LabelText,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            PresenceButton(
                label = "Create Shift",
                onClick = {
                    onConfirm(
                        name.trim(),
                        start.trim(),
                        end.trim(),
                        dayName.trim(),
                        halfDayThreshold.trim(),
                        targetPremisesId
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditShiftModal(
    template: ShiftTemplateDto,
    targetDayOfWeek: Int? = null,
    premisesList: List<PremisesDto> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        start: String,
        end: String,
        dayName: String,
        halfDayThreshold: String,
        premisesId: String?,
        isActive: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val targetDayObj = targetDayOfWeek?.let { dow -> WeekDaysList.find { it.dayOfWeek == dow } }
    var name by remember(template) { mutableStateOf(template.name) }
    var dayName by remember(template, targetDayOfWeek) {
        mutableStateOf(targetDayObj?.fullName ?: template.dayName)
    }
    var start by remember(template) { mutableStateOf(template.startTime.take(5)) }
    var end by remember(template) { mutableStateOf(template.endTime.take(5)) }
    var halfDayThreshold by remember(template) { mutableStateOf(template.halfDayThreshold.take(5)) }
    var isActive by remember(template) { mutableStateOf(template.isActive) }

    val valid = name.trim().length >= 2 &&
            start.matches(Regex("^\\d{2}:\\d{2}$")) &&
            end.matches(Regex("^\\d{2}:\\d{2}$")) &&
            halfDayThreshold.matches(Regex("^\\d{2}:\\d{2}$"))

    val premiseObj = premisesList.find { it.id == template.premisesId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Shift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            PresenceFieldGroup(title = "Scope Details") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (premiseObj != null) {
                        PresenceDetailRow(label = "Premises", value = Dates.cleanPremisesName(premiseObj.name))
                    }
                    PresenceDetailRow(label = "Applicable Days", value = dayName)
                }
            }

            PresenceFieldGroup(title = "Details & Days") {
                PresenceTextField(value = name, onValueChange = { name = it }, label = "Shift name")
                DayNameSelector(
                    selectedDayName = dayName,
                    onDayNameChanged = { dayName = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active status",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }

            PresenceFieldGroup(title = "Timing & Thresholds") {
                PresenceTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = "Start time (HH:MM)"
                )
                PresenceTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = "End time (HH:MM)"
                )
                PresenceTextField(
                    value = halfDayThreshold,
                    onValueChange = { halfDayThreshold = it },
                    label = "Half-day threshold (HH:MM)"
                )
                Text(
                    text = "Cutoff time (e.g. 11:00 or 13:00) when shift counts as Half-Day if clock-out is before this threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.LabelText,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            PresenceButton(
                label = "Save Changes",
                onClick = {
                    onConfirm(
                        name.trim(),
                        start.trim(),
                        end.trim(),
                        dayName.trim(),
                        halfDayThreshold.trim(),
                        template.premisesId,
                        isActive
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            if (onDelete != null) {
                PresenceButton(
                    label = "Delete Shift",
                    onClick = onDelete,
                    variant = PresenceVariant.Danger,
                    size = PresenceSize.Sm,
                    icon = Icons.Default.Delete,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
