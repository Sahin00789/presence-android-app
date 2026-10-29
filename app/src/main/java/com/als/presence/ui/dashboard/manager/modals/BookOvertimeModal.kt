package com.als.presence.ui.dashboard.manager.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookOvertimeModal(
    staff: List<StaffDto>,
    onDismiss: () -> Unit,
    onBook: (employeeId: String, workDate: String, startTime: String, endTime: String, reason: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedEmployeeId by remember { mutableStateOf(staff.firstOrNull()?.id) }
    var workDate by remember { mutableStateOf(Dates.todayIso()) }
    var startTime by remember { mutableStateOf("18:00") }
    var endTime by remember { mutableStateOf("21:00") }
    var reason by remember { mutableStateOf("Overtime") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Book overtime",
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

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Staff member", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (staff.isEmpty()) {
                Text(
                    text = "No staff available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                staff.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (member.id == selectedEmployeeId) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = member.id == selectedEmployeeId, onClick = { selectedEmployeeId = member.id })
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(member.fullName, fontWeight = FontWeight.SemiBold)
                            Text(member.staffCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val staffId = selectedEmployeeId
                    if (staffId != null) {
                        onBook(staffId, workDate, startTime, endTime, reason)
                    }
                },
                enabled = selectedEmployeeId != null &&
                    workDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                    startTime.matches(Regex("\\d{2}:\\d{2}")) &&
                    endTime.matches(Regex("\\d{2}:\\d{2}")),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book overtime", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
