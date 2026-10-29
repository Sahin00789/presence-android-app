package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "shifts")
data class ShiftEntity(@PrimaryKey val id: String)

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts")
    suspend fun getAll(): List<ShiftEntity>
}
