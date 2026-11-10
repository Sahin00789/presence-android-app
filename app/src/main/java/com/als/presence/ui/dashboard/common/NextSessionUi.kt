package com.als.presence.ui.dashboard.common

import com.als.presence.data.model.LocationStaffCardDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class SessionKind { SHIFT, OVERTIME }

/**
 * The one session that matters on the Attendance tab: the shift or overtime
 * that is running now, otherwise the earlier of today's scheduled sessions.
 *
 * It also carries the premises geofence so the clock-in step can tell whether
 * the fetched GPS fix is inside the premises before enabling face verification.
 */
data class NextSessionUi(
    val kind: SessionKind,
    val badge: String,
    val title: String,
    val window: String,
    val day: String,
    val isActive: Boolean,
    val startTime: String? = null,
    val endTime: String? = null,
    val halfDayThreshold: String? = null,
    val overtimeId: String? = null,
    val premisesLatitude: Double? = null,
    val premisesLongitude: Double? = null,
    val radiusMeters: Int = 50,
    val workDate: String? = null
)

/** Finds the caller's own card and turns it into the next-session summary. */
fun LocationTodayPresenceResponseDto.nextSessionFor(userId: String?): NextSessionUi? {
    if (userId.isNullOrBlank()) return null
    val card = staffList.firstOrNull {
        it.userId == userId || it.staffProfileId == userId || it.employeeId == userId
    } ?: if (staffList.size == 1) staffList.first() else null
    return card?.toNextSession(
        premisesLatitude = latitude,
        premisesLongitude = longitude,
        radiusMeters = radiusMeters ?: 50
    )
}

/**
 * Collapses a presence card into the one session that matters next:
 * an active overtime/shift if one is running, otherwise the earlier of the
 * day's scheduled shift and overtime. If today's session has already ended or
 * completed, returns null so the caller can look ahead to future shifts/overtimes.
 */
fun LocationStaffCardDto.toNextSession(
    premisesLatitude: Double? = null,
    premisesLongitude: Double? = null,
    radiusMeters: Int = 50
): NextSessionUi? {
    val activeOvertime = overtimeClockInTime != null && overtimeClockOutTime == null
    val activeShift = clockInTime != null && clockOutTime == null
    val hasShift = shiftName.isNotBlank() &&
        !shiftName.equals("No shift assigned", ignoreCase = true)

    val startShiftTime = regularShiftStartTime ?: assignedStartTime
    val endShiftTime = regularShiftEndTime ?: assignedEndTime

    val shiftCompleted = clockOutTime != null
    val shiftOver = shiftCompleted || Dates.isSessionOver(Dates.todayIso(), endShiftTime, startShiftTime)

    val overtimeCompleted = overtimeClockOutTime != null
    val overtimeOver = overtimeCompleted || Dates.isSessionOver(Dates.todayIso(), overtimeEndTime, overtimeStartTime)

    val isEligibleShift = hasShift && !shiftOver
    val isEligibleOvertime = hasOvertimeToday && !overtimeOver

    val isRunningShift = activeShift || (isEligibleShift && Dates.isWithinTimeWindow(startShiftTime, endShiftTime, bufferMinutesBeforeStart = 120L, bufferMinutesAfterEnd = 120L))
    val isRunningOvertime = activeOvertime || (isEligibleOvertime && Dates.isWithinTimeWindow(overtimeStartTime, overtimeEndTime, bufferMinutesBeforeStart = 120L, bufferMinutesAfterEnd = 120L))

    val overtimeIsEarlier = isEligibleOvertime && (
        !isEligibleShift ||
            startShiftTime == null ||
            (overtimeStartTime != null && overtimeStartTime < startShiftTime)
        )

    return when {
        activeOvertime -> session(
            kind = SessionKind.OVERTIME,
            badge = "ON OVERTIME",
            title = overtimeName ?: "Overtime",
            start = overtimeStartTime,
            end = overtimeEndTime,
            isActive = true,
            overtimeId = overtimeId,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        activeShift -> session(
            kind = SessionKind.SHIFT,
            badge = "ON SHIFT",
            title = shiftName,
            start = startShiftTime,
            end = endShiftTime,
            isActive = true,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        isRunningOvertime -> session(
            kind = SessionKind.OVERTIME,
            badge = "RUNNING OVERTIME",
            title = overtimeName ?: "Overtime",
            start = overtimeStartTime,
            end = overtimeEndTime,
            isActive = true,
            overtimeId = overtimeId,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        isRunningShift -> session(
            kind = SessionKind.SHIFT,
            badge = "RUNNING SHIFT",
            title = shiftName,
            start = startShiftTime,
            end = endShiftTime,
            isActive = true,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        overtimeIsEarlier -> session(
            kind = SessionKind.OVERTIME,
            badge = "NEXT OVERTIME",
            title = overtimeName ?: "Overtime",
            start = overtimeStartTime,
            end = overtimeEndTime,
            isActive = false,
            overtimeId = overtimeId,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        isEligibleShift -> session(
            kind = SessionKind.SHIFT,
            badge = "NEXT SHIFT",
            title = shiftName,
            start = startShiftTime,
            end = endShiftTime,
            isActive = false,
            premisesLatitude = premisesLatitude,
            premisesLongitude = premisesLongitude,
            radiusMeters = radiusMeters,
            workDate = Dates.todayIso()
        )

        else -> null
    }
}

private fun LocationStaffCardDto.session(
    kind: SessionKind,
    badge: String,
    title: String,
    start: String?,
    end: String?,
    isActive: Boolean,
    overtimeId: String? = null,
    premisesLatitude: Double? = null,
    premisesLongitude: Double? = null,
    radiusMeters: Int = 50,
    workDate: String? = Dates.todayIso()
) = NextSessionUi(
    kind = kind,
    badge = badge,
    title = title,
    window = Dates.formatWindow(start, end),
    day = dayOfWeek,
    isActive = isActive,
    startTime = start,
    endTime = end,
    overtimeId = overtimeId,
    premisesLatitude = premisesLatitude,
    premisesLongitude = premisesLongitude,
    radiusMeters = radiusMeters,
    workDate = workDate
)

/** Great-circle distance in metres between two WGS-84 points. */
fun distanceMeters(
    latitudeA: Double,
    longitudeA: Double,
    latitudeB: Double,
    longitudeB: Double
): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(latitudeB - latitudeA)
    val dLon = Math.toRadians(longitudeB - longitudeA)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(latitudeA)) * cos(Math.toRadians(latitudeB)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a))
}
