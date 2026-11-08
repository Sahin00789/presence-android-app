package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ShiftApiService {
    @GET("api/v1/shifts/assignments")
    suspend fun listAssignments(
        @Query("premisesId") premisesId: String? = null
    ): Response<List<ShiftAssignmentDto>>

    @POST("api/v1/shifts/assignments")
    suspend fun createAssignment(@Body request: CreateShiftAssignmentRequest): Response<ShiftAssignmentDto>

    @DELETE("api/v1/shifts/assignments/{id}")
    suspend fun deleteAssignment(@Path("id") id: String): Response<Unit>
}
