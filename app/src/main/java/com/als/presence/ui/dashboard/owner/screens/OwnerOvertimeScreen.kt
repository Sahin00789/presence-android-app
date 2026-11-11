package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.InitialAvatar
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.owner.modals.BookOvertimeModal
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/**
 * Compact Owner Overtime Console Screen.
 * Provides organization-wide management for booking assigned employee overtime directly from employee roster cards
 * and monitoring/cancelling active overtime logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOvertimeScreen(
    onBack: () -> Unit,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val premisesState by viewModel.premisesState.collectAsStateWithLifecycle()
    val shiftAssignmentsState by viewModel.shiftAssignmentsState.collectAsStateWithLifecycle()
    val shiftTemplatesState by viewModel.shiftTemplatesState.collectAsStateWithLifecycle()
    val overtimeState by viewModel.overtimeState.collectAsStateWithLifecycle()
    val message by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadOvertime()
    }

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.clearActionMessage()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Team Roster, 1 = Overtime Log
    var selectedPremisesId by remember { mutableStateOf("ALL") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var bookingStaffMember by remember { mutableStateOf<StaffDto?>(null) }

    val allStaffList = (staffState as? UiState.Success)?.data.orEmpty()
    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()
    val allOvertimeList = (overtimeState as? UiState.Success)?.data.orEmpty()
    val shiftAssignmentsList = (shiftAssignmentsState as? UiState.Success)?.data.orEmpty()
    val shiftsList = (shiftTemplatesState as? UiState.Success)?.data.orEmpty()

    // Premises Filtering
    val premisesFilteredStaffList = remember(allStaffList, selectedPremisesId) {
        if (selectedPremisesId == "ALL") allStaffList
        else allStaffList.filter { it.premisesId == selectedPremisesId }
    }

    val filteredStaffList = remember(premisesFilteredStaffList, searchQuery) {
        if (searchQuery.isBlank()) premisesFilteredStaffList
        else {
            val q = searchQuery.trim().lowercase()
            premisesFilteredStaffList.filter { s ->
                s.fullName.lowercase().contains(q) ||
                s.staffCode.lowercase().contains(q) ||
                s.email.lowercase().contains(q) ||
                (s.department?.lowercase()?.contains(q) == true) ||
                (s.designation?.lowercase()?.contains(q) == true)
            }
        }
    }

    val premisesFilteredOvertimeList = remember(allOvertimeList, selectedPremisesId, allStaffList) {
        if (selectedPremisesId == "ALL") allOvertimeList
        else {
            val validStaffIds = allStaffList.filter { it.premisesId == selectedPremisesId }.map { it.id }.toSet()
            allOvertimeList.filter { it.staffProfileId in validStaffIds }
        }
    }

    val filteredOvertimeList = remember(premisesFilteredOvertimeList, statusFilter) {
        if (statusFilter == "ALL") premisesFilteredOvertimeList
        else premisesFilteredOvertimeList.filter { it.status.equals(statusFilter, ignoreCase = true) }
    }

    val staffMap = remember(allStaffList) {
        allStaffList.associateBy { it.id }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "Scheduling",
            title = "Overtime Console",
            onBack = onBack,
            actions = {
                PresenceButton(
                    label = "+ Book OT",
                    icon = Icons.Default.Add,
                    size = PresenceSize.Sm,
                    onClick = {
                        val defaultStaff = premisesFilteredStaffList.firstOrNull() ?: allStaffList.firstOrNull()
                        if (defaultStaff != null) {
                            bookingStaffMember = defaultStaff
                        }
                    }
                )
            },
            onRefresh = { viewModel.refresh() }
        ) {

            // Premises Selection Chips (only if multiple premises)
            if (premisesList.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPremisesId == "ALL",
                        onClick = { selectedPremisesId = "ALL" },
                        label = { Text("All Premises (${allStaffList.size})") },
                        leadingIcon = if (selectedPremisesId == "ALL") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    premisesList.forEach { p ->
                        val count = allStaffList.count { it.premisesId == p.id }
                        FilterChip(
                            selected = selectedPremisesId == p.id,
                            onClick = { selectedPremisesId = p.id },
                            label = { Text("${p.name} ($count)") },
                            leadingIcon = if (selectedPremisesId == p.id) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                indicator = {},
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Roster (${premisesFilteredStaffList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Logs (${premisesFilteredOvertimeList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Tab Content
            if (selectedTab == 0) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search employee by name, code or department...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                when (staffState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading workforce…")
                    is UiState.Error -> SectionError((staffState as UiState.Error).message)
                    is UiState.Success -> {
                        if (filteredStaffList.isEmpty()) {
                            SectionEmpty(if (searchQuery.isNotBlank()) "No employees match \"$searchQuery\"." else "No employees found.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredStaffList.forEach { member ->
                                    val memberOvertimes = allOvertimeList.filter { it.staffProfileId == member.id }

                                    CompactEmployeeOvertimeCard(
                                        member = member,
                                        employeeOvertimes = memberOvertimes,
                                        onBookOvertime = { bookingStaffMember = member }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Status Filter Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf("ALL", "CREATED", "IN_PROGRESS", "COMPLETED", "CANCELLED").forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = {
                                Text(
                                    text = when (st) {
                                        "ALL" -> "All"
                                        "CREATED" -> "Scheduled"
                                        "APPROVED" -> "Scheduled"
                                        "IN_PROGRESS" -> "Active"
                                        "COMPLETED" -> "Completed"
                                        "CANCELLED" -> "Cancelled"
                                        else -> st
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                when (overtimeState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading overtime entries…")
                    is UiState.Error -> SectionError((overtimeState as UiState.Error).message)
                    is UiState.Success -> {
                        if (filteredOvertimeList.isEmpty()) {
                            SectionEmpty("No overtime entries match selected filter.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredOvertimeList.forEach { entry ->
                                    val staffMember = staffMap[entry.staffProfileId]
                                    CompactOvertimeCard(
                                        entry = entry,
                                        staffMember = staffMember,
                                        onDelete = { viewModel.cancelOvertime(entry.id) },
                                        isSubmitting = isSubmitting
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    val currentBookingTarget = bookingStaffMember
    if (currentBookingTarget != null) {
        BookOvertimeModal(
            staff = premisesFilteredStaffList.ifEmpty { allStaffList },
            preselectedEmployeeId = currentBookingTarget.id,
            shiftAssignments = shiftAssignmentsList,
            shifts = shiftsList,
            onDismiss = {
                bookingStaffMember = null
            },
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
                bookingStaffMember = null
            }
        )
    }
}

/**
 * High-density compact card for employee roster overtime booking with rich overtime statistics summary.
 */
