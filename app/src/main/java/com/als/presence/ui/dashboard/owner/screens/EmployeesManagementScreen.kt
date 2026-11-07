package com.als.presence.ui.dashboard.owner.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFilterChip
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.als.presence.ui.dashboard.common.PhotoUploadBox
import com.als.presence.ui.dashboard.common.processImageUriWithProgress
import com.als.presence.ui.dashboard.common.uriToBase64
import com.als.presence.ui.dashboard.owner.modals.AssignShiftWithRulesModal
import com.als.presence.ui.dashboard.owner.modals.BookOvertimeModal
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

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
    val premisesId: String?,
    val premisesName: String?,
    val joinedAt: String,
    val isActive: Boolean,
    val employeeId: String
)

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
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showAddModal by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var faceTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var revokeTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var shiftTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }
    var overtimeTarget by remember { mutableStateOf<UnifiedEmployeeItem?>(null) }

    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val shiftAssignments = (shiftAssignmentsState as? UiState.Success)?.data.orEmpty()
    val shiftTemplates = (shiftTemplatesState as? UiState.Success)?.data.orEmpty()

    val allEmployees = remember(staff, premises) {
        val premisesMap = premises.associateBy { it.id }
        staff.map { s ->
            val resolvedPremisesName = s.premisesName?.takeIf { it.isNotBlank() }
                ?: premisesMap[s.premisesId]?.name
            UnifiedEmployeeItem(
                id = s.id,
                userId = s.userId,
                fullName = s.fullName,
                email = s.email,
                phone = s.phone,
                address = s.address,
                avatarUrl = s.avatarUrl,
                role = "EMPLOYEE",
                staffCode = s.staffCode,
                department = s.department,
                premisesId = s.premisesId,
                premisesName = resolvedPremisesName,
                joinedAt = s.hireDate.orEmpty(),
                isActive = s.isActive,
                employeeId = s.id
            )
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    PresenceSecondaryScaffold(
        crumb = "Workforce",
        title = "Employees Manager",
        onBack = onBack,
        onRefresh = { viewModel.refresh() }
    ) {
        PresencePageIntro(
            eyebrow = "Directory",
            title = "Workforce Directory",
            description = "Manage workforce employees: assign shift schedules with attendance payment rules and enroll facial biometrics."
        )

        PresenceMetaStrip(
            items = listOf(
                "Employees" to allEmployees.size.toString(),
                "Active" to allEmployees.count { it.isActive }.toString()
            )
        )

        PresenceSectionTitle(
            title = "Organization Roster",
            description = "Enrolled employees for the organization.",
            action = if (canManage) {
                {
                    PresenceButton(
                        label = "Add Employee",
                        icon = Icons.Default.Add,
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
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    allEmployees.forEach { emp ->
                        EmployeeItemCard(
                            employee = emp,
                            canManage = canManage,
                            onEdit = { editTarget = emp },
                            onFaceEnroll = { faceTarget = emp },
                            onShiftAssign = { shiftTarget = emp },
                            onOvertimeAssign = { overtimeTarget = emp },
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
            onAdd = { _, name, email, department, address, phone, avatarBase64, premisesId ->
                viewModel.addStaff(
                    fullName = name,
                    email = email,
                    department = department,
                    designation = null,
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
            onSave = { name, email, department, address, phone, avatarBase64, premisesId ->
                viewModel.updateStaff(
                    employeeId = target.employeeId,
                    fullName = name,
                    email = email,
                    department = department,
                    designation = null,
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
        val staffList = staff.map { s ->
            StaffDto(
                id = s.id,
                fullName = s.fullName,
                email = s.email,
                role = "EMPLOYEE",
                premisesId = s.premisesId
            )
        }
        AssignShiftWithRulesModal(
            staff = staffList,
            templates = shiftTemplates,
            initialStaffId = target.employeeId,
            initialPremisesId = target.premisesId ?: premises.firstOrNull()?.id,
            onDismiss = { shiftTarget = null },
            onAssignWithRules = { employeeId, assignments, startDate, targetPremisesId, _, _ ->
                assignments.forEach { req ->
                    viewModel.assignShiftWithRequest(
                        req.copy(
                            employeeId = employeeId,
                            premisesId = targetPremisesId ?: req.premisesId
                        )
                    )
                }
                shiftTarget = null
            }
        )
    }

    overtimeTarget?.let { target ->
        val staffList = staff.map { s ->
            StaffDto(
                id = s.id,
                fullName = s.fullName,
                email = s.email,
                role = "EMPLOYEE",
                premisesId = s.premisesId
            )
        }
        BookOvertimeModal(
            staff = staffList,
            preselectedEmployeeId = target.employeeId,
            shiftAssignments = shiftAssignments,
            shifts = shiftTemplates,
            onDismiss = { overtimeTarget = null },
            onBook = { employeeId, workDate, startTime, endTime, paymentAmount, reason ->
                val rateInr = Money.parseToInr(paymentAmount) ?: 0L
                viewModel.createOvertime(
                    employeeId = employeeId,
                    workDate = workDate,
                    startTime = startTime,
                    endTime = endTime,
                    paymentInr = rateInr,
                    reason = reason
                )
                overtimeTarget = null
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
                    onClick = {
                        viewModel.revokeStaff(target.employeeId)
                        revokeTarget = null
                    }
                )
            },
            dismissButton = {
                PresenceButton(
                    label = "Cancel",
                    onClick = { revokeTarget = null }
                )
            }
        )
    }
}

@Composable
private fun EmployeeItemCard(
    employee: UnifiedEmployeeItem,
    canManage: Boolean,
    onEdit: () -> Unit,
    onFaceEnroll: () -> Unit,
    onShiftAssign: () -> Unit,
    onOvertimeAssign: () -> Unit,
    onRevoke: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    PresenceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Avatar + Info + Active Badge + 3-Dots Menu
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = employee.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = employee.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF747895)
                        )
                        if (!employee.phone.isNullOrBlank()) {
                            Text(
                                text = employee.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PresenceBadge(
                        label = if (employee.isActive) "Active" else "Deactivated",
                        tone = if (employee.isActive) PresenceTone.Success else PresenceTone.Neutral
                    )

                    if (canManage && employee.isActive) {
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Book Overtime") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MoreTime, contentDescription = null, tint = Color(0xFF8B5CF6))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onOvertimeAssign()
                                    }
                                )
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
            }

            if (!employee.address.isNullOrBlank()) {
                Text(
                    text = employee.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF747895)
                )
            }

            // Sleek Metadata Info Bar (Single Line Non-Wrapping Grid)
            Surface(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Code",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFF747895)
                        )
                        Text(
                            text = employee.staffCode,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34),
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Department",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFF747895)
                        )
                        Text(
                            text = employee.department?.ifBlank { "General" } ?: "General",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF171A34),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Premises",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFF747895)
                        )
                        Text(
                            text = employee.premisesName?.ifBlank { "Main Office" } ?: "Main Office",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF171A34),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (canManage && employee.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresenceButton(
                        label = "Assign Shift",
                        icon = Icons.Default.Schedule,
                        size = PresenceSize.Sm,
                        onClick = onShiftAssign,
                        modifier = Modifier.weight(1f)
                    )
                    PresenceButton(
                        label = "Biometrics",
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
        address: String?,
        phone: String?,
        avatarBase64: String?,
        premisesId: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedPremisesId by remember(initialPremisesId, premisesList) {
        mutableStateOf(initialPremisesId ?: premisesList.firstOrNull()?.id)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            coroutineScope.launch {
                isUploading = true
                uploadProgress = 0
                val b64 = processImageUriWithProgress(context, uri) { progress ->
                    uploadProgress = progress
                }
                avatarBase64 = b64
                isUploading = false
            }
        }
    }

    val isEmailValid = email.trim().contains("@") && email.trim().contains(".")
    val valid = name.trim().length >= 2 && isEmailValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8EEFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Add New Employee",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = "Onboard staff profile & workplace details",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF747895)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Top Row: Inline Photo Card + Name & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Photo Upload Box with Cloudinary Loader and percentage
                PhotoUploadBox(
                    model = selectedImageUri,
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    onSelectImage = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.size(width = 84.dp, height = 98.dp),
                    shape = RoundedCornerShape(14.dp),
                    placeholderText = name.take(1).uppercase().ifBlank { "E" },
                    helperText = if (selectedImageUri != null) "Change Photo" else "Upload Photo"
                )

                // Name & Email Fields (Right side Column)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Contact & Work Section: Phone & Department side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Address Field
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Premises Selection
            if (premisesList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Assign Premises Workspace",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B5563)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            // Validation Helper Note
            if (!valid) {
                Text(
                    text = "* Full Name and a valid Email Address are required to onboard an employee.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            // Submit Button
            PresenceButton(
                label = "Add Employee",
                icon = Icons.Default.Add,
                onClick = {
                    onAdd(
                        "EMPLOYEE",
                        name.trim(),
                        email.trim(),
                        department.trim(),
                        address.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        avatarBase64,
                        selectedPremisesId
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
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
        address: String?,
        phone: String?,
        avatarBase64: String?,
        premisesId: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(employee) { mutableStateOf(employee.fullName) }
    var email by remember(employee) { mutableStateOf(employee.email) }
    var phone by remember(employee) { mutableStateOf(employee.phone.orEmpty()) }
    var address by remember(employee) { mutableStateOf(employee.address.orEmpty()) }
    var department by remember(employee) { mutableStateOf(employee.department.orEmpty()) }
    var selectedPremisesId by remember(employee, premisesList) {
        mutableStateOf(employee.premisesId ?: premisesList.firstOrNull()?.id)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            coroutineScope.launch {
                isUploading = true
                uploadProgress = 0
                val b64 = processImageUriWithProgress(context, uri) { progress ->
                    uploadProgress = progress
                }
                avatarBase64 = b64
                isUploading = false
            }
        }
    }

    val valid = name.trim().length >= 2 && email.trim().contains("@")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8EEFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Employee Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = "Update profile details & workplace assignments",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF747895)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF3F4F6))

            // Inline Top Row: Rectangular Photo Card + Name & Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Photo Upload Box with Cloudinary percentage loader
                PhotoUploadBox(
                    model = selectedImageUri ?: employee.avatarUrl,
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    onSelectImage = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.size(width = 84.dp, height = 98.dp),
                    shape = RoundedCornerShape(14.dp),
                    placeholderText = name.take(1).uppercase().ifBlank { "E" },
                    helperText = "Change Photo"
                )

                // Name & Email Fields (Right side Column)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 11.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *", fontSize = 11.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Contact & Work Section: Phone & Department side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number", fontSize = 11.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department", fontSize = 11.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Address Field
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address", fontSize = 11.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            PresenceButton(
                label = "Save Changes",
                onClick = {
                    onSave(
                        name.trim(),
                        email.trim(),
                        department.trim(),
                        address.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        avatarBase64,
                        selectedPremisesId
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
