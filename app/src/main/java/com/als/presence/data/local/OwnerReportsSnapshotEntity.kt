package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owner_reports_snapshots")
data class OwnerReportsSnapshotEntity(
    @PrimaryKey val id: String, // "$month-$year-$premisesId"
    val month: Int,
    val year: Int,
    val premisesId: String? = null,
    val grossSalaryPaise: Long = 0L,
    val netSalaryPaise: Long = 0L,
    val payrollItemsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val grossSalaryInr: Long get() = grossSalaryPaise
    val netSalaryInr: Long get() = netSalaryPaise
}

@Dao
interface OwnerReportsSnapshotDao {
    @Query("SELECT * FROM owner_reports_snapshots WHERE month = :month AND year = :year LIMIT 1")
    fun observeByMonth(month: Int, year: Int): Flow<OwnerReportsSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OwnerReportsSnapshotEntity)

    @Query("DELETE FROM owner_reports_snapshots")
    suspend fun clear()
}
