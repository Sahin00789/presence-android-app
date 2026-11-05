package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LocationApiService {
    @GET("api/v1/location/presence")
    suspend fun getTodayLocationPresence(
        @Query("premisesId") premisesId: String? = null,
        @Query("date") date: String? = null
    ): Response<LocationTodayPresenceResponseDto>

    @GET("api/v1/location/active")
    suspend fun getActiveLocationSession(): Response<LocationSessionDto>

    @POST("api/v1/location/checks")
    suspend fun submitLocationCheck(
        @Body body: LocationCheckRequestDto
    ): Response<LocationCheckResultDto>
}

