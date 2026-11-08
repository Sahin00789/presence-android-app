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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun LocationsTab(
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Workplace Locations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Daily employee presence & GPS monitoring",
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

        Spacer(modifier = Modifier.height(12.dp))

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
                    .padding(bottom = 12.dp),
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
                    .padding(bottom = 12.dp)
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
                val staffMembers = data.staffList

                if (staffMembers.isEmpty()) {
                    EmptyStaffPresenceState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            LocationSectionHeader(
                                title = "Employees",
                                count = staffMembers.size,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        items(staffMembers, key = { "staff_${it.employeeId}" }) { card ->
                            DailyStaffPresenceCard(
                                card = card,
                                onResolveGpsException = onResolveGpsException,
                                onCancelOvertime = onCancelOvertime
                            )
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
private fun LocationSectionHeader(
    title: String,
    count: Int,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
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

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar + Name/ID + Role Badge + Single Location Badge
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

                // Geofence Location Chip (Only shown when active)
                if (card.locationStatus != "GPS Unavailable") {
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Status, Status Info & Earnings Bar
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

                // Earned Salary Badge
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

            Spacer(modifier = Modifier.height(8.dp))

            // Clock-In Time & Completion Time Row
            val completedTimeText = when {
                card.clockOutTime != null -> "Completed: ${Dates.formatTime(card.clockOutTime)}"
                card.clockInTime != null && !card.regularShiftEndTime.isNullOrBlank() -> "Expected End: ${Dates.formatTime(card.regularShiftEndTime)}"
                !card.regularShiftEndTime.isNullOrBlank() -> "Scheduled End: ${Dates.formatTime(card.regularShiftEndTime)}"
                else -> "End: —"
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

                Text(
                    text = completedTimeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (card.clockOutTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GPS Summary Badges Row
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

            // GPS Exception Action Banner. The buttons remain available until the
            // active shift/overtime session is completely finished.
            val sessionFinished =
                card.clockOutTime != null &&
                    (!card.hasOvertimeToday || card.overtimeClockOutTime != null)
            if (card.exceptionId != null && !sessionFinished) {
                val exceptionId = card.exceptionId
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
                                text = if (isOvertimeException) "More than 3 Outside GPS Pings on Overtime — Owner/Manager Action Required"
                                       else "More than 3 Outside GPS Pings — Owner/Manager Action Required",
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
                            // Resolution Action Buttons (Absent, Half-Day)
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

            // Lower Container:
            // - If not clocked in: "Upcoming Shift Scheduled" card
            // - If clocked in: "Active Shift" container with clock-in details & 10-minute GPS pings table
            Surface(
                color = if (card.clockInTime != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (card.clockInTime != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (card.clockInTime == null) {
                        // Not Clocked In -> Upcoming Shift / Overtime Card
                        val regStart = card.regularShiftStartTime
                        val otStart = card.overtimeStartTime
                        val isOtEarlier = card.hasOvertimeToday && (
                            regStart == null ||
                            card.shiftName == "No shift assigned" ||
                            (otStart != null && otStart < regStart)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isOtEarlier) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isOtEarlier) "Upcoming Overtime Scheduled" else "Upcoming Shift Scheduled",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOtEarlier) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
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
                                    if (card.regularShiftStartTime != null && card.shiftName != "No shift assigned") {
                                        Text(
                                            text = "Regular Shift: ${card.shiftName} (${Dates.formatWindow(card.regularShiftStartTime, card.regularShiftEndTime)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
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
                                }
                            }
                        }
                    } else {
                        // Clocked In -> Active Shift Container with GPS Pings
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
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
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

                        // GPS Pings start here!
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun GpsPingRow(ping: GpsPingSummaryDto) {
    val statusColor = when (ping.status) {
        "Inside" -> SecondaryEmerald
        "Outside" -> Color(0xFFDC2626)
        "Uncertain" -> TertiaryAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Dates.formatTime(ping.checkTime),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (ping.distanceMeters != null) "${ping.distanceMeters.toInt()} m" else "No distance",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ping.status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// Staff Locations View
// ---------------------------------------------------------------------------

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
                    text = "My Workplace Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Geofence boundaries & monitoring status",
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
                val myCard = data.staff.firstOrNull()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Dates.cleanPremisesName(data.premisesName),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!data.address.isNullOrBlank()) {
                                    Text(
                                        text = data.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Geofence Radius: ${data.radiusMeters ?: 50} meters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (data.latitude != null && data.longitude != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Coordinates: ${data.latitude}, ${data.longitude}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (myCard != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Geofence Status",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        color = when (myCard.locationStatus) {
                                            "Inside" -> SecondaryEmerald.copy(alpha = 0.15f)
                                            "Outside" -> Color(0xFFDC2626).copy(alpha = 0.15f)
                                            else -> TertiaryAmber.copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = myCard.locationStatus,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = when (myCard.locationStatus) {
                                                "Inside" -> SecondaryEmerald
                                                "Outside" -> Color(0xFFDC2626)
                                                else -> TertiaryAmber
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "During active shifts, location checks are recorded approximately every 10 minutes to verify geofence presence. Ensure GPS location services are enabled on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers & Loading States
// ---------------------------------------------------------------------------

@Composable
private fun PresenceLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading location & employee presence...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PresenceErrorState(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyStaffPresenceState() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No employees assigned to this location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
