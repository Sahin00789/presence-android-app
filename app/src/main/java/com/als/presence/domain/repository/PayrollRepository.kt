package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*
import okhttp3.ResponseBody

interface PayrollRepository {
    suspend fun getMonthlyPayrollSummary(month: Int, year: Int, premisesId: String? = null): Resource<List<PayrollItemDto>>
    suspend fun getEarningLedger(month: Int? = null, year: Int? = null): Resource<List<StaffEarningLedgerDto>>
    suspend fun getMyMonthlyEarningLedger(month: Int, year: Int): Resource<MonthlyEarningLedgerResponseDto>
    suspend fun getStaffMonthlyEarningLedger(staffProfileId: String, month: Int, year: Int): Resource<MonthlyEarningLedgerResponseDto>
    suspend fun getMyOverallPayrollSummary(): Resource<MyOverallPayrollSummaryDto>
    suspend fun downloadPayslipPdf(month: Int, year: Int, staffProfileId: String? = null): Resource<ResponseBody>
    suspend fun recordPayrollTransaction(request: RecordPayrollTransactionRequest): Resource<PaymentRecordDto>
    suspend fun getUnworkedRules(employeeId: String? = null, premisesId: String? = null): Resource<List<UnworkedPaymentRuleDto>>
    suspend fun saveUnworkedRule(request: CreateUnworkedPaymentRuleRequest): Resource<UnworkedPaymentRuleDto>
}
