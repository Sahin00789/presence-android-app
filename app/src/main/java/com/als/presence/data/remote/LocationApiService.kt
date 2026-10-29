package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LocationApiService {
    @GET("api/v1/locations/today")
    suspend fun getToday(): Response<LocationTodayPresenceResponseDto>
}
