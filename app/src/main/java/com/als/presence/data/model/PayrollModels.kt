package com.als.presence.data.model

import kotlinx.serialization.Serializable

typealias PayslipDto = PayrollItemDto

@Serializable
data class PayrollPeriodDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String? = null,
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val periodStatus: String = "OPEN",
    val status: String = "OPEN",
    val grossSalaryCents: Long = 0L,
    val additionsCents: Long = 0L,
    val deductionsCents: Long = 0L,
    val netSalaryCents: Long = 0L,
    val lockedAt: String? = null,
    val lockedBy: String? = null
) {
    val startDate: String get() = periodStartDate
    val itemCount: Int get() = 0
}

@Serializable
data class PayrollItemDto(
    val id: String = "",
    val organizationId: String = "",
    val monthlyPayrollId: String = "",
    val staffProfileId: String = "",
    val employeeName: String? = null,
    val employeeCode: String? = null,
    val role: String? = null,
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val periodStatus: String = "OPEN",
    val grossSalaryCents: Long = 0L,
    val additionsCents: Long = 0L,
    val deductionsCents: Long = 0L,
    val netSalaryCents: Long = 0L,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val payment: PaymentRecordDto? = null
) {
    val staffName: String? get() = employeeName
    val staffCode: String? get() = employeeCode
    val staffEmail: String? get() = null
    val employeeId: String get() = staffProfileId
}

@Serializable
data class PaymentRecordDto(
    val id: String = "",
    val amountCents: Long = 0L,
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val referenceNumber: String? = null,
    val recordedBy: String? = null
) {
    val paidAt: String get() = paymentDate
}

@Serializable
data class PayrollBreakdownDto(
    val earnings: List<PayrollLineItemDto> = emptyList(),
    val deductions: List<PayrollLineItemDto> = emptyList()
)

@Serializable
data class PayrollLineItemDto(
    val code: String = "",
    val label: String = "",
    val amountCents: Long = 0L,
    val isDeduction: Boolean = false
)

@Serializable
data class StaffEarningLedgerDto(
    val id: String = "",
    val organizationId: String = "",
    val staffProfileId: String = "",
    val type: String = "",
    val amount: Long = 0L,
    val amountCents: Long = 0L,
    val earningDate: String = "",
    val createdAt: String = ""
)
