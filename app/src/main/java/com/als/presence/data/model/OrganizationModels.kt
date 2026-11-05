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
    val id: String = "",
    val organizationId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val premisesId: String? = null,
    val premisesName: String? = null,
    val staffProfileId: String? = null,
    val staffCode: String? = null,
    val earnedBalancePaise: Long = 0L,
    val approvedUnpaidBalancePaise: Long = 0L,
    val paidBalancePaise: Long = 0L,
    val isActive: Boolean = true
) {
    val name: String get() = fullName
    val employeeId: String get() = staffProfileId ?: id.ifBlank { userId }
    val staffEmail: String get() = email
    val joinedAt: String? get() = null
    val earnedBalanceInr: Long get() = earnedBalancePaise
    val approvedUnpaidBalanceInr: Long get() = approvedUnpaidBalancePaise
    val paidBalanceInr: Long get() = paidBalancePaise
}
