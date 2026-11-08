package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.OvertimeDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "overtime")
data class OvertimeEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val staffProfileId: String = "",
    val staffName: String = "",
    val staffEmail: String = "",
    val staffCode: String = "",
    val requestedBy: String = "",
    val approvedBy: String? = null,
    val workDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val plannedMinutes: Int = 0,
    val workedMinutes: Int = 0,
    val paymentInr: Long = 0L,
    val status: String = "REQUESTED",
    val reason: String? = null,
    val attendanceEventId: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): OvertimeDto {
        return OvertimeDto(
            id = id,
            organizationId = organizationId,
            staffProfileId = staffProfileId,
            staffName = staffName,
            staffEmail = staffEmail,
            staffCode = staffCode,
            requestedBy = requestedBy,
            approvedBy = approvedBy,
            workDate = workDate,
            startTime = startTime,
            endTime = endTime,
            plannedMinutes = plannedMinutes,
            workedMinutes = workedMinutes,
            paymentInr = paymentInr,
            status = status,
            reason = reason,
            attendanceEventId = attendanceEventId,
            resolvedAt = resolvedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: OvertimeDto, version: Long = 1L): OvertimeEntity {
            return OvertimeEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                staffProfileId = dto.staffProfileId,
                staffName = dto.staffName,
                staffEmail = dto.staffEmail,
                staffCode = dto.staffCode,
                requestedBy = dto.requestedBy,
                approvedBy = dto.approvedBy,
                workDate = dto.workDate,
                startTime = dto.startTime,
                endTime = dto.endTime,
                plannedMinutes = dto.plannedMinutes,
                workedMinutes = dto.workedMinutes,
                paymentInr = dto.paymentInr,
                status = dto.status,
                reason = dto.reason,
                attendanceEventId = dto.attendanceEventId,
                resolvedAt = dto.resolvedAt,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}

@Dao
interface OvertimeDao {
    @Query("SELECT * FROM overtime WHERE isDeleted = 0 ORDER BY workDate DESC, startTime DESC")
    fun observeAll(): Flow<List<OvertimeEntity>>

    @Query("SELECT * FROM overtime WHERE isDeleted = 0 ORDER BY workDate DESC, startTime DESC")
    suspend fun getAll(): List<OvertimeEntity>

    @Query("SELECT * FROM overtime WHERE staffProfileId = :staffProfileId AND isDeleted = 0 ORDER BY workDate DESC")
    fun observeByStaff(staffProfileId: String): Flow<List<OvertimeEntity>>

    @Query("SELECT * FROM overtime WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): OvertimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OvertimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OvertimeEntity>)

    @Query("UPDATE overtime SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM overtime")
    suspend fun clear()
}
