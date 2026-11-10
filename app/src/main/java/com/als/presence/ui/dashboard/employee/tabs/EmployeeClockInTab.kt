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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
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
    onClockIn: (overtimeId: String?) -> Unit,
    onClockOut: () -> Unit = {},
    engine: FaceEmbeddingEngine,
    onFaceCaptured: (FaceEmbeddingEngine.Embedding) -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    onSilentRefresh: () -> Unit = onRefresh,
    onClaimUnhandledEvent: (UnhandledEventItemDto) -> Unit = {}
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
    val todayOvertime = state.openOvertime.firstOrNull {
        Dates.isToday(it.workDate) &&
            (it.status in listOf("APPROVED", "IN_PROGRESS", "CREATED", "REQUESTED")) &&
            !Dates.isSessionOver(it.workDate, it.endTime, it.startTime)
    }

    val todayIso = Dates.todayIso()
    val todayDayName = Dates.getDayNameByDate(todayIso)
    val todayDateFormatted = Dates.formatDate(todayIso)

    val workDateIso = active?.workDate ?: nextSession?.workDate ?: todayOvertime?.workDate ?: todayIso
    val workDayName = Dates.getDayNameByDate(workDateIso)
    val workDateFormatted = Dates.formatDate(workDateIso)
    val isSameDay = Dates.isToday(workDateIso)

    val todayHoliday = state.holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
    val isTodayHoliday = todayHoliday != null

    val hasMatchingShiftToday = state.assignedShifts.any { a ->
        val template = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
        template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
    } || (state.assignedShifts.isEmpty() && state.shiftTemplates.any { t ->
        Dates.isShiftMatchingDay(t.dayName, todayIso, t.daysOfWeek)
    }) || (nextSession?.kind == SessionKind.SHIFT && (nextSession.day.equals(todayDayName, ignoreCase = true) || nextSession.day.equals("Scheduled Shift", ignoreCase = true)))
    val isTodayOffDay = !isTodayHoliday && !hasMatchingShiftToday
    val isNonWorkingDay = isTodayHoliday || isTodayOffDay
    val hasTodayOvertime = isRunningOvertime || todayOvertime != null || (isSameDay && (nextSession?.kind == SessionKind.OVERTIME || nextSession?.overtimeId != null))

    val showOffDayHolidayGreeting = isNonWorkingDay && !hasTodayOvertime && !isClockedIn

    val hasScheduledWork = isClockedIn || nextSession != null || todayOvertime != null

    val rawTitle = when {
        isRunningOvertime -> "Overtime Session"
        isRunningShift -> active?.shiftName?.ifBlank { "Assigned Shift" } ?: "Assigned Shift"
        nextSession != null && nextSession.kind == SessionKind.OVERTIME -> nextSession.title.ifBlank { "Overtime Session" }
        nextSession != null && !nextSession.title.equals("day", ignoreCase = true) && nextSession.title.isNotBlank() -> nextSession.title
        todayOvertime != null -> todayOvertime.reason?.takeIf { it.isNotBlank() } ?: "Overtime Session"
        else -> if (hasTodayOvertime) "Overtime Session" else "Assigned Shift"
    }
    val workTitle = if (rawTitle.equals("day", ignoreCase = true) || rawTitle.equals("No Shift Scheduled", ignoreCase = true) || rawTitle.isBlank()) {
        if (hasTodayOvertime) "Overtime Session" else "Assigned Shift"
    } else rawTitle

    val isOvertimeWork = isRunningOvertime || (!isClockedIn && (nextSession?.kind == SessionKind.OVERTIME || nextSession?.overtimeId != null || todayOvertime != null))

    val startTimeRaw = when {
        isRunningOvertime -> active?.assignedStartTime ?: nextSession?.startTime ?: todayOvertime?.startTime
        isRunningShift -> active?.assignedStartTime ?: nextSession?.startTime
        nextSession != null -> nextSession.startTime
        todayOvertime != null -> todayOvertime.startTime
        else -> null
    }

    val endTimeRaw = when {
        isRunningOvertime -> active?.assignedEndTime ?: nextSession?.endTime ?: todayOvertime?.endTime
        isRunningShift -> active?.assignedEndTime ?: nextSession?.endTime
        nextSession != null -> nextSession.endTime
        todayOvertime != null -> todayOvertime.endTime
        else -> null
    }

    val isTimeWindowValid = if (hasScheduledWork && isSameDay) Dates.isWithinTimeWindow(startTimeRaw, endTimeRaw) else false
    val canClockIn = !isClockedIn && hasScheduledWork && isSameDay && isGeofenceValid && isTimeWindowValid && !state.isSubmitting

    val isSessionRunningNow = !isClockedIn && isSameDay && (nextSession?.isActive == true || (hasScheduledWork && isTimeWindowValid))

    // Pay Rules calculation for the active/upcoming shift
    val currentAssignment = state.assignedShifts.firstOrNull { a ->
        val tmpl = state.shiftTemplates.firstOrNull { it.id == a.shiftId }
        tmpl != null && Dates.isShiftMatchingDay(tmpl.dayName, workDateIso, tmpl.daysOfWeek)
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
    val isTooLateForHalfDay = isSameDay && Dates.isPastHalfDayThreshold(startTimeRaw, halfDayThresholdStr)
    val clockInButtonText = when {
        !isSameDay -> "Clock In (Upcoming)"
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(horizontal = if (isScrollable) 12.dp else 0.dp)
            .padding(top = if (isScrollable) 10.dp else 0.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    when {
                        isClockedIn -> PresenceBadge(label = "On Duty", tone = PresenceTone.Success, dot = true)
                        isSessionRunningNow -> PresenceBadge(label = "Shift Open", tone = PresenceTone.Info, dot = true)
                        isTodayHoliday -> PresenceBadge(label = "Public Holiday", tone = PresenceTone.Purple)
                        isTodayOffDay -> PresenceBadge(label = "Rest Day", tone = PresenceTone.Neutral)
                        else -> PresenceBadge(label = "Upcoming", tone = PresenceTone.Neutral)
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
                                    text = "±$accuracyMeters m",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (accuracyMeters <= 20) PresenceDesign.Positive else PresenceDesign.CautionText
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
                            color = PresenceDesign.Muted
                        )
                    }

                    OutlinedButton(
                        onClick = onLocate,
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
                                imageVector = Icons.Default.Refresh,
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
                text = "Scheduled shifts, overtime sessions, and off-day status for today.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = PresenceDesign.Muted
            )
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

        if (state.unhandledEvents.isNotEmpty()) {
            val currentEvent = state.unhandledEvents.first()
            val totalEvents = state.unhandledEvents.size

            AnimatedContent(
                targetState = currentEvent,
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn() + scaleIn(initialScale = 0.92f))
                        .togetherWith(
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() + scaleOut(targetScale = 0.88f)
                        )
                },
                label = "poker_card_transition"
            ) { event ->
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
                                    rotationZ = -3f
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = PresenceDesign.Surface,
                            border = BorderStroke(1.dp, PresenceDesign.Line),
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
                                    alpha = 0.7f
                                    rotationZ = 2f
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = PresenceDesign.Surface,
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            shadowElevation = 3.dp
                        ) {}
                    }

                    PresenceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                shadowElevation = 8f
                                shape = RoundedCornerShape(20.dp)
                                clip = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PresenceDesign.Brand)
                                    )
                                    Text(
                                        text = "Action 1 of $totalEvents",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PresenceDesign.Brand
                                    )
                                }

                                PresenceBadge(
                                    label = event.date,
                                    tone = PresenceTone.Neutral
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (iconVector, iconBg, iconTint) = when (event.type) {
                                    "HOLIDAY" -> Triple(Icons.Default.Celebration, PresenceDesign.GrapeSoft, PresenceDesign.Grape)
                                    "WEEKLY_OFF" -> Triple(Icons.Default.Weekend, PresenceDesign.PositiveSoft, PresenceDesign.Positive)
                                    "LEAVE" -> Triple(Icons.Default.EventBusy, PresenceDesign.CautionSoft, PresenceDesign.CautionText)
                                    else -> Triple(Icons.Default.WarningAmber, PresenceDesign.DangerSoft, PresenceDesign.DangerText)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
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
                                        shape = RoundedCornerShape(10.dp),
                                        color = PresenceDesign.PositiveSoft,
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Text(
                                            text = "+${Money.format(event.amount.toLong())}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PresenceDesign.Positive,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = PresenceDesign.Line,
                                thickness = 0.5.dp
                            )

                            Button(
                                onClick = { onClaimUnhandledEvent(event) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !state.isClaimingEvent,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (event.type) {
                                        "ABSENT" -> PresenceDesign.Ink
                                        else -> PresenceDesign.Brand
                                    },
                                    contentColor = Color.White
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
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = event.actionLabel.ifBlank { "Claim / Acknowledge" },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
        // CARD 1: Public Holiday Greeting Card
        if (isTodayHoliday) {
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
                                    text = "$todayDayName, $todayDateFormatted • Paid Day Off",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PresenceDesign.Grape,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        PresenceBadge(
                            label = "Public Holiday",
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
                }
            }
        }

        // CARD 2: Weekly Off Greeting Card
        if (isTodayOffDay && !isTodayHoliday) {
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
                                    imageVector = Icons.Default.WavingHand,
                                    contentDescription = null,
                                    tint = PresenceDesign.Brand,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (hasMatchingShiftToday) "Premises Weekly Off" else "My Weekly Off",
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
                            label = "Weekly Off",
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
                }
            }
        }

        // CARDS 3 & 4: SHIFT CARD and OVERTIME CARD
        if (hasScheduledWork) {
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
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = workTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$workDayName, $workDateFormatted • ${if (isOvertimeWork) "Overtime" else "Shift"} • $premisesName",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = PresenceDesign.Muted
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        when {
                            isClockedIn -> PresenceBadge(label = "In Progress", tone = PresenceTone.Success, dot = true)
                            isSessionRunningNow -> PresenceBadge(label = "Window Open", tone = PresenceTone.Warning, dot = true)
                            else -> PresenceBadge(label = "Upcoming", tone = PresenceTone.Neutral)
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
                                        text = "${Dates.formatTime(startTimeRaw)} – ${Dates.formatTime(endTimeRaw)}",
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

                    // SECTION B: ATTENDANCE STATUS TIMELINE SECTION (Delivery Timeline Style)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Attendance Delivery Timeline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresenceDesign.Ink
                        )

                        AttendanceTimelineView(
                            isClockedIn = isClockedIn,
                            isSessionRunningNow = isTimeWindowValid,
                            isApprovedLeave = isApprovedLeaveToday,
                            clockInTime = if (isClockedIn) active?.clockIn else null,
                            insideCount = insideCount,
                            outsideCount = outsideCount,
                            isCompleted = active?.clockOut != null
                        )
                    }

                    HorizontalDivider(color = PresenceDesign.Line)

                    // SECTION C: PAY RATES SECTION (Shift 4 outcome chips with active highlighted / Overtime compensation breakdown)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isOvertimeWork) "Overtime Pay & Session Details" else "Shift Earning Rates",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PresenceDesign.Ink
                            )
                            Text(
                                text = if (isOvertimeWork) "Agreed fixed sum for session" else "Payment as per attendance status",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = PresenceDesign.Muted
                            )
                        }

                        if (isOvertimeWork) {
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
                                    activeBgColor = PresenceDesign.BrandSoft,
                                    activeTextColor = PresenceDesign.BrandDark,
                                    activeBorderColor = Color(0xFFC7D2FE)
                                )
                            }
                        } else {
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
                            label = if (isClockedIn) "Active Monitoring ($insideCount inside)" else "Inactive",
                            tone = if (isClockedIn) PresenceTone.Success else PresenceTone.Neutral,
                            dot = isClockedIn
                        )
                    }

                    // Action Buttons (Clock In / Clock Out)
                    if (!isClockedIn) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCapture = true },
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
                                            !hasScheduledWork -> "No shift or overtime is scheduled."
                                            !isSameDay -> "Shift is scheduled for $workDayName ($workDateFormatted). Clock-in opens on shift date."
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
                                            color = PresenceDesign.CautionText
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                upcomingSchedule.take(4).forEach { item ->
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
        verticalAlignment = Alignment.CenterVertically
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
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            color = if (tone == PresenceTone.Danger) Color(0xFFDC2626) else Color(0xFF747895),
            maxLines = 1
        )
    }
}

@Composable
private fun TimelineConnector(isDone: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(2.dp)
            .background(if (isDone) Color(0xFF16A34A) else Color(0xFFE5E7EB))
    )
}
