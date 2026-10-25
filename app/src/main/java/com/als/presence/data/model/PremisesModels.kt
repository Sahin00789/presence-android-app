package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PremisesDto(
    val id: String,
    val organizationId: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val radiusMeters: Int,
    val isActive: Boolean
)

@Serializable
data class CreatePremisesRequest(
    val name: String,
    val latitude: String,
    val longitude: String,
    val radiusMeters: Int = 50
)

@Serializable
data class UpdatePremisesRequest(
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radiusMeters: Int? = null,
    val isActive: Boolean? = null
)
