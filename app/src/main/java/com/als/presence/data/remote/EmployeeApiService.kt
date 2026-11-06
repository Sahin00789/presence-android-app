package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface EmployeeApiService {
    @GET("api/v1/employees")
    suspend fun list(): Response<List<StaffDto>>

    @GET("api/v1/employees/me")
    suspend fun getSelf(): Response<EmployeeDto>

    @GET("api/v1/employees/{employeeId}")
    suspend fun getById(@Path("employeeId") employeeId: String): Response<EmployeeDto>

    @POST("api/v1/employees")
    suspend fun create(@Body request: CreateEmployeeRequest): Response<EmployeeDto>

    @PATCH("api/v1/employees/{employeeId}")
    suspend fun update(
        @Path("employeeId") employeeId: String,
        @Body request: UpdateEmployeeRequest
    ): Response<EmployeeDto>

    @DELETE("api/v1/employees/{employeeId}")
    suspend fun deactivate(@Path("employeeId") employeeId: String): Response<Unit>

    @GET("api/v1/organizations/members")
    suspend fun getMembers(): Response<List<OrganizationMemberDto>>
}
