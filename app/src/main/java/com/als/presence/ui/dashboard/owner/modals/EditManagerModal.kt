package com.als.presence.ui.dashboard.owner.modals

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceDetailRow
import com.als.presence.ui.dashboard.common.PresenceFieldGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditManagerModal(
    manager: OrganizationMemberDto,
    premisesList: List<PremisesDto> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (userId: String, employeeId: String?, email: String, name: String, department: String, premisesId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(manager) { mutableStateOf(manager.fullName) }
    var department by remember(manager) { mutableStateOf("Operations") }
    var selectedPremisesId by remember(manager, premisesList) {
        mutableStateOf(manager.premisesId ?: premisesList.firstOrNull()?.id)
    }

    val isValid = name.trim().length >= 2

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
                text = "Edit Manager Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Update operational manager details, branch/department, and geofenced premises assignment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PresenceFieldGroup(title = "Account Overview") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresenceDetailRow(label = "Role", value = manager.role)
                    PresenceDetailRow(label = "Email Address", value = manager.email)
                    manager.staffCode?.let {
                        PresenceDetailRow(label = "Staff / Manager Code", value = it)
                    }
                    PresenceDetailRow(label = "Joined Date", value = Dates.formatDate(manager.joinedAt))
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Manager Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Department / Branch") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (premisesList.isNotEmpty()) {
                Text(
                    text = "Assigned Premises",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    premisesList.forEach { p ->
                        FilterChip(
                            selected = selectedPremisesId == p.id,
                            onClick = { selectedPremisesId = p.id },
                            label = { Text(Dates.cleanPremisesName(p.name), style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        manager.userId,
                        manager.employeeId,
                        manager.email,
                        name.trim(),
                        department.trim(),
                        selectedPremisesId
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Manager Details", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
