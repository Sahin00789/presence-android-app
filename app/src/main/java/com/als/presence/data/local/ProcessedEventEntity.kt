package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "processed_events")
data class ProcessedEventEntity(
    @PrimaryKey val eventId: String,
    val eventType: String = "",
    val resourceName: String = "",
    val processedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProcessedEventDao {
    @Query("SELECT EXISTS(SELECT 1 FROM processed_events WHERE eventId = :eventId)")
    suspend fun isProcessed(eventId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markProcessed(event: ProcessedEventEntity)

    @Query("DELETE FROM processed_events WHERE processedAt < :threshold")
    suspend fun pruneOldEvents(threshold: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
}
