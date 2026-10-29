package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.manager.screens.WeekDaysList
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceAvatar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceDetailRow
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.manager.modals.AssignShiftWithRulesModal
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

private data class UnifiedEmployeeItem(
    val id: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String, // "MANAGER" or "STAFF"
    val staffCode: String?,
    val department: String?,
    val premisesId: String?,
    val premisesName: String?,
    val joinedAt: String?,
    val isActive: Boolean,
    val employeeId: String?
)

/**
 * Unified "Employees Manager" directory screen.
 * Handles staff/manager profiles, assigned shifts with attendance payment rules, and facial biometrics.
 */
@Composable
fun EmployeesManagementScreen(
    onBack: () -> Unit,
    canManage: Boolean,
    user: User? = null,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val staffState by viewModel.staff.collectAsStateWithLifecycle()
    val membersState by viewModel.members.collectAsStateWithLifecycle()
    val faceStatuses by viewModel.faceStatuses.collectAsStateWithLifecycle()
    val faceSubmittingFor by viewModel.faceSubmittingFor.collectAsStateWithLifecycle()
    val premisesState by viewModel.premises.collectAsStateWithLifecycle()
    val shiftAssignmentsState by viewModel.shiftAssignments.collectAsStateWithLifecycle()
    val shiftTemplatesState by viewModel.shiftTemplates.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) activeMessage = message
    }

    var showAddModal by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var deleteTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var faceTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var revokeTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var shiftTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }

    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val members = (membersState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val shiftAssignments = (shiftAssignmentsState as? UiState.Success)?.data.orEmpty()
    val shiftTemplates = (shiftTemplatesState as? UiState.Success)?.data.orEmpty()

    var selectedPremisesId by remember { mutableStateOf<String?>(null) }
    var selectedRoleFilter by remember { mutableStateOf("ALL") } // "ALL", "STAFF", "MANAGER"

    // Merge staff and manager members into a single deduplicated list
    val allEmployees = remember(staff, members, premises) {
        val managerUserIds = members.filter { it.role == "MANAGER" }.map { it.userId }.toSet()
        val staffUserIds = staff.map { it.userId }.toSet()
        val premisesMap = premises.associateBy { it.id }

        val staffList = staff.map { s ->
            val resolvedPremisesName = s.premisesName?.takeIf { it.isNotBlank() }
                ?: premisesMap[s.premisesId]?.name
            UnifiedEmployeeItem(
                id = s.id,
                userId = s.userId,
                fullName = s.fullName,
                email = s.email,
                role = if (managerUserIds.contains(s.userId)) "MANAGER" else "STAFF",
                staffCode = s.staffCode,
                department = s.department,
                premisesId = s.premisesId,
                premisesName = resolvedPremisesName,
                joinedAt = s.hireDate,
                isActive = s.isActive,
                employeeId = s.id
            )
        }
        val managerList = members.filter { m -> m.role == "MANAGER" && !staffUserIds.contains(m.userId) }.map { m ->
            val resolvedPremisesName = m.premisesName?.takeIf { it.isNotBlank() }
                ?: premisesMap[m.premisesId]?.name
            val effectiveProfileId = m.employeeId ?: m.userId
            UnifiedEmployeeItem(
                id = effectiveProfileId,
                userId = m.userId,
                fullName = m.fullName,
                email = m.email,
                role = "MANAGER",
                staffCode = m.staffCode ?: "MGR",
                department = "Operations",
                premisesId = m.premisesId,
                premisesName = resolvedPremisesName,
                joinedAt = m.joinedAt,
                isActive = m.isActive,
                employeeId = effectiveProfileId
            )
        }
        staffList + managerList
    }

    val managerPremisesId = remember(allEmployees, user) {
        if (user?.role == Role.MANAGER) {
            allEmployees.find { it.userId == user.id || it.email.equals(user.email, ignoreCase = true) }?.premisesId
        } else null
    }

    val filteredEmployees = allEmployees.filter { emp ->
        // Manager's own card will not be listed in staff directory
        if (user?.role == Role.MANAGER && (emp.userId == user.id || emp.email.equals(user.email, ignoreCase = true))) {
            return@filter false
        }
        // Manager can see staff of his assigned premises only
        val matchesPremises = if (user?.role == Role.MANAGER && managerPremisesId != null) {
            emp.premisesId == managerPremisesId
        } else {
            selectedPremisesId == null || emp.premisesId == null || emp.premisesId == selectedPremisesId
        }
        val matchesRole = when (selectedRoleFilter) {
            "STAFF" -> emp.role == "STAFF"
            "MANAGER" -> emp.role == "MANAGER"
            else -> true
        }
        matchesPremises && matchesRole
    }

    Box {
        PresenceSecondaryScaffold(
            crumb = "People",
            title = "Employees Manager",
            onBack = onBack,
            actions = {
                PresenceIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh employee directory",
                    onClick = viewModel::refresh
                )
            }
        ) {
            PresencePageIntro(
                eyebrow = "Directory",
                title = "Employees Manager",
                description = "Manage staff and management team: assign shift schedules with attendance payment rules and enroll facial biometrics."
            )

            PresenceMetaStrip(
                items = listOf(
                    "Employees" to filteredEmployees.size.toString(),
                    "Active" to filteredEmployees.count { it.isActive }.toString(),
                    "Staff" to filteredEmployees.count { it.role == "STAFF" }.toString(),
                    "Managers" to filteredEmployees.count { it.role == "MANAGER" }.toString()
                )
            )

            // Premises & Role Filter Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (premises.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPremisesId == null,
                            onClick = { selectedPremisesId = null },
                            label = { Text("All Premises", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        premises.forEach { p ->
                            FilterChip(
                                selected = selectedPremisesId == p.id,
                                onClick = { selectedPremisesId = p.id },
                                label = { Text(Dates.cleanPremisesName(p.name), style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRoleFilter == "ALL",
                        onClick = { selectedRoleFilter = "ALL" },
                        label = { Text("All Employees", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedRoleFilter == "STAFF",
                        onClick = { selectedRoleFilter = "STAFF" },
                        label = { Text("Staff", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedRoleFilter == "MANAGER",
                        onClick = { selectedRoleFilter = "MANAGER" },
                        label = { Text("Managers", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            PresenceSectionTitle(
                title = "Organisation Roster",
                description = "Enrolled staff and managers.",
                action = {
                    if (canManage) {
                        PresenceButton(
                            label = "Add employee",
                            onClick = { showAddModal = true },
                            variant = PresenceVariant.Soft,
                            size = PresenceSize.Sm,
                            icon = Icons.Default.PersonAdd,
                            loading = isSubmitting
                        )
                    }
                }
            )

            if (filteredEmployees.isEmpty()) {
                PresenceEmptyState(
                    icon = Icons.Default.SupervisorAccount,
                    title = "No employees found",
                    description = "Add staff or manager members to start managing rosters, shift rules, and facial enrollment."
                )
            } else {
                filteredEmployees.forEach { employee ->
                    val targetProfileId = employee.employeeId ?: employee.id
                    EmployeeCard(
                        employee = employee,
                        face = faceStatuses[targetProfileId],
                        faceBusy = faceSubmittingFor == targetProfileId,
                        canManage = canManage,
                        shiftAssignments = shiftAssignments,
                        shiftTemplates = shiftTemplates,
                        onEdit = { editTarget = employee },
                        onDelete = { deleteTarget = employee },
                        onEnrollFace = { faceTarget = employee },
                        onRevokeFace = { revokeTarget = employee },
                        onAssignShift = { shiftTarget = employee }
                    )
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showAddModal) {
        val availablePremisesForAdd = if (user?.role == Role.MANAGER && managerPremisesId != null) {
            premises.filter { it.id == managerPremisesId }
        } else {
            premises
        }
        AddEmployeesModal(
            premisesList = availablePremisesForAdd,
            initialPremisesId = managerPremisesId ?: selectedPremisesId,
            onDismiss = { showAddModal = false },
            onAdd = { role, name, email, department, premisesId ->
                if (role == "MANAGER") {
                    viewModel.inviteManager(name, email, department, null, premisesId)
                } else {
                    viewModel.addStaff(name, email, department, null, premisesId)
                }
                showAddModal = false
            }
        )
    }

    editTarget?.let { target ->
        EditEmployeeModal(
            employee = target,
            premisesList = premises,
            onDismiss = { editTarget = null },
            onSave = { name, email, department, premisesId ->
                if (target.role == "MANAGER") {
                    viewModel.inviteManager(name, email, department, null, premisesId)
                } else {
                    viewModel.addStaff(name, email, department, null, premisesId)
                }
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Deactivate Employee Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to deactivate ${target.fullName}? This will revoke their access and purge facial biometric templates.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deactivateMember(target.userId, target.fullName)
                        deleteTarget = null
                    }
                ) { Text("Deactivate", color = PresenceDesign.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    faceTarget?.let { target ->
        val profileId = target.employeeId ?: target.id
        if (profileId.isNotBlank()) {
            FaceCaptureDialog(
                engine = viewModel.faceEmbeddingEngine,
                mode = "ENROLL",
                onCapturedMulti = { embeddings ->
                    viewModel.enrollFace(profileId, embeddings)
                    faceTarget = null
                },
                onCapturedSingle = { _ -> },
                onDismiss = { faceTarget = null }
            )
        }
    }

    revokeTarget?.let { target ->
        val profileId = target.employeeId ?: target.id
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("Reset face enrolment?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This revokes ${target.fullName}'s template. They cannot clock in until you re-enrol them in person.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileId.isNotBlank()) {
                            viewModel.revokeFace(profileId)
                        }
                        revokeTarget = null
                    }
                ) { Text("Reset", color = PresenceDesign.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text("Cancel") }
            }
        )
    }

    val activeShiftTarget = shiftTarget
    if (activeShiftTarget != null) {
        val targetProfileId = activeShiftTarget.employeeId ?: activeShiftTarget.id
        val staffAdapter = StaffDto(
            id = targetProfileId,
            userId = activeShiftTarget.userId,
            email = activeShiftTarget.email,
            fullName = activeShiftTarget.fullName,
            staffCode = activeShiftTarget.staffCode ?: "EMP",
            department = activeShiftTarget.department ?: "General",
            designation = activeShiftTarget.role,
            premisesId = activeShiftTarget.premisesId,
            premisesName = activeShiftTarget.premisesName,
            hireDate = activeShiftTarget.joinedAt ?: Dates.todayIso(),
            isActive = activeShiftTarget.isActive
        )
        AssignShiftWithRulesModal(
            staff = listOf(staffAdapter),
            templates = shiftTemplates,
            initialStaffId = staffAdapter.id,
            initialPremisesId = activeShiftTarget.premisesId,
            onDismiss = { shiftTarget = null },
            onAssignWithRules = { employeeId, assignments, startDate, premisesId, _, _ ->
                viewModel.assignShifts(employeeId, assignments, startDate, premisesId)
                shiftTarget = null
            }
        )
    }
}

@Composable
private fun EmployeeCard(
    employee: UnifiedEmployeeItem,
    face: FaceStatusDto?,
    faceBusy: Boolean,
    canManage: Boolean,
    shiftAssignments: List<ShiftAssignmentDto>,
    shiftTemplates: List<ShiftTemplateDto>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnrollFace: () -> Unit,
    onRevokeFace: () -> Unit,
    onAssignShift: () -> Unit
) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Avatar, Name, Email, Role, Edit & Delete Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                PresenceAvatar(
                    name = employee.fullName,
                    tone = if (employee.role == "MANAGER") PresenceTone.Purple else PresenceTone.Info
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            employee.staffCode?.let { append(it).append(" • ") }
                            append(employee.email)
                            employee.premisesName?.let { append(" • 📍 ").append(Dates.cleanPremisesName(it)) }
                        },
                        fontSize = 11.5.sp,
                        color = PresenceDesign.LabelText
                    )
                }
                PresenceBadge(
                    label = employee.role,
                    tone = if (employee.role == "MANAGER") PresenceTone.Purple else PresenceTone.Info
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (canManage) {
                    PresenceIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit profile for ${employee.fullName}",
                        onClick = onEdit
                    )
                    PresenceIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Deactivate account for ${employee.fullName}",
                        onClick = onDelete
                    )
                }
            }

            // Employee Details Group
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PresenceDetailRow(
                    label = "Department",
                    value = employee.department?.takeIf { it.isNotBlank() } ?: "Operations"
                )
                PresenceDetailRow(
                    label = "Email Address",
                    value = employee.email
                )
                PresenceDetailRow(
                    label = "Premises",
                    value = employee.premisesName?.let { Dates.cleanPremisesName(it) } ?: "Not Assigned"
                )
                employee.joinedAt?.let {
                    PresenceDetailRow(
                        label = "Joined Date",
                        value = Dates.formatDate(it)
                    )
                }
                PresenceDetailRow(
                    label = "Status",
                    value = if (employee.isActive) "Active" else "Deactivated"
                )
            }

            // Assigned Shifts & Attendance Payment Rules
            AssignedShiftsSection(
                employeeId = employee.employeeId,
                fullName = employee.fullName,
                email = employee.email,
                staffCode = employee.staffCode,
                shiftAssignments = shiftAssignments,
                shiftTemplates = shiftTemplates
            )

            // Action Buttons: Assign Shift & Payments, Enrol Face
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAssignShift,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign Shift & Payments", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onEnrollFace,
                    enabled = !faceBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (face?.enrolled == true) "Re-enrol" else "Enrol Face", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (face?.enrolled == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRevokeFace, enabled = !faceBusy) {
                        Text("Reset face", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignedShiftsSection(
    employeeId: String?,
    fullName: String?,
    email: String?,
    staffCode: String?,
    shiftAssignments: List<ShiftAssignmentDto>,
    shiftTemplates: List<ShiftTemplateDto>
) {
    val myAssignments = remember(employeeId, fullName, email, staffCode, shiftAssignments) {
        shiftAssignments.filter { a ->
            (employeeId != null && a.employeeId == employeeId) ||
            (!fullName.isNullOrBlank() && a.staffName.equals(fullName, ignoreCase = true)) ||
            (!email.isNullOrBlank() && a.staffName.equals(email, ignoreCase = true)) ||
            (!staffCode.isNullOrBlank() && a.staffName.equals(staffCode, ignoreCase = true))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Schedule & Shift Rules (7-Day View)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val activeDaysCount = remember(myAssignments, shiftTemplates) {
                WeekDaysList.count { dayInfo ->
                    myAssignments.any { a ->
                        val template = shiftTemplates.firstOrNull { t -> t.id == a.shiftTemplateId }
                        val activeDays = a.daysOfWeek ?: template?.daysOfWeek
                        activeDays.isNullOrEmpty() || activeDays.contains(dayInfo.dayOfWeek)
                    }
                }
            }
            Text(
                text = "$activeDaysCount / 7 days active",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (activeDaysCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (myAssignments.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No shift assigned • Tap Assign Shift & Payments below",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 7 Day Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekDaysList.forEach { dayInfo ->
                    val matchingAssignment = myAssignments.find { a ->
                        val template = shiftTemplates.firstOrNull { t -> t.id == a.shiftTemplateId }
                        val activeDays = a.daysOfWeek ?: template?.daysOfWeek
                        activeDays != null && activeDays.contains(dayInfo.dayOfWeek)
                    } ?: myAssignments.find { a ->
                        val template = shiftTemplates.firstOrNull { t -> t.id == a.shiftTemplateId }
                        val activeDays = a.daysOfWeek ?: template?.daysOfWeek
                        activeDays.isNullOrEmpty()
                    }

                    val template = matchingAssignment?.let { a ->
                        shiftTemplates.firstOrNull { t -> t.id == a.shiftTemplateId }
                    }

                    val isShiftActive = matchingAssignment != null
                    val timeWindow = if (isShiftActive) Dates.formatWindow(template?.startTime, template?.endTime) else "Off Day"
                    val presentPayCents = matchingAssignment?.paymentCents

                    Surface(
                        modifier = Modifier.width(155.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isShiftActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isShiftActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Day Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isShiftActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayInfo.shortName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isShiftActive) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = dayInfo.fullName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            if (matchingAssignment != null) {
                                Text(
                                    text = matchingAssignment.shiftName?.ifBlank { null } ?: template?.name ?: "Shift",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.Ink,
                                    maxLines = 1
                                )
                                Text(
                                    text = timeWindow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                // Attendance status-wise payment rates
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text(presentPayCents?.let { Money.format(it) } ?: "₹0.00", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald, fontSize = 9.5.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Half-Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text(presentPayCents?.let { Money.format(it / 2) } ?: "₹0.00", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 9.5.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Leave", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text(presentPayCents?.let { Money.format(it) } ?: "₹0.00", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), fontSize = 9.5.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Absent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text("₹0.00", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PresenceDesign.Danger, fontSize = 9.5.sp)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Weekly Off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "No shift",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEmployeesModal(
    premisesList: List<PremisesDto>,
    initialPremisesId: String?,
    onDismiss: () -> Unit,
    onAdd: (role: String, name: String, email: String, department: String, premisesId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var role by remember { mutableStateOf("STAFF") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedPremisesId by remember(initialPremisesId) { mutableStateOf(initialPremisesId) }

    val valid = name.trim().length >= 2 && email.trim().contains("@")

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
                text = "Add New Employee",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            PresenceFieldGroup(title = "Employee Role") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "STAFF", onClick = { role = "STAFF" })
                        Text("Staff Member", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "MANAGER", onClick = { role = "MANAGER" })
                        Text("Manager", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            PresenceFieldGroup(title = "Profile Details") {
                PresenceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )
                PresenceTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address"
                )
                PresenceTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = "Department"
                )
            }

            if (premisesList.isNotEmpty()) {
                PresenceFieldGroup(title = "Assigned Premises") {
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
            }

            PresenceButton(
                label = if (role == "MANAGER") "Add Manager" else "Add Staff",
                onClick = {
                    onAdd(role, name.trim(), email.trim(), department.trim(), selectedPremisesId)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEmployeeModal(
    employee: UnifiedEmployeeItem,
    premisesList: List<PremisesDto>,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, department: String, premisesId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(employee) { mutableStateOf(employee.fullName) }
    var email by remember(employee) { mutableStateOf(employee.email) }
    var department by remember(employee) { mutableStateOf(employee.department.orEmpty()) }
    var selectedPremisesId by remember(employee, premisesList) {
        mutableStateOf(employee.premisesId ?: premisesList.firstOrNull()?.id)
    }

    val valid = name.trim().length >= 2 && email.trim().contains("@")

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
                text = "Edit Employee Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            PresenceFieldGroup(title = "Account Overview") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresenceDetailRow(label = "Role", value = employee.role)
                    employee.staffCode?.let { PresenceDetailRow(label = "Code", value = it) }
                }
            }

            PresenceFieldGroup(title = "Profile Details") {
                PresenceTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )
                PresenceTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address"
                )
                PresenceTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = "Department"
                )
            }

            if (premisesList.isNotEmpty()) {
                PresenceFieldGroup(title = "Assigned Premises") {
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
            }

            PresenceButton(
                label = "Save Profile Changes",
                onClick = {
                    onSave(name.trim(), email.trim(), department.trim(), selectedPremisesId)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
