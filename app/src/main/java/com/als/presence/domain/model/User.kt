package com.als.presence.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: Role,
    val organizationId: String,
    val organizationName: String,
    val avatarUrl: String? = null,
    val organizationLogoUrl: String? = null
)
