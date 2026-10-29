package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface HolidayApiService {
    @GET("api/v1/holidays")
    suspend fun list(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("premisesId") premisesId: String? = null
    ): Response<List<HolidayDto>>

    @POST("api/v1/holidays")
    suspend fun create(@Body request: CreateHolidayRequest): Response<HolidayDto>

    @PATCH("api/v1/holidays/{holidayId}")
    suspend fun update(
        @Path("holidayId") holidayId: String,
        @Body request: UpdateHolidayRequest
    ): Response<HolidayDto>

    @DELETE("api/v1/holidays/{holidayId}")
    suspend fun delete(@Path("holidayId") holidayId: String): Response<DeleteResponse>
}
