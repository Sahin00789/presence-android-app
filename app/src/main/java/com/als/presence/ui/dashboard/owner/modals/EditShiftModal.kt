package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.owner.screens.WeekDaysList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShiftModal(
    template: ShiftTemplateDto,
    targetDayOfWeek: Int? = null,
    premisesList: List<PremisesDto> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        start: String,
        end: String,
        dayName: String,
        halfDayThresholdMinutes: Int,
        premisesId: String?,
        isActive: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val targetDayObj = targetDayOfWeek?.let { dow -> WeekDaysList.find { it.dayOfWeek == dow } }
    val dayName = targetDayObj?.fullName ?: template.dayName
    val premiseObj = premisesList.find { it.id == template.premisesId }
    val premisesName = premiseObj?.name?.let { Dates.cleanPremisesName(it) } ?: "Assigned Premises"

    var name by remember(template) { mutableStateOf(template.name) }
    var start by remember(template) { mutableStateOf(template.startTime.take(5)) }
    var end by remember(template) { mutableStateOf(template.endTime.take(5)) }
    var halfDayThresholdMinutesStr by remember(template) {
        mutableStateOf(template.halfDayThresholdMinutes.toString())
    }
    var isActive by remember(template) { mutableStateOf(template.isActive) }

    val thresholdMinutesInt = halfDayThresholdMinutesStr.toIntOrNull()
    val valid = name.trim().length >= 2 &&
            start.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$")) &&
            end.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$")) &&
            thresholdMinutesInt != null && thresholdMinutesInt in 1..1439

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Shift Template",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            PresenceFieldGroup(title = "Fixed Scope (Non-Editable)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModalScopeInfoItem(
                        icon = Icons.Default.LocationOn,
                        label = "Premises",
                        value = premisesName
                    )
                    ModalScopeInfoItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Applicable Day",
                        value = dayName
                    )
                }
            }

            PresenceFieldGroup(title = "Shift Identity & Status") {
                PresenceTextField(value = name, onValueChange = { name = it }, label = "Shift Name *")

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Status",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PresenceDesign.Ink
                            )
                            Text(
                                text = if (isActive) "Available for roster assignment" else "Disabled for scheduling",
                                style = MaterialTheme.typography.bodySmall,
                                color = PresenceDesign.LabelText
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            }

            PresenceFieldGroup(title = "Timing & Threshold Rules") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PresenceTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = "Start Time (HH:MM) *"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PresenceTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = "End Time (HH:MM) *"
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PresenceTextField(
                        value = halfDayThresholdMinutesStr,
                        onValueChange = { halfDayThresholdMinutesStr = it.filter { char -> char.isDigit() } },
                        label = "Half-Day Threshold (minutes) *",
                        keyboardType = KeyboardType.Number
                    )
                    Text(
                        text = "Minimum worked duration in minutes required for full day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PresenceDesign.LabelText,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    val previewText = formatThresholdDisplay(start, thresholdMinutesInt)
                    if (previewText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = previewText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            PresenceButton(
                label = "Save Changes",
                onClick = {
                    val thresholdM = thresholdMinutesInt ?: 240
                    onConfirm(
                        name.trim(),
                        start.trim(),
                        end.trim(),
                        dayName,
                        thresholdM,
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
