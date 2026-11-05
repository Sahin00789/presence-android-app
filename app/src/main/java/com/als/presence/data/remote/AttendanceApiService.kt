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
}
