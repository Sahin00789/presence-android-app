package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.MonthlyEarningSnapshotEntity
import com.als.presence.data.local.PayrollSnapshotDao
import com.als.presence.data.local.PayrollSnapshotEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.PayrollApiService
import com.als.presence.domain.repository.PayrollRepository
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayrollRepositoryImpl @Inject constructor(
    private val payrollApiService: PayrollApiService,
    private val payrollSnapshotDao: PayrollSnapshotDao
) : PayrollRepository {

    override suspend fun getMonthlyPayrollSummary(month: Int, year: Int, premisesId: String?): Resource<List<PayrollItemDto>> {
        val result = safeApiCall("Error loading payroll summary") {
            payrollApiService.listMonthlySummary(month, year, premisesId)
        }
        return when (result) {
            is Resource.Success -> {
                val entities = result.data.map { dto ->
                    PayrollSnapshotEntity.fromDto(month, year, dto)
                }
                payrollSnapshotDao.insertPayrollSnapshots(entities)
                val cached = payrollSnapshotDao.getMonthlyPayrollSnapshots(month, year)
                val filtered = if (premisesId != null) {
                    cached.filter { it.premisesId == premisesId }
                } else cached
                Resource.Success(filtered.map { it.toDto() })
            }
            is Resource.Error -> {
                val cached = payrollSnapshotDao.getMonthlyPayrollSnapshots(month, year)
                if (cached.isNotEmpty()) {
                    val filtered = if (premisesId != null) {
                        cached.filter { it.premisesId == premisesId }
                    } else cached
                    Resource.Success(filtered.map { it.toDto() })
                } else {
                    result
                }
            }
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getEarningLedger(month: Int?, year: Int?): Resource<List<StaffEarningLedgerDto>> {
        val now = java.time.LocalDate.now()
        val m = month ?: now.monthValue
        val y = year ?: now.year
        return when (val res = getMyMonthlyEarningLedger(m, y)) {
            is Resource.Success -> Resource.Success(res.data.entries)
            is Resource.Error -> Resource.Error(res.exception, res.message)
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getMyMonthlyEarningLedger(month: Int, year: Int): Resource<MonthlyEarningLedgerResponseDto> {
        val result = safeApiCall("Error loading monthly earning ledger") {
            payrollApiService.getMyMonthlyEarningLedger(month, year)
        }
        return when (result) {
            is Resource.Success -> {
                payrollSnapshotDao.insertMonthlyEarningSnapshot(
                    MonthlyEarningSnapshotEntity.fromDto(result.data)
                )
                result
            }
            is Resource.Error -> {
                val cached = payrollSnapshotDao.getMonthlyEarningSnapshotsForMonth(month, year).firstOrNull()
                if (cached != null) {
                    Resource.Success(cached.toDto())
                } else {
                    result
                }
            }
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getStaffMonthlyEarningLedger(staffProfileId: String, month: Int, year: Int): Resource<MonthlyEarningLedgerResponseDto> {
        val result = safeApiCall("Error loading staff monthly earning ledger") {
            payrollApiService.getStaffMonthlyEarningLedger(month, year, staffProfileId)
        }
        return when (result) {
            is Resource.Success -> {
                payrollSnapshotDao.insertMonthlyEarningSnapshot(
                    MonthlyEarningSnapshotEntity.fromDto(result.data)
                )
                result
            }
            is Resource.Error -> {
                val cached = payrollSnapshotDao.getMonthlyEarningSnapshotsForMonth(month, year)
                    .firstOrNull { it.staffProfileId == staffProfileId }
                if (cached != null) {
                    Resource.Success(cached.toDto())
                } else {
                    result
                }
            }
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getMyOverallPayrollSummary(): Resource<MyOverallPayrollSummaryDto> {
        return safeApiCall("Error loading overall payroll summary") {
            payrollApiService.getMyOverallPayrollSummary()
        }
    }

    override suspend fun downloadPayslipPdf(month: Int, year: Int, staffProfileId: String?): Resource<ResponseBody> {
        return safeApiCall("Error downloading payslip PDF") {
            payrollApiService.downloadPayslipPdf(month, year, staffProfileId)
        }
    }

    override suspend fun recordPayrollTransaction(request: RecordPayrollTransactionRequest): Resource<PaymentRecordDto> {
        return safeApiCall("Error recording payment transaction") {
            payrollApiService.recordTransaction(request)
        }
    }

    override suspend fun getUnworkedRules(employeeId: String?, premisesId: String?): Resource<List<UnworkedPaymentRuleDto>> {
        return safeApiCall("Error loading unworked rules") {
            payrollApiService.getUnworkedRules(employeeId, premisesId)
        }
    }

    override suspend fun saveUnworkedRule(request: CreateUnworkedPaymentRuleRequest): Resource<UnworkedPaymentRuleDto> {
        return safeApiCall("Error saving unworked rule") {
            payrollApiService.saveUnworkedRule(request)
        }
    }
}
