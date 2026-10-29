package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.PayrollApiService
import com.als.presence.domain.repository.PayrollRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayrollRepositoryImpl @Inject constructor(
    private val payrollApiService: PayrollApiService
) : PayrollRepository {

    override suspend fun getMonthlyPayrollSummary(month: Int, year: Int, premisesId: String?): Resource<List<PayrollItemDto>> {
        return safeApiCall("Error loading payroll summary") {
            payrollApiService.listMonthlySummary(month, year, premisesId)
        }
    }

    override suspend fun getEarningLedger(): Resource<List<StaffEarningLedgerDto>> {
        return Resource.Success(emptyList())
    }

    override suspend fun getMyPayslips(): Resource<List<PayrollItemDto>> {
        return safeApiCall("Error loading payslips") {
            payrollApiService.getMyPayslips()
        }
    }
}
