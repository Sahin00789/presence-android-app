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
    val workDate: String = "",
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
data class AssignedShiftIdWithDate(
    val shiftAssignmentId: String,
    val workDate: String
)

@Serializable
data class LeaveRequestCreate(
    val shiftAssignmentId: String? = null,
    val workDate: String? = null,
    val assignedShiftIdWithDate: List<AssignedShiftIdWithDate>? = null,
    val assignedshiftidwithdate: List<AssignedShiftIdWithDate>? = null,
    val reason: String
)

@Serializable
data class UpcomingShiftDto(
    val shiftAssignmentId: String,
    val shiftId: String? = null,
    val shiftName: String,
    val workDate: String,
    val dayOfWeek: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val premisesName: String? = null,
    val alreadyRequested: Boolean = false
)

@Serializable
data class ReviewLeaveBody(
    val reviewRemarks: String? = null,
    val note: String? = null
)

@Serializable
data class ReviewLeaveRequest(
    val decision: String,
    val reviewRemarks: String? = null
)
