package com.als.presence.data.local.premises

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PremisesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<PremisesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PremisesEntity)

    @Query("DELETE FROM premises WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM premises WHERE isActive = 1 ORDER BY name ASC")
    fun observeAll(): Flow<List<PremisesEntity>>

    @Query("SELECT * FROM premises WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAll(): List<PremisesEntity>

    @Query("DELETE FROM premises")
    suspend fun clear()
}
