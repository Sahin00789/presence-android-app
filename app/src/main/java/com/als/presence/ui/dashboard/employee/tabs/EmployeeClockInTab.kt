package com.als.presence.ui.dashboard.employee.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.SessionKind
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeClockInViewModel

private data class HistoryItem(
    val title: String,
    val isOvertime: Boolean,
    val scheduledWindow: String,
    val actualStart: String,
    val actualEnd: String,
    val durationText: String,
    val status: String,
    val earnedInr: Long,
    val completionTimestamp: String
)

@Composable
private fun PayRuleChip(
    modifier: Modifier = Modifier,
    label: String,
    amountText: String,
    isSelected: Boolean,
    activeBgColor: Color,
    activeTextColor: Color,
    activeBorderColor: Color
) {
    Surface(
        color = if (isSelected) activeBgColor else Color(0xFFF9FAFB),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) activeBorderColor else Color(0xFFE5E7EB)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(activeTextColor)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeTextColor else Color(0xFF4B5563),
                    maxLines = 1
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) activeTextColor else Color(0xFF1F2937),
                maxLines = 1
            )
        }
    }
}

/**
 * Compact Employee Clock In Screen conforming strictly to the pixel-exact visual reference & spec.
 */
@Composable
fun EmployeeClockInTab(
    user: User,
    state: EmployeeClockInViewModel.ClockInUiState,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onClockIn: (overtimeId: String?) -> Unit,
    onClockOut: () -> Unit = {},
    engine: FaceEmbeddingEngine,
    onFaceCaptured: (FaceEmbeddingEngine.Embedding) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    onSilentRefresh: () -> Unit = onRefresh
) {
    val scrollState = rememberScrollState()
    var showCapture by remember { mutableStateOf(false) }

    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier

    // Premises Data
    val premisesName = state.premisesName ?: user.premisesName?.let { Dates.cleanPremisesName(it) }?.takeIf { it.isNotBlank() } ?: "Main Office"
    val userLat = state.location?.latitude
    val userLng = state.location?.longitude
    val premLat = state.premisesLatitude
    val premLng = state.premisesLongitude
    val distanceMeters = state.distanceToPremisesMeters?.toInt()
    val allowedRadiusMeters = state.premisesRadiusMeters
    val accuracyMeters = state.location?.accuracyMeters?.toInt() ?: 8
    val lastVerifiedTime = state.lastGpsVerifiedTime ?: "Just now"

    val isGeofenceValid = state.isWithinGeofence ?: (distanceMeters != null && distanceMeters <= allowedRadiusMeters)

    // Active Session & Clock-In check
    val active = state.activeEvent
    val isClockedIn = active != null && !active.clockIn.isNullOrBlank() && active.clockOut == null
    val isRunningShift = isClockedIn && active?.overtimeId == null
    val isRunningOvertime = isClockedIn && active?.overtimeId != null

    // Determine Single Next-Work Card
    val nextSession = state.nextSession
    val todayOvertime = state.openOvertime.firstOrNull { Dates.isToday(it.workDate) && (it.status == "APPROVED" || it.status == "IN_PROGRESS") }

    val todayIso = Dates.todayIso()
    val todayHoliday = state.holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
    val isTodayHoliday = todayHoliday != null

    val hasMatchingShiftToday = state.assignedShifts.any { a ->
        val template = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
        template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
    }
    val isTodayOffDay = !isTodayHoliday && !hasMatchingShiftToday
    val isNonWorkingDay = isTodayHoliday || isTodayOffDay
    val hasTodayOvertime = isRunningOvertime || todayOvertime != null || nextSession?.kind == SessionKind.OVERTIME || nextSession?.overtimeId != null

    val showOffDayHolidayGreeting = isNonWorkingDay && !hasTodayOvertime && !isClockedIn

    val hasScheduledWork = isClockedIn || nextSession != null || todayOvertime != null

    val rawTitle = when {
        isRunningOvertime -> "Overtime Session"
        isRunningShift -> active?.shiftName?.ifBlank { "Assigned Shift" } ?: "Assigned Shift"
        todayOvertime != null -> todayOvertime.reason?.takeIf { it.isNotBlank() } ?: "Overtime"
        nextSession != null && nextSession.kind == SessionKind.OVERTIME -> nextSession.title.ifBlank { "Overtime" }
        nextSession != null && !nextSession.title.equals("day", ignoreCase = true) && nextSession.title.isNotBlank() -> nextSession.title
        else -> "No Shift Scheduled"
    }
    val workTitle = if (rawTitle.equals("day", ignoreCase = true) || rawTitle.isBlank()) "Assigned Shift" else rawTitle

    val isOvertimeWork = isRunningOvertime || (!isClockedIn && (todayOvertime != null || nextSession?.kind == SessionKind.OVERTIME || nextSession?.overtimeId != null))

    val startTimeRaw = when {
        isRunningOvertime -> active?.assignedStartTime ?: todayOvertime?.startTime ?: nextSession?.startTime
        isRunningShift -> active?.assignedStartTime ?: nextSession?.startTime
        todayOvertime != null -> todayOvertime.startTime
        nextSession != null -> nextSession.startTime
        else -> null
    }

    val endTimeRaw = when {
        isRunningOvertime -> active?.assignedEndTime ?: todayOvertime?.endTime ?: nextSession?.endTime
        isRunningShift -> active?.assignedEndTime ?: nextSession?.endTime
        todayOvertime != null -> todayOvertime.endTime
        nextSession != null -> nextSession.endTime
        else -> null
    }

    val isTimeWindowValid = if (hasScheduledWork) Dates.isWithinTimeWindow(startTimeRaw, endTimeRaw) else false
    val canClockIn = !isClockedIn && hasScheduledWork && isGeofenceValid && isTimeWindowValid && !state.isSubmitting

    // Pay Rules calculation for the active/upcoming shift
    val currentAssignment = state.assignedShifts.firstOrNull {
        (nextSession?.title != null && it.shiftName != null && it.shiftName.equals(nextSession.title, ignoreCase = true)) ||
        (active?.shiftName != null && it.shiftName != null && it.shiftName.equals(active.shiftName, ignoreCase = true)) ||
        (it.shiftName != null && it.shiftName.equals(workTitle, ignoreCase = true))
    } ?: state.assignedShifts.firstOrNull()

    val presentPayAmount = currentAssignment?.presentPay?.takeIf { it > 0 } ?: 600.0
    val halfDayPayAmount = currentAssignment?.halfDayPay?.takeIf { it > 0 } ?: (presentPayAmount / 2.0)
    val leavePayAmount = currentAssignment?.leavePay?.takeIf { it >= 0 } ?: presentPayAmount
    val absentPayAmount = currentAssignment?.absentPay ?: 0.0

    val presentPayInr = presentPayAmount.toLong()
    val halfDayPayInr = halfDayPayAmount.toLong()
    val leavePayInr = leavePayAmount.toLong()
    val absentPayInr = absentPayAmount.toLong()

    val activeOvertime = state.openOvertime.firstOrNull { it.id == active?.overtimeId || it.id == nextSession?.overtimeId } ?: todayOvertime
    val overtimePayInr = activeOvertime?.paymentInr ?: 0L

    // Late Minutes & Clock-in Time Calculation
    val clockInTimeStr = if (isClockedIn) active?.clockIn else null
    val calculatedLateMinutes = when {
        isClockedIn && active?.lateMinutes != null && active.lateMinutes > 0 -> active.lateMinutes
        isClockedIn && !clockInTimeStr.isNullOrBlank() -> Dates.calculateLateMinutes(clockInTimeStr, startTimeRaw)
        else -> 0
    }

    val clockInDisplay = if (isClockedIn && !clockInTimeStr.isNullOrBlank()) {
        Dates.formatTime(clockInTimeStr)
    } else {
        "Not Clocked In"
    }

    val lateMinutesDisplay = when {
        calculatedLateMinutes > 0 && isClockedIn -> "$calculatedLateMinutes mins late"
        isClockedIn -> "0 mins late (On time)"
        else -> "Not calculated"
    }

    // Active attendance status
    val currentStatus = when {
        !isClockedIn -> "NOT_CLOCKED_IN"
        active?.status?.equals("HALF_DAY", ignoreCase = true) == true -> "HALF_DAY"
        active?.status?.equals("LEAVE", ignoreCase = true) == true -> "LEAVE"
        active?.status?.equals("ABSENT", ignoreCase = true) == true -> "ABSENT"
        else -> "PRESENT"
    }

    // Active Card Details (GPS monitoring is active ONLY after clock-in)
    val staffCard = state.activeStaffCard
    val insideCount = if (isClockedIn) (staffCard?.counts?.inside ?: 0) else 0
    val outsideCount = if (isClockedIn) (staffCard?.counts?.outside ?: 0) else 0
    val missedCount = if (isClockedIn) (staffCard?.counts?.missed ?: 0) else 0
    val consecutiveFailures = if (isClockedIn) (staffCard?.consecutiveFailures ?: 0) else 0
    val lastPings = if (isClockedIn) staffCard?.latestPings.orEmpty().take(3) else emptyList()

    // Periodically silent refresh when active
    LaunchedEffect(isClockedIn) {
        if (isClockedIn) {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                onSilentRefresh()
            }
        }
    }

    // Unified Completed Work History
    val completedShifts = state.todayRecords
        .filter { it.clockOut != null }
        .map { record ->
            val matchingAssignment = state.assignedShifts.firstOrNull { it.shiftName == record.shiftName }
            val rate = matchingAssignment?.presentPay ?: 600.0
            val rateInr = rate.toLong()
            HistoryItem(
                title = record.shiftName?.ifBlank { "Assigned Shift" } ?: "Assigned Shift",
                isOvertime = record.overtimeId != null,
                scheduledWindow = Dates.formatWindow(record.assignedStartTime, record.assignedEndTime),
                actualStart = Dates.formatTime(record.clockIn),
                actualEnd = Dates.formatTime(record.clockOut),
                durationText = Dates.formatMinutes(record.workedMinutes),
                status = record.status.ifBlank { "Present" },
                earnedInr = rateInr,
                completionTimestamp = record.clockOut ?: record.workDate
            )
        }

    val completedOvertime = state.openOvertime
        .filter { (it.status == "COMPLETED" || it.status == "PAID") && Dates.isToday(it.workDate) }
        .map { ot ->
            HistoryItem(
                title = "Overtime",
                isOvertime = true,
                scheduledWindow = Dates.formatWindow(ot.startTime, ot.endTime),
                actualStart = Dates.formatTime(ot.startTime),
                actualEnd = Dates.formatTime(ot.endTime),
                durationText = Dates.formatMinutes(ot.workedMinutes.takeIf { it > 0 } ?: ot.plannedMinutes),
                status = "Completed",
                earnedInr = ot.paymentInr,
                completionTimestamp = ot.resolvedAt ?: ot.createdAt
            )
        }

    val unifiedHistory = (completedShifts + completedOvertime)
        .sortedByDescending { it.completionTimestamp }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) 12.dp else 0.dp)
            .padding(top = if (isScrollable) 10.dp else 0.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SCREEN COMPACT TITLE & SUBTITLE
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Clock In",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF171A34)
            )
            Text(
                text = "Verify your location, then clock in for your shift.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF747895)
            )
        }

        // =====================================================================
        // SECTION A: ASSIGNED PREMISES CARD
        // =====================================================================
        PresenceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Combined Premises Header Row: Icon + Premises Name & Subtitle + Inside/Outside Badge
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8EEFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = premisesName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF171A34),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Assigned premises",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF747895),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = if (isGeofenceValid) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isGeofenceValid) "Inside premises" else "Outside premises",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGeofenceValid) Color(0xFF137333) else Color(0xFFC5221F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Coordinates & Distance Grid Surface Container
                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Your coordinates", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                                Text(
                                    text = if (userLat != null && userLng != null) "${String.format(java.util.Locale.US, "%.5f", userLat)}, ${String.format(java.util.Locale.US, "%.5f", userLng)}" else "GPS Pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171A34)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Premises coordinates", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                                Text(
                                    text = if (premLat != null && premLng != null) "${String.format(java.util.Locale.US, "%.5f", premLat)}, ${String.format(java.util.Locale.US, "%.5f", premLng)}" else "Not Configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171A34)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Distance from premises", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                                Text(
                                    text = if (distanceMeters != null) "$distanceMeters metres" else "Calculating...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGeofenceValid) Color(0xFF137333) else Color(0xFFC5221F)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Allowed radius", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                                Text(
                                    text = "$allowedRadiusMeters metres",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171A34)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Footer Row: Accuracy + Refresh Location Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS accuracy: $accuracyMeters m • Last verified: $lastVerifiedTime",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color(0xFF747895)
                    )

                    OutlinedButton(
                        onClick = onLocate,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF171A34))
                            Text("Refresh location", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171A34))
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION B: TODAY'S WORK - SINGLE ACTIVE OR NEXT-WORK CARD
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Today's work",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF171A34)
            )
            Text(
                text = "Only the next upcoming or currently running shift / overtime is shown here.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color(0xFF747895)
            )
        }

        if (showOffDayHolidayGreeting) {
            if (isTodayHoliday) {
                // Distinct Public Holiday Card
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3E8FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = Color(0xFF7E22CE),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = todayHoliday?.name?.takeIf { it.isNotBlank() } ?: "Public Holiday",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF171A34)
                                    )
                                    Text(
                                        text = "Official Premises Holiday • Paid Day",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E22CE),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFF3E8FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                            ) {
                                Text(
                                    text = "PUBLIC HOLIDAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7E22CE),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF3E8FF))

                        Surface(
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF7E22CE),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Today is an official public holiday. Regular shifts do not run today. If you are requested to work today, ask your Owner to create an overtime session for today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF581C87)
                                )
                            }
                        }
                    }
                }
            } else {
                // Distinct Weekly Off-Day Card
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WavingHand,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Weekly Off-Day",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF171A34)
                                    )
                                    Text(
                                        text = "Scheduled Rest Day • Enjoy your time off",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF4F46E5),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = "WEEKLY OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEF2FF))

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Regular shifts are not scheduled on off-days. If you are working today, ask your Owner to assign overtime for today so you can clock in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            PresenceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Work Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF171A34),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val isSessionRunning = !isClockedIn && (nextSession?.isActive == true || (hasScheduledWork && isTimeWindowValid))

                    val statusBadgeText = when {
                        isClockedIn -> "In Progress"
                        isSessionRunning -> "Running"
                        else -> "Upcoming"
                    }
                    val statusBadgeBg = when {
                        isClockedIn -> Color(0xFFE6F4EA)
                        isSessionRunning -> Color(0xFFFEF3C7)
                        else -> Color(0xFFEEF2FF)
                    }
                    val statusBadgeColor = when {
                        isClockedIn -> Color(0xFF137333)
                        isSessionRunning -> Color(0xFFD97706)
                        else -> Color(0xFF4F46E5)
                    }

                    Surface(
                        color = statusBadgeBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusBadgeColor)
                            )
                            Text(
                                text = statusBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusBadgeColor
                            )
                        }
                    }
                }

                // Time Display
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Dates.formatTime(startTimeRaw),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = " to ",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                        Text(
                            text = Dates.formatTime(endTimeRaw),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                    }
                    Text(
                        text = "${if (isOvertimeWork) "Overtime" else "Regular shift"} • $premisesName",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF747895)
                    )
                }

                // Clock-In Time & Calculated Late Minutes Bar
                Surface(
                    color = if (isClockedIn) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isClockedIn) Color(0xFFBBF7D0) else Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isClockedIn) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isClockedIn) Color(0xFF16A34A) else Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Clock In Time:",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563)
                            )
                            Text(
                                text = clockInDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClockedIn) Color(0xFF15803D) else Color(0xFF111827)
                            )
                        }

                        Surface(
                            color = when {
                                calculatedLateMinutes > 0 && isClockedIn -> Color(0xFFFEF2F2)
                                isClockedIn -> Color(0xFFDCFCE7)
                                else -> Color(0xFFF3F4F6)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    calculatedLateMinutes > 0 && isClockedIn -> "Late: $calculatedLateMinutes mins"
                                    isClockedIn -> "On time (0 min late)"
                                    else -> "Pending Clock In"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    calculatedLateMinutes > 0 && isClockedIn -> Color(0xFFDC2626)
                                    isClockedIn -> Color(0xFF15803D)
                                    else -> Color(0xFF6B7280)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Shift / Overtime Pay Rules Header & Chips Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOvertimeWork) "Overtime Pay Rules" else "Shift Pay Rules",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = if (isOvertimeWork) "Approved pay upon completion" else "Active status is highlighted",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOvertimeWork) {
                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Present",
                                amountText = Money.format(overtimePayInr),
                                isSelected = currentStatus == "PRESENT",
                                activeBgColor = Color(0xFFE6F4EA),
                                activeTextColor = Color(0xFF137333),
                                activeBorderColor = Color(0xFF34D399)
                            )

                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Absent",
                                amountText = Money.format(0L),
                                isSelected = currentStatus == "ABSENT",
                                activeBgColor = Color(0xFFFCE8E6),
                                activeTextColor = Color(0xFFC5221F),
                                activeBorderColor = Color(0xFFF87171)
                            )
                        } else {
                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Present",
                                amountText = Money.format(presentPayInr),
                                isSelected = currentStatus == "PRESENT",
                                activeBgColor = Color(0xFFE6F4EA),
                                activeTextColor = Color(0xFF137333),
                                activeBorderColor = Color(0xFF34D399)
                            )

                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Half-Day",
                                amountText = Money.format(halfDayPayInr),
                                isSelected = currentStatus == "HALF_DAY",
                                activeBgColor = Color(0xFFFEF3C7),
                                activeTextColor = Color(0xFFD97706),
                                activeBorderColor = Color(0xFFFBBF24)
                            )

                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Leave",
                                amountText = Money.format(leavePayInr),
                                isSelected = currentStatus == "LEAVE",
                                activeBgColor = Color(0xFFEEF2FF),
                                activeTextColor = Color(0xFF4338CA),
                                activeBorderColor = Color(0xFF818CF8)
                            )

                            PayRuleChip(
                                modifier = Modifier.weight(1f),
                                label = "Absent",
                                amountText = Money.format(absentPayInr),
                                isSelected = currentStatus == "ABSENT",
                                activeBgColor = Color(0xFFFCE8E6),
                                activeTextColor = Color(0xFFC5221F),
                                activeBorderColor = Color(0xFFF87171)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // 3 Status Columns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Attendance status", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                        Text(
                            text = if (isClockedIn) {
                                when (currentStatus) {
                                    "PRESENT" -> "Present"
                                    "HALF_DAY" -> "Half-Day"
                                    "LEAVE" -> "Leave"
                                    "ABSENT" -> "Absent"
                                    else -> active?.status?.ifBlank { "Present" } ?: "Present"
                                }
                            } else if (active?.status?.equals("ABSENT", ignoreCase = true) == true) {
                                "Absent"
                            } else if (active?.status?.equals("LEAVE", ignoreCase = true) == true) {
                                "Leave"
                            } else "Not clocked in",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isClockedIn) Color(0xFF137333) else Color(0xFF171A34)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Clock-in time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                        Text(
                            text = clockInDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF171A34)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Late minutes", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF747895))
                        Text(
                            text = lateMinutesDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (calculatedLateMinutes > 0) Color(0xFFDC2626) else Color(0xFF171A34)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // GPS Location Monitoring Counts Row inside Shift Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS Monitoring Checks",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF171A34)
                    )
                    Surface(
                        color = if (isClockedIn) Color(0xFFE6F4EA) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isClockedIn) "Active Monitoring" else "Inactive (Not Clocked In)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isClockedIn) Color(0xFF137333) else Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Inside Count
                    Surface(
                        color = if (isClockedIn && insideCount > 0) Color(0xFFE6F4EA) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Inside", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isClockedIn && insideCount > 0) Color(0xFF137333) else Color(0xFF6B7280))
                            Text("$insideCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isClockedIn && insideCount > 0) Color(0xFF137333) else Color(0xFF171A34))
                        }
                    }

                    // Outside Count
                    Surface(
                        color = if (isClockedIn && outsideCount > 0) Color(0xFFFCE8E6) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Outside", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isClockedIn && outsideCount > 0) Color(0xFFC5221F) else Color(0xFF6B7280))
                            Text("$outsideCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isClockedIn && outsideCount > 0) Color(0xFFC5221F) else Color(0xFF171A34))
                        }
                    }

                    // Missed Count
                    Surface(
                        color = if (isClockedIn && missedCount > 0) Color(0xFFFEF3C7) else Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Missed", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isClockedIn && missedCount > 0) Color(0xFFD97706) else Color(0xFF6B7280))
                            Text("$missedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isClockedIn && missedCount > 0) Color(0xFFD97706) else Color(0xFF171A34))
                        }
                    }
                }

                // Warning banner if 3 consecutive failures
                if (consecutiveFailures >= 3) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Your workplace location could not be verified for the last three monitoring intervals.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Latest GPS Pings inside Shift Card when Clocked In
                if (isClockedIn) {
                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    Text("Latest GPS Pings (10-min interval)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF171A34))

                    if (lastPings.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(Dates.formatTime(active?.clockIn), style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFF747895))
                            Surface(
                                color = Color(0xFFE6F4EA),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Inside",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF137333),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(if (distanceMeters != null) "$distanceMeters m" else "—", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171A34))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            lastPings.forEach { ping ->
                                val isInsidePing = ping.status.equals("Inside", ignoreCase = true) || (!ping.isOutside && !ping.status.equals("Missed", ignoreCase = true))
                                val isOutsidePing = ping.status.equals("Outside", ignoreCase = true) || ping.isOutside
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Dates.formatTime(ping.checkTime),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF747895)
                                    )
                                    Surface(
                                        color = when {
                                            isInsidePing -> Color(0xFFE6F4EA)
                                            isOutsidePing -> Color(0xFFFCE8E6)
                                            else -> Color(0xFFFEF3C7)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isInsidePing -> "Inside"
                                                isOutsidePing -> "Outside"
                                                else -> "Missed"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isInsidePing -> Color(0xFF137333)
                                                isOutsidePing -> Color(0xFFC5221F)
                                                else -> Color(0xFFD97706)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = if (ping.distanceMeters != null) "${ping.distanceMeters.toInt()} m" else "No location",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF171A34)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons / Active Shift Status
                if (!isClockedIn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showCapture = true },
                            enabled = canClockIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                disabledContainerColor = Color(0xFFE5E7EB)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Clock In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        if (!canClockIn) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    val guidanceReason = when {
                                        !hasScheduledWork -> "No shift or overtime is scheduled for today."
                                        !isGeofenceValid -> "Move inside premises (${distanceMeters ?: "—"} m). Radius limit is $allowedRadiusMeters m to clock in."
                                        !isTimeWindowValid -> "Shift clock-in window is currently inactive."
                                        state.isSubmitting -> "Clocking in..."
                                        else -> "Clock-in is unavailable right now."
                                    }
                                    Text(
                                        text = guidanceReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color(0xFFE6F4EA),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF137333))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shift in progress — GPS monitoring active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF137333),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Button(
                            onClick = onClockOut,
                            enabled = !state.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Clock Out",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION C: TODAY'S HISTORY - UNIFIED COMPLETED WORK TIMELINE
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Today's History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF171A34)
            )
            Text(
                text = "All completed shifts and overtime for today.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color(0xFF747895)
            )
        }

        if (unifiedHistory.isEmpty()) {
            PresenceEmptyState(
                icon = Icons.Default.History,
                title = "No completed work entries today",
                description = "Once your scheduled shift or overtime ends, it will automatically appear here in your completed work history."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                unifiedHistory.forEach { item ->
                    PresenceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171A34)
                                )
                                PresenceBadge(
                                    label = if (item.isOvertime) "Overtime" else "Regular Shift",
                                    tone = if (item.isOvertime) PresenceTone.Purple else PresenceTone.Info
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scheduled: ${item.scheduledWindow}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF747895)
                                )
                                Text(
                                    text = "Actual: ${item.actualStart} - ${item.actualEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF171A34)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Duration: ${item.durationText} • Status: ${item.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF137333)
                                )
                                Text(
                                    text = "Earned: ${Money.format(item.earnedInr)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF171A34)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCapture) {
            val targetOvertimeId = active?.overtimeId ?: nextSession?.overtimeId ?: todayOvertime?.id
            FaceCaptureDialog(
                engine = engine,
                mode = "VERIFY",
                premisesName = premisesName,
                distanceMeters = distanceMeters ?: 0,
                accuracyMeters = accuracyMeters,
                onCapturedSingle = { embedding ->
                    onFaceCaptured(embedding)
                    onClockIn(targetOvertimeId)
                    showCapture = false
                },
                onDismiss = { showCapture = false }
            )
        }
    }
}
}
