package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ShiftTemplateDto(
    val id: String,
    val organizationId: String,
    val premisesId: String? = null,
    val name: String,
    val shiftName: String = name,
    val dayName: String = "Everyday",
    val startTime: String,
    val endTime: String,
    val halfDayThreshold: String = "11:00",
    val breakMinutes: Int = 0,
    val lateGraceMinutes: Int = 15,
    val halfDayThresholdMinutes: Int = 240,
    val daysOfWeek: List<Int>? = null,
    val lateClockInAllowed: Boolean = true,
    val overnight: Boolean = false,
    val isActive: Boolean = true
)

@Serializable
data class ShiftAssignmentDto(
    val id: String,
    val organizationId: String? = null,
    val premisesId: String? = null,
    val staffProfileId: String,
    val staffId: String = staffProfileId,
    val shiftTemplateId: String,
    val shiftId: String = shiftTemplateId,
    val staffName: String,
    val shiftName: String,
    val presentPay: Double = 0.0,
    val absentPay: Double = 0.0,
    val leavePay: Double = 0.0,
    val halfDayPay: Double = 0.0,
    val startTime: String? = null,
    val endTime: String? = null,
    val daysOfWeek: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val paymentCents: Long? = null,
    val lateClockInAllowed: Boolean? = true,
    val isActive: Boolean = true
)

@Serializable
data class CreateShiftAssignmentRequest(
    val organizationId: String? = null,
    val premisesId: String? = null,
    val staffProfileId: String,
    val staffId: String = staffProfileId,
    val shiftTemplateId: String,
    val shiftId: String = shiftTemplateId,
    val presentPay: Double = 0.0,
    val absentPay: Double = 0.0,
    val leavePay: Double = 0.0,
    val halfDayPay: Double = 0.0,
    val startTime: String? = null,
    val endTime: String? = null,
    val daysOfWeek: List<Int>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val paymentCents: Long? = null,
    val lateClockInAllowed: Boolean? = true,
    val isActive: Boolean = true
)

@Serializable
data class DayShiftAssignmentRequest(
    val shiftTemplateId: String,
    val daysOfWeek: List<Int>,
    val presentPay: String? = null,
    val halfDayPay: String? = null,
    val leavePay: String? = null,
    val absentPay: String? = null,
    val lateClockInAllowed: Boolean? = true
)

@Serializable
data class CreateShiftTemplateRequest(
    val organizationId: String? = null,
    val name: String,
    val shiftName: String = name,
    val dayName: String = "Everyday",
    val premisesId: String? = null,
    val startTime: String,
    val endTime: String,
    val halfDayThreshold: String = "11:00",
    val breakMinutes: Int = 0,
    val lateGraceMinutes: Int = 15,
    val halfDayThresholdMinutes: Int = 240,
    val daysOfWeek: List<Int>? = null,
    val lateClockInAllowed: Boolean = true,
    val isActive: Boolean = true
)

@Serializable
data class UpdateShiftTemplateRequest(
    val name: String? = null,
    val shiftName: String? = name,
    val dayName: String? = null,
    val premisesId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val halfDayThreshold: String? = null,
    val breakMinutes: Int? = null,
    val lateGraceMinutes: Int? = null,
    val halfDayThresholdMinutes: Int? = null,
    val daysOfWeek: List<Int>? = null,
    val lateClockInAllowed: Boolean? = null,
    val isActive: Boolean? = null
)

@Serializable
data class ShiftPaymentRuleDto(
    val id: String,
    val organizationId: String,
    val staffProfileId: String? = null,
    val shiftTemplateId: String? = null,
    val attendanceStatus: String,
    val paymentCents: Long,
    val penaltyCents: Long = 0,
    val effectiveFrom: String,
    val createdAt: String
)

@Serializable
data class CreateShiftPaymentRuleRequest(
    val staffProfileId: String? = null,
    val shiftTemplateId: String? = null,
    val attendanceStatus: String = "PRESENT",
    val paymentCents: Long,
    val penaltyCents: Long = 0,
    val effectiveFrom: String
)

@Serializable
data class UnworkedPaymentRuleDto(
    val id: String,
    val organizationId: String,
    val staffProfileId: String? = null,
    val dayType: String,
    val paymentCents: Long = 0,
    val multiplierBasisPoints: Int = 15000,
    val effectiveFrom: String,
    val createdAt: String
)

@Serializable
data class CreateUnworkedPaymentRuleRequest(
    val staffProfileId: String? = null,
    val dayType: String,
    val paymentCents: Long = 0,
    val multiplierBasisPoints: Int = 15000,
    val effectiveFrom: String
)

@Serializable
data class HolidayDto(
    val id: String,
    val organizationId: String,
    val name: String,
    val date: String,
    val isPaid: Boolean
)

@Serializable
data class CreateHolidayRequest(
    val name: String,
    val date: String,
    val isPaid: Boolean = true
)

@Serializable
data class DeletedResponseDto(
    val deleted: Boolean = false
)

@Serializable
data class ScheduleEntryDto(
    val date: String,
    val dayOfWeek: Int,
    val type: String,
    val shiftTemplateId: String? = null,
    val shiftName: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val breakMinutes: Int? = null,
    val lateGraceMinutes: Int? = null,
    val halfDayThresholdMinutes: Int? = null,
    val scheduledMinutes: Int = 0,
    val overnight: Boolean = false,
    val startAt: String? = null,
    val endAt: String? = null,
    val holidayName: String? = null,
    val isPaidHoliday: Boolean? = null,
    val lateClockInAllowed: Boolean? = true
)
