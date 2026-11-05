package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.PayrollItemDto

@Entity(tableName = "payroll_snapshots")
data class PayrollSnapshotEntity(
    @PrimaryKey val id: String,
    val monthlyPayrollId: String = "",
    val staffProfileId: String = "",
    val employeeName: String? = null,
    val employeeCode: String? = null,
    val role: String? = null,
    val month: Int = 1,
    val year: Int = 2025,
    val premisesId: String? = null,
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val periodStatus: String = "OPEN",
    val grossSalaryPaise: Long = 0L,
    val additionsPaise: Long = 0L,
    val deductionsPaise: Long = 0L,
    val netSalaryPaise: Long = 0L,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val holidayDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val totalWorkedMinutes: Long = 0L,
    val totalOvertimeMinutes: Long = 0L,
    val presentShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0,
    val overtimeCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val grossSalaryInr: Long get() = grossSalaryPaise
    val additionsInr: Long get() = additionsPaise
    val deductionsInr: Long get() = deductionsPaise
    val netSalaryInr: Long get() = netSalaryPaise

    fun toDto(): PayrollItemDto {
        return PayrollItemDto(
            id = id,
            monthlyPayrollId = monthlyPayrollId,
            staffProfileId = staffProfileId,
            employeeName = employeeName,
            employeeCode = employeeCode,
            role = role,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            periodStatus = periodStatus,
            grossSalaryPaise = grossSalaryPaise,
            additionsPaise = additionsPaise,
            deductionsPaise = deductionsPaise,
            netSalaryPaise = netSalaryPaise,
            attendanceSummary = AttendanceSummaryDto(
                totalDays = presentDays + absentDays + halfDays + leaveDays + holidayDays + weeklyOffDays,
                presentDays = presentDays,
                absentDays = absentDays,
                halfDays = halfDays,
                leaveDays = leaveDays,
                holidayDays = holidayDays,
                weeklyOffDays = weeklyOffDays,
                totalWorkedMinutes = totalWorkedMinutes,
                totalOvertimeMinutes = totalOvertimeMinutes,
                presentShiftsCount = presentShiftsCount,
                absentShiftsCount = absentShiftsCount,
                halfDayShiftsCount = halfDayShiftsCount,
                leaveShiftsCount = leaveShiftsCount,
                holidayCount = holidayCount,
                offDayCount = offDayCount,
                overtimeCount = overtimeCount
            )
        )
    }

    companion object {
        fun fromDto(month: Int, year: Int, dto: PayrollItemDto): PayrollSnapshotEntity {
            val key = "$month-$year-${dto.staffProfileId}"
            val att = dto.attendanceSummary
            return PayrollSnapshotEntity(
                id = key,
                monthlyPayrollId = dto.monthlyPayrollId,
                staffProfileId = dto.staffProfileId,
                employeeName = dto.employeeName,
                employeeCode = dto.employeeCode,
                role = dto.role,
                month = month,
                year = year,
                periodStartDate = dto.periodStartDate,
                periodEndDate = dto.periodEndDate,
                periodStatus = dto.periodStatus,
                grossSalaryPaise = dto.grossSalaryPaise,
                additionsPaise = dto.additionsPaise,
                deductionsPaise = dto.deductionsPaise,
                netSalaryPaise = dto.netSalaryPaise,
                presentDays = att.presentDays,
                absentDays = att.absentDays,
                halfDays = att.halfDays,
                leaveDays = att.leaveDays,
                holidayDays = att.holidayDays,
                weeklyOffDays = att.weeklyOffDays,
                totalWorkedMinutes = att.totalWorkedMinutes,
                totalOvertimeMinutes = att.totalOvertimeMinutes,
                presentShiftsCount = att.presentShiftsCount,
                absentShiftsCount = att.absentShiftsCount,
                halfDayShiftsCount = att.halfDayShiftsCount,
                leaveShiftsCount = att.leaveShiftsCount,
                holidayCount = att.holidayCount,
                offDayCount = att.offDayCount,
                overtimeCount = att.overtimeCount
            )
        }
    }
}
