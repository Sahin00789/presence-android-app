package com.als.presence.ui.dashboard.manager.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FilledTonalButton
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.AsyncContent
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PayStaffModal
import com.als.presence.ui.dashboard.common.SalaryDialog
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.StatusPill

import com.als.presence.domain.model.User

/** Manager "Staff" tab — the roster this manager operates on a daily basis. */
@Composable
fun ManagerStaffTab(
    user: User? = null,
    staffState: UiState<List<StaffDto>>,
    faceStatuses: Map<String, FaceStatusDto>,
    faceSubmittingFor: String?,
    isSubmitting: Boolean,
    engine: FaceEmbeddingEngine,
    onRefresh: () -> Unit,
    onAddStaff: () -> Unit,
    onSetSalary: (staffProfileId: String, monthlySalary: String) -> Unit,
    onEnrollFace: (staffProfileId: String, embeddings: Map<String, FaceEmbeddingEngine.Embedding>) -> Unit,
    onRevokeFace: (staffProfileId: String) -> Unit,
    onDeactivateStaff: ((userId: String, name: String) -> Unit)? = null,
    onAssignShift: (staff: StaffDto) -> Unit = {},
    modifier: Modifier = Modifier,
    premisesState: UiState<List<PremisesDto>> = UiState.Success(emptyList()),
    payrollPeriodsState: UiState<List<PayrollPeriodDto>> = UiState.Success(emptyList()),
    payrollItemsMap: Map<String, List<PayrollItemDto>> = emptyMap(),
    onRecordPayment: ((itemId: String, amountCents: Long, paymentMethod: String, reference: String) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var salaryTarget by remember { mutableStateOf<StaffDto?>(null) }
    var payTargetStaff by remember { mutableStateOf<StaffDto?>(null) }
    var deleteTarget by remember { mutableStateOf<StaffDto?>(null) }
    var faceTarget by remember { mutableStateOf<StaffDto?>(null) }
    var revokeTarget by remember { mutableStateOf<StaffDto?>(null) }

    val premises = (premisesState as? UiState.Success)?.data.orEmpty()
    val periods = (payrollPeriodsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "My team",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val count = (staffState as? UiState.Success)?.data?.size
                Text(
                    text = if (count == null) "Syncing roster…" else "$count staff members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh roster",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onAddStaff,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Add staff",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search name, code or department") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AsyncContent(
            state = staffState,
            onRetry = onRefresh,
            isEmpty = { it.isEmpty() },
            emptyTitle = "No staff yet",
            emptyMessage = "Use the add button above to onboard your first staff member."
        ) { staff ->
            val managerPremisesId = remember(staff, user) {
                if (user != null) {
                    staff.find { it.userId == user.id || it.email.equals(user.email, ignoreCase = true) }?.premisesId
                } else null
            }

            val filtered = staff.filter { member ->
                // Manager's own card will not be shown in roster list
                val isCurrentManager = user != null && (member.userId == user.id || member.email.equals(user.email, ignoreCase = true))
                if (isCurrentManager) return@filter false

                // Manager can see staff of his assigned premises only
                val matchesPremises = if (managerPremisesId != null) {
                    member.premisesId == managerPremisesId
                } else true

                matchesPremises && (
                    query.isBlank() ||
                    member.fullName.contains(query, ignoreCase = true) ||
                    member.staffCode.contains(query, ignoreCase = true) ||
                    (member.department?.contains(query, ignoreCase = true) == true)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Team Salary & Wallet Summary Card
                val totalEarnedCents = staff.sumOf { it.earnedBalanceCents }
                val totalWalletCents = staff.sumOf { it.totalBalanceCents }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Team Salary & Wallet Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${staff.size} Staff Members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Earned Salary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Money.format(totalEarnedCents),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Wallet Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Money.format(totalWalletCents),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    SectionHeader(
                        title = "No staff found",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { member ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        InitialAvatar(name = member.fullName)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                member.fullName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = buildString {
                                                    append(member.staffCode)
                                                    member.department?.let { append(" • ").append(it) }
                                                    member.premisesName?.let { append(" • 📍 ").append(Dates.cleanPremisesName(it)) }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPill(if (member.isActive) "ACTIVE" else "CANCELLED")
                                        if (onDeactivateStaff != null) {
                                            IconButton(
                                                onClick = { deleteTarget = member },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Deactivate ${member.fullName}",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    val totalEarned = member.earnedBalanceCents
                                    val totalPaid = member.paidBalanceCents
                                    val dueUnpaid = (totalEarned - totalPaid).coerceAtLeast(0L)

                                    // Simple Earned and Paid Summary Banner
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(Money.format(totalEarned), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(Money.format(totalPaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(Money.format(dueUnpaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                                        }
                                    }

                                    // Pay Salary Button
                                    FilledTonalButton(
                                        onClick = { payTargetStaff = member },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pay Salary (${Money.format(dueUnpaid)})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    // Action chips row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssistChip(
                                            onClick = { onAssignShift(member) },
                                            label = { Text("Salary & Shifts") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        val face = faceStatuses[member.id]
                                        val faceBusy = faceSubmittingFor == member.id
                                        AssistChip(
                                            onClick = { faceTarget = member },
                                            enabled = !faceBusy,
                                            label = { Text(if (face?.enrolled == true) "Re-enrol Face" else "Enrol Face") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Face,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    
                                    val face = faceStatuses[member.id]
                                    val faceBusy = faceSubmittingFor == member.id
                                    if (face?.enrolled == true) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { revokeTarget = member },
                                                enabled = !faceBusy
                                            ) {
                                                Text("Reset face", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    salaryTarget?.let { target ->
        SalaryDialog(
            staffName = target.fullName,
            initialAmount = target.baseRateCents?.let { (it / 100).toString() } ?: "",
            onDismiss = { salaryTarget = null },
            onConfirm = { shiftAmt ->
                onSetSalary(target.id, shiftAmt)
                salaryTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Deactivate Staff Member?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to deactivate ${target.fullName}? This will remove them from active rosters and purge facial biometric templates."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeactivateStaff?.invoke(target.userId, target.fullName)
                        deleteTarget = null
                    }
                ) { Text("Deactivate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    faceTarget?.let { target ->
        FaceCaptureDialog(
            engine = engine,
            mode = "ENROLL",
            onCapturedMulti = { embeddings ->
                onEnrollFace(target.id, embeddings)
                faceTarget = null
            },
            onCapturedSingle = { _ -> },
            onDismiss = { faceTarget = null }
        )
    }

    revokeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("Reset face enrolment?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This revokes ${target.fullName}'s template. They will not be able to clock in until you re-enrol them in person."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRevokeFace(target.id)
                        revokeTarget = null
                    }
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text("Cancel") }
            }
        )
    }

    payTargetStaff?.let { target ->
        val foundItem = remember(target, periods, payrollItemsMap) {
            periods.firstNotNullOfOrNull { period ->
                payrollItemsMap[period.id]?.firstOrNull {
                    it.staffProfileId == target.id || it.staffEmail.equals(target.email, ignoreCase = true)
                }
            }
        }
        val earned = target.earnedBalanceCents.takeIf { it > 0 } ?: foundItem?.grossSalaryCents ?: 0L
        val paid = target.paidBalanceCents.takeIf { it > 0 } ?: foundItem?.payment?.amountCents ?: 0L

        PayStaffModal(
            staffName = target.fullName,
            staffCode = target.staffCode,
            earnedCents = earned,
            paidCents = paid,
            onDismiss = { payTargetStaff = null },
            onConfirmPay = { amountCents, method, reference ->
                val itemId = foundItem?.id
                if (itemId != null && onRecordPayment != null) {
                    onRecordPayment(itemId, amountCents, method, reference)
                }
                payTargetStaff = null
            }
        )
    }
}
