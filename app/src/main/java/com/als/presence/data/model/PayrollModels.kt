package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyPayrollDto(
    val id: String,
    val organizationId: String,
    val premisesId: String,
    val employeeId: String,
    val staffName: String? = null,
    val staffCode: String? = null,
    val staffEmail: String? = null,
    val month: Int,
    val year: Int,
    val earnedAmount: String,
    val deductionAmount: String,
    val paidAmount: String,
    val dueAmount: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class MonthlyAttendanceStatsDto(
    val id: String,
    val organizationId: String,
    val premisesId: String,
    val employeeId: String,
    val month: Int,
    val year: Int,
    val presentShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0,
    val overtimeCount: Int = 0
)

@Serializable
data class StaffEarningLedgerDto(
    val id: String,
    val organizationId: String,
    val premisesId: String,
    val employeeId: String,
    val earningDate: String,
    val type: String,
    val amount: String,
    val shiftAssignmentId: String? = null,
    val attendanceEventId: String? = null,
    val overtimeId: String? = null,
    val createdAt: String
)

@Serializable
data class DailyEarningTotalDto(
    val earningDate: String,
    val totalAmount: String,
    val entryCount: Int,
    val entries: List<StaffEarningLedgerDto> = emptyList()
)

@Serializable
data class PayrollDeductionDto(
    val id: String,
    val monthlyPayrollId: String,
    val amount: String,
    val reason: String,
    val createdBy: String? = null,
    val createdAt: String
)

@Serializable
data class PayrollTransactionDto(
    val id: String,
    val monthlyPayrollId: String,
    val amount: String,
    val paymentDate: String,
    val paymentMethod: String,
    val reference: String? = null,
    val createdBy: String? = null,
    val createdAt: String
)

@Serializable
data class CreateDeductionRequest(
    val monthlyPayrollId: String,
    val amount: Double,
    val reason: String
)

@Serializable
data class RecordPayrollTransactionRequest(
    val monthlyPayrollId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String = "MANUAL_BANK_TRANSFER",
    val reference: String? = null
)
