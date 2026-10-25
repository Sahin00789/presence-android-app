package com.als.presence.data.remote

import com.als.presence.data.model.ApiErrorDto
import com.als.presence.data.model.AttendanceEventDto
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.CorrectDailyAttendanceRequest
import com.als.presence.data.model.DailyAttendanceDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.ClockOutRequest
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.ManualClockInRequest
import com.als.presence.data.model.ManualClockInResponse
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ResolveExceptionRequest
import com.als.presence.data.model.ResolveExceptionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AttendanceApiService {

    @POST("api/v1/attendance/clock-in")
    suspend fun clockIn(@Body request: ClockInRequest): Response<AttendanceEventDto>

    @POST("api/v1/attendance/clock-in/manual")
    suspend fun clockInManual(@Body request: ManualClockInRequest): Response<ManualClockInResponse>

    @POST("api/v1/attendance/clock-out")
    suspend fun clockOut(@Body request: ClockOutRequest): Response<AttendanceEventDto>

    @GET("api/v1/attendance/records")
    suspend fun getRecords(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("staffProfileId") staffProfileId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<AttendanceRecordDto>>

    @GET("api/v1/attendance/exceptions")
    suspend fun getExceptions(): Response<List<AttendanceExceptionDto>>

    @POST("api/v1/attendance/exceptions/{exceptionId}/resolve")
    suspend fun resolveException(
        @Path("exceptionId") exceptionId: String,
        @Body request: ResolveExceptionRequest
    ): Response<ResolveExceptionResponse>

    @GET("api/v1/attendance/daily")
    suspend fun getDailyAttendance(
        @Query("workDate") workDate: String,
        @Query("premisesId") premisesId: String? = null
    ): Response<List<DailyAttendanceDto>>

    @POST("api/v1/attendance/daily/{id}/correct")
    suspend fun correctDailyAttendance(
        @Path("id") id: String,
        @Body request: CorrectDailyAttendanceRequest
    ): Response<DailyAttendanceDto>

    @GET("api/v1/attendance/my-history")
    suspend fun getMyEarningsHistory(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<DailyAttendanceDto>>
}

interface OvertimeApiService {

    @GET("api/v1/overtime")
    suspend fun list(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("staffProfileId") staffProfileId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<OvertimeDto>>

    @POST("api/v1/overtime")
    suspend fun create(@Body request: CreateOvertimeRequest): Response<OvertimeDto>

    @POST("api/v1/overtime/{overtimeId}/cancel")
    suspend fun cancel(@Path("overtimeId") overtimeId: String): Response<OvertimeDto>
}

/** Shared error body parser helper. */
internal fun parseApiError(body: ApiErrorDto?): String? = body?.message
