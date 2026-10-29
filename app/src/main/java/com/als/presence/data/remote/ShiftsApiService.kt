package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ShiftsApiService {
    @GET("api/v1/shifts")
    suspend fun list(
        @Query("premisesId") premisesId: String? = null
    ): Response<List<ShiftTemplateDto>>

    @POST("api/v1/shifts")
    suspend fun create(@Body request: ShiftTemplateDto): Response<ShiftTemplateDto>
}