@Composable
private fun CompactEmployeeOvertimeCard(
    member: StaffDto,
    employeeOvertimes: List<OvertimeDto>,
    onBookOvertime: () -> Unit
) {
    val activeCount = remember(employeeOvertimes) {
        employeeOvertimes.count { it.status.equals("IN_PROGRESS", ignoreCase = true) }
    }
    val scheduledCount = remember(employeeOvertimes) {
        employeeOvertimes.count { it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("REQUESTED", ignoreCase = true) }
    }
    val completedCount = remember(employeeOvertimes) {
        employeeOvertimes.count { it.status.equals("COMPLETED", ignoreCase = true) }
    }
    val totalOtHours = remember(employeeOvertimes) {
        val totalMins = employeeOvertimes.filter { !it.status.equals("CANCELLED", ignoreCase = true) }
            .sumOf { if (it.workedMinutes > 0) it.workedMinutes else it.plannedMinutes }
        totalMins / 60.0
    }
    val totalEarned = remember(employeeOvertimes) {
        employeeOvertimes.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            .sumOf { it.paymentInr }
    }

    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Avatar + Name + Staff Code | Inline "+ Book OT" Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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

                    Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = member.staffCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = member.designation ?: member.department ?: member.premisesName ?: "Assigned Employee",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = PresenceDesign.LabelText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        PresenceButton(
            label = "+ Book OT",
            icon = Icons.Default.Add,
            size = PresenceSize.Sm,
            onClick = onBookOvertime
        )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

    // Overtime Summary Strip: Status Badges + Total OT Hours & Earnings
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Badges
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (activeCount > 0) {
                PresenceBadge(
                    label = "$activeCount Active OT",
                    tone = PresenceTone.Success,
                    dot = true
                )
            }
            if (scheduledCount > 0) {
                PresenceBadge(
                    label = "$scheduledCount Scheduled",
                    tone = PresenceTone.Info
                )
            }
            if (activeCount == 0 && scheduledCount == 0) {
                PresenceBadge(
                    label = if (completedCount > 0) "$completedCount Completed" else "No Active OT",
                    tone = PresenceTone.Neutral
                )
            }
        }

        // Hours & Earnings Summary
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%.1f hrs OT", totalOtHours),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (totalEarned > 0) {
                Text(
                    text = "• ${Money.format(totalEarned)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
}
}

/**
 * High-density compact card for overtime log records.
 */
@Composable
private fun CompactOvertimeCard(
    entry: OvertimeDto,
    staffMember: StaffDto?,
    onDelete: () -> Unit,
    isSubmitting: Boolean
) {
    val canCancel = entry.status.equals("REQUESTED", ignoreCase = true) ||
            entry.status.equals("APPROVED", ignoreCase = true) ||
            entry.status.equals("IN_PROGRESS", ignoreCase = true)

    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Avatar + Name + Staff Code | Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!staffMember?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = staffMember.avatarUrl,
                                contentDescription = entry.staffName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            InitialAvatar(name = entry.staffName.ifBlank { entry.staffCode })
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.staffName.ifBlank { entry.staffCode },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresenceDesign.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Code: ${entry.staffCode.ifBlank { "—" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = PresenceDesign.LabelText
                        )
                    }
                }

                StatusPill(entry.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Details Row: Date & Time Window | Planned & Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${Dates.formatDate(entry.workDate)} • ${Dates.formatWindow(entry.startTime, entry.endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = PresenceDesign.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${Dates.formatMinutes(entry.plannedMinutes)} • ${Money.format(entry.paymentInr)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!entry.reason.isNullOrBlank() || canCancel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!entry.reason.isNullOrBlank()) {
                        Text(
                            text = "Note: ${entry.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = PresenceDesign.LabelText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (canCancel) {
                        PresenceButton(
                            label = "Cancel",
                            icon = Icons.Default.Cancel,
                            size = PresenceSize.Sm,
                            variant = PresenceVariant.Danger,
                            onClick = onDelete,
                            enabled = !isSubmitting
                        )
                    }
                }
            }
        }
    }
}
