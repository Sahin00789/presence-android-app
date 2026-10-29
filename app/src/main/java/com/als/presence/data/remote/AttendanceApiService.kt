package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApiService {
    @GET("api/v1/attendance")
    suspend fun list(): Response<List<AttendanceRecordDto>>
}
