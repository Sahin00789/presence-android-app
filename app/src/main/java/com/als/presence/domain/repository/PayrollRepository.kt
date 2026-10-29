package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface PayrollRepository {
    suspend fun getMonthlyPayrollSummary(month: Int, year: Int, premisesId: String? = null): Resource<List<PayrollItemDto>>
    suspend fun getEarningLedger(): Resource<List<StaffEarningLedgerDto>>
    suspend fun getMyPayslips(): Resource<List<PayrollItemDto>>
}
