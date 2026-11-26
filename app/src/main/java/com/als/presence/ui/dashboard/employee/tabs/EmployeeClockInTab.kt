package com.als.presence.ui.dashboard.employee.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.GpsPingSummaryDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.UnhandledEventItemDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.GpsAccuracyDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.SessionKind
import com.als.presence.ui.dashboard.common.components.PresenceAvatar
import com.als.presence.ui.dashboard.common.components.PresenceBadge
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.common.components.PresenceSectionHeader
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeClockInViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class TodayCardType {
    CLOCKED_IN_SHIFT,
    RUNNING_SHIFT,
    UPCOMING_SHIFT,
    CLOCKED_IN_OVERTIME,
    RUNNING_OVERTIME,
    UPCOMING_OVERTIME,
    HOLIDAY,
    OFF_DAY,
    NO_WORK_SCHEDULED
}

/**
 * Editorial, 2026 Android Product Redesign for Employee Clock In.
 * Replaces stacked generic cards with a rich, hierarchical, action-oriented interface:
 * 1. Personalized greeting, editorial date & telemetry status
 * 2. Atmospheric Gradient Hero Surface (attendance state, shift schedule, duration progress & instant CTA)
 * 3. Today's Expected Earnings breakdown with multi-segment ratio bar
 * 4. Asymmetric Premises & Telemetry Bar with coordinates, geofence, and instant Locate button
 * 5. Verification status indicators (FaceID & GPS calibrated accuracy)
 * 6. Location Ping Telemetry surfaces (Inside, Outside, Missed)
 * 7. Connected vertical timeline for recent location verification pings
 */
