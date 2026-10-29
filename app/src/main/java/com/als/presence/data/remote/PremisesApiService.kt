package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PremisesApiService {
    @GET("api/v1/premises")
    suspend fun list(): Response<List<PremisesDto>>

    @POST("api/v1/premises")
    suspend fun create(@Body request: CreatePremisesRequest): Response<PremisesDto>

    @PUT("api/v1/premises/{id}")
    suspend fun update(
        @Path("id") premisesId: String,
        @Body request: UpdatePremisesRequest
    ): Response<PremisesDto>
}
