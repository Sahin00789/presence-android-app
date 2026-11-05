package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "employee_salary_snapshots")
data class EmployeeSalarySnapshotEntity(
    @PrimaryKey val id: String, // "$employeeId-$month-$year"
    val employeeId: String,
    val month: Int,
    val year: Int,
    val earnedAmountPaise: Long = 0L,
    val deductionAmountPaise: Long = 0L,
    val paidAmountPaise: Long = 0L,
    val dueAmountPaise: Long = 0L,
    val status: String = "OPEN",
    val breakdownJson: String = "{}",
    val dailyTotalsJson: String = "[]",
    val entriesJson: String = "[]",
    val paymentsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val earnedAmountInr: Long get() = earnedAmountPaise
    val deductionAmountInr: Long get() = deductionAmountPaise
    val paidAmountInr: Long get() = paidAmountPaise
    val dueAmountInr: Long get() = dueAmountPaise
}

@Dao
interface EmployeeSalarySnapshotDao {
    @Query("SELECT * FROM employee_salary_snapshots WHERE employeeId = :employeeId AND month = :month AND year = :year LIMIT 1")
    fun observeByMonth(employeeId: String, month: Int, year: Int): Flow<EmployeeSalarySnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmployeeSalarySnapshotEntity)

    @Query("DELETE FROM employee_salary_snapshots")
    suspend fun clear()
}