@Composable
fun EmployeeClockInTab(
    user: User,
    state: EmployeeClockInViewModel.ClockInUiState,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onClockIn: (overtimeId: String?, embedding: FaceEmbeddingEngine.Embedding?) -> Unit,
    engine: FaceEmbeddingEngine,
    onFaceCaptured: (FaceEmbeddingEngine.Embedding) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    onSilentRefresh: () -> Unit = onRefresh,
    onClaimUnhandledEvent: (UnhandledEventItemDto) -> Unit = {},
    onUpdateLocation: (GpsLocation) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showCapture by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var pendingClockInAfterFace by remember { mutableStateOf<FaceEmbeddingEngine.Embedding?>(null) }

    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier
    val isDark = isSystemInDarkTheme()

    // Premises Data
    val premisesName = state.premisesName ?: user.premisesName?.let { Dates.cleanPremisesName(it) }?.takeIf { it.isNotBlank() } ?: "Main Headquarters"
    val premisesLat = state.premisesLatitude
    val premisesLng = state.premisesLongitude
    val distanceMeters = state.distanceToPremisesMeters?.toInt()
    val allowedRadiusMeters = state.premisesRadiusMeters
    val accuracyMeters = state.location?.accuracyMeters?.toInt() ?: 99
    val isAccuracyValid = state.location != null && accuracyMeters <= 10
    val isGeofenceValid = state.isWithinGeofence ?: (distanceMeters != null && distanceMeters <= allowedRadiusMeters)

    // Active Session & Clock-In check
    val active = state.activeEvent
    val isClockedIn = active != null && !active.clockIn.isNullOrBlank()
    val isRunningShift = isClockedIn && active?.overtimeId == null
    val isRunningOvertime = isClockedIn && active?.overtimeId != null

    val todayIso = Dates.todayIso()
    val todayDayName = Dates.getDayNameByDate(todayIso)

    // Completed works today
    val isShiftCompletedToday = state.todayRecords.any { it.overtimeId == null && !it.clockIn.isNullOrBlank() }
    val isOvertimeCompletedToday = state.todayRecords.any { it.overtimeId != null && !it.clockIn.isNullOrBlank() }

    // Next Session from ViewModel
    val nextSession = state.nextSession?.takeIf { Dates.isToday(it.workDate ?: todayIso) }

    // Today's Shift & Overtime candidates
    val todayAssignment = state.assignedShifts.firstOrNull { a ->
        val template = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
        template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
    } ?: if (state.assignedShifts.isEmpty()) {
        state.shiftTemplates.firstOrNull { Dates.isShiftMatchingDay(it.dayName, todayIso, it.daysOfWeek) }
    } else null

    val todayShiftTemplate = when (todayAssignment) {
        is ShiftAssignmentDto -> state.shiftTemplates.firstOrNull { it.id == todayAssignment.shiftId }
        is ShiftDto -> todayAssignment
        else -> null
    }

    val hasShiftToday = isRunningShift || todayShiftTemplate != null ||
        (nextSession?.kind == SessionKind.SHIFT && (nextSession.day.equals(todayDayName, ignoreCase = true) || nextSession.day.equals("Scheduled Shift", ignoreCase = true)))
    val isShiftEligibleToday = hasShiftToday && !isShiftCompletedToday

    val todayOvertime = state.openOvertime.firstOrNull {
        Dates.isToday(it.workDate) &&
            (it.status in listOf("APPROVED", "IN_PROGRESS", "CREATED", "REQUESTED")) &&
            state.todayRecords.none { r -> r.overtimeId == it.id && !r.clockOut.isNullOrBlank() } &&
            !Dates.isSessionOver(it.workDate, it.endTime, it.startTime)
    } ?: (if (nextSession?.kind == SessionKind.OVERTIME && !isOvertimeCompletedToday) {
        state.openOvertime.firstOrNull { it.id == nextSession.overtimeId }
    } else null)

    val hasOvertimeToday = isRunningOvertime || (todayOvertime != null && !isOvertimeCompletedToday)
    val todayHoliday = state.holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
    val isTodayHoliday = todayHoliday != null

    val shiftStartTime = todayShiftTemplate?.startTime?.ifBlank { null }
        ?: (if (nextSession?.kind == SessionKind.SHIFT) nextSession.startTime else null)
        ?: "09:00:00"
    val shiftEndTime = todayShiftTemplate?.endTime?.ifBlank { null }
        ?: (if (nextSession?.kind == SessionKind.SHIFT) nextSession.endTime else null)
        ?: "18:00:00"

    val otStartTime = todayOvertime?.startTime ?: (if (nextSession?.kind == SessionKind.OVERTIME) nextSession.startTime else null)
    val otEndTime = todayOvertime?.endTime ?: (if (nextSession?.kind == SessionKind.OVERTIME) nextSession.endTime else null)

    val isShiftWindowValid = if (isShiftEligibleToday) Dates.isWithinTimeWindow(shiftStartTime, shiftEndTime) else false
    val isOvertimeWindowValid = if (hasOvertimeToday) Dates.isWithinTimeWindow(otStartTime, otEndTime) else false
    val isShiftPastEndTime = Dates.isPastEndTime(shiftEndTime)

    val todayCardType = when {
        isRunningShift -> TodayCardType.CLOCKED_IN_SHIFT
        isRunningOvertime -> TodayCardType.CLOCKED_IN_OVERTIME
        isShiftEligibleToday && isShiftWindowValid && !isShiftPastEndTime -> TodayCardType.RUNNING_SHIFT
        hasOvertimeToday && isOvertimeWindowValid -> TodayCardType.RUNNING_OVERTIME
        isShiftEligibleToday -> TodayCardType.UPCOMING_SHIFT
        hasOvertimeToday -> TodayCardType.UPCOMING_OVERTIME
        isTodayHoliday -> TodayCardType.HOLIDAY
        else -> TodayCardType.NO_WORK_SCHEDULED
    }

    val workTitle = when (todayCardType) {
        TodayCardType.CLOCKED_IN_OVERTIME, TodayCardType.RUNNING_OVERTIME, TodayCardType.UPCOMING_OVERTIME ->
            todayOvertime?.reason?.takeIf { it.isNotBlank() } ?: "Overtime Session"
        TodayCardType.CLOCKED_IN_SHIFT, TodayCardType.RUNNING_SHIFT, TodayCardType.UPCOMING_SHIFT ->
            active?.shiftName?.ifBlank { null } ?: todayShiftTemplate?.shiftName?.ifBlank { "Scheduled Shift" } ?: "Assigned Shift"
        TodayCardType.HOLIDAY -> todayHoliday?.name?.takeIf { it.isNotBlank() } ?: "Public Holiday"
        else -> "No Shift Scheduled"
    }

    val startTimeRaw = when (todayCardType) {
        TodayCardType.CLOCKED_IN_SHIFT -> active?.assignedStartTime ?: shiftStartTime
        TodayCardType.RUNNING_SHIFT, TodayCardType.UPCOMING_SHIFT -> shiftStartTime
        TodayCardType.CLOCKED_IN_OVERTIME -> active?.assignedStartTime ?: otStartTime
        TodayCardType.RUNNING_OVERTIME, TodayCardType.UPCOMING_OVERTIME -> otStartTime
        else -> null
    }

    val endTimeRaw = when (todayCardType) {
        TodayCardType.CLOCKED_IN_SHIFT -> active?.assignedEndTime ?: shiftEndTime
        TodayCardType.RUNNING_SHIFT, TodayCardType.UPCOMING_SHIFT -> shiftEndTime
        TodayCardType.CLOCKED_IN_OVERTIME -> active?.assignedEndTime ?: otEndTime
        TodayCardType.RUNNING_OVERTIME, TodayCardType.UPCOMING_OVERTIME -> otEndTime
        else -> null
    }

    // Earnings calculation
    val shiftExpectedEarningInr = (todayAssignment as? ShiftAssignmentDto)?.paymentInr ?: 0L
    val overtimeExpectedEarningInr = todayOvertime?.paymentInr ?: 0L
    val totalExpectedEarningInr = shiftExpectedEarningInr + overtimeExpectedEarningInr

    val screenInfo = LocalScreenSizeInfo.current

    // Punctuality & late check
    val clockInTimeStr = active?.clockIn
    val isLate = remember(clockInTimeStr, startTimeRaw) {
        if (clockInTimeStr != null && startTimeRaw != null) {
            try {
                val start = LocalTime.parse(startTimeRaw.take(8))
                val inTime = LocalTime.parse(clockInTimeStr.take(8))
                inTime.isAfter(start.plusMinutes(5))
            } catch (_: Exception) {
                false
            }
        } else false
    }

    val lateMinutesDelta = remember(clockInTimeStr, startTimeRaw) {
        if (clockInTimeStr != null && startTimeRaw != null) {
            try {
                val start = LocalTime.parse(startTimeRaw.take(8))
                val inTime = LocalTime.parse(clockInTimeStr.take(8))
                val diff = java.time.Duration.between(start, inTime).toMinutes()
                diff.coerceAtLeast(0)
            } catch (_: Exception) {
                0L
            }
        } else 0L
    }

    // Live Shift Elapsed Time & Duration Progress
    val elapsedMinutes = remember(clockInTimeStr) {
        if (clockInTimeStr != null) {
            try {
                val inTime = LocalTime.parse(clockInTimeStr.take(8))
                val now = LocalTime.now()
                val minutes = java.time.Duration.between(inTime, now).toMinutes().toInt().coerceAtLeast(0)
                minutes
            } catch (_: Exception) {
                0
            }
        } else 0
    }

    val totalShiftMinutes = remember(startTimeRaw, endTimeRaw) {
        if (startTimeRaw != null && endTimeRaw != null) {
            try {
                val start = LocalTime.parse(startTimeRaw.take(8))
                val end = LocalTime.parse(endTimeRaw.take(8))
                val minutes = java.time.Duration.between(start, end).toMinutes().toInt().coerceAtLeast(60)
                minutes
            } catch (_: Exception) {
                480
            }
        } else 480
    }

    val shiftProgress = if (isClockedIn && totalShiftMinutes > 0) {
        (elapsedMinutes.toFloat() / totalShiftMinutes).coerceIn(0.02f, 1.0f)
    } else 0f

    val canClockIn = !isClockedIn && (todayCardType == TodayCardType.RUNNING_SHIFT || todayCardType == TodayCardType.RUNNING_OVERTIME) && !state.isSubmitting

    // Location Ping data
    val staffCard = state.activeStaffCard
    val insidePingsCount = staffCard?.counts?.inside ?: (if (isClockedIn) 1 else 0)
    val outsidePingsCount = staffCard?.counts?.outside ?: 0
    val missedPingsCount = staffCard?.counts?.missed ?: 0
    val latestPingsList: List<GpsPingSummaryDto> = staffCard?.latestPings.orEmpty()

    val todayFormattedDate = remember {
        val now = LocalDate.now()
        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (screenInfo.isTabletOrFoldable) 680.dp else Dp.Unspecified)
                .fillMaxWidth()
                .then(scrollModifier)
                .padding(
                    horizontal = screenInfo.horizontalScreenPadding,
                    vertical = if (screenInfo.isVeryCompact) 12.dp else 18.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (screenInfo.isVeryCompact) 14.dp else 20.dp)
        ) {
            // -------------------------------------------------------------
            // 1. PERSONALIZED EDITORIAL HEADER & DATE TELEMETRY
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PresenceAvatar(
                            name = user.fullName,
                            size = if (screenInfo.isVeryCompact) 44.dp else 50.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isClockedIn) PresenceDesign.Positive else MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${user.fullName.split(" ").firstOrNull() ?: user.fullName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = todayFormattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
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
            // 2. ATMOSPHERIC HERO SURFACE: ATTENDANCE STATE & SHIFT CONTEXT
            // -------------------------------------------------------------
            val heroStart = if (isDark) Color(0xFF1B2338) else Color(0xFFE8EEFB)
            val heroEnd = if (isDark) Color(0xFF231C38) else Color(0xFFF3EBFF)
            val heroBorder = if (isDark) Color(0xFF333E59) else Color(0xFFD3DCF5)
            val glowColor = if (isDark) Color(0xFF4263EB).copy(alpha = 0.16f) else Color(0xFF4263EB).copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(heroStart, heroEnd)))
                    .border(BorderStroke(1.dp, heroBorder), RoundedCornerShape(24.dp))
                    .drawBehind {
                        // Subtle radial illumination bloom
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                center = Offset(size.width * 0.88f, size.height * 0.15f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.88f, size.height * 0.15f)
                        )
                        // Restrained subtle geometric ring
                        drawCircle(
                            color = if (isDark) Color.White.copy(alpha = 0.025f) else Color.White.copy(alpha = 0.5f),
                            radius = size.width * 0.38f,
                            center = Offset(size.width * 0.95f, size.height * 0.95f),
                            style = Stroke(width = 24.dp.toPx())
                        )
                    }
                    .padding(if (screenInfo.isVeryCompact) 16.dp else 22.dp)
            ) {
                Column {
                    // Status Badge & Timing Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PresenceBadge(
                            text = when (todayCardType) {
                                TodayCardType.CLOCKED_IN_SHIFT -> "LIVE ACTIVE SHIFT"
                                TodayCardType.CLOCKED_IN_OVERTIME -> "LIVE ACTIVE OVERTIME"
                                TodayCardType.RUNNING_SHIFT -> "ACTIVE SHIFT WINDOW"
                                TodayCardType.RUNNING_OVERTIME -> "ACTIVE OVERTIME WINDOW"
                                TodayCardType.UPCOMING_SHIFT -> "SCHEDULED SHIFT"
                                TodayCardType.UPCOMING_OVERTIME -> "UPCOMING OVERTIME"
                                TodayCardType.HOLIDAY -> "PUBLIC HOLIDAY"
                                else -> if (isShiftCompletedToday) "COMPLETED TODAY" else "NO SCHEDULE"
                            },
                            tone = when (todayCardType) {
                                TodayCardType.CLOCKED_IN_SHIFT, TodayCardType.CLOCKED_IN_OVERTIME -> PresenceTone.Success
                                TodayCardType.RUNNING_SHIFT, TodayCardType.RUNNING_OVERTIME -> PresenceTone.Info
                                TodayCardType.HOLIDAY -> PresenceTone.Purple
                                else -> if (isShiftCompletedToday) PresenceTone.Success else PresenceTone.Neutral
                            },
                            showLiveBeacon = isClockedIn
                        )

                        if (startTimeRaw != null && endTimeRaw != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.35f else 0.7f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${Dates.formatTime(startTimeRaw)} – ${Dates.formatTime(endTimeRaw)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = workTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // LIVE SHIFT DURATION & PUNCTUALITY IN-PROGRESS BAR
                    if (isClockedIn && active?.clockIn != null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PresenceDesign.PositiveSoft.copy(alpha = if (isDark) 0.35f else 0.6f))
                                .border(BorderStroke(0.75.dp, PresenceDesign.Positive.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
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
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PresenceDesign.Positive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Clocked in at ${Dates.formatTime(active.clockIn)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PresenceDesign.Positive,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = if (isLate) "Late (+${lateMinutesDelta}m)" else "On Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isLate) PresenceDesign.CautionText else PresenceDesign.Positive
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { shiftProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = PresenceDesign.Positive,
                                    trackColor = PresenceDesign.Positive.copy(alpha = 0.16f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Elapsed: ${elapsedMinutes / 60}h ${elapsedMinutes % 60}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(shiftProgress * 100).toInt()}% completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------------------------
                    // 3. TODAY'S EXPECTED EARNINGS (FINTECH STRIP)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.85f))
                            .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            PresenceMicroLabel(text = "Today's Remuneration")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = Money.format(totalExpectedEarningInr),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalExpectedEarningInr > 0L) PresenceDesign.Positive else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "projected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                PresenceMicroLabel(text = "Base Wage")
                                Text(
                                    text = Money.format(shiftExpectedEarningInr),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (overtimeExpectedEarningInr > 0L) {
                                Column(horizontalAlignment = Alignment.End) {
                                    PresenceMicroLabel(text = "Overtime")
                                    Text(
                                        text = Money.format(overtimeExpectedEarningInr),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Brand
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---------------------------------------------------------
                    // 4. ACTION-ORIENTED ATTENDANCE CTA
                    // ---------------------------------------------------------
                    if (isClockedIn) {
                        PresenceButton(
                            text = if (isRunningOvertime) "Overtime In Progress • Active" else "Shift In Progress • GPS Monitored",
                            onClick = { },
                            variant = PresenceVariant.Primary,
                            enabled = false,
                            icon = Icons.Default.VerifiedUser,
                            loading = state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isShiftCompletedToday) {
                        PresenceButton(
                            text = "Shift Completed for Today",
                            onClick = { },
                            variant = PresenceVariant.Soft,
                            enabled = false,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val ctaText = when {
                            !isGeofenceValid -> "Outside Geofence (${distanceMeters ?: 0}m • Limit ${allowedRadiusMeters}m)"
                            todayCardType == TodayCardType.UPCOMING_SHIFT -> "Shift Starts at ${Dates.formatTime(startTimeRaw ?: "")}"
                            todayCardType == TodayCardType.HOLIDAY -> "Holiday – No Work Scheduled"
                            todayCardType == TodayCardType.NO_WORK_SCHEDULED -> "No Shift Scheduled"
                            else -> "Clock In with FaceID"
                        }

                        PresenceButton(
                            text = ctaText,
                            onClick = {
                                // Face biometric verification FIRST
                                showCapture = true
                            },
                            variant = PresenceVariant.Primary,
                            icon = Icons.Default.CameraAlt,
                            enabled = canClockIn && isGeofenceValid,
                            loading = state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. ASYMMETRIC PREMISES & TELEMETRY STRIP (NOT A GENERIC CARD)
            // -------------------------------------------------------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PresenceDesign.BrandSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = PresenceDesign.Brand,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                PresenceMicroLabel(text = "Assigned Premises")
                                Text(
                                    text = premisesName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Compact Locate Action Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PresenceDesign.BrandSoft)
                                .clickable { onLocate() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = PresenceDesign.Brand,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Locate",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Brand
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Coordinates & Radius Information Row
                    if (premisesLat != null && premisesLng != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.4f", premisesLat)}° N, ${String.format(java.util.Locale.US, "%.4f", premisesLng)}° E",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Radius: ${allowedRadiusMeters}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Brand
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. VERIFICATION STATUS ROW: GEOFENCE & SENSORS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Geofence status pill
                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isGeofenceValid) PresenceDesign.PositiveSoft else PresenceDesign.CautionSoft)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isGeofenceValid) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isGeofenceValid) PresenceDesign.Positive else PresenceDesign.Caution,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = if (isGeofenceValid) "Inside Geofence" else "Outside Zone",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGeofenceValid) PresenceDesign.Positive else PresenceDesign.CautionText,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (distanceMeters != null) "${distanceMeters}m from center" else "Acquiring GPS…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // GPS Calibration status pill
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isAccuracyValid) PresenceDesign.Positive else PresenceDesign.Caution,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = if (isAccuracyValid) "GPS ±${accuracyMeters}m" else "Calibrating",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccuracyValid) MaterialTheme.colorScheme.onSurface else PresenceDesign.CautionText
                                )
                                Text(
                                    text = if (isAccuracyValid) "High Precision" else "Needs Lock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. LOCATION PING SUMMARY & TIMELINE ACTIVITY
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresenceSectionHeader(
                    title = "Location Telemetry",
                    subtitle = "Automated active shift periodic location checks"
                )

                // 3 Compact Metric Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Inside Pings
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = PresenceDesign.PositiveSoft,
                        border = BorderStroke(0.5.dp, PresenceDesign.Positive.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$insidePingsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PresenceDesign.Positive
                            )
                            Text(
                                text = "Inside",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Positive
                            )
                        }
                    }

                    // Outside Pings
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (outsidePingsCount > 0) PresenceDesign.DangerSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, if (outsidePingsCount > 0) PresenceDesign.Danger.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$outsidePingsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (outsidePingsCount > 0) PresenceDesign.DangerText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Outside",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (outsidePingsCount > 0) PresenceDesign.DangerText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Missed Pings
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (missedPingsCount > 0) PresenceDesign.CautionSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, if (missedPingsCount > 0) PresenceDesign.Caution.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$missedPingsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (missedPingsCount > 0) PresenceDesign.CautionText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Missed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (missedPingsCount > 0) PresenceDesign.CautionText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Recent Ping Timeline List
                if (latestPingsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Recent Verification Timeline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            latestPingsList.take(4).forEachIndexed { idx, ping ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (ping.effectiveIsOutside) PresenceDesign.DangerText else PresenceDesign.Positive)
                                        )
                                        Text(
                                            text = Dates.formatTime(ping.effectiveTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (ping.effectiveIsOutside) "Outside (${ping.distanceMeters?.toInt() ?: 0}m)" else "Inside Premises",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ping.effectiveIsOutside) PresenceDesign.DangerText else PresenceDesign.Positive
                                    )
                                }
                                if (idx < latestPingsList.take(4).lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODALS: FACE CAPTURE -> THEN GPS VERIFICATION
    // -------------------------------------------------------------
    if (showCapture) {
        FaceCaptureDialog(
            engine = engine,
            onDismiss = { showCapture = false },
            onCapturedSingle = { embedding ->
                showCapture = false
                onFaceCaptured(embedding)
                // Biometric succeeded! Now initiate GPS verification if accuracy is low
                if (!isAccuracyValid) {
                    pendingClockInAfterFace = embedding
                    showGpsDialog = true
                } else {
                    val otId = if (todayCardType == TodayCardType.RUNNING_OVERTIME) todayOvertime?.id else null
                    onClockIn(otId, embedding)
                }
            }
        )
    }

    if (showGpsDialog) {
        GpsAccuracyDialog(
            premisesLatitude = premisesLat,
            premisesLongitude = premisesLng,
            premisesRadiusMeters = allowedRadiusMeters,
            onDismiss = {
                showGpsDialog = false
                pendingClockInAfterFace = null
            },
            onLocationAcquired = { location ->
                onUpdateLocation(location)
                showGpsDialog = false
                val pending = pendingClockInAfterFace
                if (pending != null) {
                    pendingClockInAfterFace = null
                    val otId = if (todayCardType == TodayCardType.RUNNING_OVERTIME) todayOvertime?.id else null
                    onClockIn(otId, pending)
                }
            }
        )
    }
}
