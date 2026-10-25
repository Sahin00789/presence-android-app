package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val id: String,
    val organizationId: String,
    val email: String,
    val role: String,
    val token: String,
    val invitedBy: String,
    val expiresAt: String,
    val acceptedAt: String? = null,
    val createdAt: String
)

@Serializable
data class CreateInvitationRequest(
    val email: String,
    val role: String,
    val fullName: String? = null,
    val staffCode: String? = null,
    val department: String? = null,
    val designation: String? = null,
    val premisesId: String? = null
)

@Serializable
data class OrganizationMemberDto(
    val userId: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val role: String,
    val isActive: Boolean,
    val joinedAt: String,
    val staffProfileId: String? = null,
    val staffCode: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null
)
