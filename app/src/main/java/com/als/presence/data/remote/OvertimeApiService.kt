package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

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
    suspend fun cancel(@Path("overtimeId") overtimeId: String): Response<Map<String, String>>
}
