package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRecordDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String = "",
    val employeeId: String = "",
    val staffName: String? = null,
    val staffCode: String? = null,
    val workDate: String = "",
    val status: String = "",
    val clockIn: String? = null,
    val clockOut: String? = null,
    val overtimeId: String? = null,
    val outsidePingCount: Int = 0,
    val manualOverride: Boolean = false,
    val workedMinutes: Int = 0,
    val createdAt: String? = null
)

@Serializable
data class AttendanceExceptionDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String = "",
    val employeeId: String = "",
    val staffName: String? = null,
    val employeeName: String? = null,
    val staffCode: String? = null,
    val workDate: String = "",
    val exceptionType: String = "",
    val status: String = "",
    val reason: String? = null,
    val details: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ResolveExceptionRequest(
    val correctedStatus: String,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ManualMarkAttendanceRequest(
    val attendanceEventId: String? = null,
    val employeeId: String? = null,
    val workDate: String? = null,
    val overtimeId: String? = null,
    val action: String,
    val reason: String? = null
)

@Serializable
data class AttendanceSummaryDto(
    val totalDays: Int = 0,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val lateDays: Int = 0,
    val holidayDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val totalWorkedMinutes: Long = 0L,
    val totalOvertimeMinutes: Long = 0L,
    val presentShiftsCount: Int = 0,
    val absentShiftsCount: Int = 0,
    val halfDayShiftsCount: Int = 0,
    val leaveShiftsCount: Int = 0,
    val holidayCount: Int = 0,
    val offDayCount: Int = 0,
    val overtimeCount: Int = 0
)

@Serializable
data class ClockInRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val isMocked: Boolean = false,
    val faceVerified: Boolean = true,
    val overtimeId: String? = null,
    val embeddingVector: List<Double> = emptyList()
)

@Serializable
data class ClockOutResponseDto(
    val id: String = "",
    val clockOut: String = ""
)

@Serializable
data class FaceStatusDto(
    val enrolled: Boolean = false,
    val modelVersion: String? = null,
    val staffProfileId: String? = null,
    val enrolledAt: String? = null,
    val embeddingDimensions: Int? = null,
    val revokedAt: String? = null
)
