package com.als.presence.data.remote

import com.als.presence.data.model.CreatePayrollPeriodRequest
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.RecordPaymentRequest
import com.als.presence.data.model.WalletSummaryDto
import com.als.presence.data.model.WalletTransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WalletApiService {

    @GET("api/v1/wallet/summary")
    suspend fun summary(): Response<WalletSummaryDto>

    @GET("api/v1/wallet/transactions")
    suspend fun transactions(
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0
    ): Response<List<WalletTransactionDto>>
}

interface PayrollApiService {

    @GET("api/v1/payroll/periods")
    suspend fun periods(): Response<List<PayrollPeriodDto>>

    @GET("api/v1/payroll/periods/{periodId}/items")
    suspend fun items(
        @Path("periodId") periodId: String
    ): Response<List<PayrollItemDto>>

    @GET("api/v1/payroll/my-payslips")
    suspend fun myPayslips(): Response<List<PayslipDto>>

    @POST("api/v1/payroll/periods")
    suspend fun createPeriod(
        @Body request: CreatePayrollPeriodRequest
    ): Response<PayrollPeriodDto>

    @POST("api/v1/payroll/periods/{periodId}/calculate")
    suspend fun calculate(
        @Path("periodId") periodId: String
    ): Response<Map<String, String>>

    @POST("api/v1/payroll/periods/{periodId}/approve")
    suspend fun approve(
        @Path("periodId") periodId: String
    ): Response<Map<String, String>>

    @POST("api/v1/payroll/periods/{periodId}/submit")
    suspend fun submitPayrollPeriod(
        @Path("periodId") periodId: String
    ): Response<Map<String, String>>

    @POST("api/v1/payroll/items/{itemId}/payments")
    suspend fun recordPayment(
        @Path("itemId") itemId: String,
        @Body request: RecordPaymentRequest
    ): Response<kotlinx.serialization.json.JsonElement>
}
