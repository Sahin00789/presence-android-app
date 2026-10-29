package com.als.presence.data.model

import kotlinx.serialization.Serializable

typealias ShiftTemplateDto = ShiftDto
typealias CreateShiftTemplateRequest = CreateShiftRequest
typealias UpdateShiftTemplateRequest = UpdateShiftRequest

@Serializable
data class ShiftDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String = "",
    val shiftName: String = "",
    val dayName: String = "ALL",
    val startTime: String = "",
    val endTime: String = "",
    val halfDayThreshold: String = "11:00:00",
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val name: String get() = shiftName
    val shiftId: String get() = id
    val shiftTemplateId: String get() = id
    val halfDayThresholdMinutes: Int get() = 240
    val daysOfWeek: List<Int>?
        get() {
            val dn = dayName.trim()
            if (dn.isEmpty() || dn.equals("ALL", ignoreCase = true) || dn.equals("Everyday", ignoreCase = true) || dn.equals("Every Day", ignoreCase = true)) {
                return null
            }
            val list = mutableListOf<Int>()
            if (dn.contains("Monday", ignoreCase = true) || dn.contains("Mon", ignoreCase = true)) list.add(1)
            if (dn.contains("Tuesday", ignoreCase = true) || dn.contains("Tue", ignoreCase = true)) list.add(2)
            if (dn.contains("Wednesday", ignoreCase = true) || dn.contains("Wed", ignoreCase = true)) list.add(3)
            if (dn.contains("Thursday", ignoreCase = true) || dn.contains("Thu", ignoreCase = true)) list.add(4)
            if (dn.contains("Friday", ignoreCase = true) || dn.contains("Fri", ignoreCase = true)) list.add(5)
            if (dn.contains("Saturday", ignoreCase = true) || dn.contains("Sat", ignoreCase = true)) list.add(6)
            if (dn.contains("Sunday", ignoreCase = true) || dn.contains("Sun", ignoreCase = true)) list.add(0)
            return list.ifEmpty { null }
        }
}

@Serializable
data class CreateShiftRequest(
    val premisesId: String = "",
    val shiftName: String = "",
    val dayName: String = "ALL",
    val startTime: String = "",
    val endTime: String = "",
    val halfDayThreshold: String = "11:00:00"
)

@Serializable
data class UpdateShiftRequest(
    val shiftName: String? = null,
    val dayName: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val halfDayThreshold: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class ShiftAssignmentDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String = "",
    val employeeId: String = "",
    val staffId: String = employeeId,
    val shiftId: String = "",
    val absentPay: Double = 0.0,
    val presentPay: Double = 0.0,
    val leavePay: Double = 0.0,
    val halfDayPay: Double = 0.0,
    val isActive: Boolean = true,
    val staffName: String? = null,
    val shiftName: String? = null
) {
    val shiftTemplateId: String get() = shiftId
    val daysOfWeek: List<Int>? get() = null
    val paymentCents: Long get() = (presentPay * 100).toLong()
}

@Serializable
data class CreateShiftAssignmentRequest(
    val premisesId: String = "",
    val employeeId: String = "",
    val staffId: String = employeeId,
    val shiftId: String = "",
    val absentPay: Double = 0.0,
    val presentPay: Double = 0.0,
    val leavePay: Double = 0.0,
    val halfDayPay: Double = 0.0,
    val isActive: Boolean = true
) {
    val shiftTemplateId: String get() = shiftId
    val daysOfWeek: List<Int> get() = emptyList()
}
