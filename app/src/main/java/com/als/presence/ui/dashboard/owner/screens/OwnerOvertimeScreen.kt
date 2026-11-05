package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.als.presence.core.common.UiState
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
import com.als.presence.ui.dashboard.common.PresencePanel
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
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
 * Owner Overtime Console Screen.
 * Provides organization-wide management for booking assigned employee overtime,
 * viewing workforce roster schedules, and monitoring/cancelling active overtime logs.
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
    var showBookModal by remember { mutableStateOf(false) }

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
            onRefresh = { viewModel.refresh() }
        ) {
            PresencePageIntro(
                eyebrow = "Workforce Overtime",
                title = "Overtime Management",
                description = "Assign extra working hours, specify overtime pay rates, and monitor employee overtime records."
            )

            PresenceMetaStrip(
                items = listOf(
                    "Total Entries" to allOvertimeList.size.toString(),
                    "Active" to allOvertimeList.count { it.status.equals("IN_PROGRESS", ignoreCase = true) }.toString(),
                    "Scheduled" to allOvertimeList.count { it.status.equals("APPROVED", ignoreCase = true) }.toString()
                )
            )

            // Premises Selection Chip Row (as required by AGENTS.md conventions)
            if (premisesList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Premises Workspace Filter",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresenceSectionTitle(
                    title = "Overtime Roster & Logs",
                    description = "Manage workforce overtime bookings and history."
                )
                PresenceButton(
                    label = "Assign Overtime",
                    icon = Icons.Default.Add,
                    onClick = { showBookModal = true }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
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
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Workforce Roster (${premisesFilteredStaffList.size})",
                                style = MaterialTheme.typography.titleSmall,
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
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Overtime Log (${premisesFilteredOvertimeList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Tab Content
                if (selectedTab == 0) {
                    // Search Bar for Workforce Roster
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search employee by name, code or department...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
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
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredStaffList.forEach { member ->
                                        val activeCount = allOvertimeList.count { ot ->
                                            ot.staffProfileId == member.id && ot.status.equals("IN_PROGRESS", ignoreCase = true)
                                        }
                                        val scheduledCount = allOvertimeList.count { ot ->
                                            ot.staffProfileId == member.id && ot.status.equals("APPROVED", ignoreCase = true)
                                        }

                                        EmployeeOvertimeCard(
                                            member = member,
                                            activeOvertimeCount = activeCount,
                                            scheduledOvertimeCount = scheduledCount,
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf("ALL", "APPROVED", "IN_PROGRESS", "COMPLETED", "CANCELLED").forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = st },
                                label = {
                                    Text(
                                        text = when (st) {
                                            "ALL" -> "All"
                                            "APPROVED" -> "Scheduled"
                                            "IN_PROGRESS" -> "Active"
                                            "COMPLETED" -> "Completed"
                                            "CANCELLED" -> "Cancelled"
                                            else -> st
                                        },
                                        style = MaterialTheme.typography.labelSmall
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
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredOvertimeList.forEach { entry ->
                                        val staffMember = staffMap[entry.staffProfileId]
                                        OvertimeCard(
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
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showBookModal || bookingStaffMember != null) {
        BookOvertimeModal(
            staff = premisesFilteredStaffList.ifEmpty { allStaffList },
            preselectedEmployeeId = bookingStaffMember?.id ?: premisesFilteredStaffList.firstOrNull()?.id ?: allStaffList.firstOrNull()?.id,
            shiftAssignments = shiftAssignmentsList,
            shifts = shiftsList,
            onDismiss = {
                showBookModal = false
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
                showBookModal = false
                bookingStaffMember = null
            }
        )
    }
}

@Composable
private fun EmployeeOvertimeCard(
    member: StaffDto,
    activeOvertimeCount: Int,
    scheduledOvertimeCount: Int,
    onBookOvertime: () -> Unit
) {
    PresenceCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .size(48.dp)
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

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = member.staffCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!member.department.isNullOrBlank()) {
                                Text(
                                    text = member.department,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                PresenceBadge(
                    label = if (member.isActive) "Active" else "Inactive",
                    tone = if (member.isActive) PresenceTone.Success else PresenceTone.Neutral
                )
            }

            // Sleek Info Strip
            PresencePanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assigned Premises",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = member.premisesName?.ifBlank { "Main Office" } ?: "Main Office",
                            style = MaterialTheme.typography.bodySmall,
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
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Overtime Status",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                activeOvertimeCount > 0 -> "$activeOvertimeCount Active Session"
                                scheduledOvertimeCount > 0 -> "$scheduledOvertimeCount Scheduled"
                                else -> "No Active OT"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                activeOvertimeCount > 0 -> Color(0xFF2563EB)
                                scheduledOvertimeCount > 0 -> Color(0xFF059669)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PresenceButton(
                    label = "Book Overtime",
                    icon = Icons.Default.Add,
                    size = PresenceSize.Sm,
                    onClick = onBookOvertime
                )
            }
        }
    }
}

@Composable
private fun OvertimeCard(
    entry: OvertimeDto,
    staffMember: StaffDto?,
    onDelete: () -> Unit,
    isSubmitting: Boolean
) {
    PresenceCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .size(44.dp)
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

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = entry.staffName.ifBlank { entry.staffCode },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (entry.staffCode.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = entry.staffCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!staffMember?.premisesName.isNullOrBlank()) {
                                Text(
                                    text = "• ${staffMember.premisesName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                StatusPill(entry.status)
            }

            // Info Panel
            PresencePanel {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Dates.formatDate(entry.workDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = "(${Dates.formatWindow(entry.startTime, entry.endTime)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Planned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Dates.formatMinutes(entry.plannedMinutes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Pay Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(entry.paymentInr), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        Column {
                            Text("Worked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (entry.workedMinutes > 0) Dates.formatMinutes(entry.workedMinutes) else "Not recorded",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!entry.reason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${entry.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val canCancel = entry.status.equals("REQUESTED", ignoreCase = true) ||
                    entry.status.equals("APPROVED", ignoreCase = true) ||
                    entry.status.equals("IN_PROGRESS", ignoreCase = true)

            if (canCancel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PresenceButton(
                        label = "Cancel Overtime",
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
