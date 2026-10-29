package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface EmployeeApiService {
    @GET("api/v1/employees")
    suspend fun list(): Response<List<StaffDto>>
}
