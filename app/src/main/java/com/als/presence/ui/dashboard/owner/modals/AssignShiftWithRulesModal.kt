package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.owner.screens.DayInfo
import com.als.presence.ui.dashboard.owner.screens.WeekDaysList

data class ShiftPaymentState(
    val templateId: String,
    val templateName: String,
    val startTime: String,
    val endTime: String,
    val isEnabled: Boolean = false,
    val isEditing: Boolean = false,
    val presentPay: String = "600",
    val halfDayPay: String = "300",
    val leavePay: String = "600",
    val absentPay: String = "0"
)

data class DayCardState(
    val dayInfo: DayInfo,
    val isDayEnabled: Boolean = true,
    val shiftStates: List<ShiftPaymentState> = emptyList()
)

/**
 * Assigns a shift schedule and payment rules directly to a staff member or manager.
 * Supports per-shift various payment rates on 7-day cards and global unworked payment rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignShiftWithRulesModal(
    staff: List<StaffDto>,
    templates: List<ShiftTemplateDto>,
    initialStaffId: String? = null,
    initialPremisesId: String? = null,
    onDismiss: () -> Unit,
    onAssignWithRules: (
        employeeId: String,
        assignments: List<CreateShiftAssignmentRequest>,
        startDate: String,
        premisesId: String?,
        holidayPay: String,
        offDayPay: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedStaffId by remember(staff, initialStaffId) {
        mutableStateOf(initialStaffId ?: staff.firstOrNull()?.id)
    }

    val selectedStaff = staff.find { it.id == selectedStaffId }
    val targetPremisesId = selectedStaff?.premisesId ?: initialPremisesId

    val availableTemplates = remember(templates, targetPremisesId) {
        if (targetPremisesId.isNullOrBlank()) templates
        else templates.filter { t -> t.premisesId == null || t.premisesId == targetPremisesId }
    }

    val startDate = remember { Dates.todayIso() }

    // Initialize 7 day cards, each having its list of available premises shifts
    var dayCardStates by remember(availableTemplates) {
        mutableStateOf(
            WeekDaysList.map { dayInfo ->
                val matchingShifts = availableTemplates.filter { t ->
                    val days = t.daysOfWeek
                    days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                }

                val shiftStates = matchingShifts.mapIndexed { shiftIndex, template ->
                    ShiftPaymentState(
                        templateId = template.id,
                        templateName = template.name,
                        startTime = template.startTime,
                        endTime = template.endTime,
                        isEnabled = shiftIndex == 0,
                        presentPay = "600",
                        halfDayPay = "300",
                        leavePay = "600",
                        absentPay = "0"
                    )
                }

                DayCardState(
                    dayInfo = dayInfo,
                    isDayEnabled = shiftStates.any { it.isEnabled },
                    shiftStates = shiftStates
                )
            }
        )
    }

    // Global payment rules state
    var isEditingGlobal by remember { mutableStateOf(false) }
    var holidayPay by remember { mutableStateOf("600") }
    var offDayPay by remember { mutableStateOf("600") }

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
                text = "Assign Shift & Payment Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Configure weekly shift schedules and per-shift attendance payment rates for your workforce.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Employee Selection
            Text("Select Employee / Manager", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (staff.isEmpty()) {
                Text(
                    text = "No employees available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                staff.forEach { member ->
                    val cleanPremises = member.premisesName?.let { Dates.cleanPremisesName(it) }
                    OptionRow(
                        selected = member.id == selectedStaffId,
                        title = member.fullName,
                        subtitle = "${member.staffCode}${if (!cleanPremises.isNullOrBlank()) " • $cleanPremises" else ""}",
                        onClick = { selectedStaffId = member.id }
                    )
                }
            }

            // Target Premises Info Badge
            if (selectedStaff?.premisesName != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigned Premises: ${Dates.cleanPremisesName(selectedStaff.premisesName)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 7 Day Cards Section
            Text("Weekly Day Cards (Per-Shift Payment Rates)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (availableTemplates.isEmpty()) {
                Text(
                    text = if (selectedStaff?.premisesName != null)
                        "No shift templates defined for premises “${Dates.cleanPremisesName(selectedStaff.premisesName)}”."
                    else
                        "No shift templates defined for this premises.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                dayCardStates.forEachIndexed { dayIndex, dayCardState ->
                    DayCardWithShifts(
                        dayCardState = dayCardState,
                        onToggleDayEnabled = { isDayEnabled ->
                            dayCardStates = dayCardStates.toMutableList().apply {
                                val currentDay = this[dayIndex]
                                val updatedShifts = currentDay.shiftStates.mapIndexed { idx, shift ->
                                    shift.copy(
                                        isEnabled = if (isDayEnabled) {
                                            if (currentDay.shiftStates.none { it.isEnabled }) idx == 0 else shift.isEnabled
                                        } else {
                                            false
                                        }
                                    )
                                }
                                val hasEnabledShift = updatedShifts.any { it.isEnabled }
                                this[dayIndex] = currentDay.copy(
                                    isDayEnabled = isDayEnabled && hasEnabledShift,
                                    shiftStates = updatedShifts
                                )
                            }
                        },
                        onToggleShiftEnabled = { shiftIndex, isEnabled ->
                            dayCardStates = dayCardStates.toMutableList().apply {
                                val currentDay = this[dayIndex]
                                val updatedShifts = currentDay.shiftStates.toMutableList()
                                updatedShifts[shiftIndex] = updatedShifts[shiftIndex].copy(isEnabled = isEnabled)
                                val hasAnyEnabled = updatedShifts.any { it.isEnabled }
                                this[dayIndex] = currentDay.copy(
                                    isDayEnabled = hasAnyEnabled,
                                    shiftStates = updatedShifts
                                )
                            }
                        },
                        onToggleShiftEditing = { shiftIndex ->
                            dayCardStates = dayCardStates.toMutableList().apply {
                                val currentShifts = this[dayIndex].shiftStates.toMutableList()
                                currentShifts[shiftIndex] = currentShifts[shiftIndex].copy(isEditing = !currentShifts[shiftIndex].isEditing)
                                this[dayIndex] = this[dayIndex].copy(shiftStates = currentShifts)
                            }
                        },
                        onUpdateShiftRates = { shiftIndex, present, halfDay, leave, absent ->
                            dayCardStates = dayCardStates.toMutableList().apply {
                                val currentShifts = this[dayIndex].shiftStates.toMutableList()
                                currentShifts[shiftIndex] = currentShifts[shiftIndex].copy(
                                    presentPay = present,
                                    halfDayPay = halfDay,
                                    leavePay = leave,
                                    absentPay = absent
                                )
                                this[dayIndex] = this[dayIndex].copy(shiftStates = currentShifts)
                            }
                        }
                    )
                }
            }

            // Global Unworked Payment Rules
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Global Unworked Payment Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Payment rates for weekly off-days and public holidays",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PresenceButton(
                            label = if (isEditingGlobal) "Save" else "Edit",
                            onClick = { isEditingGlobal = !isEditingGlobal },
                            variant = if (isEditingGlobal) PresenceVariant.Primary else PresenceVariant.Secondary,
                            size = PresenceSize.Sm,
                            icon = if (isEditingGlobal) Icons.Default.Check else Icons.Default.Edit
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PresenceTextField(
                            value = offDayPay,
                            onValueChange = { offDayPay = it.filter { ch -> ch.isDigit() } },
                            label = "Weekly Off Rate (₹)",
                            enabled = isEditingGlobal,
                            modifier = Modifier.weight(1f)
                        )
                        PresenceTextField(
                            value = holidayPay,
                            onValueChange = { holidayPay = it.filter { ch -> ch.isDigit() } },
                            label = "Public Holiday Rate (₹)",
                            enabled = isEditingGlobal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Submit Button
            data class AssignmentKey(
                val templateId: String,
                val presentPay: String,
                val halfDayPay: String,
                val leavePay: String,
                val absentPay: String
            )

            val disabledDayNumbers = remember(dayCardStates) {
                dayCardStates
                    .filter { !it.isDayEnabled || it.shiftStates.none { s -> s.isEnabled } }
                    .map { it.dayInfo.dayOfWeek }
            }

            val compiledAssignments = remember(dayCardStates, disabledDayNumbers) {
                val map = mutableMapOf<AssignmentKey, MutableList<Int>>()
                dayCardStates.filter { it.isDayEnabled }.forEach { dayCard ->
                    dayCard.shiftStates.filter { it.isEnabled }.forEach { shiftState ->
                        val key = AssignmentKey(
                            templateId = shiftState.templateId,
                            presentPay = shiftState.presentPay,
                            halfDayPay = shiftState.halfDayPay,
                            leavePay = shiftState.leavePay,
                            absentPay = shiftState.absentPay
                        )
                        map.getOrPut(key) { mutableListOf() }.add(dayCard.dayInfo.dayOfWeek)
                    }
                }
                map.map { (key, days) ->
                    CreateShiftAssignmentRequest(
                        shiftId = key.templateId,
                        presentPay = key.presentPay.toDoubleOrNull() ?: 0.0,
                        halfDayPay = key.halfDayPay.toDoubleOrNull() ?: 0.0,
                        leavePay = key.leavePay.toDoubleOrNull() ?: 0.0,
                        absentPay = key.absentPay.toDoubleOrNull() ?: 0.0,
                        weeklyOffs = disabledDayNumbers
                    )
                }
            }

            Button(
                onClick = {
                    val staffId = selectedStaffId
                    if (staffId != null && compiledAssignments.isNotEmpty()) {
                        onAssignWithRules(
                            staffId,
                            compiledAssignments,
                            startDate,
                            targetPremisesId,
                            holidayPay,
                            offDayPay
                        )
                    }
                },
                enabled = selectedStaffId != null && compiledAssignments.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Shift & Payment Rules", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayCardWithShifts(
    dayCardState: DayCardState,
    onToggleDayEnabled: (Boolean) -> Unit,
    onToggleShiftEnabled: (shiftIndex: Int, isEnabled: Boolean) -> Unit,
    onToggleShiftEditing: (shiftIndex: Int) -> Unit,
    onUpdateShiftRates: (shiftIndex: Int, present: String, halfDay: String, leave: String, absent: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day Header Row with Switch
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
                            text = dayCardState.dayInfo.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = dayCardState.dayInfo.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val activeShiftsCount = dayCardState.shiftStates.count { it.isEnabled }
                        Text(
                            text = if (dayCardState.isDayEnabled)
                                if (activeShiftsCount > 0) "$activeShiftsCount shift(s) configured" else "No shifts active"
                            else "Weekly Off / Day Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = dayCardState.isDayEnabled,
                    onCheckedChange = onToggleDayEnabled
                )
            }

            // Expanded Shifts List when Day is Enabled
            if (dayCardState.isDayEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (dayCardState.shiftStates.isEmpty()) {
                    Text(
                        text = "No premises shifts configured for ${dayCardState.dayInfo.fullName}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    dayCardState.shiftStates.forEachIndexed { shiftIndex, shiftState ->
                        ShiftPaymentConfigRow(
                            shiftState = shiftState,
                            onToggleEnabled = { isEnabled ->
                                onToggleShiftEnabled(shiftIndex, isEnabled)
                            },
                            onToggleEditing = {
                                onToggleShiftEditing(shiftIndex)
                            },
                            onUpdateRates = { present, halfDay, leave, absent ->
                                onUpdateShiftRates(shiftIndex, present, halfDay, leave, absent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftPaymentConfigRow(
    shiftState: ShiftPaymentState,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleEditing: () -> Unit,
    onUpdateRates: (present: String, halfDay: String, leave: String, absent: String) -> Unit
) {
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
            // Shift Header Row with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shiftState.templateName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "${Dates.formatTime(shiftState.startTime)} – ${Dates.formatTime(shiftState.endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = shiftState.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            // Rates Config when Shift is Enabled
            if (shiftState.isEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Rates for ${shiftState.templateName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.LabelText
                    )

                    PresenceButton(
                        label = if (shiftState.isEditing) "Save" else "Edit",
                        onClick = onToggleEditing,
                        variant = if (shiftState.isEditing) PresenceVariant.Primary else PresenceVariant.Secondary,
                        size = PresenceSize.Sm,
                        icon = if (shiftState.isEditing) Icons.Default.Check else Icons.Default.Edit
                    )
                }

                // 4 Payment Fields
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresenceTextField(
                            value = shiftState.presentPay,
                            onValueChange = { present ->
                                onUpdateRates(present, shiftState.halfDayPay, shiftState.leavePay, shiftState.absentPay)
                            },
                            label = "Present Rate (₹)",
                            enabled = shiftState.isEditing,
                            modifier = Modifier.weight(1f)
                        )
                        PresenceTextField(
                            value = shiftState.halfDayPay,
                            onValueChange = { halfDay ->
                                onUpdateRates(shiftState.presentPay, halfDay, shiftState.leavePay, shiftState.absentPay)
                            },
                            label = "Half-Day Rate (₹)",
                            enabled = shiftState.isEditing,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresenceTextField(
                            value = shiftState.leavePay,
                            onValueChange = { leave ->
                                onUpdateRates(shiftState.presentPay, shiftState.halfDayPay, leave, shiftState.absentPay)
                            },
                            label = "Leave Rate (₹)",
                            enabled = shiftState.isEditing,
                            modifier = Modifier.weight(1f)
                        )
                        PresenceTextField(
                            value = shiftState.absentPay,
                            onValueChange = { absent ->
                                onUpdateRates(shiftState.presentPay, shiftState.halfDayPay, shiftState.leavePay, absent)
                            },
                            label = "Absent Pay (₹)",
                            enabled = shiftState.isEditing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
