package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StaffDto(
    val id: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val staffCode: String,
    val department: String? = null,
    val designation: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val hireDate: String,
    val isActive: Boolean,
    val compensationType: String? = null,
    val baseRateCents: Long? = null,
    val overtimeRateCents: Long? = null,
    val earnedBalanceCents: Long = 0,
    val approvedUnpaidBalanceCents: Long = 0,
    val paidBalanceCents: Long = 0,
    val totalBalanceCents: Long = 0
)

@Serializable
data class InviteStaffRequest(
    val email: String,
    val fullName: String,
    val staffCode: String,
    val department: String? = null,
    val designation: String? = null,
    val role: String = "STAFF",
    val premisesId: String? = null
)

@Serializable
data class SalaryStructureDto(
    val id: String,
    val staffProfileId: String,
    val compensationType: String = "SHIFT",
    val baseRateCents: Long,
    val overtimeRateCents: Long = 0,
    val effectiveFrom: String
)

@Serializable
data class UpsertSalaryRequest(
    val compensationType: String = "SHIFT",
    val baseRateCents: Long,
    val overtimeRateCents: Long = 0,
    val effectiveFrom: String? = null
)
