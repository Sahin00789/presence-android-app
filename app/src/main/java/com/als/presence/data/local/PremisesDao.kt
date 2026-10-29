package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "premises")
data class PremisesEntity(@PrimaryKey val id: String)

@Dao
interface PremisesDao {
    @Query("SELECT * FROM premises")
    suspend fun getAll(): List<PremisesEntity>
}
