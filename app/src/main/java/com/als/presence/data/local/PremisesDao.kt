package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PremisesDao {
    @Query("SELECT * FROM premises WHERE isDeleted = 0 ORDER BY premisesName ASC")
    fun observeAll(): Flow<List<PremisesEntity>>

    @Query("SELECT * FROM premises WHERE isDeleted = 0 ORDER BY premisesName ASC")
    suspend fun getAll(): List<PremisesEntity>

    @Query("SELECT * FROM premises WHERE organizationId = :orgId AND isDeleted = 0 ORDER BY premisesName ASC")
    fun observeByOrganization(orgId: String): Flow<List<PremisesEntity>>

    @Query("SELECT * FROM premises WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): PremisesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PremisesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PremisesEntity>)

    @Query("UPDATE premises SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM premises")
    suspend fun clear()
}
