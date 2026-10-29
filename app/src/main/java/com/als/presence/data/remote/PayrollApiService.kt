package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PayrollApiService {
    @GET("api/v1/payroll/monthly/summary")
    suspend fun listMonthlySummary(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Query("premisesId") premisesId: String? = null
    ): Response<List<PayrollItemDto>>

    @GET("api/v1/payroll/monthly")
    suspend fun getMonthlyPayroll(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Query("premisesId") premisesId: String? = null,
        @Query("staffProfileId") staffProfileId: String? = null
    ): Response<PayrollItemDto>

    @GET("api/v1/payroll/my-payslips")
    suspend fun getMyPayslips(): Response<List<PayrollItemDto>>
}
