package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "attendance")
data class AttendanceEntity(@PrimaryKey val id: String)

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance")
    suspend fun getAll(): List<AttendanceEntity>
}
