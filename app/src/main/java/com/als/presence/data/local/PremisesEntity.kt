package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.PremisesDto

@Entity(tableName = "premises")
data class PremisesEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesName: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geofenceRadiusMeters: Int = 50,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): PremisesDto {
        return PremisesDto(
            id = id,
            organizationId = organizationId,
            premisesName = premisesName,
            address = address,
            latitude = latitude,
            longitude = longitude,
            geofenceRadiusMeters = geofenceRadiusMeters,
            isActive = isActive,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: PremisesDto, version: Long = 1L): PremisesEntity {
            return PremisesEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                premisesName = dto.premisesName,
                address = dto.address,
                latitude = dto.latitude,
                longitude = dto.longitude,
                geofenceRadiusMeters = dto.geofenceRadiusMeters,
                isActive = dto.isActive,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}
