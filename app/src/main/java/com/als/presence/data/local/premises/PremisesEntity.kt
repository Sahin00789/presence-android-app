package com.als.presence.data.local.premises

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "premises")
data class PremisesEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val radiusMeters: Int,
    val isActive: Boolean
)
