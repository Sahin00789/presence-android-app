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

    @PUT("api/v1/shifts/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body request: UpdateShiftRequest
    ): Response<ShiftTemplateDto>

    @DELETE("api/v1/shifts/{id}")
    suspend fun delete(
        @Path("id") id: String
    ): Response<Unit>
}
