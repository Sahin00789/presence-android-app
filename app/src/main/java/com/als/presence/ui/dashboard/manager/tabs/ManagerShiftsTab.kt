package com.als.presence.ui.dashboard.manager.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceDetailRow
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.manager.screens.DayInfo
import com.als.presence.ui.dashboard.manager.screens.WeekDaysList

/** Manager "Shifts" tab — 7-day card view with Add button on each day card and Edit on shifts. */
@Composable
fun ManagerShiftsTab(
    templatesState: UiState<List<ShiftTemplateDto>>,
    assignmentsState: UiState<List<ShiftAssignmentDto>>,
    modifier: Modifier = Modifier,
    premisesState: UiState<List<PremisesDto>> = UiState.Success(emptyList()),
    onRefresh: () -> Unit,
    onCreateTemplate: (premisesId: String?, dayOfWeek: Int) -> Unit = { _, _ -> },
    onUpdateTemplate: (
        templateId: String,
        name: String,
        start: String,
        end: String,
        dayName: String,
        halfDayThreshold: String,
        premisesId: String?,
        isActive: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteTemplate: (templateId: String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val templates = (templatesState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()

    var selectedPremisesId by remember(premises) { mutableStateOf(premises.firstOrNull()?.id) }
    var editingTemplate by remember { mutableStateOf<ShiftTemplateDto?>(null) }

    val filteredTemplates = templates.filter { template ->
        (selectedPremisesId == null) ||
                (template.premisesId == selectedPremisesId) ||
                (template.premisesId == null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Shift Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${templates.size} shift templates • 7-day view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh shifts",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Top Direct Premises Selector
            if (premises.isNotEmpty()) {
                PremisesSelectionHeader(
                    premises = premises,
                    selectedPremisesId = selectedPremisesId,
                    onSelectPremises = { selectedPremisesId = it }
                )
            }

            SectionHeader(title = "Weekly Shift Cards")

            when (templatesState) {
                is UiState.Loading, is UiState.Idle -> ShiftsNotice("Loading shift schedule…")
                is UiState.Error -> ShiftsNotice(templatesState.message, isError = true)
                is UiState.Success -> {
                    WeekDaysList.forEach { dayInfo ->
                        val dayShifts = filteredTemplates.filter { template ->
                            val days = template.daysOfWeek
                            days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                        }

                        TabDayShiftCard(
                            dayInfo = dayInfo,
                            shifts = dayShifts,
                            premisesList = premises,
                            onAddShift = { dayOfWeek ->
                                onCreateTemplate(selectedPremisesId, dayOfWeek)
                            },
                            onEditShift = { template ->
                                editingTemplate = template
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        val activeEditing = editingTemplate
        if (activeEditing != null) {
            EditShiftModal(
                template = activeEditing,
                premisesList = premises,
                onDismiss = { editingTemplate = null },
                onConfirm = { name, start, end, dayName, halfDayThreshold, premisesId, isActive ->
                    onUpdateTemplate(
                        activeEditing.id,
                        name,
                        start,
                        end,
                        dayName,
                        halfDayThreshold,
                        premisesId,
                        isActive
                    )
                    editingTemplate = null
                },
                onDelete = {
                    onDeleteTemplate(activeEditing.id)
                    editingTemplate = null
                }
            )
        }
    }
}

@Composable
private fun PremisesSelectionHeader(
    premises: List<PremisesDto>,
    selectedPremisesId: String?,
    onSelectPremises: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Select Premises",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            premises.forEach { p ->
                val parts = p.name.split(",", limit = 2)
                val shortName = parts[0].trim()
                FilterChip(
                    selected = selectedPremisesId == p.id,
                    onClick = { onSelectPremises(p.id) },
                    label = { Text(shortName, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun TabDayShiftCard(
    dayInfo: DayInfo,
    shifts: List<ShiftTemplateDto>,
    premisesList: List<PremisesDto>,
    onAddShift: (dayOfWeek: Int) -> Unit,
    onEditShift: (template: ShiftTemplateDto) -> Unit
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

                PresenceButton(
                    label = "Add",
                    onClick = { onAddShift(dayInfo.dayOfWeek) },
                    variant = PresenceVariant.Secondary,
                    size = PresenceSize.Sm,
                    icon = Icons.Default.Add
                )
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
                    TabShiftItemRow(
                        template = template,
                        premisesName = premisesName,
                        onEditShift = { onEditShift(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabShiftItemRow(
    template: ShiftTemplateDto,
    premisesName: String?,
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
                            if (!premisesName.isNullOrBlank()) " • 📍 ${Dates.cleanPremisesName(premisesName)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            PresenceIconButton(
                icon = Icons.Default.Edit,
                contentDescription = "Edit shift ${template.name}",
                onClick = onEditShift
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditShiftModal(
    template: ShiftTemplateDto,
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

    var name by remember(template) { mutableStateOf(template.name) }
    var dayName by remember(template) { mutableStateOf(template.dayName) }
    var start by remember(template) { mutableStateOf(template.startTime.take(5)) }
    var end by remember(template) { mutableStateOf(template.endTime.take(5)) }
    var halfDayThreshold by remember(template) { mutableStateOf(template.halfDayThreshold.take(5)) }
    var isActive by remember(template) { mutableStateOf(template.isActive) }

    val valid = name.trim().length >= 2 &&
            start.matches(Regex("^\\d{2}:\\d{2}$")) &&
            end.matches(Regex("^\\d{2}:\\d{2}$"))

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

            PresenceFieldGroup(title = "Details") {
                PresenceTextField(value = name, onValueChange = { name = it }, label = "Shift name")
                PresenceTextField(value = dayName, onValueChange = { dayName = it }, label = "Day Name (e.g. Monday, ALL)")
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

            PresenceFieldGroup(title = "Timing") {
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

@Composable
private fun ShiftsNotice(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
