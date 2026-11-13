package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.als.presence.ui.dashboard.common.Dates

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color

/**
 * Dedicated Owner Modal for Manual Attendance Correction (Shift & Overtime).
 * Supports setting attendance action (ABSENT, HALF_DAY, LEAVE, PRESENT for shifts; DELETE_OVERTIME for overtime),
 * manual mark reason, and optional departure/clock-out time.
 */
@Composable
fun ManualMarkAttendanceModal(
    isOvertime: Boolean = false,
    employeeName: String,
    workDate: String,
    initialAction: String = if (isOvertime) "DELETE_OVERTIME" else "ABSENT",
    onDismiss: () -> Unit,
    onConfirmShiftCorrection: (action: String, reason: String, departureTime: String?) -> Unit,
    onConfirmOvertimeCorrection: (action: String, reason: String, departureTime: String?) -> Unit
) {
    var selectedAction by remember { mutableStateOf(initialAction) }
    var reasonText by remember { mutableStateOf("") }
    var departureTimeText by remember { mutableStateOf("") }

    val shiftActions = listOf("ABSENT", "HALF_DAY", "LEAVE", "PRESENT")
    val overtimeActions = listOf("DELETE_OVERTIME")

    val currentActions = if (isOvertime) overtimeActions else shiftActions

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
                        .background(if (isOvertime) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isOvertime) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isOvertime) "Cancel Overtime" else "Correct Shift Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$employeeName • ${Dates.formatDate(workDate)}",
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
                            tint = if (isOvertime) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isOvertime) {
                                "Overtime does not use Absent, Half-Day, or Present statuses. Cancelling overtime voids the scheduled overtime and any associated payment."
                            } else {
                                "Manual correction allows setting attendance status, early departure time, and recording correction reason."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Filter Chips
                Text(
                    text = if (isOvertime) "OVERTIME ACTION" else "SELECT ATTENDANCE ACTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentActions.forEach { act ->
                        FilterChip(
                            selected = selectedAction == act,
                            onClick = { selectedAction = act },
                            label = {
                                Text(
                                    text = when (act) {
                                        "ABSENT" -> "Absent"
                                        "HALF_DAY" -> "Half-Day"
                                        "LEAVE" -> "Leave"
                                        "PRESENT" -> "Present"
                                        "DELETE_OVERTIME" -> "Cancel Overtime"
                                        else -> act
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                if (!isOvertime) {
                    // Departure / Clock-Out Time Input (HH:mm)
                    OutlinedTextField(
                        value = departureTimeText,
                        onValueChange = { departureTimeText = it },
                        label = { Text("Departure Time (HH:mm) • Optional") },
                        placeholder = { Text("e.g. 15:30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Correction Reason Input
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text(if (isOvertime) "Cancellation Reason" else "Correction Reason") },
                    placeholder = { Text(if (isOvertime) "e.g. Overtime session called off by owner" else "e.g. Early departure due to medical emergency") },
                    singleLine = false,
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dep = departureTimeText.trim().ifBlank { null }
                    val r = reasonText.trim().ifBlank { if (isOvertime) "Overtime cancelled by owner" else "Owner manual correction" }
                    if (isOvertime) {
                        onConfirmOvertimeCorrection(selectedAction, r, dep)
                    } else {
                        onConfirmShiftCorrection(selectedAction, r, dep)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = if (isOvertime) ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isOvertime) "Cancel Overtime" else "Save Correction", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
