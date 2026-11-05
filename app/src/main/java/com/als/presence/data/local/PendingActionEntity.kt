package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String, // e.g. "CREATE_SHIFT", "UPDATE_EMPLOYEE", "RESOLVE_EXCEPTION"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    val error: String? = null
)

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions ORDER BY createdAt DESC")
    fun observeAllPending(): Flow<List<PendingActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(action: PendingActionEntity)

    @Query("UPDATE pending_actions SET status = :status, retryCount = retryCount + 1, error = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_actions WHERE status = 'SYNCED'")
    suspend fun clearSynced()
}
