package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationDto(
    val id: String,
    val ownerUserId: String,
    val organizationName: String,
    val timezone: String = "UTC",
    val currency: String = "INR",
    val createdAt: String? = null
)

@Serializable
data class OrganizationMemberDto(
    val id: String,
    val organizationId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val earnedBalanceCents: Long = 0L,
    val approvedUnpaidBalanceCents: Long = 0L,
    val paidBalanceCents: Long = 0L,
    val isActive: Boolean = true
) {
    val name: String get() = fullName
    val staffCode: String get() = ""
    val employeeId: String get() = id
    val staffEmail: String get() = email
    val joinedAt: String? get() = null
}
