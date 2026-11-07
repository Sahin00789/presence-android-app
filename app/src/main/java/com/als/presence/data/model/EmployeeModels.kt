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
    val phone: String? = null,
    val address: String? = null,
    val avatarUrl: String? = null,
    val role: String = "EMPLOYEE",
    val token: String? = null,
    val addedBy: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val hireDate: String? = null,
    val joinedAt: String? = null,
    val createdAt: String? = null,
    val isActive: Boolean = true,
    val earnedBalancePaise: Long = 0L,
    val approvedUnpaidBalancePaise: Long = 0L,
    val paidBalancePaise: Long = 0L
) {
    val name: String get() = fullName
    val employeeId: String get() = id
    val startDate: String get() = joinedAt ?: hireDate ?: createdAt ?: ""
    val earnedBalanceInr: Long get() = earnedBalancePaise
    val approvedUnpaidBalanceInr: Long get() = approvedUnpaidBalancePaise
    val paidBalanceInr: Long get() = paidBalancePaise
    val earnedBalanceCents: Long get() = earnedBalanceInr
    val approvedUnpaidBalanceCents: Long get() = approvedUnpaidBalanceInr
    val paidBalanceCents: Long get() = paidBalanceInr
}

typealias CreateEmployee = CreateEmployeeRequest

@Serializable
data class CreateEmployeeRequest(
    val email: String,
    val role: String = "EMPLOYEE",
    val fullName: String? = null,
    val staffCode: String? = null,
    val department: String? = null,
    val designation: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val avatarBase64: String? = null,
    val avatarUrl: String? = null,
    val premisesId: String? = null
)

@Serializable
data class UpdateEmployeeRequest(
    val email: String? = null,
    val fullName: String? = null,
    val department: String? = null,
    val designation: String? = null,
    val premisesId: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val avatarBase64: String? = null,
    val avatarUrl: String? = null
)
