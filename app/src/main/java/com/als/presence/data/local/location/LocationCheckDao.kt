package com.als.presence.data.local.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationCheckDao {

    @Insert
    suspend fun enqueue(entry: LocationCheckEntity): Long

    @Query("SELECT * FROM location_check_outbox ORDER BY capturedAt ASC LIMIT :limit")
    suspend fun pending(limit: Int = 50): List<LocationCheckEntity>

    @Query("DELETE FROM location_check_outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE location_check_outbox SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, error: String?)

    @Query("SELECT COUNT(*) FROM location_check_outbox")
    suspend fun pendingCount(): Int

    @Query("DELETE FROM location_check_outbox")
    suspend fun clear()
}
