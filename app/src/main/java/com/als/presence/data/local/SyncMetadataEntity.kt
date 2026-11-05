package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val resourceName: String,
    val lastSyncTimestamp: Long = 0L,
    val watermarkCursor: String? = null,
    val lastAppliedVersion: Long = 0L,
    val syncStatus: String = "IDLE", // IDLE, SYNCING, SUCCESS, ERROR
    val errorDetails: String? = null
)

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE resourceName = :resourceName LIMIT 1")
    suspend fun getMetadata(resourceName: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clear()
}
