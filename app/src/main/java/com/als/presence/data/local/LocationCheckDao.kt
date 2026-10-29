package com.als.presence.data.local

import androidx.room.*

@Entity(tableName = "location_checks")
data class LocationCheckEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val attendanceEventId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val isMock: Boolean = false,
    val capturedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val error: String? = null
)

@Dao
interface LocationCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LocationCheckEntity): Long

    suspend fun enqueue(entry: LocationCheckEntity): String {
        insert(entry)
        return entry.id
    }

    @Query("SELECT * FROM location_checks WHERE status = 'PENDING'")
    suspend fun pending(): List<LocationCheckEntity>

    @Query("DELETE FROM location_checks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE location_checks SET status = 'FAILED', error = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String?)
}
