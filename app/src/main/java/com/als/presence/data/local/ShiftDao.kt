package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE isDeleted = 0 ORDER BY shiftName ASC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE isDeleted = 0 ORDER BY shiftName ASC")
    suspend fun getAll(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE premisesId = :premisesId AND isDeleted = 0 ORDER BY shiftName ASC")
    fun observeByPremises(premisesId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ShiftEntity>)

    @Query("UPDATE shifts SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shifts")
    suspend fun clear()
}
