package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaveDto(
    val id: String = "",
    val organizationId: String = "",
    val staffProfileId: String = "",
    val staffName: String? = null,
    val staffCode: String? = null,
    val shiftName: String? = null,
    val type: String = "PAID",
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val reviewNote: String? = null,
    val approvedBy: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateLeaveRequest(
    val startDate: String,
    val endDate: String,
    val reason: String
)

@Serializable
data class LeaveRequestCreate(
    val shiftAssignmentId: String,
    val workDate: String,
    val reason: String
)
