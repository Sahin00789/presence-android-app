package com.als.presence.ui.dashboard.owner.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.PremisesDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PhotoUploadBox
import com.als.presence.ui.dashboard.common.PhotoUploadModal
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFilterChip
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.processImageUriWithProgress
import com.als.presence.ui.dashboard.owner.modals.AssignShiftWithRulesModal
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel
import kotlinx.coroutines.launch

private data class EmployeeShiftSummaryInfo(
    val assignmentId: String,
    val shiftId: String,
    val shiftName: String,
    val startTime: String?,
    val endTime: String?,
    val dayName: String?,
    val presentPay: Double,
    val halfDayPay: Double,
    val absentPay: Double,
    val leavePay: Double
)

private data class UnifiedEmployeeItem(
    val id: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val avatarUrl: String?,
    val role: String,
    val staffCode: String,
    val department: String?,
    val designation: String?,
    val premisesId: String?,
    val premisesName: String?,
    val joinedAt: String,
    val isActive: Boolean,
    val employeeId: String,
    val isFaceEnrolled: Boolean,
    val faceEnrolledAt: String?,
    val assignedShifts: List<EmployeeShiftSummaryInfo>,
    val earnedBalanceInr: Long,
    val approvedUnpaidBalanceInr: Long,
    val paidBalanceInr: Long
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeesManagementScreen(
    user: User? = null,
    onBack: () -> Unit,
    canManage: Boolean = true,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val staffState by viewModel.staffState.collectAsState()
    val premisesState by viewModel.premisesState.collectAsState()
    val shiftAssignmentsState by viewModel.shiftAssignmentsState.collectAsState()
    val shiftTemplatesState by viewModel.shiftTemplatesState.collectAsState()
    val unworkedRulesState by viewModel.unworkedRulesState.collectAsState()
    val faceStatusesState by viewModel.faceStatuses.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showAddModal by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var faceTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var revokeTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var shiftTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }

    // Search and Filtering State
    var searchQuery by remember { mutableStateOf("") }
    var selectedPremisesFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedShiftFilter by remember { mutableStateOf("ALL") }

    val staff = (staffState as? UiState.Success)?.data.orEmpty().filter { it.id.isNotBlank() }.distinctBy { it.id }
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val shiftAssignments = (shiftAssignmentsState as? UiState.Success)?.data.orEmpty()
    val shiftTemplates = (shiftTemplatesState as? UiState.Success)?.data.orEmpty()

    val allEmployees = remember(staff, premises, shiftAssignments, shiftTemplates, faceStatusesState) {
        val premisesMap = premises.associateBy { it.id }
        val templatesMap = shiftTemplates.associateBy { it.id }

        staff.map { s ->
            val resolvedPremisesName = s.premisesName?.takeIf { it.isNotBlank() }
                ?: premisesMap[s.premisesId]?.name

            val faceStatus = faceStatusesState[s.id]
            val isFaceEnrolled = faceStatus?.enrolled == true

            val empShifts = shiftAssignments
                .filter { (it.employeeId == s.id || it.staffId == s.id) && it.isActive }
                .map { a ->
                    val t = templatesMap[a.shiftId]
                    EmployeeShiftSummaryInfo(
                        assignmentId = a.id,
                        shiftId = a.shiftId,
                        shiftName = a.shiftName?.takeIf { it.isNotBlank() } ?: t?.shiftName ?: "Shift",
                        startTime = t?.startTime,
                        endTime = t?.endTime,
                        dayName = t?.dayName ?: "ALL",
                        presentPay = a.presentPay,
                        halfDayPay = a.halfDayPay,
                        absentPay = a.absentPay,
                        leavePay = a.leavePay
                    )
                }

            UnifiedEmployeeItem(
                id = s.id,
                userId = s.userId,
                fullName = s.fullName,
                email = s.email,
                phone = s.phone,
                address = s.address,
                avatarUrl = s.avatarUrl,
                role = if (s.role.equals("MANAGER", ignoreCase = true)) "MANAGER" else "EMPLOYEE",
                staffCode = s.staffCode,
                department = s.department,
                designation = s.designation,
                premisesId = s.premisesId,
                premisesName = resolvedPremisesName,
                joinedAt = s.startDate,
                isActive = s.isActive,
                employeeId = s.id,
                isFaceEnrolled = isFaceEnrolled,
                faceEnrolledAt = faceStatus?.enrolledAt,
                assignedShifts = empShifts,
                earnedBalanceInr = s.earnedBalanceInr,
                approvedUnpaidBalanceInr = s.approvedUnpaidBalanceInr,
                paidBalanceInr = s.paidBalanceInr
            )
        }
    }

    val filteredEmployees = remember(
        allEmployees,
        searchQuery,
        selectedPremisesFilter,
        selectedStatusFilter,
        selectedShiftFilter
    ) {
        val q = searchQuery.trim().lowercase()
        allEmployees.filter { emp ->
            val matchesQuery = q.isBlank() ||
                    emp.fullName.lowercase().contains(q) ||
                    emp.email.lowercase().contains(q) ||
                    emp.staffCode.lowercase().contains(q) ||
                    (emp.phone?.lowercase()?.contains(q) == true) ||
                    (emp.department?.lowercase()?.contains(q) == true) ||
                    (emp.designation?.lowercase()?.contains(q) == true) ||
                    (emp.premisesName?.lowercase()?.contains(q) == true)

            val matchesStatus = when (selectedStatusFilter) {
                "ACTIVE" -> emp.isActive
                "DEACTIVATED" -> !emp.isActive
                else -> true
            }

            val matchesPremises = selectedPremisesFilter == null || emp.premisesId == selectedPremisesFilter

            val matchesShift = when (selectedShiftFilter) {
                "ASSIGNED" -> emp.assignedShifts.isNotEmpty()
                "UNASSIGNED" -> emp.assignedShifts.isEmpty()
                else -> true
            }

            matchesQuery && matchesStatus && matchesPremises && matchesShift
        }
    }

    val totalCount = allEmployees.size
    val activeCount = allEmployees.count { it.isActive }
    val shiftAssignedCount = allEmployees.count { it.assignedShifts.isNotEmpty() }
    val faceEnrolledCount = allEmployees.count { it.isFaceEnrolled }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    PresenceSecondaryScaffold(
        crumb = "Workforce",
        title = "Workforce Directory",
        subtitle = "Manage employee profiles, shift schedules, rates & biometrics",
        onBack = onBack,
        onRefresh = { viewModel.refresh() }
    ) {
        // Top KPI Summary Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkforceSummaryMetricCard(
                label = "Total Staff",
                value = "$totalCount",
                icon = Icons.Default.Group,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            WorkforceSummaryMetricCard(
                label = "Active",
                value = "$activeCount",
                icon = Icons.Default.CheckCircle,
                color = SecondaryEmerald,
                modifier = Modifier.weight(1f)
            )
            WorkforceSummaryMetricCard(
                label = "Face Enrolled",
                value = "$faceEnrolledCount",
                icon = Icons.Default.Face,
                color = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            WorkforceSummaryMetricCard(
                label = "Shift Assigned",
                value = "$shiftAssignedCount",
                icon = Icons.Default.Schedule,
                color = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )
        }

        // Search & Filter Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search employees...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresenceFilterChip(
                        label = "All Status",
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" }
                    )
                    PresenceFilterChip(
                        label = "Active ($activeCount)",
                        selected = selectedStatusFilter == "ACTIVE",
                        onClick = { selectedStatusFilter = "ACTIVE" }
                    )
                    PresenceFilterChip(
                        label = "Deactivated (${totalCount - activeCount})",
                        selected = selectedStatusFilter == "DEACTIVATED",
                        onClick = { selectedStatusFilter = "DEACTIVATED" }
                    )

                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    PresenceFilterChip(
                        label = "All Shifts",
                        selected = selectedShiftFilter == "ALL",
                        onClick = { selectedShiftFilter = "ALL" }
                    )
                    PresenceFilterChip(
                        label = "Assigned ($shiftAssignedCount)",
                        selected = selectedShiftFilter == "ASSIGNED",
                        onClick = { selectedShiftFilter = "ASSIGNED" }
                    )

                    if (premises.size > 1) {
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        PresenceFilterChip(
                            label = "All Premises",
                            selected = selectedPremisesFilter == null,
                            onClick = { selectedPremisesFilter = null }
                        )

                        premises.forEach { p ->
                            PresenceFilterChip(
                                label = p.name,
                                selected = selectedPremisesFilter == p.id,
                                onClick = { selectedPremisesFilter = p.id }
                            )
                        }
                    }
                }
            }
        }

        PresenceSectionTitle(
            title = "Workforce (${filteredEmployees.size})",
            action = if (canManage) {
                {
                    PresenceButton(
                        label = "Add Employee",
                        icon = Icons.Default.Add,
                        size = PresenceSize.Sm,
                        onClick = { showAddModal = true }
                    )
                }
            } else null
        )

        when {
            staffState is UiState.Loading && staff.isEmpty() -> {
                PresenceLoading(label = "Loading workforce directory...")
            }
            staffState is UiState.Error && staff.isEmpty() -> {
                PresenceErrorState(
                    message = (staffState as UiState.Error).message,
                    onRetry = { viewModel.refresh() }
                )
            }
            allEmployees.isEmpty() -> {
                PresenceEmptyState(
                    icon = Icons.Default.Badge,
                    title = "No employees found",
                    description = "Add workforce employees to start managing rosters, shift assignments, and facial verification."
                )
            }
            filteredEmployees.isEmpty() -> {
                PresenceEmptyState(
                    icon = Icons.Default.Search,
                    title = "No matching employees",
                    description = "No employees match your current search query or filter criteria."
                )
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredEmployees.forEach { emp ->
                        EmployeeItemCard(
                            employee = emp,
                            canManage = canManage,
                            onEdit = { editTarget = emp },
                            onFaceEnroll = { faceTarget = emp },
                            onShiftAssign = { shiftTarget = emp },
                            onRevoke = { revokeTarget = emp }
                        )
                    }
                }
            }
        }
    }

    if (showAddModal) {
        AddEmployeesModal(
            premisesList = premises,
            initialPremisesId = premises.firstOrNull()?.id,
            onDismiss = { showAddModal = false },
            onAdd = { _, name, email, department, designation, address, phone, avatarBase64, premisesId ->
                viewModel.addStaff(
                    fullName = name,
                    email = email,
                    department = department,
                    designation = designation,
                    address = address,
                    phone = phone,
                    avatarBase64 = avatarBase64,
                    premisesId = premisesId
                )
                showAddModal = false
            }
        )
    }

    editTarget?.let { target ->
        EditEmployeeModal(
            employee = target,
            premisesList = premises,
            onDismiss = { editTarget = null },
            onSave = { name, email, department, designation, address, phone, avatarBase64, premisesId ->
                viewModel.updateStaff(
                    employeeId = target.employeeId,
                    fullName = name,
                    email = email,
                    department = department,
                    designation = designation,
                    address = address,
                    phone = phone,
                    avatarBase64 = avatarBase64,
                    premisesId = premisesId
                )
                editTarget = null
            }
        )
    }

    faceTarget?.let { target ->
        val engine = remember { FaceEmbeddingEngine(context) }
        FaceCaptureDialog(
            engine = engine,
            mode = "ENROLL",
            onCapturedMulti = { embeddings ->
                viewModel.enrollFaceTemplates(target.employeeId, embeddings)
                faceTarget = null
            },
            onDismiss = { faceTarget = null }
        )
    }

    shiftTarget?.let { target ->
        LaunchedEffect(target.employeeId) {
            viewModel.loadUnworkedRules(employeeId = target.employeeId, premisesId = target.premisesId)
        }

        val staffList = staff.map { s ->
            s.copy(
                role = s.role,
                organizationName = s.organizationName ?: user?.organizationName,
                staffCode = s.staffCode,
                premisesName = s.premisesName
            )
        }
        val unworkedRules = (unworkedRulesState as? UiState.Success)?.data.orEmpty()

        AssignShiftWithRulesModal(
            staff = staffList,
            templates = shiftTemplates,
            existingAssignments = shiftAssignments,
            unworkedRules = unworkedRules,
            initialStaffId = target.employeeId,
            initialPremisesId = target.premisesId ?: premises.firstOrNull()?.id,
            onDismiss = { shiftTarget = null },
            onSaveAssignment = { request ->
                viewModel.assignShiftWithRequest(request)
            },
            onDeleteAssignment = { assignmentId ->
                viewModel.deleteShiftAssignment(assignmentId)
            },
            onSaveUnworkedRules = { empId, premId, hPay, oPay ->
                viewModel.saveUnworkedRules(empId, premId, hPay, oPay)
            },
            onStaffSelected = { empId, premId ->
                viewModel.loadUnworkedRules(employeeId = empId, premisesId = premId)
            }
        )
    }

    revokeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("Deactivate Employee Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to deactivate ${target.fullName}? This will revoke session access and remove biometric templates.") },
            confirmButton = {
                PresenceButton(
                    label = "Deactivate",
                    variant = PresenceVariant.Danger,
                    onClick = {
                        viewModel.revokeStaff(target.employeeId)
                        revokeTarget = null
                    }
                )
            },
            dismissButton = {
                PresenceButton(
                    label = "Cancel",
                    variant = PresenceVariant.Secondary,
                    onClick = { revokeTarget = null }
                )
            }
        )
    }
}

