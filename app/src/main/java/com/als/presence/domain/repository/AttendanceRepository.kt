package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface AttendanceRepository {
    suspend fun getAttendanceRecords(from: String? = null, to: String? = null, staffProfileId: String? = null): Resource<List<AttendanceRecordDto>>
    suspend fun getFaceStatus(): Resource<FaceStatusDto>
    suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto>
    suspend fun clockOut(eventId: String): Resource<ClockOutResponseDto>
    suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>>
    suspend fun resolveException(exceptionId: String, correctedStatus: String, reason: String, note: String? = null): Resource<Unit>
    suspend fun manualMarkShift(request: ManualMarkShiftAttendanceRequest): Resource<Unit>
    suspend fun manualMarkOvertime(request: ManualMarkOvertimeAttendanceRequest): Resource<Unit>

    // Location methods merged into Attendance Repository
    suspend fun getTodayPresence(): Resource<LocationTodayPresenceResponseDto>
    suspend fun getTodayLocationPresence(premisesId: String? = null): Resource<LocationTodayPresenceResponseDto>
    suspend fun getActiveLocationSession(): Resource<LocationSessionDto>
    suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto>
    suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit>
    suspend fun getShiftLocationPings(shiftAttendanceId: String): Resource<List<GpsPingSummaryDto>>
    suspend fun getOvertimeLocationPings(overtimeAttendanceId: String): Resource<List<GpsPingSummaryDto>>
    suspend fun getUnhandledEvents(): Resource<UnhandledEventsResponseDto>
    suspend fun claimUnhandledEvent(request: ClaimUnhandledEventRequestDto): Resource<ClaimUnhandledEventResponseDto>
}

