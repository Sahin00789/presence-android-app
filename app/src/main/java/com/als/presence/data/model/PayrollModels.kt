package com.als.presence.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import com.als.presence.ui.dashboard.common.Money

object FlexibleAmountStringSerializer : KSerializer<String> {
    private val delegate = JsonElement.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return try {
            val element = decoder.decodeSerializableValue(delegate)
            if (element is JsonPrimitive) {
                element.content
            } else {
                "0.00"
            }
        } catch (_: Exception) {
            "0.00"
        }
    }
}

object FlexibleInrLongSerializer : KSerializer<Long> {
    private val delegate = JsonElement.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        return try {
            val element = decoder.decodeSerializableValue(delegate)
            if (element is JsonPrimitive) {
                element.longOrNull?.let { return it }
                element.doubleOrNull?.let { return Math.round(it) }
                val content = element.content.trim().replace("₹", "").replace(",", "")
                content.toLongOrNull()?.let { return it }
                content.toDoubleOrNull()?.let { return Math.round(it) }
            }
            0L
        } catch (_: Exception) {
            0L
        }
    }
}

typealias FlexiblePaiseLongSerializer = FlexibleInrLongSerializer

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
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val grossSalaryPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val additionsPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val deductionsPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val netSalaryPaise: Long = 0L,
    val lockedAt: String? = null,
    val lockedBy: String? = null
) {
    val startDate: String get() = periodStartDate
    val itemCount: Int get() = 0
    val grossSalaryInr: Long get() = grossSalaryPaise
    val additionsInr: Long get() = additionsPaise
    val deductionsInr: Long get() = deductionsPaise
    val netSalaryInr: Long get() = netSalaryPaise
    val grossSalaryCents: Long get() = grossSalaryInr
    val additionsCents: Long get() = additionsInr
    val deductionsCents: Long get() = deductionsInr
    val netSalaryCents: Long get() = netSalaryInr
}

@Serializable
data class PayrollItemDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String? = null,
    val monthlyPayrollId: String = "",
    val staffProfileId: String = "",
    val employeeId: String = "",
    val staffName: String? = null,
    val staffCode: String? = null,
    val staffEmail: String? = null,
    val employeeName: String? = null,
    val employeeCode: String? = null,
    val role: String? = null,
    val month: Int = 1,
    val year: Int = 2025,
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val periodStatus: String = "OPEN",
    val status: String = "OPEN",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val earnedAmount: String = "0.00",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val paidAmount: String = "0.00",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val dueAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val grossSalaryPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val additionsPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val deductionsPaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val netSalaryPaise: Long = 0L,
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val payment: PaymentRecordDto? = null
) {
    val resolvedStaffName: String get() = staffName ?: employeeName ?: "Employee"
    val resolvedStaffCode: String get() = staffCode ?: employeeCode ?: ""
    val resolvedEmployeeId: String get() = employeeId.ifBlank { staffProfileId.ifBlank { id } }
    val resolvedGrossSalaryPaise: Long get() = if (grossSalaryPaise != 0L) grossSalaryPaise else Money.parseToInr(earnedAmount) ?: 0L
    val resolvedNetSalaryPaise: Long get() = if (netSalaryPaise != 0L) netSalaryPaise else (resolvedGrossSalaryPaise - deductionsPaise).coerceAtLeast(0L)

    val effectiveStaffName: String get() = resolvedStaffName
    val effectiveStaffCode: String get() = resolvedStaffCode
    val effectiveEmployeeId: String get() = resolvedEmployeeId
    val effectiveGrossSalaryPaise: Long get() = resolvedGrossSalaryPaise
    val effectiveNetSalaryPaise: Long get() = resolvedNetSalaryPaise

    val grossSalaryInr: Long get() = resolvedGrossSalaryPaise
    val additionsInr: Long get() = additionsPaise
    val deductionsInr: Long get() = deductionsPaise
    val netSalaryInr: Long get() = resolvedNetSalaryPaise
    val grossSalaryCents: Long get() = grossSalaryInr
    val additionsCents: Long get() = additionsInr
    val deductionsCents: Long get() = deductionsInr
    val netSalaryCents: Long get() = netSalaryInr
}