/**
 * Compact, small-width screen safe, highly polished Employee Card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkforceSummaryMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Modern, clean, structured Employee Card layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeItemCard(
    employee: UnifiedEmployeeItem,
    canManage: Boolean,
    onEdit: () -> Unit,
    onFaceEnroll: () -> Unit,
    onShiftAssign: () -> Unit,
    onRevoke: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Row: Avatar + Name/Role/ID + 3-Dots Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!employee.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = employee.avatarUrl,
                                contentDescription = employee.fullName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = employee.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Face Status Indicator Overlay Dot
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (employee.isFaceEnrolled) SecondaryEmerald else TertiaryAmber)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (employee.isFaceEnrolled) Icons.Default.Check else Icons.Default.Face,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = employee.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (employee.staffCode.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#${employee.staffCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isManager = employee.role.equals("MANAGER", ignoreCase = true)
                            Surface(
                                color = if (isManager) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isManager) "Manager" else "Employee",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isManager) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            PresenceBadge(
                                label = if (employee.isActive) "Active" else "Inactive",
                                tone = if (employee.isActive) PresenceTone.Success else PresenceTone.Neutral
                            )
                        }
                    }
                }

                // 3-Dots Menu Button
                if (canManage && employee.isActive) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Profile") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB))
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Assign Shift & Rates") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF059669))
                                },
                                onClick = {
                                    showMenu = false
                                    onShiftAssign()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enroll Biometrics") },
                                leadingIcon = {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF7C3AED))
                                },
                                onClick = {
                                    showMenu = false
                                    onFaceEnroll()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            DropdownMenuItem(
                                text = { Text("Deactivate Account", color = Color(0xFFDC2626)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626))
                                },
                                onClick = {
                                    showMenu = false
                                    onRevoke()
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // 2. Metadata Chips Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Premises Chip
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = employee.premisesName?.ifBlank { "Main Office" } ?: "Main Office",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Department Chip
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = employee.department?.takeIf { it.isNotBlank() } ?: "Operations",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Designation Chip
                if (!employee.designation.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = employee.designation,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Face Verification Status Chip
                Surface(
                    color = if (employee.isFaceEnrolled) SecondaryEmerald.copy(alpha = 0.12f) else TertiaryAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (employee.isFaceEnrolled) SecondaryEmerald.copy(alpha = 0.3f) else TertiaryAmber.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (employee.isFaceEnrolled) Icons.Default.Check else Icons.Default.Face,
                            contentDescription = null,
                            tint = if (employee.isFaceEnrolled) SecondaryEmerald else TertiaryAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (employee.isFaceEnrolled) "Face Enrolled" else "Face Pending",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (employee.isFaceEnrolled) SecondaryEmerald else TertiaryAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Contact info strip (Email & Phone)
            if (!employee.email.isNullOrBlank() || !employee.phone.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!employee.email.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = employee.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!employee.phone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = employee.phone,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Address (if present)
            if (!employee.address.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = employee.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Shift Schedule & Pay Rates Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Shift Schedule & Pay Rates",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (canManage && employee.isActive && employee.assignedShifts.isEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onShiftAssign() }
                            ) {
                                Text(
                                    text = "+ Assign Shift",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (employee.assignedShifts.isNotEmpty()) {
                        employee.assignedShifts.forEach { shift ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = shift.shiftName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (shift.startTime != null && shift.endTime != null) {
                                            Text(
                                                text = "${shift.startTime.take(5)} – ${shift.endTime.take(5)} (${shift.dayName ?: "Everyday"})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SecondaryEmerald.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${Money.format(shift.presentPay.toLong())}/day",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (shift.halfDayPay > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = TertiaryAmber.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "Half: ${Money.format(shift.halfDayPay.toLong())}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TertiaryAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No active shift schedule assigned yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // 4. Wallet & Salary Accrual Summary Bar
            if (employee.earnedBalanceInr > 0 || employee.approvedUnpaidBalanceInr > 0 || employee.paidBalanceInr > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Earned", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(employee.earnedBalanceInr), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Paid", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(employee.paidBalanceInr), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unpaid Due", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(employee.approvedUnpaidBalanceInr), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (employee.approvedUnpaidBalanceInr > 0) TertiaryAmber else SecondaryEmerald)
                        }
                    }
                }
            }

            // 5. Primary Action Buttons
            if (canManage && employee.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresenceButton(
                        label = "Assign Shift",
                        icon = Icons.Default.Schedule,
                        size = PresenceSize.Sm,
                        onClick = onShiftAssign,
                        modifier = Modifier.weight(1f)
                    )
                    PresenceButton(
                        label = if (employee.isFaceEnrolled) "Biometrics" else "Enroll Face",
                        icon = Icons.Default.Face,
                        size = PresenceSize.Sm,
                        variant = PresenceVariant.Secondary,
                        onClick = onFaceEnroll,
                        modifier = Modifier.weight(1f)
                    )
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
    onAdd: (
        role: String,
        name: String,
        email: String,
        department: String,
        designation: String?,
        address: String?,
        phone: String?,
        avatarBase64: String?,
        premisesId: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var selectedPremisesId by remember(initialPremisesId, premisesList) {
        mutableStateOf(initialPremisesId ?: premisesList.firstOrNull()?.id)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var showPhotoModal by remember { mutableStateOf(false) }

    val isEmailValid = email.trim().contains("@") && email.trim().contains(".")
    val valid = name.trim().length >= 2 && isEmailValid

    if (showPhotoModal) {
        PhotoUploadModal(
            selectedImageUri = selectedImageUri,
            currentAvatarUrl = null,
            placeholderText = name.take(1).uppercase().ifBlank { "E" },
            onDismiss = { showPhotoModal = false },
            onPhotoSelected = { uri, base64 ->
                selectedImageUri = uri
                avatarBase64 = base64
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Add New Employee",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Onboard staff profile & assign workplace location",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Dedicated Profile Photo Section Banner
            Surface(
                onClick = { showPhotoModal = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE))
                                .border(2.dp, Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Employee Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = name.take(1).uppercase().ifBlank { "E" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedImageUri != null) "Profile Photo Attached ✓" else "Upload Profile Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Tap to open photo camera or gallery modal",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            text = if (selectedImageUri != null) "Change" else "Upload",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Input Fields Group 1: Personal Details
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Personal & Contact Information",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 11.sp) },
                        placeholder = { Text("Enter employee full name", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *", fontSize = 11.sp) },
                        placeholder = { Text("Enter email address", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Phone Number", fontSize = 11.sp) },
                        placeholder = { Text("e.g. +91 98765 43210", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Input Fields Group 2: Role & Workplace
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Department & Workplace Role",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Operations", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Engineer", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address", fontSize = 11.sp) },
                        placeholder = { Text("Enter address", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (premisesList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Assigned Premises Location *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                premisesList.forEach { p ->
                                    val isSelected = p.id == selectedPremisesId
                                    PresenceFilterChip(
                                        label = p.name,
                                        selected = isSelected,
                                        onClick = { selectedPremisesId = p.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!valid) {
                Text(
                    text = "* Full Name (min 2 chars) and a valid Email Address are required to add employee.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFE11D48)
                )
            }

            PresenceButton(
                label = "Add Employee",
                icon = Icons.Default.Add,
                onClick = {
                    onAdd(
                        "EMPLOYEE",
                        name.trim(),
                        email.trim(),
                        department.trim(),
                        designation.trim().ifBlank { null },
                        address.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        avatarBase64,
                        selectedPremisesId
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEmployeeModal(
    employee: UnifiedEmployeeItem,
    premisesList: List<PremisesDto>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        email: String,
        department: String,
        designation: String?,
        address: String?,
        phone: String?,
        avatarBase64: String?,
        premisesId: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(employee) { mutableStateOf(employee.fullName) }
    var email by remember(employee) { mutableStateOf(employee.email) }
    var phone by remember(employee) { mutableStateOf(employee.phone.orEmpty()) }
    var address by remember(employee) { mutableStateOf(employee.address.orEmpty()) }
    var department by remember(employee) { mutableStateOf(employee.department.orEmpty()) }
    var designation by remember(employee) { mutableStateOf(employee.designation.orEmpty()) }
    var selectedPremisesId by remember(employee, premisesList) {
        mutableStateOf(employee.premisesId ?: premisesList.firstOrNull()?.id)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var showPhotoModal by remember { mutableStateOf(false) }

    val valid = name.trim().length >= 2 && email.trim().contains("@")

    if (showPhotoModal) {
        PhotoUploadModal(
            selectedImageUri = selectedImageUri,
            currentAvatarUrl = employee.avatarUrl,
            placeholderText = name.take(1).uppercase().ifBlank { "E" },
            onDismiss = { showPhotoModal = false },
            onPhotoSelected = { uri, base64 ->
                selectedImageUri = uri
                avatarBase64 = base64
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Employee Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Update profile details & workplace assignments",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Dedicated Profile Photo Section Banner
            Surface(
                onClick = { showPhotoModal = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE))
                                .border(2.dp, Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoModel = selectedImageUri ?: employee.avatarUrl
                            if (photoModel != null) {
                                AsyncImage(
                                    model = photoModel,
                                    contentDescription = "Employee Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = name.take(1).uppercase().ifBlank { "E" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedImageUri != null) "New Photo Selected ✓" else "Change Profile Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Tap to open photo camera or gallery modal",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Personal Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Personal & Contact Information",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 11.sp) },
                        placeholder = { Text("Enter full name", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *", fontSize = 11.sp) },
                        placeholder = { Text("Enter email address", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number", fontSize = 11.sp) },
                        placeholder = { Text("Enter mobile number", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Role & Workplace Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Department & Workplace Role",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department", fontSize = 11.sp) },
                            placeholder = { Text("Enter department", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation", fontSize = 11.sp) },
                            placeholder = { Text("Enter designation", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address", fontSize = 11.sp) },
                        placeholder = { Text("Enter address", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (premisesList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Assigned Premises Workspace *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                premisesList.forEach { p ->
                                    val isSelected = p.id == selectedPremisesId
                                    PresenceFilterChip(
                                        label = p.name,
                                        selected = isSelected,
                                        onClick = { selectedPremisesId = p.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PresenceButton(
                label = "Save Changes",
                onClick = {
                    onSave(
                        name.trim(),
                        email.trim(),
                        department.trim(),
                        designation.trim().ifBlank { null },
                        address.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        avatarBase64,
                        selectedPremisesId
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
