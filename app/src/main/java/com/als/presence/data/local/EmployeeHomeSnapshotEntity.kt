package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "employee_home_snapshots")
data class EmployeeHomeSnapshotEntity(
    @PrimaryKey val employeeId: String,
    val fullName: String = "",
    val staffCode: String = "",
    val premisesName: String = "",
    val shiftName: String = "",
    val shiftStartTime: String = "",
    val shiftEndTime: String = "",
    val attendanceStatus: String = "NOT_CLOCKED_IN",
    val clockInTime: String? = null,
    val clockOutTime: String? = null,
    val earnedBalancePaise: Long = 0L,
    val countsJson: String = "{}",
    val latestPingsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val earnedBalanceInr: Long get() = earnedBalancePaise
}

@Dao
interface EmployeeHomeSnapshotDao {
    @Query("SELECT * FROM employee_home_snapshots WHERE employeeId = :employeeId LIMIT 1")
    fun observeByEmployee(employeeId: String): Flow<EmployeeHomeSnapshotEntity?>

    @Query("SELECT * FROM employee_home_snapshots WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getByEmployee(employeeId: String): EmployeeHomeSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmployeeHomeSnapshotEntity)

    @Query("DELETE FROM employee_home_snapshots")
    suspend fun clear()
}
