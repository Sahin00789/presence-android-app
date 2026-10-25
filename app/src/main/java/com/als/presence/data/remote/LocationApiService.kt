package com.als.presence.data.remote

import com.als.presence.data.model.LocationCheckRequest
import com.als.presence.data.model.LocationCheckResultDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LocationApiService {

    @POST("api/v1/location/checks")
    suspend fun submitCheck(@Body request: LocationCheckRequest): Response<LocationCheckResultDto>

    /**
     * Returns the active monitoring session of the authenticated staff member, if
     * any. Used by the periodic worker to resume checks after a process restart.
     */
    @GET("api/v1/location/active")
    suspend fun activeSession(): Response<ActiveLocationSessionDto>

    @GET("api/v1/location/presence")
    suspend fun getTodayPresence(
        @Query("premisesId") premisesId: String? = null,
        @Query("date") date: String? = null
    ): Response<LocationTodayPresenceResponseDto>
}

@Serializable
data class ActiveLocationSessionDto(
    val attendanceEventId: String? = null,
    val isActive: Boolean = false,
    val consecutiveFailures: Int = 0
)
