package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
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
    val presentPay: String = "0",
    val halfDayPay: String = "0",
    val leavePay: String = "0",
    val absentPay: String = "0"
)

data class DayCardState(
    val dayInfo: DayInfo,
    val isDayEnabled: Boolean = false,
    val shiftStates: List<ShiftPaymentState> = emptyList()
)

/**
 * Assigns a shift schedule and payment rules directly to a staff member or manager.
 * Supports per-shift payment rates on 7-day cards and global unworked payment rules.
 * Shifts and days start disabled, configured via a compact sub-modal with clear field labels.
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

    // Initialize 7 day cards - all disabled initially, defaults to "0" if nothing from server
    var dayCardStates by remember(availableTemplates) {
        mutableStateOf(
            WeekDaysList.map { dayInfo ->
                val matchingShifts = availableTemplates.filter { t ->
                    val days = t.daysOfWeek
                    days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                }

                val shiftStates = matchingShifts.map { template ->
                    ShiftPaymentState(
                        templateId = template.id,
                        templateName = template.name,
                        startTime = template.startTime,
                        endTime = template.endTime,
                        isEnabled = false,
                        presentPay = "0",
                        halfDayPay = "0",
                        leavePay = "0",
                        absentPay = "0"
                    )
                }

                DayCardState(
                    dayInfo = dayInfo,
                    isDayEnabled = false,
                    shiftStates = shiftStates
                )
            }
        )
    }

    // Active shift being configured in sub-modal: Pair(dayIndex, shiftIndex)
    var activeConfigTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Global payment rules state (defaults to "0")
    var isEditingGlobal by remember { mutableStateOf(false) }
    var holidayPay by remember { mutableStateOf("0") }
    var offDayPay by remember { mutableStateOf("0") }

    // Sub-modal for configuring shift rates
    activeConfigTarget?.let { (dayIdx, shiftIdx) ->
        val dayCard = dayCardStates.getOrNull(dayIdx)
        val shiftState = dayCard?.shiftStates?.getOrNull(shiftIdx)
        if (dayCard != null && shiftState != null) {
            ConfigureShiftRatesSubModal(
                dayName = dayCard.dayInfo.fullName,
                shiftState = shiftState,
                onDismiss = { activeConfigTarget = null },
                onSave = { isEnabled, presentPay, halfDayPay, leavePay, absentPay ->
                    dayCardStates = dayCardStates.mapIndexed { dIdx, dCard ->
                        if (dIdx == dayIdx) {
                            val updatedShifts = dCard.shiftStates.mapIndexed { sIdx, sState ->
                                if (sIdx == shiftIdx) {
                                    sState.copy(
                                        isEnabled = isEnabled,
                                        presentPay = presentPay.ifBlank { "0" },
                                        halfDayPay = halfDayPay.ifBlank { "0" },
                                        leavePay = leavePay.ifBlank { "0" },
                                        absentPay = absentPay.ifBlank { "0" }
                                    )
                                } else {
                                    sState
                                }
                            }
                            val hasEnabledShift = updatedShifts.any { it.isEnabled }
                            dCard.copy(
                                isDayEnabled = if (isEnabled) true else hasEnabledShift,
                                shiftStates = updatedShifts
                            )
                        } else {
                            dCard
                        }
                    }
                    activeConfigTarget = null
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Assign Shift & Payment Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Select an employee, enable working days, and tap to configure shift payment rates.",
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
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
            Text("Weekly Schedule & Shift Rates", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                                val updatedShifts = currentDay.shiftStates.map { shift ->
                                    if (!isDayEnabled) shift.copy(isEnabled = false) else shift
                                }
                                this[dayIndex] = currentDay.copy(
                                    isDayEnabled = isDayEnabled,
                                    shiftStates = updatedShifts
                                )
                            }
                        },
                        onOpenConfig = { shiftIndex ->
                            activeConfigTarget = Pair(dayIndex, shiftIndex)
                        }
                    )
                }
            }

            // Global Unworked Payment Rules
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Global Unworked Payment Rules",
                                style = MaterialTheme.typography.titleSmall,
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresenceTextField(
                            value = offDayPay,
                            onValueChange = { offDayPay = it.filter { ch -> ch.isDigit() } },
                            label = "Weekly Off (₹)",
                            enabled = isEditingGlobal,
                            modifier = Modifier.weight(1f)
                        )
                        PresenceTextField(
                            value = holidayPay,
                            onValueChange = { holidayPay = it.filter { ch -> ch.isDigit() } },
                            label = "Public Holiday (₹)",
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
                            presentPay = shiftState.presentPay.ifBlank { "0" },
                            halfDayPay = shiftState.halfDayPay.ifBlank { "0" },
                            leavePay = shiftState.leavePay.ifBlank { "0" },
                            absentPay = shiftState.absentPay.ifBlank { "0" }
                        )
                        map.getOrPut(key) { mutableListOf() }.add(dayCard.dayInfo.dayOfWeek)
                    }
                }
                map.map { (key, _) ->
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
                            holidayPay.ifBlank { "0" },
                            offDayPay.ifBlank { "0" }
                        )
                    }
                },
                enabled = selectedStaffId != null && compiledAssignments.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit Shift & Payment Rules", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayCardWithShifts(
    dayCardState: DayCardState,
    onToggleDayEnabled: (Boolean) -> Unit,
    onOpenConfig: (shiftIndex: Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            if (dayCardState.isDayEnabled) 1.5.dp else 1.dp,
            if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayCardState.dayInfo.shortName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Column {
                        Text(
                            text = dayCardState.dayInfo.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val activeShiftsCount = dayCardState.shiftStates.count { it.isEnabled }
                        Text(
                            text = if (dayCardState.isDayEnabled)
                                if (activeShiftsCount > 0) "$activeShiftsCount shift(s) active" else "Tap shift to configure"
                            else "Weekly Off / Day Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dayCardState.isDayEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (dayCardState.shiftStates.isEmpty()) {
                    Text(
                        text = "No premises shifts configured for ${dayCardState.dayInfo.fullName}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    dayCardState.shiftStates.forEachIndexed { shiftIndex, shiftState ->
                        ShiftPaymentConfigSummaryRow(
                            shiftState = shiftState,
                            onClick = { onOpenConfig(shiftIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftPaymentConfigSummaryRow(
    shiftState: ShiftPaymentState,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (shiftState.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(
            if (shiftState.isEnabled) 1.5.dp else 1.dp,
            if (shiftState.isEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = shiftState.templateName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (shiftState.isEnabled) PresenceDesign.Ink else PresenceDesign.Ink.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "• ${Dates.formatTime(shiftState.startTime)} – ${Dates.formatTime(shiftState.endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shiftState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (shiftState.isEnabled) {
                    val pres = shiftState.presentPay.ifBlank { "0" }
                    val half = shiftState.halfDayPay.ifBlank { "0" }
                    val leave = shiftState.leavePay.ifBlank { "0" }
                    val abs = shiftState.absentPay.ifBlank { "0" }
                    Text(
                        text = "Present: ₹$pres • Half: ₹$half • Leave: ₹$leave • Absent: ₹$abs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Disabled • Tap to configure rates",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (shiftState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (shiftState.isEnabled) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (shiftState.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (shiftState.isEnabled) "Edit" else "Configure",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (shiftState.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigureShiftRatesSubModal(
    dayName: String,
    shiftState: ShiftPaymentState,
    onDismiss: () -> Unit,
    onSave: (
        isEnabled: Boolean,
        presentPay: String,
        halfDayPay: String,
        leavePay: String,
        absentPay: String
    ) -> Unit
) {
    var isEnabled by remember { mutableStateOf(shiftState.isEnabled) }
    var presentPay by remember { mutableStateOf(shiftState.presentPay.ifBlank { "0" }) }
    var halfDayPay by remember { mutableStateOf(shiftState.halfDayPay.ifBlank { "0" }) }
    var leavePay by remember { mutableStateOf(shiftState.leavePay.ifBlank { "0" }) }
    var absentPay by remember { mutableStateOf(shiftState.absentPay.ifBlank { "0" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Configure ${shiftState.templateName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$dayName • ${Dates.formatTime(shiftState.startTime)} – ${Dates.formatTime(shiftState.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Enable switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Shift", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Assign this shift to $dayName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                }

                if (isEnabled) {
                    Text("Attendance Outcome Rates (₹)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presentPay,
                            onValueChange = { presentPay = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Present (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = halfDayPay,
                            onValueChange = { halfDayPay = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Half-Day (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = leavePay,
                            onValueChange = { leavePay = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Leave (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = absentPay,
                            onValueChange = { absentPay = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Absent (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isEnabled, presentPay.ifBlank { "0" }, halfDayPay.ifBlank { "0" }, leavePay.ifBlank { "0" }, absentPay.ifBlank { "0" })
                },
                enabled = true
            ) {
                Text("Save Rates", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
