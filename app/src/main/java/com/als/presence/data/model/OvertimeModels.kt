package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OvertimeDto(
    val id: String,
    val organizationId: String,
    val staffProfileId: String,
    val staffName: String,
    val staffEmail: String,
    val staffCode: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val workDate: String,
    val startTime: String,
    val endTime: String,
    val plannedMinutes: Int,
    val workedMinutes: Int = 0,
    val paymentInr: Long,
    val status: String,
    val reason: String? = null,
    val attendanceEventId: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String
)

@Serializable
data class CreateOvertimeRequest(
    val staffProfileId: String? = null,
    val workDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val paymentInr: Long = 0L,
    val reason: String = ""
)
