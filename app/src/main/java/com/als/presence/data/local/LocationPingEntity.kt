package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.GpsPingSummaryDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "location_pings")
data class LocationPingEntity(
    @PrimaryKey val id: String,
    val employeeId: String = "",
    val attendanceEventId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double? = null,
    val accuracyMeters: Double? = null,
    val isOutside: Boolean = false,
    val pingTime: String = "",
    val status: String = if (isOutside) "Outside" else "Inside",
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): GpsPingSummaryDto {
        return GpsPingSummaryDto(
            id = id,
            employeeId = employeeId,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            accuracyMeters = accuracyMeters,
            isOutside = isOutside,
            pingTime = pingTime,
            checkTime = pingTime,
            status = status
        )
    }

    companion object {
        fun fromDto(dto: GpsPingSummaryDto, attendanceEventId: String = ""): LocationPingEntity {
            return LocationPingEntity(
                id = dto.id.ifBlank { java.util.UUID.randomUUID().toString() },
                employeeId = dto.employeeId,
                attendanceEventId = attendanceEventId,
                latitude = dto.latitude,
                longitude = dto.longitude,
                distanceMeters = dto.distanceMeters,
                accuracyMeters = dto.accuracyMeters,
                isOutside = dto.isOutside,
                pingTime = dto.pingTime,
                status = dto.status,
                createdAt = System.currentTimeMillis(),
                version = 1L,
                isDeleted = false
            )
        }
    }
}

@Dao
interface LocationPingDao {
    @Query("SELECT * FROM location_pings WHERE employeeId = :employeeId AND isDeleted = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun observeLatestPingsForEmployee(employeeId: String, limit: Int = 3): Flow<List<LocationPingEntity>>

    @Query("SELECT * FROM location_pings WHERE attendanceEventId = :eventId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getPingsForEvent(eventId: String): List<LocationPingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocationPingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LocationPingEntity>)

    @Query("DELETE FROM location_pings WHERE employeeId = :employeeId")
    suspend fun clearForEmployee(employeeId: String)
}
