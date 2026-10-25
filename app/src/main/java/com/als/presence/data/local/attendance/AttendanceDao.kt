package com.als.presence.data.local.attendance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceEntity)

    @Query("SELECT * FROM attendance_records ORDER BY workDate DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE staffProfileId = :staffProfileId ORDER BY workDate DESC")
    fun observeForStaff(staffProfileId: String): Flow<List<AttendanceEntity>>

    @Query("DELETE FROM attendance_records")
    suspend fun clear()
}
