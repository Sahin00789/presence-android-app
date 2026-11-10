package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE isDeleted = 0 ORDER BY fullName ASC")
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE isDeleted = 0 ORDER BY fullName ASC")
    suspend fun getAll(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE organizationId = :orgId AND isDeleted = 0 ORDER BY fullName ASC")
    fun observeByOrganization(orgId: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE premisesId = :premisesId AND isDeleted = 0 ORDER BY fullName ASC")
    fun observeByPremises(premisesId: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    suspend fun getByUserId(userId: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<EmployeeEntity>)

    @Query("UPDATE employees SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM employees WHERE id = '' OR id IS NULL")
    suspend fun deleteInvalidEntries()

    @Query("DELETE FROM employees")
    suspend fun clear()
}
