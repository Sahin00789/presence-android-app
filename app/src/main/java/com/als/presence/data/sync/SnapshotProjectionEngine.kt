package com.als.presence.data.sync

import com.als.presence.data.local.EmployeeAttendanceSnapshotEntity
import com.als.presence.data.local.EmployeeHomeSnapshotEntity
import com.als.presence.data.local.EmployeeSalarySnapshotEntity
import com.als.presence.data.local.OwnerEmployeesSnapshotEntity
import com.als.presence.data.local.OwnerPingsSnapshotEntity
import com.als.presence.data.local.OwnerReportsSnapshotEntity
import com.als.presence.data.local.PresenceDatabase
import com.als.presence.data.model.GpsCountsDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotProjectionEngine @Inject constructor(
    private val db: PresenceDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun rebuildEmployeeHomeSnapshot(employeeId: String) {
        val employee = db.employeeDao().getById(employeeId) ?: return
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val todayAttendance = db.attendanceDao().getTodayAttendanceForEmployee(employeeId, todayStr)
        val latestPings = db.locationPingDao().getPingsForEvent(todayAttendance?.id ?: "").map { it.toDto() }

        val pingsSummary = db.pingSummaryDao().getSummary(employeeId, todayStr)
        val gpsCounts = GpsCountsDto(
            inside = pingsSummary?.insideCount ?: 0,
            outside = pingsSummary?.outsideCount ?: 0,
            missed = pingsSummary?.missedCount ?: 0
        )

        val snapshot = EmployeeHomeSnapshotEntity(
            employeeId = employeeId,
            fullName = employee.fullName,
            staffCode = employee.staffCode,
            premisesName = employee.premisesName ?: "",
            shiftName = todayAttendance?.shiftName ?: "",
            shiftStartTime = todayAttendance?.assignedStartTime ?: "",
            shiftEndTime = todayAttendance?.assignedEndTime ?: "",
            attendanceStatus = todayAttendance?.status ?: "NOT_CLOCKED_IN",
            clockInTime = todayAttendance?.clockIn,
            clockOutTime = todayAttendance?.clockOut,
            earnedBalancePaise = employee.earnedBalancePaise,
            countsJson = json.encodeToString(gpsCounts),
            latestPingsJson = json.encodeToString(latestPings),
            updatedAt = System.currentTimeMillis()
        )

        db.employeeHomeSnapshotDao().upsert(snapshot)
    }

    suspend fun rebuildEmployeeAttendanceSnapshot(employeeId: String, month: Int, year: Int) {
        val key = "$employeeId-$month-$year"
        val records = db.attendanceDao().getAll().filter { it.employeeId == employeeId }.map { it.toDto() }

        var present = 0
        var absent = 0
        var halfDays = 0
        var leaveDays = 0
        var holidayDays = 0
        var weeklyOffDays = 0
        var totalMinutes = 0L

        records.forEach { r ->
            when (r.status.uppercase()) {
                "PRESENT" -> present++
                "ABSENT" -> absent++
                "HALF_DAY" -> halfDays++
                "LEAVE" -> leaveDays++
                "HOLIDAY" -> holidayDays++
                "WEEKLY_OFF" -> weeklyOffDays++
            }
            totalMinutes += r.workedMinutes
        }

        val snapshot = EmployeeAttendanceSnapshotEntity(
            id = key,
            employeeId = employeeId,
            month = month,
            year = year,
            presentDays = present,
            absentDays = absent,
            halfDays = halfDays,
            leaveDays = leaveDays,
            holidayDays = holidayDays,
            weeklyOffDays = weeklyOffDays,
            totalWorkedMinutes = totalMinutes,
            recordsJson = json.encodeToString(records),
            updatedAt = System.currentTimeMillis()
        )

        db.employeeAttendanceSnapshotDao().upsert(snapshot)
    }

    suspend fun rebuildEmployeeSalarySnapshot(employeeId: String, month: Int, year: Int) {
        val key = "$employeeId-$month-$year"
        val transactions = db.salaryTransactionDao().getByStaff(employeeId)
        val transactionDtos = transactions.map { it.toDto() }

        val snapshot = EmployeeSalarySnapshotEntity(
            id = key,
            employeeId = employeeId,
            month = month,
            year = year,
            earnedAmountPaise = 0L,
            deductionAmountPaise = 0L,
            paidAmountPaise = transactionDtos.sumOf { it.amountPaise },
            dueAmountPaise = 0L,
            status = "OPEN",
            breakdownJson = "{}",
            dailyTotalsJson = "[]",
            entriesJson = "[]",
            paymentsJson = json.encodeToString(transactionDtos),
            updatedAt = System.currentTimeMillis()
        )

        db.employeeSalarySnapshotDao().upsert(snapshot)
    }

    suspend fun rebuildOwnerEmployeesSnapshot(premisesId: String = "ALL") {
        val allEmployees = db.employeeDao().getAll().filter { it.id.isNotBlank() }.distinctBy { it.id }
        val employees = if (premisesId == "ALL" || premisesId.isBlank()) {
            allEmployees
        } else {
            allEmployees.filter { it.premisesId == premisesId }
        }

        val dtoList = employees.map { it.toDto() }
        val snapshot = OwnerEmployeesSnapshotEntity(
            premisesId = premisesId,
            totalEmployees = dtoList.size,
            activeEmployees = dtoList.count { it.isActive },
            employeesJson = json.encodeToString(dtoList),
            updatedAt = System.currentTimeMillis()
        )

        db.ownerEmployeesSnapshotDao().upsert(snapshot)
    }

    suspend fun rebuildOwnerPingsSnapshot(premisesId: String = "ALL", date: String = "") {
        val todayStr = if (date.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time) else date
        val snapshot = OwnerPingsSnapshotEntity(
            premisesId = premisesId,
            date = todayStr,
            totalActiveStaff = 0,
            totalClockedIn = 0,
            outsideGeofenceCount = 0,
            staffCardsJson = "[]",
            updatedAt = System.currentTimeMillis()
        )

        db.ownerPingsSnapshotDao().upsert(snapshot)
    }

    suspend fun rebuildOwnerReportsSnapshot(month: Int, year: Int, premisesId: String? = null) {
        val key = "$month-$year-${premisesId ?: "ALL"}"
        val snapshot = OwnerReportsSnapshotEntity(
            id = key,
            month = month,
            year = year,
            premisesId = premisesId,
            grossSalaryPaise = 0L,
            netSalaryPaise = 0L,
            payrollItemsJson = "[]",
            updatedAt = System.currentTimeMillis()
        )

        db.ownerReportsSnapshotDao().upsert(snapshot)
    }
}
