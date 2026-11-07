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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.owner.screens.DayInfo
import java.util.Locale

/**
 * Helper to produce a user-friendly preview string showing formatted duration and calculated cutoff time.
 */
internal fun formatThresholdDisplay(startTime: String, thresholdMinutesInt: Int?): String {
    if (thresholdMinutesInt == null || thresholdMinutesInt <= 0) return ""
    val parts = startTime.split(":")
    val startH = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val totalM = (startH * 60 + startM + thresholdMinutesInt) % (24 * 60)
    val cutoffH = totalM / 60
    val cutoffM = totalM % 60
    val cutoffStr = String.format(Locale.US, "%02d:%02d", cutoffH, cutoffM)
    val hrs = thresholdMinutesInt / 60
    val mins = thresholdMinutesInt % 60
    val durationStr = if (hrs > 0 && mins > 0) "${hrs}h ${mins}m" else if (hrs > 0) "${hrs}h" else "${mins}m"
    return "$durationStr worked duration • Cutoff time: $cutoffStr"
}

/**
 * Display-only non-editable info card for modal parameters (Selected Premises & Day).
 */
@Composable
internal fun ModalScopeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PresenceDesign.LabelText
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Fixed",
                        tint = PresenceDesign.LabelText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Fixed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = PresenceDesign.LabelText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShiftModal(
    selectedPremises: PremisesDto?,
    selectedDay: DayInfo,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        start: String,
        end: String,
        dayName: String,
        halfDayThresholdMinutes: Int,
        premisesId: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("18:00") }
    var halfDayThresholdMinutesStr by remember { mutableStateOf("240") }

    val dayName = selectedDay.fullName
    val premisesId = selectedPremises?.id.orEmpty()
    val premisesName = selectedPremises?.name?.let { Dates.cleanPremisesName(it) } ?: "All Premises"

    val thresholdMinutesInt = halfDayThresholdMinutesStr.toIntOrNull()
    val valid = name.trim().length >= 2 &&
            premisesId.isNotBlank() &&
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
                .heightIn(max = 700.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Shift to $dayName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Create a timing schedule for $dayName at $premisesName.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PresenceDesign.LabelText
                    )
                }
            }

            PresenceFieldGroup(title = "Fixed Scope (Non-Editable)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModalScopeInfoItem(
                        icon = Icons.Default.LocationOn,
                        label = "Premises",
                        value = premisesName
                    )
                    ModalScopeInfoItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Selected Day",
                        value = dayName
                    )
                }
            }

            PresenceFieldGroup(title = "Shift Identity") {
                PresenceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Shift Name *"
                )
            }

            PresenceFieldGroup(title = "Timing & Half-Day Rules") {
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
                        text = "Minimum worked duration in minutes required for a full day (e.g. 240 mins = 4 hours).",
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
                label = "Add Shift to $dayName",
                onClick = {
                    val thresholdM = thresholdMinutesInt ?: 240
                    onConfirm(
                        name.trim(),
                        start.trim(),
                        end.trim(),
                        dayName,
                        thresholdM,
                        premisesId
                    )
                },
                enabled = valid,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
