package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ping_summaries")
data class PingSummaryEntity(
    @PrimaryKey val id: String, // format: "$employeeId-$workDate"
    val employeeId: String,
    val premisesId: String? = null,
    val workDate: String,
    val insideCount: Int = 0,
    val outsideCount: Int = 0,
    val missedCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val locationStatus: String = "Inside",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface PingSummaryDao {
    @Query("SELECT * FROM ping_summaries WHERE employeeId = :employeeId AND workDate = :workDate LIMIT 1")
    suspend fun getSummary(employeeId: String, workDate: String): PingSummaryEntity?

    @Query("SELECT * FROM ping_summaries WHERE employeeId = :employeeId AND workDate = :workDate LIMIT 1")
    fun observeSummary(employeeId: String, workDate: String): Flow<PingSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PingSummaryEntity)

    @Query("DELETE FROM ping_summaries")
    suspend fun clear()
}
