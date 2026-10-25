package com.als.presence.data.local.shift

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(list: List<ShiftTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ShiftTemplateEntity)

    @Query("DELETE FROM shift_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("SELECT * FROM shift_templates WHERE isActive = 1 ORDER BY startTime ASC")
    fun observeTemplates(): Flow<List<ShiftTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(list: List<ShiftAssignmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: ShiftAssignmentEntity)

    @Query("SELECT * FROM shift_assignments ORDER BY startDate DESC")
    fun observeAssignments(): Flow<List<ShiftAssignmentEntity>>

    @Query("DELETE FROM shift_templates")
    suspend fun clearTemplates()

    @Query("DELETE FROM shift_assignments")
    suspend fun clearAssignments()
}
