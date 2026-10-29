package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OrganizationApiService {
    @GET("api/v1/organization")
    suspend fun get(): Response<OrganizationDto>
}
