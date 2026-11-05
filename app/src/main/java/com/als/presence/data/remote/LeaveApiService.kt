package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LeaveApiService {
    @GET("api/v1/leave")
    suspend fun list(): Response<List<LeaveDto>>

    @GET("api/v1/leave/upcoming-shifts")
    suspend fun getUpcomingShifts(): Response<List<UpcomingShiftDto>>

    @POST("api/v1/leave")
    suspend fun create(@Body body: LeaveRequestCreate): Response<List<LeaveDto>>

    @POST("api/v1/leave/{id}/review")
    suspend fun review(
        @Path("id") id: String,
        @Body body: ReviewLeaveRequest
    ): Response<LeaveDto>
}
