package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PayrollSnapshotDao {

    @Query("SELECT * FROM payroll_snapshots WHERE month = :month AND year = :year")
    suspend fun getMonthlyPayrollSnapshots(month: Int, year: Int): List<PayrollSnapshotEntity>

    @Query("SELECT * FROM payroll_snapshots WHERE month = :month AND year = :year AND staffProfileId = :staffProfileId LIMIT 1")
    suspend fun getPayrollSnapshotForEmployee(month: Int, year: Int, staffProfileId: String): PayrollSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollSnapshots(entities: List<PayrollSnapshotEntity>)

    @Query("DELETE FROM payroll_snapshots WHERE month = :month AND year = :year")
    suspend fun clearPayrollSnapshotsForMonth(month: Int, year: Int)

    @Query("SELECT * FROM monthly_earning_snapshots WHERE month = :month AND year = :year AND staffProfileId = :staffProfileId LIMIT 1")
    suspend fun getMonthlyEarningSnapshot(month: Int, year: Int, staffProfileId: String): MonthlyEarningSnapshotEntity?

    @Query("SELECT * FROM monthly_earning_snapshots WHERE month = :month AND year = :year")
    suspend fun getMonthlyEarningSnapshotsForMonth(month: Int, year: Int): List<MonthlyEarningSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyEarningSnapshot(entity: MonthlyEarningSnapshotEntity)
}
