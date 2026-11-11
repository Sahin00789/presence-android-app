package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.AttendanceRecordDto

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesId: String = "",
    val employeeId: String = "",
    val staffName: String? = null,
    val staffCode: String? = null,
    val workDate: String = "",
    val status: String = "PRESENT",
    val clockIn: String? = null,
    val clockOut: String? = null,
    val overtimeId: String? = null,
    val attendanceType: String? = "SHIFT",
    val shiftName: String? = null,
    val assignedStartTime: String? = null,
    val assignedEndTime: String? = null,
    val outsidePingCount: Int = 0,
    val manualOverride: Boolean = false,
    val workedMinutes: Int = 0,
    val scheduledMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): AttendanceRecordDto {
        return AttendanceRecordDto(
            id = id,
            organizationId = organizationId,
            premisesId = premisesId,
            employeeId = employeeId,
            staffName = staffName,
            staffCode = staffCode,
            workDate = workDate,
            status = status,
            clockIn = clockIn,
            clockOut = clockOut,
            overtimeId = overtimeId,
            attendanceType = attendanceType,
            shiftName = shiftName,
            assignedStartTime = assignedStartTime,
            assignedEndTime = assignedEndTime,
            outsidePingCount = outsidePingCount,
            manualOverride = manualOverride,
            workedMinutes = workedMinutes,
            scheduledMinutes = scheduledMinutes,
            lateMinutes = lateMinutes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: AttendanceRecordDto, version: Long = 1L): AttendanceEntity {
            return AttendanceEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                premisesId = dto.premisesId,
                employeeId = dto.employeeId.ifBlank { dto.staffProfileId.orEmpty() },
                staffName = dto.staffName,
                staffCode = dto.staffCode,
                workDate = dto.workDate,
                status = dto.status,
                clockIn = dto.clockIn,
                clockOut = dto.clockOut,
                overtimeId = dto.overtimeId,
                attendanceType = dto.attendanceType,
                shiftName = dto.shiftName,
                assignedStartTime = dto.assignedStartTime,
                assignedEndTime = dto.assignedEndTime,
                outsidePingCount = dto.outsidePingCount,
                manualOverride = dto.manualOverride,
                workedMinutes = dto.workedMinutes,
                scheduledMinutes = dto.scheduledMinutes,
                lateMinutes = dto.lateMinutes,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}
