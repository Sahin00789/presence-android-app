package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UnworkedPaymentRuleDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceTone
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
    val absentPay: String = "0",
    val assignmentId: String? = null
)

data class DayCardState(
    val dayInfo: DayInfo,
    val isDayEnabled: Boolean = false,
    val shiftStates: List<ShiftPaymentState> = emptyList()
)

/**
 * Assigns a shift schedule and payment rules directly to a staff member or manager.
 * Sends API requests immediately when shift is enabled/disabled or rates are saved.
 * Features dedicated sub-modals for shift rate configuration and unworked payment rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignShiftWithRulesModal(
    staff: List<StaffDto>,
    templates: List<ShiftTemplateDto>,
    existingAssignments: List<ShiftAssignmentDto> = emptyList(),
    unworkedRules: List<UnworkedPaymentRuleDto> = emptyList(),
    initialStaffId: String? = null,
    initialPremisesId: String? = null,
    onDismiss: () -> Unit,
    onSaveAssignment: (CreateShiftAssignmentRequest) -> Unit,
    onDeleteAssignment: (assignmentId: String) -> Unit,
    onSaveUnworkedRules: ((employeeId: String?, premisesId: String?, holidayPay: Long, offDayPay: Long) -> Unit)? = null,
    onStaffSelected: ((employeeId: String, premisesId: String?) -> Unit)? = null,
    onAssignWithRules: ((employeeId: String, assignments: List<CreateShiftAssignmentRequest>, startDate: String, premisesId: String?, holidayPay: String, offDayPay: String) -> Unit)? = null
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

    val staffAssignments = remember(existingAssignments, selectedStaffId) {
        existingAssignments.filter { it.employeeId == selectedStaffId || it.staffId == selectedStaffId }
    }

    var dayCardStates by remember(availableTemplates, selectedStaffId, staffAssignments) {
        mutableStateOf(
            WeekDaysList.map { dayInfo ->
                val matchingShifts = availableTemplates.filter { t ->
                    val days = t.daysOfWeek
                    days.isNullOrEmpty() || days.contains(dayInfo.dayOfWeek)
                }

                val shiftStates = matchingShifts.map { template ->
                    val existingAss = staffAssignments.find { it.shiftId == template.id }
                    ShiftPaymentState(
                        templateId = template.id,
                        templateName = template.name,
                        startTime = template.startTime,
                        endTime = template.endTime,
                        isEnabled = existingAss != null && existingAss.isActive,
                        presentPay = existingAss?.presentPay?.toLong()?.toString() ?: "0",
                        halfDayPay = existingAss?.halfDayPay?.toLong()?.toString() ?: "0",
                        leavePay = existingAss?.leavePay?.toLong()?.toString() ?: "0",
                        absentPay = existingAss?.absentPay?.toLong()?.toString() ?: "0",
                        assignmentId = existingAss?.id
                    )
                }

                val hasActiveShift = shiftStates.any { it.isEnabled }
                DayCardState(
                    dayInfo = dayInfo,
                    isDayEnabled = hasActiveShift,
                    shiftStates = shiftStates
                )
            }
        )
    }

    // Active shift being configured in sub-modal: Pair(dayIndex, shiftIndex)
    var activeConfigTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(selectedStaffId, targetPremisesId) {
        if (!selectedStaffId.isNullOrBlank()) {
            onStaffSelected?.invoke(selectedStaffId!!, targetPremisesId)
        }
    }

    // Unworked payment rules state & sub-modal visibility
    val employeeUnworkedRules = remember(unworkedRules, selectedStaffId) {
        unworkedRules.filter { it.employeeId == null || it.employeeId == selectedStaffId }
    }
    var showUnworkedRulesSubModal by remember { mutableStateOf(false) }
    var holidayPay by remember(employeeUnworkedRules, selectedStaffId) {
        val rule = employeeUnworkedRules.find { it.employeeId == selectedStaffId && it.dayType == "HOLIDAY" }
            ?: employeeUnworkedRules.find { it.employeeId == null && it.dayType == "HOLIDAY" }
        mutableStateOf(rule?.paymentInr?.toString() ?: "0")
    }
    var offDayPay by remember(employeeUnworkedRules, selectedStaffId) {
        val rule = employeeUnworkedRules.find { it.employeeId == selectedStaffId && it.dayType == "WEEKLY_OFF" }
            ?: employeeUnworkedRules.find { it.employeeId == null && it.dayType == "WEEKLY_OFF" }
        mutableStateOf(rule?.paymentInr?.toString() ?: "0")
    }

    fun computeDisabledDays(states: List<DayCardState>): List<Int> {
        return states
            .filter { !it.isDayEnabled || it.shiftStates.none { s -> s.isEnabled } }
            .map { it.dayInfo.dayOfWeek }
    }

    // Sub-modal for configuring individual shift outcome rates
    activeConfigTarget?.let { (dayIdx, shiftIdx) ->
        val dayCard = dayCardStates.getOrNull(dayIdx)
        val shiftState = dayCard?.shiftStates?.getOrNull(shiftIdx)
        if (dayCard != null && shiftState != null) {
            ConfigureShiftRatesSubModal(
                dayName = dayCard.dayInfo.fullName,
                shiftState = shiftState,
                onDismiss = { activeConfigTarget = null },
                onSave = { isEnabled, presentPay, halfDayPay, leavePay, absentPay ->
                    val updatedStates = dayCardStates.mapIndexed { dIdx, dCard ->
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
                    dayCardStates = updatedStates
                    activeConfigTarget = null

                    // Send API request immediately on save
                    val staffId = selectedStaffId
                    if (staffId != null) {
                        val disabledDays = computeDisabledDays(updatedStates)
                        if (isEnabled) {
                            val request = CreateShiftAssignmentRequest(
                                employeeId = staffId,
                                premisesId = targetPremisesId ?: "",
                                shiftId = shiftState.templateId,
                                presentPay = presentPay.toDoubleOrNull() ?: 0.0,
                                halfDayPay = halfDayPay.toDoubleOrNull() ?: 0.0,
                                leavePay = leavePay.toDoubleOrNull() ?: 0.0,
                                absentPay = absentPay.toDoubleOrNull() ?: 0.0,
                                isActive = true,
                                weeklyOffs = disabledDays
                            )
                            onSaveAssignment(request)
                        } else if (shiftState.assignmentId != null) {
                            onDeleteAssignment(shiftState.assignmentId)
                        }
                    }
                }
            )
        }
    }

    // Sub-modal for configuring unworked payment rules (Weekly Off & Public Holiday)
    if (showUnworkedRulesSubModal) {
        ConfigureUnworkedRulesSubModal(
            currentOffDayPay = offDayPay,
            currentHolidayPay = holidayPay,
            onDismiss = { showUnworkedRulesSubModal = false },
            onSave = { newOffDayPay, newHolidayPay ->
                offDayPay = newOffDayPay
                holidayPay = newHolidayPay
                showUnworkedRulesSubModal = false

                val hPay = newHolidayPay.toLongOrNull() ?: 0L
                val oPay = newOffDayPay.toLongOrNull() ?: 0L

                onSaveUnworkedRules?.invoke(
                    selectedStaffId,
                    targetPremisesId,
                    hPay,
                    oPay
                )

                selectedStaffId?.let { empId ->
                    val activeRequests = dayCardStates.flatMap { d ->
                        d.shiftStates.filter { s -> s.isEnabled }.map { s ->
                            CreateShiftAssignmentRequest(
                                employeeId = empId,
                                premisesId = targetPremisesId ?: "",
                                shiftId = s.templateId,
                                presentPay = s.presentPay.toDoubleOrNull() ?: 0.0,
                                halfDayPay = s.halfDayPay.toDoubleOrNull() ?: 0.0,
                                leavePay = s.leavePay.toDoubleOrNull() ?: 0.0,
                                absentPay = s.absentPay.toDoubleOrNull() ?: 0.0,
                                isActive = true,
                                weeklyOffs = computeDisabledDays(dayCardStates)
                            )
                        }
                    }
                    onAssignWithRules?.invoke(
                        empId,
                        activeRequests,
                        Dates.todayIso(),
                        targetPremisesId,
                        newHolidayPay,
                        newOffDayPay
                    )
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 740.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Assign Shift & Rules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Employee Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT EMPLOYEE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                if (staff.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No employees available for shift assignment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        staff.forEach { member ->
                            EmployeeSelectionCard(
                                selected = member.id == selectedStaffId,
                                member = member,
                                onClick = { selectedStaffId = member.id }
                            )
                        }
                    }
                }
            }

            // 7 Day Roster Cards Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEEKLY SCHEDULE & RATES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    val activeDaysCount = dayCardStates.count { it.isDayEnabled }
                    Surface(
                        shape = CircleShape,
                        color = if (activeDaysCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "$activeDaysCount / 7 Active Days",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeDaysCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                if (availableTemplates.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedStaff?.premisesName != null)
                                "No shift templates defined for premises “${Dates.cleanPremisesName(selectedStaff.premisesName)}”."
                            else
                                "No shift templates defined for this premises workspace.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        dayCardStates.forEachIndexed { dayIndex, dayCardState ->
                            DayCardWithShifts(
                                dayCardState = dayCardState,
                                onOpenConfig = { shiftIndex ->
                                    activeConfigTarget = Pair(dayIndex, shiftIndex)
                                }
                            )
                        }
                    }
                }
            }

            // Global Unworked Payment Rules Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showUnworkedRulesSubModal = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Unwork Payments",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to configure off-day & holiday compensation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showUnworkedRulesSubModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UnworkedMetricBadge(
                            label = "Weekly Off Rate",
                            value = "₹$offDayPay",
                            modifier = Modifier.weight(1f)
                        )
                        UnworkedMetricBadge(
                            label = "Public Holiday Rate",
                            value = "₹$holidayPay",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun UnworkedMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmployeeSelectionCard(
    selected: Boolean,
    member: StaffDto,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val formattedRole = when {
                            member.role.equals("OWNER", ignoreCase = true) -> "Owner"
                            member.role.equals("MANAGER", ignoreCase = true) -> "Manager"
                            else -> "Employee"
                        }
                        Text(
                            text = "($formattedRole)",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    val cleanPremises = member.premisesName?.let { Dates.cleanPremisesName(it) }
                    val subtitleParts = listOfNotNull(
                        cleanPremises?.takeIf { it.isNotBlank() },
                        member.staffCode.takeIf { it.isNotBlank() }
                    )
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun DayCardWithShifts(
    dayCardState: DayCardState,
    onOpenConfig: (shiftIndex: Int) -> Unit
) {
    val hasActiveShift = dayCardState.shiftStates.any { it.isEnabled }
    val activeShiftsCount = dayCardState.shiftStates.count { it.isEnabled }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (hasActiveShift) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            if (hasActiveShift) 1.5.dp else 1.dp,
            if (hasActiveShift) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day Header Row (Clean summary without redundant header switch)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasActiveShift) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayCardState.dayInfo.shortName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActiveShift) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Column {
                        Text(
                            text = dayCardState.dayInfo.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasActiveShift)
                                if (activeShiftsCount > 1) "$activeShiftsCount active shifts" else "1 active shift"
                            else "Weekly Off / No active shift",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasActiveShift) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (hasActiveShift) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (hasActiveShift) "Active" else "Weekly Off",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasActiveShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (dayCardState.shiftStates.isEmpty()) {
                Text(
                    text = "No premises shifts configured for ${dayCardState.dayInfo.fullName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
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

@Composable
private fun ShiftPaymentConfigSummaryRow(
    shiftState: ShiftPaymentState,
    onClick: () -> Unit
) {
    val pres = shiftState.presentPay.ifBlank { "0" }
    val half = shiftState.halfDayPay.ifBlank { "0" }
    val leave = shiftState.leavePay.ifBlank { "0" }
    val abs = shiftState.absentPay.ifBlank { "0" }
    val windowText = Dates.formatWindow(shiftState.startTime, shiftState.endTime)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (shiftState.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            if (shiftState.isEnabled) 1.5.dp else 1.dp,
            if (shiftState.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Shift Name & Window on Left, Action Button on Right
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
                        color = if (shiftState.isEnabled) PresenceDesign.Ink else MaterialTheme.colorScheme.onSurfaceVariant,
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

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (shiftState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (shiftState.isEnabled) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (shiftState.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (shiftState.isEnabled) "Edit" else "Rates",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (shiftState.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Payment Rates Grid (2x2 Grid matching MyShiftsScreen day card)
            if (shiftState.isEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
                        PaymentRateChip(
                            label = "Present Rate",
                            value = "₹$pres",
                            tone = PresenceTone.Success,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentRateChip(
                            label = "Half-Day Rate",
                            value = "₹$half",
                            tone = PresenceTone.Warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentRateChip(
                            label = "Leave Pay",
                            value = "₹$leave",
                            tone = PresenceTone.Info,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentRateChip(
                            label = "Absent Pay",
                            value = "₹$abs",
                            tone = PresenceTone.Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = "Disabled • Tap to configure rates",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PaymentRateChip(
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

/**
 * Sub-modal for configuring attendance outcome rates for a specific shift template.
 */
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
    var isEnabled by remember { mutableStateOf(if (shiftState.assignmentId != null) shiftState.isEnabled else true) }
    var presentPay by remember { mutableStateOf(shiftState.presentPay.ifBlank { "0" }) }
    var halfDayPay by remember { mutableStateOf(shiftState.halfDayPay.ifBlank { "0" }) }
    var leavePay by remember { mutableStateOf(shiftState.leavePay.ifBlank { "0" }) }
    var absentPay by remember { mutableStateOf(shiftState.absentPay.ifBlank { "0" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Configure ${shiftState.templateName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$dayName • ${Dates.formatTime(shiftState.startTime)} – ${Dates.formatTime(shiftState.endTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enable switch container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Shift Assignment",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Assign this shift to $dayName roster",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                }

                if (isEnabled) {
                    Text(
                        text = "Attendance Outcome Rates (₹)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RateInputField(
                            value = presentPay,
                            onValueChange = { presentPay = it.filter { ch -> ch.isDigit() } },
                            label = "Present (₹)",
                            modifier = Modifier.weight(1f)
                        )
                        RateInputField(
                            value = halfDayPay,
                            onValueChange = { halfDayPay = it.filter { ch -> ch.isDigit() } },
                            label = "Half-Day (₹)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RateInputField(
                            value = leavePay,
                            onValueChange = { leavePay = it.filter { ch -> ch.isDigit() } },
                            label = "Leave (₹)",
                            modifier = Modifier.weight(1f)
                        )
                        RateInputField(
                            value = absentPay,
                            onValueChange = { absentPay = it.filter { ch -> ch.isDigit() } },
                            label = "Absent (₹)",
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
                shape = RoundedCornerShape(10.dp)
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

/**
 * Sub-modal for configuring Global Unworked Payment Rules (Weekly Off & Public Holiday).
 */
@Composable
private fun ConfigureUnworkedRulesSubModal(
    currentOffDayPay: String,
    currentHolidayPay: String,
    onDismiss: () -> Unit,
    onSave: (offDayPay: String, holidayPay: String) -> Unit
) {
    var offDayPay by remember(currentOffDayPay) { mutableStateOf(currentOffDayPay.ifBlank { "0" }) }
    var holidayPay by remember(currentHolidayPay) { mutableStateOf(currentHolidayPay.ifBlank { "0" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Unwork Payments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Set rates for off-days and public holidays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Unworked payment rules define compensation for days off or public holidays during payroll processing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RateInputField(
                    value = offDayPay,
                    onValueChange = { offDayPay = it.filter { ch -> ch.isDigit() } },
                    label = "Weekly Off Pay (₹)",
                    modifier = Modifier.fillMaxWidth()
                )

                RateInputField(
                    value = holidayPay,
                    onValueChange = { holidayPay = it.filter { ch -> ch.isDigit() } },
                    label = "Public Holiday Pay (₹)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(offDayPay.ifBlank { "0" }, holidayPay.ifBlank { "0" })
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Rules", fontWeight = FontWeight.Bold)
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
private fun RateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    )
}
