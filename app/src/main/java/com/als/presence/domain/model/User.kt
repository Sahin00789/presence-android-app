package com.als.presence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: Role,
    val isActive: Boolean = true,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val organizationName: String? = null,
    val organizationLogoUrl: String? = null,
    val organizationId: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null
)
