package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookOvertimeModal(
    staff: List<StaffDto>,
    preselectedEmployeeId: String? = null,
    shiftAssignments: List<ShiftAssignmentDto> = emptyList(),
    shifts: List<ShiftDto> = emptyList(),
    onDismiss: () -> Unit,
    onBook: (
        employeeId: String,
        workDate: String,
        startTime: String,
        endTime: String,
        paymentAmount: String,
        reason: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedEmployeeId by remember(preselectedEmployeeId, staff) {
        mutableStateOf(preselectedEmployeeId ?: staff.firstOrNull()?.id)
    }
    var workDate by remember { mutableStateOf(Dates.todayIso()) }
    var startTime by remember { mutableStateOf("18:00") }
    var endTime by remember { mutableStateOf("21:00") }
    var paymentAmount by remember { mutableStateOf("500") }
    var reason by remember { mutableStateOf("Overtime") }

    val rateInr = Money.parseToInr(paymentAmount) ?: 0L

    val overlapWarning = remember(selectedEmployeeId, workDate, startTime, endTime, shiftAssignments, shifts) {
        val empId = selectedEmployeeId ?: return@remember null
        if (!workDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
            !startTime.matches(Regex("\\d{2}:\\d{2}")) ||
            !endTime.matches(Regex("\\d{2}:\\d{2}"))
        ) return@remember null

        val userAssignments = shiftAssignments.filter { it.employeeId == empId || it.staffId == empId }
        val dayName = Dates.dayName(workDate)

        for (assignment in userAssignments) {
            val template = shifts.find { it.id == assignment.shiftId } ?: continue
            val shiftDay = template.dayName
            val matchesDay = shiftDay.isBlank() ||
                    shiftDay.equals("ALL", ignoreCase = true) ||
                    shiftDay.contains("Everyday", ignoreCase = true) ||
                    (dayName.isNotBlank() && shiftDay.contains(dayName, ignoreCase = true))

            if (matchesDay) {
                fun toMins(raw: String): Int {
                    val parts = raw.take(5).split(":")
                    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                }

                val s1 = toMins(startTime)
                var e1 = toMins(endTime)
                if (e1 <= s1) e1 += 24 * 60

                val s2 = toMins(template.startTime)
                var e2 = toMins(template.endTime)
                if (e2 <= s2) e2 += 24 * 60

                if (maxOf(s1, s2) < minOf(e1, e2)) {
                    return@remember "Overtime window ($startTime – $endTime) overlaps with assigned shift \"${template.shiftName}\" (${Dates.formatTime(template.startTime)} – ${Dates.formatTime(template.endTime)})."
                }
            }
        }
        null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Book Overtime",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = workDate,
                onValueChange = { workDate = it },
                label = { Text("Work date (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (overlapWarning != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = overlapWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it.filter { ch -> ch.isDigit() } },
                label = { Text("Agreed Rate (₹)") },
                placeholder = { Text("e.g. 500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Select Employee", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (staff.isEmpty()) {
                Text(
                    text = "No employees available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    staff.forEach { member ->
                        val isSelected = member.id == selectedEmployeeId
                        Surface(
                            onClick = { selectedEmployeeId = member.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedEmployeeId = member.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!member.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = member.avatarUrl,
                                            contentDescription = member.fullName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        InitialAvatar(name = member.fullName)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.fullName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = member.staffCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!member.premisesName.isNullOrBlank()) {
                                            Text(
                                                text = "• ${member.premisesName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val staffId = selectedEmployeeId
                    if (staffId != null) {
                        onBook(staffId, workDate, startTime, endTime, paymentAmount, reason)
                    }
                },
                enabled = selectedEmployeeId != null &&
                    overlapWarning == null &&
                    workDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                    startTime.matches(Regex("\\d{2}:\\d{2}")) &&
                    endTime.matches(Regex("\\d{2}:\\d{2}")) &&
                    rateInr > 0L &&
                    reason.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book Overtime", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
