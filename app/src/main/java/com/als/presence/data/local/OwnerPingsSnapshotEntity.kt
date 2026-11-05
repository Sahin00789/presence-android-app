package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owner_pings_snapshots")
data class OwnerPingsSnapshotEntity(
    @PrimaryKey val premisesId: String, // premisesId or "ALL"
    val date: String = "",
    val totalActiveStaff: Int = 0,
    val totalClockedIn: Int = 0,
    val outsideGeofenceCount: Int = 0,
    val staffCardsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface OwnerPingsSnapshotDao {
    @Query("SELECT * FROM owner_pings_snapshots WHERE premisesId = :premisesId LIMIT 1")
    fun observeByPremises(premisesId: String): Flow<OwnerPingsSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OwnerPingsSnapshotEntity)

    @Query("DELETE FROM owner_pings_snapshots")
    suspend fun clear()
}
