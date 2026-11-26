package com.als.presence.ui.dashboard.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.LocationStaffCardDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.components.PresenceAvatar
import com.als.presence.ui.dashboard.common.components.PresenceBadge
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceEmptyState
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.common.components.PresenceSectionHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class LiveMonitorFilter {
    ALL,
    CLOCKED_IN,
    NOT_CLOCKED_IN,
    OUTSIDE,
    EXCEPTIONS
}

/**
 * Editorial, 2026 Android Product Redesign for Live Monitor & Pings Tab.
 * Features:
 * - Selected date telemetry header
 * - Interactive premises selector
 * - 4 high-contrast operational metric tiles (Inside, Outside, Missed, Working Now)
 * - Visual ping distribution proportional bar
 * - Employee-wise ping directory (structured list rows, NOT repetitive cards!)
 * - Collapsible verification activity timeline
 * - Failed ping alert details with immediate manual attendance resolution actions
 */
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
    Box(modifier = Modifier.fillMaxSize()) {
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
    val screenInfo = LocalScreenSizeInfo.current
    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LiveMonitorFilter.ALL) }

    val todayStr = remember {
        val now = LocalDate.now()
        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (screenInfo.isTabletOrFoldable) 720.dp else Dp.Unspecified)
                .fillMaxSize()
                .padding(
                    horizontal = screenInfo.horizontalScreenPadding,
                    vertical = if (screenInfo.isVeryCompact) 10.dp else 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------------------------------------------------------------
            // 1. DATE TELEMETRY & REFRESH HEADER
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Location Pings & Telemetry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = todayStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. PREMISES SELECTOR FILTER STRIP
            // -------------------------------------------------------------
            if (premisesList.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(premisesList) { premises ->
                        val isSelected = premises.id == selectedPremisesId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPremises(premises.id) },
                            label = { Text(Dates.cleanPremisesName(premises.name)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
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
                UiState.Loading, UiState.Idle -> PresenceLoadingState()
                is UiState.Error -> PresenceErrorState(message = presenceState.message, onRetry = onRefresh)
                is UiState.Success -> {
                    val data = presenceState.data
                    val allStaff = data.staffList

                    val totalClockedIn = allStaff.count { it.clockInTime != null }
                    val totalNotClockedIn = allStaff.count { it.clockInTime == null }
                    val insidePingsCount = allStaff.sumOf { it.counts.inside }
                    val outsidePingsCount = allStaff.sumOf { it.counts.outside }
                    val missedPingsCount = allStaff.sumOf { it.counts.missed }
                    val totalPings = (insidePingsCount + outsidePingsCount + missedPingsCount).coerceAtLeast(1)
                    val totalExceptionsCount = allStaff.count { it.exceptionId != null || it.outsidePingCount > 3 }
                    val totalOutsideCount = allStaff.count { it.locationStatus == "Outside" || it.outsidePingCount > 0 }

                    val filteredStaff = allStaff.filter { card ->
                        val matchesSearch = searchQuery.isBlank() || card.fullName.contains(searchQuery, ignoreCase = true)
                        val matchesFilter = when (selectedFilter) {
                            LiveMonitorFilter.ALL -> true
                            LiveMonitorFilter.CLOCKED_IN -> card.clockInTime != null
                            LiveMonitorFilter.NOT_CLOCKED_IN -> card.clockInTime == null
                            LiveMonitorFilter.OUTSIDE -> card.locationStatus == "Outside" || card.outsidePingCount > 0
                            LiveMonitorFilter.EXCEPTIONS -> card.exceptionId != null || card.outsidePingCount > 3
                        }
                        matchesSearch && matchesFilter
                    }

                    // -------------------------------------------------------------
                    // 3. OPERATIONAL METRIC TILES & VISUAL PING DISTRIBUTION
                    // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 4 Metric Tiles in Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TelemetryMetricTile(
                                    label = "Inside",
                                    value = "$insidePingsCount",
                                    color = PresenceDesign.Positive,
                                    bg = PresenceDesign.PositiveSoft,
                                    modifier = Modifier.weight(1f)
                                )
                                TelemetryMetricTile(
                                    label = "Outside",
                                    value = "$outsidePingsCount",
                                    color = if (outsidePingsCount > 0) PresenceDesign.DangerText else MaterialTheme.colorScheme.onSurfaceVariant,
                                    bg = if (outsidePingsCount > 0) PresenceDesign.DangerSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.weight(1f)
                                )
                                TelemetryMetricTile(
                                    label = "Missed",
                                    value = "$missedPingsCount",
                                    color = if (missedPingsCount > 0) PresenceDesign.CautionText else MaterialTheme.colorScheme.onSurfaceVariant,
                                    bg = if (missedPingsCount > 0) PresenceDesign.CautionSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.weight(1f)
                                )
                                TelemetryMetricTile(
                                    label = "Working",
                                    value = "$totalClockedIn",
                                    color = MaterialTheme.colorScheme.primary,
                                    bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Visual Ping Distribution Bar
                            if (totalPings > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val insideRatio = (insidePingsCount.toFloat() / totalPings).coerceIn(0f, 1f)
                                val outsideRatio = (outsidePingsCount.toFloat() / totalPings).coerceIn(0f, 1f)
                                val missedRatio = (missedPingsCount.toFloat() / totalPings).coerceIn(0f, 1f)

                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (insideRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(insideRatio)
                                                    .height(6.dp)
                                                    .background(PresenceDesign.Positive)
                                            )
                                        }
                                        if (outsideRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(outsideRatio)
                                                    .height(6.dp)
                                                    .background(PresenceDesign.DangerText)
                                            )
                                        }
                                        if (missedRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(missedRatio)
                                                    .height(6.dp)
                                                    .background(PresenceDesign.CautionText)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Verification Success: ${(insideRatio * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PresenceDesign.Positive
                                        )
                                        Text(
                                            text = "Total $totalPings GPS Checks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // 4. FILTER CONTROLS & SEARCH
                    // -------------------------------------------------------------
                    PresenceSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search staff by name or code…"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == LiveMonitorFilter.ALL,
                                onClick = { selectedFilter = LiveMonitorFilter.ALL },
                                label = { Text("All (${allStaff.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == LiveMonitorFilter.CLOCKED_IN,
                                onClick = { selectedFilter = LiveMonitorFilter.CLOCKED_IN },
                                label = { Text("Clocked In ($totalClockedIn)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = PresenceDesign.Positive
                                    )
                                }
                            )
                        }
                        if (totalOutsideCount > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == LiveMonitorFilter.OUTSIDE,
                                    onClick = { selectedFilter = LiveMonitorFilter.OUTSIDE },
                                    label = { Text("Outside ($totalOutsideCount)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = PresenceDesign.DangerText
                                        )
                                    }
                                )
                            }
                        }
                        if (totalExceptionsCount > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == LiveMonitorFilter.EXCEPTIONS,
                                    onClick = { selectedFilter = LiveMonitorFilter.EXCEPTIONS },
                                    label = { Text("Alerts ($totalExceptionsCount)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = PresenceDesign.DangerText
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // 5. EMPLOYEE-WISE PING DIRECTORY (STRUCTURED ROWS, NOT CARDS!)
                    // -------------------------------------------------------------
                    if (filteredStaff.isEmpty()) {
                        PresenceEmptyState(
                            title = "No Staff Activity",
                            description = "No staff members match the selected filter criteria."
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            tonalElevation = 1.dp
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredStaff) { card ->
                                    StructuredLiveStaffRow(
                                        card = card,
                                        onResolveGpsException = onResolveGpsException
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricTile(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Compact, structured monitoring row for each staff member.
 * Displays avatar with status beacon, name, shift name, clock-in time,
 * ping count pill, and expandable activity timeline with resolution actions.
 */
@Composable
private fun StructuredLiveStaffRow(
    card: LocationStaffCardDto,
    onResolveGpsException: (exceptionId: String, status: String, reason: String) -> Unit
) {
    val isException = card.exceptionId != null || card.outsidePingCount > 3
    val isLive = card.clockInTime != null
    var showTimeline by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    PresenceAvatar(
                        name = card.fullName,
                        size = 40.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(1.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isException -> PresenceDesign.DangerText
                                        isLive -> PresenceDesign.Positive
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val shiftLabel = if (card.shiftName.isNotBlank()) card.shiftName else "No Shift Assigned"
                    val inTimeLabel = if (card.clockInTime != null) " • In: ${Dates.formatTime(card.clockInTime)}" else ""
                    Text(
                        text = "$shiftLabel$inTimeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Status Badge
            PresenceBadge(
                text = when {
                    card.exceptionId != null -> "ALERT"
                    card.clockInTime != null -> "INSIDE"
                    else -> "OFFLINE"
                },
                tone = when {
                    card.exceptionId != null -> PresenceTone.Danger
                    card.clockInTime != null -> PresenceTone.Success
                    else -> PresenceTone.Neutral
                },
                showLiveBeacon = isLive || isException
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Telemetry counters strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${card.counts.inside} In • ${card.counts.outside} Out • ${card.counts.missed} Missed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Timeline toggle
            if (card.latestPings.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { showTimeline = !showTimeline }
                ) {
                    Text(
                        text = if (showTimeline) "Hide Timeline" else "View Timeline",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showTimeline) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Expanded Recent Timeline
        AnimatedVisibility(visible = showTimeline) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                card.latestPings.take(3).forEach { ping ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Dates.formatTime(ping.effectiveTime),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (ping.effectiveIsOutside) "Outside (${ping.distanceMeters?.toInt() ?: 0}m)" else "Inside Premises",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (ping.effectiveIsOutside) PresenceDesign.DangerText else PresenceDesign.Positive
                        )
                    }
                }
            }
        }

        // Immediate Exception Resolution Action Buttons (Rule: outside count > 3)
        if (isException) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Manual Attendance Resolution Required:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PresenceDesign.DangerText
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresenceButton(
                    text = "Mark Absent",
                    onClick = {
                        card.exceptionId?.let { id -> onResolveGpsException(id, "ABSENT", "Owner manual action") }
                    },
                    variant = PresenceVariant.Danger,
                    size = com.als.presence.ui.dashboard.common.PresenceSize.Sm,
                    modifier = Modifier.weight(1f)
                )

                PresenceButton(
                    text = "Half Day",
                    onClick = {
                        card.exceptionId?.let { id -> onResolveGpsException(id, "HALF_DAY", "Owner manual action") }
                    },
                    variant = PresenceVariant.Soft,
                    size = com.als.presence.ui.dashboard.common.PresenceSize.Sm,
                    modifier = Modifier.weight(1f)
                )

                PresenceButton(
                    text = "Leave",
                    onClick = {
                        card.exceptionId?.let { id -> onResolveGpsException(id, "LEAVE", "Owner manual action") }
                    },
                    variant = PresenceVariant.Secondary,
                    size = com.als.presence.ui.dashboard.common.PresenceSize.Sm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun employeeLocationsTabContent(
    presenceState: UiState<LocationTodayPresenceResponseDto>,
    onRefresh: () -> Unit
) {
    val screenInfo = LocalScreenSizeInfo.current
    val data = (presenceState as? UiState.Success)?.data
    val card = data?.staffList?.firstOrNull()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (screenInfo.isTabletOrFoldable) 720.dp else Dp.Unspecified)
                .fillMaxSize()
                .padding(
                    horizontal = screenInfo.horizontalScreenPadding,
                    vertical = if (screenInfo.isVeryCompact) 10.dp else 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Location Telemetry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active shift periodic GPS verification status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (card != null) {
                StructuredLiveStaffRow(
                    card = card,
                    onResolveGpsException = { _, _, _ -> }
                )
            } else {
                PresenceEmptyState(
                    title = "No Active Session",
                    description = "Active shifts automatically stream GPS location checks every ~10 minutes."
                )
            }
        }
    }
}

@Composable
private fun PresenceLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Updating live location telemetry…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PresenceErrorState(message: String, onRetry: () -> Unit) {
    PresenceEmptyState(
        title = "Monitoring Error",
        description = message,
        actionLabel = "Retry",
        onAction = onRetry
    )
}
