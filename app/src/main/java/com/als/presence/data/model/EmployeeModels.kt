package com.als.presence.data.model

import kotlinx.serialization.Serializable

typealias EmployeeDto = StaffDto

@Serializable
data class StaffDto(
    val id: String = "",
    val organizationId: String = "",
    val organizationName: String? = null,
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val staffCode: String = "",
    val department: String? = null,
    val designation: String? = null,
    val role: String = "STAFF",
    val token: String? = null,
    val addedBy: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val hireDate: String? = null,
    val joinedAt: String? = null,
    val createdAt: String? = null,
    val isActive: Boolean = true,
    val earnedBalanceCents: Long = 0L,
    val approvedUnpaidBalanceCents: Long = 0L,
    val paidBalanceCents: Long = 0L
) {
    val name: String get() = fullName
    val employeeId: String get() = id
    val startDate: String get() = joinedAt ?: hireDate ?: createdAt ?: ""
}

typealias CreateEmployee = CreateEmployeeRequest

@Serializable
data class CreateEmployeeRequest(
    val email: String,
    val role: String,
    val fullName: String? = null,
    val staffCode: String? = null,
    val department: String? = null,
    val designation: String? = null,
    val premisesId: String? = null
)
