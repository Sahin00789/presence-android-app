package com.als.presence.data.remote

import com.als.presence.data.model.*
import okhttp3.ResponseBody
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

    @GET("api/v1/payroll/my-monthly-earning-ledger")
    suspend fun getMyMonthlyEarningLedger(
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<MonthlyEarningLedgerResponseDto>

    @GET("api/v1/payroll/my-overall-summary")
    suspend fun getMyOverallPayrollSummary(): Response<MyOverallPayrollSummaryDto>

    @GET("api/v1/payroll/payslip/download")
    @Streaming
    suspend fun downloadPayslipPdf(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Query("staffProfileId") staffProfileId: String? = null
    ): Response<ResponseBody>

    @POST("api/v1/payroll/transactions")
    suspend fun recordTransaction(
        @Body request: RecordPayrollTransactionRequest
    ): Response<PaymentRecordDto>
}
