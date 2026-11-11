package com.als.presence.ui.dashboard.employee.tabs

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
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.UnhandledEventItemDto
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.location.GpsLocation
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.GpsAccuracyDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.SessionKind
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeClockInViewModel

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

private data class UpcomingRosterItem(
    val isOvertime: Boolean,
    val isHoliday: Boolean = false,
    val title: String,
    val dayName: String,
    val dateFormatted: String,
    val isTomorrow: Boolean,
    val scheduledWindow: String,
    val badgeText: String,
    val badgeTone: PresenceTone,
    val statusText: String? = null,
    val presentPay: Long = 0L,
    val halfDayPay: Long = 0L,
    val leavePay: Long = 0L,
    val absentPay: Long = 0L,
    val agreedOvertimePay: Long = 0L,
    val holidayPay: Long = 0L
) {
    val rateText: String
        get() = when {
            isOvertime -> Money.format(agreedOvertimePay)
            isHoliday -> if (holidayPay > 0) Money.format(holidayPay) else "Paid"
            presentPay > 0 -> Money.format(presentPay)
            else -> "—"
        }
}

@Composable
private fun StatusEarningMiniChip(
    modifier: Modifier = Modifier,
    status: String,
    amount: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shortStatus = when (status) {
                "Half-Day" -> "Half"
                else -> status
            }
            Text(
                text = "$shortStatus: $amount",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
        color = if (isSelected) activeBgColor else PresenceDesign.Workspace,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) activeBorderColor else PresenceDesign.Line
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) activeTextColor else PresenceDesign.Muted,
                    maxLines = 1
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) activeTextColor else PresenceDesign.Ink,
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
    onClockIn: (overtimeId: String?, embedding: FaceEmbeddingEngine.Embedding?) -> Unit,
    onClockOut: () -> Unit = {},
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
    var pendingClockInAfterGps by remember { mutableStateOf(false) }

    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier

    // Premises Data
    val premisesName = state.premisesName ?: user.premisesName?.let { Dates.cleanPremisesName(it) }?.takeIf { it.isNotBlank() } ?: "Main Office"
    val userLat = state.location?.latitude
    val userLng = state.location?.longitude
    val premLat = state.premisesLatitude
    val premLng = state.premisesLongitude
    val distanceMeters = state.distanceToPremisesMeters?.toInt()
    val allowedRadiusMeters = state.premisesRadiusMeters
    val accuracyMeters = state.location?.accuracyMeters?.toInt() ?: 99
    val isAccuracyValid = state.location != null && accuracyMeters <= 10
    val lastVerifiedTime = state.lastGpsVerifiedTime ?: "Just now"

    val isGeofenceValid = state.isWithinGeofence ?: (distanceMeters != null && distanceMeters <= allowedRadiusMeters)

    // Active Session & Clock-In check
    val active = state.activeEvent
    val isClockedIn = active != null && !active.clockIn.isNullOrBlank() && active.clockOut == null
    val isRunningShift = isClockedIn && active?.overtimeId == null
    val isRunningOvertime = isClockedIn && active?.overtimeId != null

    val todayIso = Dates.todayIso()
    val todayDayName = Dates.getDayNameByDate(todayIso)
    val todayDateFormatted = Dates.formatDate(todayIso)

    // Completed works today
    val isShiftCompletedToday = state.todayRecords.any { it.overtimeId == null && !it.clockIn.isNullOrBlank() && !it.clockOut.isNullOrBlank() }
    val isOvertimeCompletedToday = state.todayRecords.any { it.overtimeId != null && !it.clockIn.isNullOrBlank() && !it.clockOut.isNullOrBlank() }

    // Next Session from ViewModel
    val nextSession = state.nextSession?.takeIf { Dates.isToday(it.workDate ?: todayIso) }

    // 1. Shift candidate for TODAY
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

    // 2. Overtime candidate for TODAY
    val todayOvertime = state.openOvertime.firstOrNull {
        Dates.isToday(it.workDate) &&
            (it.status in listOf("APPROVED", "IN_PROGRESS", "CREATED", "REQUESTED")) &&
            state.todayRecords.none { r -> r.overtimeId == it.id && !r.clockOut.isNullOrBlank() } &&
            !Dates.isSessionOver(it.workDate, it.endTime, it.startTime)
    } ?: (if (nextSession?.kind == SessionKind.OVERTIME && !isOvertimeCompletedToday) {
        state.openOvertime.firstOrNull { it.id == nextSession.overtimeId }
    } else null)

    val hasOvertimeToday = isRunningOvertime || (todayOvertime != null && !isOvertimeCompletedToday)

    // 3. Holiday candidate for TODAY
    val todayHoliday = state.holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
    val isTodayHoliday = todayHoliday != null

    // 4. Weekly Off candidate for TODAY
    val currentDayOfWeek = try {
        java.time.LocalDate.parse(todayIso).dayOfWeek
    } catch (e: Exception) {
        java.time.LocalDate.now().dayOfWeek
    }
    val isSunday = currentDayOfWeek == java.time.DayOfWeek.SUNDAY
    val hasWeeklyOffEvent = state.unhandledEvents.any {
        it.type.equals("WEEKLY_OFF", ignoreCase = true) && Dates.isToday(it.date)
    }
    val isWeeklyOffDay = isSunday || hasWeeklyOffEvent

    // Timing evaluation for Shift & Overtime
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

    // STRICT CARD PRIORITY: Active sessions > Open running windows > Upcoming sessions > Holidays/Off-days
    val todayCardType = when {
        isRunningShift -> TodayCardType.CLOCKED_IN_SHIFT
        isRunningOvertime -> TodayCardType.CLOCKED_IN_OVERTIME
        isShiftEligibleToday && isShiftWindowValid && !isShiftPastEndTime -> TodayCardType.RUNNING_SHIFT
        hasOvertimeToday && isOvertimeWindowValid -> TodayCardType.RUNNING_OVERTIME
        isShiftEligibleToday && isShiftWindowValid -> TodayCardType.RUNNING_SHIFT
        isShiftEligibleToday -> TodayCardType.UPCOMING_SHIFT
        hasOvertimeToday -> TodayCardType.UPCOMING_OVERTIME
        isTodayHoliday -> TodayCardType.HOLIDAY
        isWeeklyOffDay -> TodayCardType.OFF_DAY
        else -> TodayCardType.NO_WORK_SCHEDULED
    }

    val isOvertimeWork = when (todayCardType) {
        TodayCardType.CLOCKED_IN_OVERTIME,
        TodayCardType.RUNNING_OVERTIME,
        TodayCardType.UPCOMING_OVERTIME -> true
        else -> false
    }

    val workTitle = when (todayCardType) {
        TodayCardType.CLOCKED_IN_OVERTIME,
        TodayCardType.RUNNING_OVERTIME,
        TodayCardType.UPCOMING_OVERTIME ->
            todayOvertime?.reason?.takeIf { it.isNotBlank() }
                ?: (if (nextSession?.kind == SessionKind.OVERTIME) nextSession.title.ifBlank { "Overtime Session" } else "Overtime Session")
        TodayCardType.CLOCKED_IN_SHIFT,
        TodayCardType.RUNNING_SHIFT,
        TodayCardType.UPCOMING_SHIFT ->
            active?.shiftName?.ifBlank { null }
                ?: todayShiftTemplate?.shiftName?.ifBlank { "Assigned Shift" }
                ?: (if (nextSession?.kind == SessionKind.SHIFT) nextSession.title.ifBlank { "Assigned Shift" } else "Assigned Shift")
        TodayCardType.HOLIDAY ->
            todayHoliday?.name?.takeIf { it.isNotBlank() } ?: "Public Holiday"
        TodayCardType.OFF_DAY ->
            "Weekly Off"
        TodayCardType.NO_WORK_SCHEDULED ->
            "No Work Scheduled"
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

    val isTimeWindowValid = when (todayCardType) {
        TodayCardType.CLOCKED_IN_SHIFT, TodayCardType.CLOCKED_IN_OVERTIME -> true
        TodayCardType.RUNNING_SHIFT -> isShiftWindowValid
        TodayCardType.RUNNING_OVERTIME -> isOvertimeWindowValid
        else -> false
    }

    val hasScheduledWork = when (todayCardType) {
        TodayCardType.CLOCKED_IN_SHIFT,
        TodayCardType.RUNNING_SHIFT,
        TodayCardType.UPCOMING_SHIFT,
        TodayCardType.CLOCKED_IN_OVERTIME,
        TodayCardType.RUNNING_OVERTIME,
        TodayCardType.UPCOMING_OVERTIME -> true
        else -> false
    }

    val isSessionRunningNow = when (todayCardType) {
        TodayCardType.RUNNING_SHIFT, TodayCardType.RUNNING_OVERTIME -> true
        else -> false
    }

    val canClockIn = !isClockedIn && hasScheduledWork && isGeofenceValid && isAccuracyValid && isTimeWindowValid && !state.isSubmitting

    // Pay Rules calculation for the active/upcoming shift
    val currentAssignment = state.assignedShifts.firstOrNull { a ->
        val tmpl = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
        tmpl != null && Dates.isShiftMatchingDay(tmpl.dayName, todayIso, tmpl.daysOfWeek)
    } ?: state.assignedShifts.firstOrNull { a ->
        (active?.shiftName != null && a.shiftName != null && a.shiftName.equals(active.shiftName, ignoreCase = true)) ||
        (nextSession?.title != null && a.shiftName != null && a.shiftName.equals(nextSession.title, ignoreCase = true)) ||
        (a.shiftName != null && a.shiftName.equals(workTitle, ignoreCase = true))
    } ?: state.assignedShifts.firstOrNull()

    // Unworked day rules (weekly off / holiday)
    val weeklyOffRule = state.unworkedRules.firstOrNull { it.dayType.equals("WEEKLY_OFF", ignoreCase = true) }
    val holidayRule = state.unworkedRules.firstOrNull { it.dayType.equals("HOLIDAY", ignoreCase = true) }

    val presentPayAmount = currentAssignment?.presentPay ?: (state.assignedShifts.firstOrNull()?.presentPay ?: 0.0)
    val halfDayPayAmount = currentAssignment?.halfDayPay ?: if (presentPayAmount > 0) (presentPayAmount / 2.0) else 0.0
    val leavePayAmount = currentAssignment?.leavePay ?: 0.0
    val absentPayAmount = currentAssignment?.absentPay ?: 0.0

    val presentPayInr = presentPayAmount.toLong()
    val halfDayPayInr = halfDayPayAmount.toLong()
    val leavePayInr = leavePayAmount.toLong()
    val absentPayInr = absentPayAmount.toLong()

    val activeOvertime = state.openOvertime.firstOrNull { it.id == active?.overtimeId || it.id == nextSession?.overtimeId }
        ?: state.upcomingOvertime.firstOrNull { it.id == active?.overtimeId || it.id == nextSession?.overtimeId }
        ?: todayOvertime
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

    // Active attendance status (defaults to NOT_CLOCKED_IN if running and not clocked in)
    val currentStatus = when {
        isClockedIn -> when {
            active?.status?.equals("HALF_DAY", ignoreCase = true) == true -> "HALF_DAY"
            active?.status?.equals("LEAVE", ignoreCase = true) == true -> "LEAVE"
            active?.status?.equals("ABSENT", ignoreCase = true) == true -> "ABSENT"
            else -> "PRESENT"
        }
        active?.status?.equals("LEAVE", ignoreCase = true) == true -> "LEAVE"
        active?.status?.equals("ABSENT", ignoreCase = true) == true -> "ABSENT"
        isSessionRunningNow -> "NOT_CLOCKED_IN"
        else -> "UPCOMING"
    }

    // Half-day threshold check for Clock In button label
    val matchingTemplate = state.shiftTemplates.firstOrNull { it.id == currentAssignment?.shiftId } ?: state.shiftTemplates.firstOrNull { it.shiftName == workTitle }
    val halfDayThresholdStr = nextSession?.halfDayThreshold ?: matchingTemplate?.halfDayThreshold
    val isTooLateForHalfDay = Dates.isPastHalfDayThreshold(startTimeRaw, halfDayThresholdStr)
    val clockInButtonText = when {
        isOvertimeWork -> "Clock In (Overtime)"
        todayCardType == TodayCardType.UPCOMING_SHIFT -> "Clock In (Upcoming)"
        isTooLateForHalfDay -> "Clock In (Half-Day)"
        else -> "Clock In (Present)"
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
        .filter { it.clockOut != null || it.status in listOf("PRESENT", "HALF_DAY", "ABSENT", "LEAVE", "COMPLETED") }
        .map { record ->
            val matchingAssignment = state.assignedShifts.firstOrNull { it.shiftName == record.shiftName } ?: state.assignedShifts.firstOrNull()
            val presentPay = matchingAssignment?.presentPay ?: (state.assignedShifts.firstOrNull()?.presentPay ?: 0.0)
            val halfDayPay = matchingAssignment?.halfDayPay ?: if (presentPay > 0) (presentPay / 2.0) else 0.0
            val leavePay = matchingAssignment?.leavePay ?: 0.0
            val absentPay = matchingAssignment?.absentPay ?: 0.0

            val rateInr = when (record.status.uppercase()) {
                "HALF_DAY" -> halfDayPay.toLong()
                "LEAVE" -> leavePay.toLong()
                "ABSENT" -> absentPay.toLong()
                else -> presentPay.toLong()
            }
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

    // Upcoming 7-Day Roster & Overtime Schedule
    val upcomingSchedule = remember(state.assignedShifts, state.shiftTemplates, state.upcomingOvertime, state.holidays, state.unworkedRules, todayIso) {
        val list = mutableListOf<UpcomingRosterItem>()
        for (dayOffset in 1..7) {
            val futureIso = Dates.isoDaysAhead(dayOffset.toLong())
            val futureDayName = Dates.getDayNameByDate(futureIso)
            val futureFormatted = Dates.formatDateShort(futureIso)
            val isTomorrow = dayOffset == 1

            // 1. Check for upcoming overtime on this date
            val otEntries = state.upcomingOvertime.filter { it.workDate == futureIso }
            for (ot in otEntries) {
                list.add(
                    UpcomingRosterItem(
                        isOvertime = true,
                        isHoliday = false,
                        title = ot.reason?.takeIf { it.isNotBlank() } ?: "Overtime Session",
                        dayName = futureDayName,
                        dateFormatted = futureFormatted,
                        isTomorrow = isTomorrow,
                        scheduledWindow = Dates.formatWindow(ot.startTime, ot.endTime),
                        badgeText = "Overtime",
                        badgeTone = PresenceTone.Purple,
                        statusText = ot.status,
                        agreedOvertimePay = ot.paymentInr
                    )
                )
            }

            // 2. Check for upcoming shift on this date
            val matchingAssignment = state.assignedShifts.firstOrNull { a ->
                val tmpl = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
                tmpl != null && Dates.isShiftMatchingDay(tmpl.dayName, futureIso, tmpl.daysOfWeek)
            } ?: state.assignedShifts.firstOrNull { a ->
                state.shiftTemplates.any { t -> t.id == a.shiftId && Dates.isShiftMatchingDay(t.dayName, futureIso, t.daysOfWeek) }
            }
            val fallbackTemplate = if (matchingAssignment == null) {
                state.shiftTemplates.firstOrNull { t ->
                    Dates.isShiftMatchingDay(t.dayName, futureIso, t.daysOfWeek)
                }
            } else null

            val template = state.shiftTemplates.firstOrNull { it.id == matchingAssignment?.shiftId } ?: fallbackTemplate
            if (template != null) {
                val assignment = matchingAssignment
                    ?: state.assignedShifts.firstOrNull { it.shiftId == template.id }
                    ?: state.assignedShifts.firstOrNull()

                val presPay = assignment?.presentPay?.toLong() ?: 0L
                val halfPay = assignment?.halfDayPay?.toLong() ?: if (presPay > 0) (presPay / 2L) else 0L
                val lvePay = assignment?.leavePay?.toLong() ?: 0L
                val absPay = assignment?.absentPay?.toLong() ?: 0L

                list.add(
                    UpcomingRosterItem(
                        isOvertime = false,
                        isHoliday = false,
                        title = template.shiftName.ifBlank { "Assigned Shift" },
                        dayName = futureDayName,
                        dateFormatted = futureFormatted,
                        isTomorrow = isTomorrow,
                        scheduledWindow = Dates.formatWindow(template.startTime, template.endTime),
                        badgeText = "Shift",
                        badgeTone = PresenceTone.Info,
                        statusText = "Scheduled",
                        presentPay = presPay,
                        halfDayPay = halfPay,
                        leavePay = lvePay,
                        absentPay = absPay
                    )
                )
            } else {
                val hol = state.holidays.firstOrNull { it.holidayDate == futureIso || it.date == futureIso }
                if (hol != null) {
                    val holidayRule = state.unworkedRules.firstOrNull { it.dayType.equals("HOLIDAY", ignoreCase = true) }
                    val defaultPay = state.assignedShifts.firstOrNull()?.presentPay?.toLong() ?: 0L
                    val holPay = holidayRule?.paymentInr ?: defaultPay
                    list.add(
                        UpcomingRosterItem(
                            isOvertime = false,
                            isHoliday = true,
                            title = hol.name?.takeIf { it.isNotBlank() } ?: "Public Holiday",
                            dayName = futureDayName,
                            dateFormatted = futureFormatted,
                            isTomorrow = isTomorrow,
                            scheduledWindow = "All Day",
                            badgeText = "Holiday",
                            badgeTone = PresenceTone.Success,
                            statusText = "Off Day",
                            holidayPay = holPay
                        )
                    )
                }
            }
        }
        list
    }

    val screenInfo = LocalScreenSizeInfo.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) screenInfo.horizontalScreenPadding else 0.dp)
            .padding(top = if (isScrollable) (if (screenInfo.isVeryCompact) 6.dp else 10.dp) else 0.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (screenInfo.isVeryCompact) 8.dp else 10.dp)
    ) {
        // SCREEN COMPACT TITLE HEADER WITH REFRESH ACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Clock In",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    when (todayCardType) {
                        TodayCardType.CLOCKED_IN_SHIFT -> PresenceBadge(label = "Shift in Progress", tone = PresenceTone.Success, dot = true)
                        TodayCardType.RUNNING_SHIFT -> PresenceBadge(label = "Shift Open", tone = PresenceTone.Warning, dot = true)
                        TodayCardType.UPCOMING_SHIFT -> PresenceBadge(label = "Upcoming Shift", tone = PresenceTone.Neutral)
                        TodayCardType.CLOCKED_IN_OVERTIME -> PresenceBadge(label = "Overtime in Progress", tone = PresenceTone.Purple, dot = true)
                        TodayCardType.RUNNING_OVERTIME -> PresenceBadge(label = "Overtime Open", tone = PresenceTone.Warning, dot = true)
                        TodayCardType.UPCOMING_OVERTIME -> PresenceBadge(label = "Upcoming Overtime", tone = PresenceTone.Neutral)
                        TodayCardType.HOLIDAY -> PresenceBadge(label = "Public Holiday", tone = PresenceTone.Purple)
                        TodayCardType.OFF_DAY -> PresenceBadge(label = "Rest Day", tone = PresenceTone.Neutral)
                        TodayCardType.NO_WORK_SCHEDULED -> PresenceBadge(label = "No Shifts", tone = PresenceTone.Neutral)
                    }
                }
                Text(
                    text = "$todayDayName, $todayDateFormatted • Shift attendance & GPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.Muted
                )
            }
            PresenceRefreshButton(
                onClick = onRefresh,
                isRefreshing = state.isLoading,
                contentDescription = "Refresh session data"
            )
        }

        // =====================================================================
        // SECTION A: ASSIGNED PREMISES CARD
        // =====================================================================
        PresenceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Building Icon + Premises Title & Subtitle + Geofence Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isGeofenceValid) PresenceDesign.PositiveSoft else PresenceDesign.DangerSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = if (isGeofenceValid) PresenceDesign.Positive else PresenceDesign.Danger,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = premisesName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Assigned Geofence Premises",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = PresenceDesign.Muted
                            )
                        }
                    }

                    PresenceBadge(
                        label = if (isGeofenceValid) "Inside Premises" else "Outside Premises",
                        tone = if (isGeofenceValid) PresenceTone.Success else PresenceTone.Danger,
                        dot = true
                    )
                }

                // Coordinates & Distance Grid Surface Container with Proximity Meter
                Surface(
                    color = PresenceDesign.Workspace,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PresenceDesign.Line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Distance",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PresenceDesign.Muted
                                )
                                Text(
                                    text = if (distanceMeters != null) "$distanceMeters m" else "Acquiring GPS...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isGeofenceValid) PresenceDesign.Positive else PresenceDesign.Danger
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Allowed Boundary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PresenceDesign.Muted
                                )
                                Text(
                                    text = "$allowedRadiusMeters m",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.Ink
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "GPS Accuracy",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PresenceDesign.Muted
                                )
                                Text(
                                    text = if (state.location != null) "±$accuracyMeters m" else "Waiting...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        state.location == null -> PresenceDesign.Muted
                                        accuracyMeters <= 10 -> PresenceDesign.Positive
                                        accuracyMeters <= 20 -> PresenceDesign.CautionText
                                        else -> PresenceDesign.DangerText
                                    }
                                )
                            }
                        }

                        // Visual Proximity Meter (Bar)
                        if (distanceMeters != null) {
                            val progressFraction = (distanceMeters.toFloat() / (allowedRadiusMeters.toFloat() * 2f).coerceAtLeast(1f))
                                .coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressFraction)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isGeofenceValid) PresenceDesign.Positive
                                            else PresenceDesign.Danger
                                        )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = PresenceDesign.Line)

                // Footer Row: Coordinates & Refresh Location Button
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
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PresenceDesign.MutedLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (userLat != null && userLng != null) {
                                "${String.format(java.util.Locale.US, "%.4f", userLat)}, ${String.format(java.util.Locale.US, "%.4f", userLng)} • $lastVerifiedTime"
                            } else {
                                "Waiting for GPS • $lastVerifiedTime"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = PresenceDesign.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            pendingClockInAfterGps = false
                            showGpsDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PresenceDesign.Line),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = PresenceDesign.Brand
                            )
                            Text(
                                "Update GPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PresenceDesign.Brand
                            )
                        }
                    }
                }
            }
        }

        // Resolve Unworked Earning Rules
        val holidayRule = state.unworkedRules.find { it.employeeId != null && it.dayType == "HOLIDAY" }
            ?: state.unworkedRules.find { it.dayType == "HOLIDAY" }
        val weeklyOffRule = state.unworkedRules.find { it.employeeId != null && it.dayType == "WEEKLY_OFF" }
            ?: state.unworkedRules.find { it.dayType == "WEEKLY_OFF" }
        val holidayPayInr = holidayRule?.paymentInr ?: presentPayInr
        val weeklyOffPayInr = weeklyOffRule?.paymentInr ?: presentPayInr

        val isApprovedLeaveToday = active?.status?.equals("LEAVE", ignoreCase = true) == true ||
            state.todayRecords.any { it.status.equals("LEAVE", ignoreCase = true) }

        // =====================================================================
        // SECTION: PENDING ACTIONS (UNHANDLED EVENTS)
        // =====================================================================
        if (state.unhandledEvents.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Pending Actions (${state.unhandledEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink
                )
                Text(
                    text = "Claim your off-day or holiday pay, confirm leave, or acknowledge missed shifts.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = PresenceDesign.Muted
                )
            }
            val currentEvent = state.unhandledEvents.first()
            val totalEvents = state.unhandledEvents.size

            AnimatedContent(
                targetState = currentEvent,
                contentKey = { it.id },
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn() + scaleIn(initialScale = 0.92f))
                        .togetherWith(
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() + scaleOut(targetScale = 0.88f)
                        )
                },
                label = "poker_card_transition"
            ) { event ->
                val eventName = when (event.type) {
                    "WEEKLY_OFF" -> "Weekly Off"
                    "HOLIDAY" -> "Holiday"
                    "LEAVE" -> "Leave"
                    "ABSENT" -> "Missed Shift"
                    else -> event.title
                }

                val buttonLabel = if (event.amount > 0) {
                    "Claim ${Money.format(event.amount.toLong())} for $eventName"
                } else {
                    event.actionLabel.ifBlank {
                        when (event.type) {
                            "LEAVE" -> "Confirm Leave"
                            "ABSENT" -> "Acknowledge Missed Shift"
                            else -> "Claim $eventName"
                        }
                    }
                }

                val accentColor = when (event.type) {
                    "WEEKLY_OFF" -> Color(0xFF059669)
                    "HOLIDAY" -> Color(0xFF7C3AED)
                    "LEAVE" -> Color(0xFFD97706)
                    else -> Color(0xFF1E293B)
                }
                val borderColor = when (event.type) {
                    "WEEKLY_OFF" -> Color(0xFF86EFAC)
                    "HOLIDAY" -> Color(0xFFDDD6FE)
                    "LEAVE" -> Color(0xFFFDE68A)
                    else -> Color(0xFFFECDD3)
                }
                val headerGradient = when (event.type) {
                    "WEEKLY_OFF" -> Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFFFFFFF)))
                    "HOLIDAY" -> Brush.verticalGradient(listOf(Color(0xFFFAF5FF), Color(0xFFFFFFFF)))
                    "LEAVE" -> Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFFFFFF)))
                    else -> Brush.verticalGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFFFFF)))
                }
                val (iconVector, iconBg, iconTint) = when (event.type) {
                    "HOLIDAY" -> Triple(Icons.Default.Celebration, PresenceDesign.GrapeSoft, PresenceDesign.Grape)
                    "WEEKLY_OFF" -> Triple(Icons.Default.Weekend, PresenceDesign.PositiveSoft, PresenceDesign.Positive)
                    "LEAVE" -> Triple(Icons.Default.EventBusy, PresenceDesign.CautionSoft, PresenceDesign.CautionText)
                    else -> Triple(Icons.Default.WarningAmber, PresenceDesign.DangerSoft, PresenceDesign.DangerText)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (totalEvents > 2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .height(210.dp)
                                .offset(y = 12.dp)
                                .graphicsLayer {
                                    alpha = 0.45f
                                    rotationZ = -3.5f
                                },
                            shape = RoundedCornerShape(22.dp),
                            color = PresenceDesign.Surface,
                            border = BorderStroke(1.dp, PresenceDesign.Line.copy(alpha = 0.7f)),
                            shadowElevation = 2.dp
                        ) {}
                    }
                    if (totalEvents > 1) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(210.dp)
                                .offset(y = 6.dp)
                                .graphicsLayer {
                                    alpha = 0.75f
                                    rotationZ = 2.5f
                                },
                            shape = RoundedCornerShape(22.dp),
                            color = PresenceDesign.Surface,
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            shadowElevation = 4.dp
                        ) {}
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                shadowElevation = 10f
                                shape = RoundedCornerShape(22.dp)
                                clip = true
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, borderColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerGradient)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Top Header Row: Card counter pill & date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor)
                                            )
                                            Text(
                                                text = "CARD 1 OF $totalEvents",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                color = accentColor,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PresenceDesign.Surface,
                                        border = BorderStroke(1.dp, PresenceDesign.Line)
                                    ) {
                                        Text(
                                            text = event.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PresenceDesign.Ink,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Center Row: Icon, Title, Subtitle & Earnings Chip
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(iconBg)
                                            .border(1.dp, iconTint.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PresenceDesign.Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = event.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = PresenceDesign.Muted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (event.amount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.5.dp, Color(0xFF6EE7B7))
                                        ) {
                                            Text(
                                                text = "+${Money.format(event.amount.toLong())}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = PresenceDesign.Line.copy(alpha = 0.6f),
                                    thickness = 0.5.dp
                                )

                                // Action Button: Claim {amount} for {event}
                                Button(
                                    onClick = {
                                        android.util.Log.d("ClockInTab", "Claim button clicked for event: ${event.id}")
                                        onClaimUnhandledEvent(event)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !state.isClaimingEvent,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White,
                                        disabledContainerColor = accentColor.copy(alpha = 0.6f)
                                    )
                                ) {
                                    if (state.isClaimingEvent) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (event.amount > 0) Icons.Default.Celebration else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Text(
                                                text = buttonLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
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

        // =====================================================================
        // SECTION B: TODAY'S WORK - CONDITIONAL GREETING & WORK CARDS
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Today's Work",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PresenceDesign.Ink
            )
            Text(
                text = when (todayCardType) {
                    TodayCardType.CLOCKED_IN_SHIFT -> "Your regular shift is active and in progress."
                    TodayCardType.RUNNING_SHIFT -> "Shift clock-in window is open now."
                    TodayCardType.UPCOMING_SHIFT -> "Scheduled shift for later today."
                    TodayCardType.CLOCKED_IN_OVERTIME -> "Your overtime session is active and in progress."
                    TodayCardType.RUNNING_OVERTIME -> "Overtime clock-in window is open now."
                    TodayCardType.UPCOMING_OVERTIME -> "Approved overtime session for later today."
                    TodayCardType.HOLIDAY -> "Paid holiday status and earning rate for today."
                    TodayCardType.OFF_DAY -> "Scheduled rest day and earning rate for today."
                    TodayCardType.NO_WORK_SCHEDULED -> "No shifts or overtime scheduled for remaining hours today."
                },
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = PresenceDesign.Muted
            )
        }

        when (todayCardType) {
            // =============================================================
            // CARD 1: SHIFT CARD (Clocked In, Running, or Upcoming)
            // =============================================================
            TodayCardType.CLOCKED_IN_SHIFT,
            TodayCardType.RUNNING_SHIFT,
            TodayCardType.UPCOMING_SHIFT -> {
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Row: Work Title, Date, Premises, and Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PresenceDesign.BrandSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = PresenceDesign.Brand,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = workTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$todayDayName, $todayDateFormatted • Scheduled Shift • $premisesName",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = PresenceDesign.Muted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            when (todayCardType) {
                                TodayCardType.CLOCKED_IN_SHIFT -> PresenceBadge(label = "Shift in Progress", tone = PresenceTone.Success, dot = true)
                                TodayCardType.RUNNING_SHIFT -> PresenceBadge(label = "Shift Open", tone = PresenceTone.Warning, dot = true)
                                else -> PresenceBadge(label = "Upcoming Shift", tone = PresenceTone.Neutral)
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION A: TIME SECTION (Scheduled Window, Clock In Time, Clock Out Time, Current Status)
                        Text(
                            text = "Time Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresenceDesign.Ink
                        )

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Scheduled Window", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = "${Dates.formatTime(shiftStartTime)} – ${Dates.formatTime(shiftEndTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PresenceDesign.Ink
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Clock-In Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = if (isClockedIn) "${Dates.formatTime(active?.clockIn)} (${if (calculatedLateMinutes > 0) "$calculatedLateMinutes mins late" else "On time"})" else if (isApprovedLeaveToday) "Approved Leave" else "Not Clocked In",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isClockedIn) PresenceDesign.Positive else if (isApprovedLeaveToday) PresenceDesign.Brand else PresenceDesign.Danger
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Clock-Out Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = if (active?.clockOut != null) Dates.formatTime(active.clockOut) else if (isClockedIn) "Pending Clock Out" else "—",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PresenceDesign.Ink
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Evaluated Status", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = when (currentStatus) {
                                                "PRESENT" -> "Present"
                                                "HALF_DAY" -> "Half-Day"
                                                "LEAVE" -> "Approved Leave"
                                                "NOT_CLOCKED_IN" -> "Not Clocked In"
                                                "UPCOMING" -> "Upcoming"
                                                else -> "Absent"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (currentStatus) {
                                                "PRESENT" -> PresenceDesign.Positive
                                                "HALF_DAY" -> PresenceDesign.CautionText
                                                "LEAVE" -> PresenceDesign.Brand
                                                "NOT_CLOCKED_IN" -> PresenceDesign.CautionText
                                                "UPCOMING" -> PresenceDesign.Brand
                                                else -> PresenceDesign.Danger
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION B: ATTENDANCE STATUS TIMELINE SECTION
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Attendance Delivery Timeline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink
                            )

                            AttendanceTimelineView(
                                isClockedIn = isClockedIn,
                                isSessionRunningNow = isShiftWindowValid,
                                isApprovedLeave = isApprovedLeaveToday,
                                clockInTime = if (isClockedIn) active?.clockIn else null,
                                insideCount = insideCount,
                                outsideCount = outsideCount,
                                isCompleted = active?.clockOut != null
                            )
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION C: SHIFT EARNING RATES
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shift Earning Rates",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.Ink
                                )
                                Text(
                                    text = "Payment as per attendance status",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = PresenceDesign.Muted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Present",
                                    amountText = Money.format(presentPayInr),
                                    isSelected = currentStatus == "PRESENT",
                                    activeBgColor = PresenceDesign.PositiveSoft,
                                    activeTextColor = PresenceDesign.Positive,
                                    activeBorderColor = Color(0xFFA7F3D0)
                                )

                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Half-Day",
                                    amountText = Money.format(halfDayPayInr),
                                    isSelected = currentStatus == "HALF_DAY",
                                    activeBgColor = PresenceDesign.CautionSoft,
                                    activeTextColor = PresenceDesign.CautionText,
                                    activeBorderColor = Color(0xFFFDE68A)
                                )

                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Leave",
                                    amountText = Money.format(leavePayInr),
                                    isSelected = currentStatus == "LEAVE",
                                    activeBgColor = PresenceDesign.BrandSoft,
                                    activeTextColor = PresenceDesign.BrandDark,
                                    activeBorderColor = Color(0xFFC7D2FE)
                                )

                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Absent",
                                    amountText = Money.format(absentPayInr),
                                    isSelected = currentStatus == "ABSENT",
                                    activeBgColor = PresenceDesign.DangerSoft,
                                    activeTextColor = PresenceDesign.DangerText,
                                    activeBorderColor = Color(0xFFFECACA)
                                )
                            }

                            if (weeklyOffRule != null || holidayRule != null) {
                                Text(
                                    text = "Weekly Off: ₹${weeklyOffRule?.paymentInr ?: presentPayInr} • Holiday: ₹${holidayRule?.paymentInr ?: presentPayInr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = PresenceDesign.Muted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // GPS Monitoring Checks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS Monitoring Checks",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink
                            )
                            PresenceBadge(
                                label = if (isClockedIn) "Active Monitoring ($insideCount inside)" else "Inactive (Starts at Clock-In)",
                                tone = if (isClockedIn) PresenceTone.Success else PresenceTone.Neutral,
                                dot = isClockedIn
                            )
                        }

                        // Action Buttons (Clock In / Clock Out)
                        if (!isClockedIn) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (!isAccuracyValid || !isGeofenceValid) {
                                            pendingClockInAfterGps = true
                                            showGpsDialog = true
                                        } else {
                                            showCapture = true
                                        }
                                    },
                                    enabled = canClockIn,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PresenceDesign.Brand,
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(clockInButtonText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                if (!canClockIn) {
                                    Surface(
                                        color = PresenceDesign.CautionSoft,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = PresenceDesign.CautionText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            val guidanceReason = when {
                                                state.location == null -> "Waiting for GPS fix. Tap 'Update GPS' or 'Clock In' to calibrate."
                                                !isAccuracyValid -> "GPS accuracy is ±${accuracyMeters}m (Target: < 10m). Tap to calibrate."
                                                !isGeofenceValid -> "Move inside premises (${distanceMeters ?: "—"} m). Radius limit is $allowedRadiusMeters m to clock in."
                                                !isShiftWindowValid -> "Shift clock-in window is currently inactive."
                                                state.isSubmitting -> "Clocking in..."
                                                else -> "Clock-in is unavailable right now."
                                            }
                                            Text(
                                                text = guidanceReason,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PresenceDesign.CautionText
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = PresenceDesign.PositiveSoft,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(PresenceDesign.Positive)
                                            )
                                            Text(
                                                text = "Shift in Progress",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PresenceDesign.Positive,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = "GPS Checks: $insideCount inside",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PresenceDesign.Positive,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = onClockOut,
                                    enabled = !state.isSubmitting,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PresenceDesign.Danger,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Clock Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =============================================================
            // CARD 2: OVERTIME CARD (Clocked In, Running, or Upcoming)
            // =============================================================
            TodayCardType.CLOCKED_IN_OVERTIME,
            TodayCardType.RUNNING_OVERTIME,
            TodayCardType.UPCOMING_OVERTIME -> {
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Row: Overtime Title, Date, Premises, and Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PresenceDesign.GrapeSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Weekend,
                                        contentDescription = null,
                                        tint = PresenceDesign.Grape,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = workTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$todayDayName, $todayDateFormatted • Overtime Session • $premisesName",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = PresenceDesign.GrapeText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            when (todayCardType) {
                                TodayCardType.CLOCKED_IN_OVERTIME -> PresenceBadge(label = "Overtime in Progress", tone = PresenceTone.Purple, dot = true)
                                TodayCardType.RUNNING_OVERTIME -> PresenceBadge(label = "Overtime Open", tone = PresenceTone.Warning, dot = true)
                                else -> PresenceBadge(label = "Upcoming Overtime", tone = PresenceTone.Neutral)
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION A: TIME SECTION (Scheduled Window, Clock In Time, Clock Out Time, Current Status)
                        Text(
                            text = "Time Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresenceDesign.Ink
                        )

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Scheduled Window", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = "${Dates.formatTime(otStartTime)} – ${Dates.formatTime(otEndTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PresenceDesign.Ink
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Clock-In Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = if (isClockedIn) Dates.formatTime(active?.clockIn) else "Not Clocked In",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isClockedIn) PresenceDesign.Grape else PresenceDesign.Danger
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Clock-Out Time", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = if (active?.clockOut != null) Dates.formatTime(active.clockOut) else if (isClockedIn) "Pending Clock Out" else "—",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PresenceDesign.Ink
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Evaluated Status", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = PresenceDesign.Muted)
                                        Text(
                                            text = if (isClockedIn) "In Progress" else if (isOvertimeWindowValid) "Window Open" else "Scheduled",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isClockedIn) PresenceDesign.Grape else PresenceDesign.Brand
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION B: ATTENDANCE STATUS TIMELINE SECTION
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Attendance Delivery Timeline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink
                            )

                            AttendanceTimelineView(
                                isClockedIn = isClockedIn,
                                isSessionRunningNow = isOvertimeWindowValid,
                                isApprovedLeave = false,
                                clockInTime = if (isClockedIn) active?.clockIn else null,
                                insideCount = insideCount,
                                outsideCount = outsideCount,
                                isCompleted = active?.clockOut != null
                            )
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // SECTION C: OVERTIME PAY & SESSION DETAILS
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overtime Pay & Session Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.Ink
                                )
                                Text(
                                    text = "Agreed fixed sum for session",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = PresenceDesign.Muted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PayRuleChip(
                                    modifier = Modifier.weight(1.2f),
                                    label = "Agreed Payout",
                                    amountText = Money.format(overtimePayInr),
                                    isSelected = true,
                                    activeBgColor = PresenceDesign.PositiveSoft,
                                    activeTextColor = PresenceDesign.Positive,
                                    activeBorderColor = Color(0xFFA7F3D0)
                                )
                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Planned",
                                    amountText = "${activeOvertime?.plannedMinutes ?: 0} min",
                                    isSelected = false,
                                    activeBgColor = PresenceDesign.Workspace,
                                    activeTextColor = PresenceDesign.BodyText,
                                    activeBorderColor = PresenceDesign.Line
                                )
                                PayRuleChip(
                                    modifier = Modifier.weight(1f),
                                    label = "Status",
                                    amountText = activeOvertime?.status ?: "Approved",
                                    isSelected = false,
                                    activeBgColor = PresenceDesign.GrapeSoft,
                                    activeTextColor = PresenceDesign.GrapeText,
                                    activeBorderColor = Color(0xFFDDD6FE)
                                )
                            }
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        // GPS Monitoring Checks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS Monitoring Checks",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink
                            )
                            PresenceBadge(
                                label = if (isClockedIn) "Active Monitoring ($insideCount inside)" else "Inactive (Starts at Clock-In)",
                                tone = if (isClockedIn) PresenceTone.Purple else PresenceTone.Neutral,
                                dot = isClockedIn
                            )
                        }

                        // Action Buttons (Clock In / Clock Out)
                        if (!isClockedIn) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (!isAccuracyValid || !isGeofenceValid) {
                                            pendingClockInAfterGps = true
                                            showGpsDialog = true
                                        } else {
                                            showCapture = true
                                        }
                                    },
                                    enabled = canClockIn,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PresenceDesign.Grape,
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("Clock In (Overtime)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                if (!canClockIn) {
                                    Surface(
                                        color = PresenceDesign.CautionSoft,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = PresenceDesign.CautionText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            val guidanceReason = when {
                                                state.location == null -> "Waiting for GPS fix. Tap 'Update GPS' or 'Clock In' to calibrate."
                                                !isAccuracyValid -> "GPS accuracy is ±${accuracyMeters}m (Target: < 10m). Tap to calibrate."
                                                !isGeofenceValid -> "Move inside premises (${distanceMeters ?: "—"} m). Radius limit is $allowedRadiusMeters m to clock in."
                                                !isOvertimeWindowValid -> "Overtime clock-in window is currently inactive."
                                                state.isSubmitting -> "Clocking in..."
                                                else -> "Clock-in is unavailable right now."
                                            }
                                            Text(
                                                text = guidanceReason,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PresenceDesign.CautionText
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = PresenceDesign.GrapeSoft,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(PresenceDesign.Grape)
                                            )
                                            Text(
                                                text = "Overtime in Progress",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PresenceDesign.Grape,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = "GPS Checks: $insideCount inside",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PresenceDesign.GrapeText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = onClockOut,
                                    enabled = !state.isSubmitting,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PresenceDesign.Danger,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Clock Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =============================================================
            // CARD 3: PUBLIC HOLIDAY (Priority 3)
            // =============================================================
            TodayCardType.HOLIDAY -> {
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PresenceDesign.GrapeSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = PresenceDesign.Grape,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = todayHoliday?.name?.takeIf { it.isNotBlank() } ?: "Public Holiday",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink
                                    )
                                    Text(
                                        text = "$todayDayName, $todayDateFormatted • Paid Public Holiday",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = PresenceDesign.Grape,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            PresenceBadge(
                                label = "Paid Holiday",
                                tone = PresenceTone.Purple
                            )
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        Surface(
                            color = PresenceDesign.GrapeSoft,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = PresenceDesign.Grape,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Holiday Earning Rate:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PresenceDesign.GrapeText
                                    )
                                }
                                Text(
                                    text = "${Money.format(holidayPayInr)} / day",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PresenceDesign.Grape
                                )
                            }
                        }

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PresenceDesign.Muted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Today is a designated paid public holiday. Regular shifts are not scheduled for today, and holiday pay applies per company policy. If your team requires special holiday work, your manager can assign an Overtime session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PresenceDesign.Muted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // =============================================================
            // CARD 5: WEEKLY OFF / REST DAY (Priority 4)
            // =============================================================
            TodayCardType.OFF_DAY -> {
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PresenceDesign.BrandSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Weekend,
                                        contentDescription = null,
                                        tint = PresenceDesign.Brand,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Weekly Off",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink
                                    )
                                    Text(
                                        text = "$todayDayName, $todayDateFormatted • Scheduled Rest Day",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = PresenceDesign.Brand,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            PresenceBadge(
                                label = "Rest Day",
                                tone = PresenceTone.Neutral
                            )
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = PresenceDesign.Brand,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Weekly Off Earning Rate:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PresenceDesign.BodyText
                                    )
                                }
                                Text(
                                    text = "${Money.format(weeklyOffPayInr)} / day",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PresenceDesign.Brand
                                )
                            }
                        }

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PresenceDesign.Muted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Today is your scheduled weekly rest day. Take time to relax and recharge! If unscheduled work is required, an Overtime session can be assigned by your manager.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PresenceDesign.Muted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // =============================================================
            // CARD 6: NO WORK SCHEDULED TODAY (Empty Card)
            // =============================================================
            TodayCardType.NO_WORK_SCHEDULED -> {
                PresenceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "No Work Scheduled Today",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Ink
                                    )
                                    Text(
                                        text = "$todayDayName, $todayDateFormatted • Normal Working Day",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = PresenceDesign.Muted
                                    )
                                }
                            }

                            PresenceBadge(
                                label = "No Shifts",
                                tone = PresenceTone.Neutral
                            )
                        }

                        HorizontalDivider(color = PresenceDesign.Line)

                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line.copy(alpha = 0.7f)),
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
                                    tint = PresenceDesign.Muted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "You do not have any regular shifts scheduled for today, and no overtime is assigned in the next hours. If unscheduled work is required, your manager can assign an Overtime session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = PresenceDesign.BodyText,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Next Scheduled Shift Hint (if any in upcoming schedule)
                        val nextUpcoming = upcomingSchedule.firstOrNull { !it.isHoliday }
                        if (nextUpcoming != null) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Next: ${nextUpcoming.title} (${nextUpcoming.dayName}, ${nextUpcoming.dateFormatted})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = nextUpcoming.scheduledWindow,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = if (nextUpcoming.isOvertime) Money.format(nextUpcoming.agreedOvertimePay) else Money.format(nextUpcoming.presentPay),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION B.5: UPCOMING SCHEDULE - NEXT ROSTER & UPCOMING OVERTIME
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Upcoming Schedule",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF171A34)
                    )
                    Text(
                        text = "Next 7-day scheduled shifts & approved overtime",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF747895)
                    )
                }

                if (upcomingSchedule.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${upcomingSchedule.size} Upcoming",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (upcomingSchedule.isEmpty()) {
            Surface(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "No upcoming shifts in the next 7 days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF171A34)
                        )
                        Text(
                            text = "Your next scheduled shifts or assigned overtime will be shown here.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF747895)
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcomingSchedule.take(7).forEach { item ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row: Date Pill, Title + Schedule, and Primary Earning Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date pill
                                Surface(
                                    color = if (item.isTomorrow) Color(0xFFEEF2FF) else Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = if (item.isTomorrow) "Tomorrow" else item.dayName.take(3),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isTomorrow) Color(0xFF4F46E5) else Color(0xFF6B7280)
                                        )
                                        Text(
                                            text = item.dateFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF171A34)
                                        )
                                    }
                                }

                                // Info details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF171A34),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        PresenceBadge(
                                            label = item.badgeText,
                                            tone = item.badgeTone
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF747895),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = item.scheduledWindow,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = Color(0xFF747895)
                                        )
                                    }
                                }

                                // Primary earning rate pill (never labeled "Daily rate")
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    when {
                                        item.isOvertime -> {
                                            Text(
                                                text = Money.format(item.agreedOvertimePay),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED)
                                            )
                                            Text(
                                                text = "Agreed pay",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                        item.isHoliday -> {
                                            Text(
                                                text = if (item.holidayPay > 0) Money.format(item.holidayPay) else "Paid",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                            Text(
                                                text = "Holiday pay",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = if (item.presentPay > 0) Money.format(item.presentPay) else "—",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                            Text(
                                                text = "Present pay",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }
                            }

                            // Payment as attendance status chips (for Shift) or Session Note (for Overtime/Holiday)
                            if (!item.isOvertime && !item.isHoliday) {
                                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Earnings by attendance status",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6B7280)
                                        )
                                        Text(
                                            text = "Evaluated on completion",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 9.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusEarningMiniChip(
                                            modifier = Modifier.weight(1f),
                                            status = "Present",
                                            amount = Money.format(item.presentPay),
                                            bgColor = Color(0xFFE6F4EA),
                                            textColor = Color(0xFF137333),
                                            borderColor = Color(0xFFA7F3D0)
                                        )
                                        StatusEarningMiniChip(
                                            modifier = Modifier.weight(1f),
                                            status = "Half-Day",
                                            amount = Money.format(item.halfDayPay),
                                            bgColor = Color(0xFFFEF3C7),
                                            textColor = Color(0xFFD97706),
                                            borderColor = Color(0xFFFDE68A)
                                        )
                                        StatusEarningMiniChip(
                                            modifier = Modifier.weight(1f),
                                            status = "Leave",
                                            amount = Money.format(item.leavePay),
                                            bgColor = Color(0xFFEEF2FF),
                                            textColor = Color(0xFF4338CA),
                                            borderColor = Color(0xFFC7D2FE)
                                        )
                                        StatusEarningMiniChip(
                                            modifier = Modifier.weight(1f),
                                            status = "Absent",
                                            amount = Money.format(item.absentPay),
                                            bgColor = Color(0xFFFCE8E6),
                                            textColor = Color(0xFFC5221F),
                                            borderColor = Color(0xFFFECACA)
                                        )
                                    }
                                }
                            } else if (item.isOvertime) {
                                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Agreed session payout: ${Money.format(item.agreedOvertimePay)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF7C3AED)
                                    )
                                    Text(
                                        text = item.statusText?.let { "Status: $it" } ?: "Approved Overtime",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            } else if (item.isHoliday) {
                                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Paid public holiday • ${if (item.holidayPay > 0) Money.format(item.holidayPay) else "Full Day Compensation"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "Unworked compensation",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
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
                                    label = if (item.isOvertime) "Overtime" else "Shift",
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

        if (showGpsDialog) {
            GpsAccuracyDialog(
                targetAccuracyMeters = 10f,
                premisesLatitude = state.premisesLatitude,
                premisesLongitude = state.premisesLongitude,
                premisesRadiusMeters = state.premisesRadiusMeters,
                onLocationAcquired = { loc ->
                    onUpdateLocation(loc)
                    showGpsDialog = false
                    if (pendingClockInAfterGps) {
                        pendingClockInAfterGps = false
                        showCapture = true
                    }
                },
                onDismiss = {
                    showGpsDialog = false
                    pendingClockInAfterGps = false
                }
            )
        }

        if (showCapture) {
            val targetOvertimeId = if (isOvertimeWork) {
                active?.overtimeId ?: todayOvertime?.id ?: nextSession?.overtimeId
            } else null
            FaceCaptureDialog(
                engine = engine,
                mode = "VERIFY",
                premisesName = premisesName,
                distanceMeters = distanceMeters ?: 0,
                accuracyMeters = accuracyMeters,
                onCapturedSingle = { embedding ->
                    onFaceCaptured(embedding)
                    onClockIn(targetOvertimeId, embedding)
                    showCapture = false
                },
                onDismiss = { showCapture = false }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AttendanceTimelineView(
    isClockedIn: Boolean,
    isSessionRunningNow: Boolean,
    isApprovedLeave: Boolean,
    clockInTime: String?,
    insideCount: Int,
    outsideCount: Int,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        TimelineStep(
            stepNumber = "1",
            title = "Scheduled",
            subtitle = "Time set",
            isDone = true,
            isActive = !isClockedIn,
            tone = PresenceTone.Success,
            modifier = Modifier.weight(1f)
        )

        TimelineConnector(isDone = isClockedIn || isApprovedLeave)

        TimelineStep(
            stepNumber = "2",
            title = "Clock-In",
            subtitle = when {
                isClockedIn -> Dates.formatTime(clockInTime)
                isApprovedLeave -> "Leave"
                else -> "Absent"
            },
            isDone = isClockedIn,
            isActive = !isClockedIn,
            tone = when {
                isClockedIn -> PresenceTone.Success
                isApprovedLeave -> PresenceTone.Info
                else -> PresenceTone.Danger
            },
            modifier = Modifier.weight(1f)
        )

        TimelineConnector(isDone = isClockedIn)

        TimelineStep(
            stepNumber = "3",
            title = "GPS Check",
            subtitle = if (isClockedIn) "$insideCount in" else "Inactive",
            isDone = isClockedIn,
            isActive = isClockedIn && !isCompleted,
            tone = if (isClockedIn) PresenceTone.Info else PresenceTone.Neutral,
            modifier = Modifier.weight(1f)
        )

        TimelineConnector(isDone = isCompleted)

        TimelineStep(
            stepNumber = "4",
            title = "Clock-Out",
            subtitle = if (isCompleted) "Done" else "Pending",
            isDone = isCompleted,
            isActive = false,
            tone = if (isCompleted) PresenceTone.Success else PresenceTone.Neutral,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineStep(
    stepNumber: String,
    title: String,
    subtitle: String,
    isDone: Boolean,
    isActive: Boolean,
    tone: PresenceTone,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> when (tone) {
                            PresenceTone.Success -> Color(0xFF16A34A)
                            PresenceTone.Danger -> Color(0xFFDC2626)
                            PresenceTone.Info -> Color(0xFF0284C7)
                            else -> Color(0xFF2563EB)
                        }
                        isActive -> Color(0xFFFEF3C7)
                        else -> Color(0xFFE5E7EB)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone && tone != PresenceTone.Danger) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isDone -> Color.White
                        isActive -> Color(0xFFD97706)
                        else -> Color(0xFF9CA3AF)
                    }
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF171A34),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            color = if (tone == PresenceTone.Danger) Color(0xFFDC2626) else Color(0xFF747895),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineConnector(isDone: Boolean) {
    Box(
        modifier = Modifier
            .padding(top = 11.dp)
            .width(16.dp)
            .height(2.dp)
            .background(if (isDone) Color(0xFF16A34A) else Color(0xFFE5E7EB))
    )
}
