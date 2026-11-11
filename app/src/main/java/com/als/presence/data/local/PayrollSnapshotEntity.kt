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
    val employeeId: String = "",
    val employeeName: String? = null,
    val employeeCode: String? = null,
    val staffEmail: String? = null,
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
    val earnedAmount: String = "0.00",
    val paidAmount: String = "0.00",
    val dueAmount: String = "0.00",
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
        val realPayrollId = monthlyPayrollId.ifBlank { id }
        val realStaffId = staffProfileId.ifBlank { employeeId }
        return PayrollItemDto(
            id = realPayrollId,
            monthlyPayrollId = realPayrollId,
            staffProfileId = realStaffId,
            employeeId = realStaffId,
            staffName = employeeName,
            employeeName = employeeName,
            staffCode = employeeCode,
            employeeCode = employeeCode,
            staffEmail = staffEmail,
            role = role,
            month = month,
            year = year,
            premisesId = premisesId,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            periodStatus = periodStatus,
            earnedAmount = earnedAmount,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
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
            val resolvedPayrollId = dto.monthlyPayrollId.ifBlank { dto.id }
            val resolvedStaffId = dto.staffProfileId.ifBlank { dto.employeeId.ifBlank { dto.resolvedEmployeeId } }
            val key = "$month-$year-$resolvedStaffId"
            val att = dto.attendanceSummary
            return PayrollSnapshotEntity(
                id = key,
                monthlyPayrollId = resolvedPayrollId,
                staffProfileId = resolvedStaffId,
                employeeId = resolvedStaffId,
                employeeName = dto.resolvedStaffName,
                employeeCode = dto.resolvedStaffCode,
                staffEmail = dto.staffEmail,
                role = dto.role,
                month = month,
                year = year,
                premisesId = dto.premisesId,
                periodStartDate = dto.periodStartDate,
                periodEndDate = dto.periodEndDate,
                periodStatus = dto.periodStatus,
                grossSalaryPaise = dto.resolvedGrossSalaryPaise,
                additionsPaise = dto.additionsPaise,
                deductionsPaise = dto.deductionsPaise,
                netSalaryPaise = dto.resolvedNetSalaryPaise,
                earnedAmount = dto.earnedAmount,
                paidAmount = dto.paidAmount,
                dueAmount = dto.dueAmount,
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
