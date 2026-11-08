package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftAssignmentDao {
    @Query("SELECT * FROM shift_assignments WHERE isActive = 1 ORDER BY id ASC")
    fun observeAll(): Flow<List<ShiftAssignmentEntity>>

    @Query("SELECT * FROM shift_assignments WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getAll(): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE premisesId = :premisesId AND isActive = 1")
    fun observeByPremises(premisesId: String): Flow<List<ShiftAssignmentEntity>>

    @Query("SELECT * FROM shift_assignments WHERE premisesId = :premisesId AND isActive = 1")
    suspend fun getByPremises(premisesId: String): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE (employeeId = :employeeId OR staffId = :employeeId) AND isActive = 1")
    suspend fun getByEmployee(employeeId: String): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShiftAssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShiftAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ShiftAssignmentEntity>)

    @Query("DELETE FROM shift_assignments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shift_assignments")
    suspend fun clear()
}
