package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.LeaveDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val staffProfileId: String = "",
    val staffName: String? = null,
    val staffCode: String? = null,
    val shiftName: String? = null,
    val type: String = "PAID",
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val reviewNote: String? = null,
    val approvedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): LeaveDto {
        return LeaveDto(
            id = id,
            organizationId = organizationId,
            staffProfileId = staffProfileId,
            staffName = staffName,
            staffCode = staffCode,
            shiftName = shiftName,
            type = type,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            status = status,
            reviewNote = reviewNote,
            approvedBy = approvedBy,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: LeaveDto, version: Long = 1L): LeaveRequestEntity {
            return LeaveRequestEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                staffProfileId = dto.staffProfileId,
                staffName = dto.staffName,
                staffCode = dto.staffCode,
                shiftName = dto.shiftName,
                type = dto.type,
                startDate = dto.startDate,
                endDate = dto.endDate,
                reason = dto.reason,
                status = dto.status,
                reviewNote = dto.reviewNote,
                approvedBy = dto.approvedBy,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}

@Dao
interface LeaveRequestDao {
    @Query("SELECT * FROM leave_requests WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE staffProfileId = :staffProfileId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByStaff(staffProfileId: String): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): LeaveRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LeaveRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LeaveRequestEntity>)

    @Query("UPDATE leave_requests SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM leave_requests")
    suspend fun clear()
}
