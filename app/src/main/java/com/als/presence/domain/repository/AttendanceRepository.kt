package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

/**
 * Domain repository interface for attendance tracking, clock-in, location checks, and exception management.
 */
interface AttendanceRepository {

    /** Retrieves attendance history records filtered by date range or staff ID. */
    suspend fun getAttendanceRecords(from: String? = null, to: String? = null, staffProfileId: String? = null): Resource<List<AttendanceRecordDto>>

    /** Fetches face verification enrollment status for current staff member. */
    suspend fun getFaceStatus(): Resource<FaceStatusDto>

    /** Submits facial embedding and GPS coordinates to perform clock-in. */
    suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto>

    /** Fetches attendance monitoring exceptions raised during active shift checks. */
    suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>>

    /** Resolves an attendance exception with manual attendance status correction. */
    suspend fun resolveException(exceptionId: String, correctedStatus: String, reason: String, note: String? = null): Resource<Unit>

    /** Allows Owner to manually mark shift attendance (PRESENT, ABSENT, HALF_DAY, LEAVE). */
    suspend fun manualMarkShift(request: ManualMarkShiftAttendanceRequest): Resource<Unit>

    /** Allows Owner to manually mark overtime session attendance. */
    suspend fun manualMarkOvertime(request: ManualMarkOvertimeAttendanceRequest): Resource<Unit>

    /** Fetches today's organization-wide attendance presence overview. */
    suspend fun getTodayPresence(): Resource<LocationTodayPresenceResponseDto>

    /** Fetches today's attendance presence for a specific premises. */
    suspend fun getTodayLocationPresence(premisesId: String? = null): Resource<LocationTodayPresenceResponseDto>

    /** Gets active location session details for current staff member. */
    suspend fun getActiveLocationSession(): Resource<LocationSessionDto>

    /** Submits periodic GPS location ping check during an active shift. */
    suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto>

    /** Assigns or updates assigned premises for an employee. */
    suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit>

    /** Gets historical GPS location pings recorded for a shift attendance event. */
    suspend fun getShiftLocationPings(shiftAttendanceId: String): Resource<List<GpsPingSummaryDto>>

    /** Gets historical GPS location pings recorded for an overtime attendance event. */
    suspend fun getOvertimeLocationPings(overtimeAttendanceId: String): Resource<List<GpsPingSummaryDto>>

    /** Retrieves unhandled realtime WebSocket system events. */
    suspend fun getUnhandledEvents(): Resource<UnhandledEventsResponseDto>

    /** Claims an unhandled realtime system event. */
    suspend fun claimUnhandledEvent(request: ClaimUnhandledEventRequestDto): Resource<ClaimUnhandledEventResponseDto>
}
