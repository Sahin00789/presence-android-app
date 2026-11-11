package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApiService {
    @GET("api/v1/attendance/records")
    suspend fun list(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("staffProfileId") staffProfileId: String? = null
    ): Response<List<AttendanceRecordDto>>

    @POST("api/v1/attendance/manual-mark")
    suspend fun manualMark(
        @Body request: ManualMarkAttendanceRequest
    ): Response<Unit>

    @POST("api/v1/attendance/shift/manual-mark")
    suspend fun manualMarkShift(
        @Body request: ManualMarkShiftAttendanceRequest
    ): Response<Unit>

    @POST("api/v1/attendance/overtime/manual-mark")
    suspend fun manualMarkOvertime(
        @Body request: ManualMarkOvertimeAttendanceRequest
    ): Response<Unit>

    @POST("api/v1/attendance/shift/clock-in")
    suspend fun clockInShift(
        @Body request: ClockInRequest
    ): Response<AttendanceRecordDto>

    @POST("api/v1/attendance/overtime/clock-in")
    suspend fun clockInOvertime(
        @Body request: ClockInRequest
    ): Response<AttendanceRecordDto>

    @POST("api/v1/attendance/clock-out")
    suspend fun clockOut(
        @Body request: ClockOutRequest
    ): Response<ClockOutResponseDto>

    @GET("api/v1/live-monitor/presence")
    suspend fun getTodayLocationPresence(
        @Query("premisesId") premisesId: String? = null,
        @Query("date") date: String? = null
    ): Response<LocationTodayPresenceResponseDto>

    @GET("api/v1/live-monitor/active")
    suspend fun getActiveLocationSession(): Response<LocationSessionDto>

    @POST("api/v1/attendance/shift/location-ping")
    suspend fun submitShiftLocationPing(
        @Body request: LocationCheckRequestDto
    ): Response<LocationCheckResultDto>

    @POST("api/v1/attendance/overtime/location-ping")
    suspend fun submitOvertimeLocationPing(
        @Body request: LocationCheckRequestDto
    ): Response<LocationCheckResultDto>

    @GET("api/v1/attendance/shift/location-pings")
    suspend fun getShiftLocationPings(
        @Query("shiftAttendanceId") shiftAttendanceId: String
    ): Response<List<GpsPingSummaryDto>>

    @GET("api/v1/attendance/overtime/location-pings")
    suspend fun getOvertimeLocationPings(
        @Query("overtimeAttendanceId") overtimeAttendanceId: String
    ): Response<List<GpsPingSummaryDto>>

    @GET("api/v1/attendance/unhandled-events")
    suspend fun getUnhandledEvents(): Response<UnhandledEventsResponseDto>

    @POST("api/v1/attendance/unhandled-events/claim")
    suspend fun claimUnhandledEvent(
        @Body request: ClaimUnhandledEventRequestDto
    ): Response<ClaimUnhandledEventResponseDto>
}

