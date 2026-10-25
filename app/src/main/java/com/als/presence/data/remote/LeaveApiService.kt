package com.als.presence.data.remote

import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.LeaveReviewRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LeaveApiService {
    @GET("api/v1/leave")
    suspend fun list(): Response<List<LeaveDto>>

    @POST("api/v1/leave")
    suspend fun request(@Body body: LeaveRequestCreate): Response<LeaveDto>

    @POST("api/v1/leave/{id}/review")
    suspend fun review(@Path("id") id: String, @Body body: LeaveReviewRequest): Response<LeaveDto>
}
