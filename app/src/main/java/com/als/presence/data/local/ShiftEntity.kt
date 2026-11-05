package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.ShiftDto

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesId: String = "",
    val shiftName: String = "",
    val dayName: String = "ALL",
    val startTime: String = "",
    val endTime: String = "",
    val halfDayThreshold: String = "11:00:00",
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): ShiftDto {
        return ShiftDto(
            id = id,
            organizationId = organizationId,
            premisesId = premisesId,
            shiftName = shiftName,
            dayName = dayName,
            startTime = startTime,
            endTime = endTime,
            halfDayThreshold = halfDayThreshold,
            isActive = isActive,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: ShiftDto, version: Long = 1L): ShiftEntity {
            return ShiftEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                premisesId = dto.premisesId,
                shiftName = dto.shiftName,
                dayName = dto.dayName,
                startTime = dto.startTime,
                endTime = dto.endTime,
                halfDayThreshold = dto.halfDayThreshold,
                isActive = dto.isActive,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}
