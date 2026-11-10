package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.GpsPingSummaryDto
import com.als.presence.data.model.LocationStaffCardDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User

enum class LiveMonitorFilter {
    ALL,
    CLOCKED_IN,
    NOT_CLOCKED_IN,
    OUTSIDE,
    EXCEPTIONS
}

@Composable
fun LiveMonitorTab(
    user: User,
    premisesState: UiState<List<PremisesDto>>,
    presenceState: UiState<LocationTodayPresenceResponseDto>,
    staffState: UiState<List<StaffDto>>,
    selectedPremisesId: String?,
    isSubmitting: Boolean,
    onSelectPremises: (String) -> Unit,
    onRefresh: () -> Unit,
    onResolveGpsException: (exceptionId: String, status: String, reason: String) -> Unit = { _, _, _ -> },
    onCancelOvertime: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        when (user.role) {
            Role.OWNER -> LocationPresenceTabContent(
                user = user,
                premisesState = premisesState,
                presenceState = presenceState,
                selectedPremisesId = selectedPremisesId,
                onSelectPremises = onSelectPremises,
                onRefresh = onRefresh,
                onResolveGpsException = onResolveGpsException,
                onCancelOvertime = onCancelOvertime
            )
            Role.EMPLOYEE -> employeeLocationsTabContent(
                presenceState = presenceState,
                onRefresh = onRefresh
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Owner / Manager Locations View
// ---------------------------------------------------------------------------

@Composable
private fun LocationPresenceTabContent(
    user: User,
    premisesState: UiState<List<PremisesDto>>,
    presenceState: UiState<LocationTodayPresenceResponseDto>,
    selectedPremisesId: String?,
    onSelectPremises: (String) -> Unit,
    onRefresh: () -> Unit,
    onResolveGpsException: (exceptionId: String, status: String, reason: String) -> Unit,
    onCancelOvertime: (String) -> Unit
) {
    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LiveMonitorFilter.ALL) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time workforce attendance & GPS verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PresenceRefreshButton(
                onClick = onRefresh,
                isRefreshing = presenceState is UiState.Loading,
                contentDescription = "Refresh live monitor"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (user.role == Role.EMPLOYEE) {
            val assignedPremisesName = remember(user, premisesList, selectedPremisesId) {
                val matched = premisesList.find { it.id == (selectedPremisesId ?: user.premisesId) }
                matched?.name?.let { Dates.cleanPremisesName(it) }
                    ?: user.premisesName?.let { Dates.cleanPremisesName(it) }
                    ?.takeIf { it.isNotBlank() }
                    ?: premisesList.firstOrNull()?.name?.let { Dates.cleanPremisesName(it) }
                    ?: "Assigned Premises"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ASSIGNED PREMISES",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = assignedPremisesName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else if (premisesList.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                items(premisesList) { premises ->
                    FilterChip(
                        selected = premises.id == selectedPremisesId,
                        onClick = { onSelectPremises(premises.id) },
                        label = { Text(Dates.cleanPremisesName(premises.name)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        when (presenceState) {
            UiState.Loading -> PresenceLoadingState()
            is UiState.Error -> PresenceErrorState(message = presenceState.message, onRetry = onRefresh)
            is UiState.Success -> {
                val data = presenceState.data
                val allStaff = data.staffList

                // Summary stats
                val totalActive = data.totalActiveStaff.coerceAtLeast(allStaff.size)
                val totalClockedIn = allStaff.count { it.clockInTime != null }
                val totalOutside = allStaff.count { it.locationStatus == "Outside" }
                val totalExceptions = allStaff.count { it.exceptionId != null || it.outsidePingCount > 0 || it.consecutiveFailures > 0 }

                // Filter logic
                val filteredStaff = remember(allStaff, searchQuery, selectedFilter) {
                    allStaff.filter { card ->
                        val matchesSearch = searchQuery.isBlank() ||
                            card.fullName.contains(searchQuery, ignoreCase = true) ||
                            card.staffCode.contains(searchQuery, ignoreCase = true)

                        val matchesFilter = when (selectedFilter) {
                            LiveMonitorFilter.ALL -> true
                            LiveMonitorFilter.CLOCKED_IN -> card.clockInTime != null
                            LiveMonitorFilter.NOT_CLOCKED_IN -> card.clockInTime == null
                            LiveMonitorFilter.OUTSIDE -> card.locationStatus == "Outside"
                            LiveMonitorFilter.EXCEPTIONS -> card.exceptionId != null || card.outsidePingCount > 0 || card.consecutiveFailures > 0
                        }

                        matchesSearch && matchesFilter
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Metrics Cards Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricCard(
                            label = "Total",
                            value = "$totalActive",
                            icon = Icons.Default.Group,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            label = "Clocked In",
                            value = "$totalClockedIn",
                            icon = Icons.Default.CheckCircle,
                            color = SecondaryEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            label = "Outside",
                            value = "$totalOutside",
                            icon = Icons.Default.Place,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            label = "Alerts",
                            value = "$totalExceptions",
                            icon = Icons.Default.Warning,
                            color = TertiaryAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Search employee by name or code...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Quick Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == LiveMonitorFilter.ALL,
                                onClick = { selectedFilter = LiveMonitorFilter.ALL },
                                label = { Text("All (${allStaff.size})", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == LiveMonitorFilter.CLOCKED_IN,
                                onClick = { selectedFilter = LiveMonitorFilter.CLOCKED_IN },
                                label = { Text("Clocked In ($totalClockedIn)", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == LiveMonitorFilter.NOT_CLOCKED_IN,
                                onClick = { selectedFilter = LiveMonitorFilter.NOT_CLOCKED_IN },
                                label = { Text("Not Clocked In (${allStaff.size - totalClockedIn})", fontSize = 12.sp) }
                            )
                        }
                        if (totalOutside > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == LiveMonitorFilter.OUTSIDE,
                                    onClick = { selectedFilter = LiveMonitorFilter.OUTSIDE },
                                    label = { Text("Outside ($totalOutside)", fontSize = 12.sp) }
                                )
                            }
                        }
                        if (totalExceptions > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == LiveMonitorFilter.EXCEPTIONS,
                                    onClick = { selectedFilter = LiveMonitorFilter.EXCEPTIONS },
                                    label = { Text("Alerts ($totalExceptions)", fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    if (filteredStaff.isEmpty()) {
                        EmptyStaffPresenceState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredStaff, key = { "staff_${it.employeeId}" }) { card ->
                                DailyStaffPresenceCard(
                                    card = card,
                                    onResolveGpsException = onResolveGpsException,
                                    onCancelOvertime = onCancelOvertime
                                )
                            }
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
private fun SummaryMetricCard(
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

// ---------------------------------------------------------------------------
// Daily Staff Presence Card
// ---------------------------------------------------------------------------

@Composable
fun DailyStaffPresenceCard(
    card: LocationStaffCardDto,
    onResolveGpsException: (exceptionId: String, status: String, reason: String) -> Unit = { _, _, _ -> },
    onCancelOvertime: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingAction by remember { mutableStateOf<String?>(null) } // "ABSENT", "HALF_DAY", "CANCEL_OVERTIME"
    var reasonText by remember { mutableStateOf("") }

    val locationColor = when (card.locationStatus) {
        "Inside" -> SecondaryEmerald
        "Outside" -> Color(0xFFDC2626)
        "GPS Uncertain" -> TertiaryAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val attColor = StatusStyle.color(card.attendanceStatus)
    val isClockedIn = card.clockInTime != null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar + Name/ID + Role Badge + Location Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = card.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            val isManager = card.role.equals("MANAGER", ignoreCase = true)
                            Surface(
                                color = if (isManager) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isManager) "Manager" else "Employee",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isManager) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ID: ${card.staffCode} • ${Dates.cleanPremisesName(card.premisesName)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Geofence Location Chip
                if (isClockedIn && card.locationStatus in listOf("Inside", "Outside", "GPS Uncertain")) {
                    Surface(
                        color = locationColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(locationColor)
                            )
                            Text(
                                text = card.locationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = locationColor
                            )
                        }
                    }
                } else if (!isClockedIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Not Clocked In",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Status & Earnings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = attColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Status: ${StatusStyle.label(card.attendanceStatus)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = attColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (card.earnedAmountPaise > 0 || isClockedIn) {
                    Surface(
                        color = SecondaryEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SecondaryEmerald.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Earnings: ${Money.format(card.earnedAmountPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SecondaryEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clock-In Time & Completion Time Row
            val completedTimeText = when {
                card.clockOutTime != null -> "Completed: ${Dates.formatTime(card.clockOutTime)}"
                card.clockInTime != null && !card.regularShiftEndTime.isNullOrBlank() -> "Expected End: ${Dates.formatTime(card.regularShiftEndTime)}"
                !card.regularShiftEndTime.isNullOrBlank() && card.shiftName != "No shift assigned" -> "Scheduled End: ${Dates.formatTime(card.regularShiftEndTime)}"
                else -> null
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (card.clockInTime != null)
                        "Clock-In: ${Dates.formatTime(card.clockInTime)}"
                    else
                        "Clock-In: Not started",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (card.clockInTime != null) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (completedTimeText != null) {
                    Text(
                        text = completedTimeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (card.clockOutTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // GPS Summary Badges Row (Shown for clocked-in employees or active pings)
            if (isClockedIn || card.counts.inside > 0 || card.counts.outside > 0 || card.counts.missed > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GpsCounterBadge(
                        label = "Inside",
                        count = card.counts.inside,
                        color = SecondaryEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    GpsCounterBadge(
                        label = "Outside",
                        count = card.counts.outside,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                    GpsCounterBadge(
                        label = "Missed",
                        count = card.counts.missed,
                        color = TertiaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val sessionFinished =
                card.clockOutTime != null &&
                    (!card.hasOvertimeToday || card.overtimeClockOutTime != null)
            if (card.exceptionId != null && !sessionFinished) {
                val isOvertimeException = card.hasOvertimeToday || card.overtimeId != null
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isOvertimeException) "More than 3 Outside GPS Pings on Overtime — Action Required"
                                       else "More than 3 Outside GPS Pings — Action Required",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isOvertimeException && card.overtimeId != null) {
                            Button(
                                onClick = { pendingAction = "CANCEL_OVERTIME" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel overtime", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pendingAction = "ABSENT" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mark Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { pendingAction = "HALF_DAY" },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mark Half-Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (pendingAction != null) {
                AlertDialog(
                    onDismissRequest = { pendingAction = null },
                    title = {
                        Text(
                            text = if (pendingAction == "CANCEL_OVERTIME") "Confirm Cancel Overtime" else "Confirm Attendance Correction",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Employee: ${card.employeeName}", fontWeight = FontWeight.SemiBold)
                            Text("Shift / Work: ${card.shiftName.ifBlank { "Assigned Shift" }} (${card.dayOfWeek})")
                            Text("New Status: ${if (pendingAction == "CANCEL_OVERTIME") "CANCELLED" else pendingAction}")

                            OutlinedTextField(
                                value = reasonText,
                                onValueChange = { reasonText = it },
                                label = { Text("Reason (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val act = pendingAction
                                pendingAction = null
                                if (act == "CANCEL_OVERTIME" && card.overtimeId != null) {
                                    onCancelOvertime(card.overtimeId)
                                } else if (act != null) {
                                    onResolveGpsException(card.exceptionId ?: "", act, reasonText.ifBlank { "Manual correction by owner" })
                                }
                                reasonText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (pendingAction == "ABSENT" || pendingAction == "CANCEL_OVERTIME") Color(0xFFDC2626) else MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = if (isClockedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isClockedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (!isClockedIn) {
                        val regStart = card.regularShiftStartTime
                        val otStart = card.overtimeStartTime
                        val isOtEarlier = card.hasOvertimeToday && (
                            regStart == null ||
                            card.shiftName == "No shift assigned" ||
                            (otStart != null && otStart < regStart)
                        )
                        val hasValidShift = !regStart.isNullOrBlank() && card.shiftName != "No shift assigned"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isOtEarlier || (!hasValidShift && card.hasOvertimeToday)) MaterialTheme.colorScheme.tertiary else if (hasValidShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isOtEarlier -> "Upcoming Overtime Scheduled"
                                        hasValidShift -> "Upcoming Shift Scheduled"
                                        card.hasOvertimeToday -> "Upcoming Overtime Scheduled"
                                        else -> "No Shift Assigned"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOtEarlier || (!hasValidShift && card.hasOvertimeToday)) MaterialTheme.colorScheme.tertiary else if (hasValidShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isOtEarlier) {
                                    Text(
                                        text = "${card.dayOfWeek} • ${card.overtimeName ?: "Overtime"} (${if (card.overtimeStartTime != null && card.overtimeEndTime != null) Dates.formatWindow(card.overtimeStartTime, card.overtimeEndTime) else "Approved"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (hasValidShift) {
                                        Text(
                                            text = "Regular Shift: ${card.shiftName} (${Dates.formatWindow(card.regularShiftStartTime, card.regularShiftEndTime)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else if (hasValidShift) {
                                    Text(
                                        text = "${card.dayOfWeek} • ${card.shiftName} (${Dates.formatWindow(card.regularShiftStartTime, card.regularShiftEndTime)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (card.hasOvertimeToday) {
                                        Text(
                                            text = "OT: ${card.overtimeName ?: "Overtime"} (${if (card.overtimeStartTime != null && card.overtimeEndTime != null) Dates.formatWindow(card.overtimeStartTime, card.overtimeEndTime) else "Approved"})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else if (card.hasOvertimeToday) {
                                    Text(
                                        text = "${card.dayOfWeek} • ${card.overtimeName ?: "Overtime"} (${if (card.overtimeStartTime != null && card.overtimeEndTime != null) Dates.formatWindow(card.overtimeStartTime, card.overtimeEndTime) else "Approved"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "No active shift schedule for today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryEmerald)
                                )
                                Text(
                                    text = "Active Shift: ${card.dayOfWeek.take(3)} - ${card.shiftName} (${Dates.formatWindow(card.regularShiftStartTime, card.regularShiftEndTime)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Actual In: ${Dates.formatTime(card.clockInTime)}${if (card.clockOutTime != null) " • Out: " + Dates.formatTime(card.clockOutTime) else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryEmerald
                            )
                        }

                        if (card.hasOvertimeToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "OT: ${card.dayOfWeek.take(3)} - ${card.overtimeName ?: "Overtime"} (${if (card.overtimeStartTime != null && card.overtimeEndTime != null) Dates.formatWindow(card.overtimeStartTime, card.overtimeEndTime) else "Approved"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (card.overtimeClockInTime != null) {
                                    Text(
                                        text = "• OT In: ${Dates.formatTime(card.overtimeClockInTime)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Active GPS Checks (10m interval)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (card.latestPings.isEmpty()) {
                            Text(
                                text = "Awaiting first 10-minute GPS check...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Status",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            card.latestPings.take(3).forEachIndexed { index, ping ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                GpsPingRow(ping = ping)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsCounterBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GpsPingRow(ping: GpsPingSummaryDto) {
    val isInside = !ping.isOutside
    val timeStr = ping.pingTime.ifBlank { ping.checkTime }
    val accInt = (ping.accuracyMeters ?: 0.0).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Dates.formatTime(timeStr),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${accInt}m acc",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = if (isInside) SecondaryEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isInside) "IN GEOFENCE" else "OUTSIDE",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInside) SecondaryEmerald else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PresenceLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading real-time presence data…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PresenceErrorState(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyStaffPresenceState() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No employees match the selected criteria",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun employeeLocationsTabContent(
    presenceState: UiState<LocationTodayPresenceResponseDto>,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "My active workplace location and geofence status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (presenceState) {
            UiState.Loading -> PresenceLoadingState()
            is UiState.Error -> PresenceErrorState(message = presenceState.message, onRetry = onRefresh)
            is UiState.Success -> {
                val data = presenceState.data
                val myCard = data.staffList.firstOrNull()

                if (myCard == null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Workplace Geofence Info",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Premises: ${data.premisesName ?: "Main Workplace"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Radius: ${data.radiusMeters ?: 50} metres",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    DailyStaffPresenceCard(card = myCard)
                }
            }
            UiState.Idle -> {}
        }
    }
}
