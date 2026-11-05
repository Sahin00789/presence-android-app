package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE isDeleted = 0 ORDER BY workDate DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND isDeleted = 0 ORDER BY workDate DESC")
    fun observeByEmployee(employeeId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE premisesId = :premisesId AND workDate = :workDate AND isDeleted = 0")
    fun observeByPremisesAndDate(premisesId: String, workDate: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND workDate = :workDate AND isDeleted = 0 LIMIT 1")
    suspend fun getTodayAttendanceForEmployee(employeeId: String, workDate: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AttendanceEntity>)

    @Query("UPDATE attendance SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM attendance")
    suspend fun clear()
}