@Serializable
data class PaymentRecordDto(
    val id: String = "",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val amount: String = "0",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val amountPaise: Long = 0L,
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val referenceNumber: String? = null,
    val reference: String? = referenceNumber,
    val recordedBy: String? = null
) {
    val paidAt: String get() = paymentDate
    val amountInr: Long get() = if (amountPaise != 0L) amountPaise else Money.parseToInr(amount) ?: 0L
    val amountCents: Long get() = amountInr
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
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val amount: String = "0",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val amountPaise: Long = 0L
) {
    val amountInr: Long get() = if (amountPaise != 0L) amountPaise else Money.parseToInr(amount) ?: 0L
    val amountCents: Long get() = amountInr
}

@Serializable
data class StaffEarningLedgerDto(
    val id: String = "",
    val organizationId: String = "",
    val staffProfileId: String = "",
    val staffId: String = staffProfileId,
    val type: String = "",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val amount: String = "0",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val amountPaise: Long = 0L,
    val earningDate: String = "",
    val createdAt: String = ""
) {
    val amountInr: Long get() = if (amountPaise != 0L) amountPaise else Money.parseToInr(amount) ?: 0L
    val amountCents: Long get() = amountInr
}

@Serializable
data class DailyEarningTotalDto(
    val earningDate: String = "",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val totalAmount: String = "0.00",
    val entryCount: Int = 0,
    val entries: List<StaffEarningLedgerDto> = emptyList()
)

@Serializable
data class MonthlyEarningLedgerResponseDto(
    val staffProfileId: String = "",
    val staffName: String = "",
    val staffCode: String = "",
    val premisesId: String? = null,
    val month: Int = 1,
    val year: Int = 2025,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val earnedAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val earnedPaise: Long = 0L,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val deductionAmount: String = "0.00",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val paidAmount: String = "0.00",
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val dueAmount: String = "0.00",
    val status: String = "OPEN",
    val attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    val dailyTotals: List<DailyEarningTotalDto> = emptyList(),
    val entries: List<StaffEarningLedgerDto> = emptyList(),
    val payments: List<PaymentRecordDto> = emptyList()
) {
    val earnedInr: Long get() = if (earnedPaise != 0L) earnedPaise else Money.parseToInr(earnedAmount) ?: 0L
}

@Serializable
data class MyOverallPayrollSummaryDto(
    val staffProfileId: String = "",
    val staffName: String = "",
    val staffCode: String = "",
    val premisesId: String? = null,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val totalEarnedAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val totalEarnedPaise: Long = 0L,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val totalPaidAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val totalPaidPaise: Long = 0L,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val totalDeductionAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val totalDeductionPaise: Long = 0L,
    @Serializable(with = FlexibleAmountStringSerializer::class)
    val totalDueAmount: String = "0.00",
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val totalDuePaise: Long = 0L,
    @Serializable(with = FlexiblePaiseLongSerializer::class)
    val totalBalancePaise: Long = 0L
) {
    val totalEarnedInr: Long get() = if (totalEarnedPaise != 0L) totalEarnedPaise else Money.parseToInr(totalEarnedAmount) ?: 0L
    val totalPaidInr: Long get() = if (totalPaidPaise != 0L) totalPaidPaise else Money.parseToInr(totalPaidAmount) ?: 0L
    val totalDeductionInr: Long get() = if (totalDeductionPaise != 0L) totalDeductionPaise else Money.parseToInr(totalDeductionAmount) ?: 0L
    val totalDueInr: Long get() = if (totalDuePaise != 0L) totalDuePaise else Money.parseToInr(totalDueAmount) ?: 0L
    val totalBalanceInr: Long get() = totalBalancePaise
}

@Serializable
data class RecordPayrollTransactionRequest(
    val monthlyPayrollId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String = "MANUAL_BANK_TRANSFER",
    val reference: String? = null
)
