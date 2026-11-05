package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "employee_attendance_snapshots")
data class EmployeeAttendanceSnapshotEntity(
    @PrimaryKey val id: String, // "$employeeId-$month-$year"
    val employeeId: String,
    val month: Int,
    val year: Int,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val holidayDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val totalWorkedMinutes: Long = 0L,
    val recordsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface EmployeeAttendanceSnapshotDao {
    @Query("SELECT * FROM employee_attendance_snapshots WHERE employeeId = :employeeId AND month = :month AND year = :year LIMIT 1")
    fun observeByMonth(employeeId: String, month: Int, year: Int): Flow<EmployeeAttendanceSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmployeeAttendanceSnapshotEntity)

    @Query("DELETE FROM employee_attendance_snapshots")
    suspend fun clear()
}
