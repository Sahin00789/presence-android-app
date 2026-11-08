package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.ShiftAssignmentDto

@Entity(tableName = "shift_assignments")
data class ShiftAssignmentEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesId: String = "",
    val employeeId: String = "",
    val staffId: String = "",
    val shiftId: String = "",
    val absentPay: Double = 0.0,
    val presentPay: Double = 0.0,
    val leavePay: Double = 0.0,
    val halfDayPay: Double = 0.0,
    val isActive: Boolean = true,
    val staffName: String? = null,
    val shiftName: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDto(): ShiftAssignmentDto {
        return ShiftAssignmentDto(
            id = id,
            organizationId = organizationId,
            premisesId = premisesId,
            employeeId = employeeId,
            staffId = staffId.ifBlank { employeeId },
            shiftId = shiftId,
            absentPay = absentPay,
            presentPay = presentPay,
            leavePay = leavePay,
            halfDayPay = halfDayPay,
            isActive = isActive,
            staffName = staffName,
            shiftName = shiftName
        )
    }

    companion object {
        fun fromDto(dto: ShiftAssignmentDto): ShiftAssignmentEntity {
            return ShiftAssignmentEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                premisesId = dto.premisesId,
                employeeId = dto.employeeId,
                staffId = dto.staffId.ifBlank { dto.employeeId },
                shiftId = dto.shiftId,
                absentPay = dto.absentPay,
                presentPay = dto.presentPay,
                leavePay = dto.leavePay,
                halfDayPay = dto.halfDayPay,
                isActive = dto.isActive,
                staffName = dto.staffName,
                shiftName = dto.shiftName,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
