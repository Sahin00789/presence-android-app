package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "employees")
data class EmployeeEntity(@PrimaryKey val id: String)

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees")
    suspend fun getAll(): List<EmployeeEntity>
}
