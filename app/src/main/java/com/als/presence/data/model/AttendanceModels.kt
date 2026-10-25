package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRecordDto(
    val id: String,
    val staffProfileId: String,
    val staffName: String? = null,
    val staffEmail: String? = null,
    val staffCode: String? = null,
    val workDate: String? = null,
    val clockIn: String? = null,
    val clockOut: String? = null,
    val status: String,
    val faceVerified: Boolean = false,
    val geofenceVerified: Boolean = false,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val scheduledMinutes: Int = 0,
    val premisesId: String? = null,
    val overtimeId: String? = null
)

@Serializable
data class AttendanceSummaryDto(
    val staffProfileId: String? = null,
    val totalDays: Int = 0,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val lateDays: Int = 0,
    val halfDays: Int = 0,
    val leaveDays: Int = 0,
    val unpaidLeaveDays: Int = 0,
    val paidLeaveDays: Int = 0,
    val holidayDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val totalWorkedMinutes: Long = 0,
    val totalScheduledMinutes: Long = 0,
    val totalOvertimeMinutes: Long = 0,
    val overtimeMinutes: Long = 0,
    val exceptionsCount: Int = 0
)

@Serializable
data class AttendanceExceptionDto(
    val id: String,
    val attendanceEventId: String,
    val reason: String,
    val status: String,
    val createdAt: String,
    val staffProfileId: String? = null,
    val staffName: String? = null,
    val staffEmail: String? = null,
    val staffCode: String? = null
)

@Serializable
data class ClockInRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val faceVerified: Boolean = true,
    val faceScore: Double? = null,
    val embeddingVector: List<Double>? = null,
    val premisesId: String? = null,
    val timestamp: String? = null,
    val overtimeId: String? = null,
    val isMocked: Boolean = false
)

@Serializable
data class ClockOutRequest(
    val attendanceEventId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Double? = null,
    val timestamp: String? = null
)

@Serializable
data class ManualClockInRequest(
    val attendanceEventId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val reason: String,
    val note: String? = null
)

@Serializable
data class LocationCheckRequest(
    val attendanceEventId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val isMock: Boolean = false,
    val timestamp: String? = null
)

@Serializable
data class LocationCheckResultDto(
    val result: String,
    val consecutiveFailures: Int = 0,
    val exceptionCreated: Boolean = false
)

@Serializable
data class EnrollFaceRequest(
    val staffProfileId: String? = null,
    val embeddingVector: List<Double>,
    val modelVersion: String = "sface-yunet-v1",
    val qualityScore: Double? = null,
    val premisesId: String? = null
)

@Serializable
data class EnrollMultiPoseRequest(
    val staffProfileId: String,
    val centerVector: List<Double>,
    val leftVector: List<Double>,
    val rightVector: List<Double>,
    val upVector: List<Double>,
    val downVector: List<Double>,
    val qualityScore: Double? = null,
    val modelVersion: String = "sface-yunet-v1",
    val premisesId: String? = null,
    val liveness: LivenessEvidenceDto = LivenessEvidenceDto()
)

@Serializable
data class LivenessEvidenceDto(
    val blinkCount: Int = 1,
    val passiveTextureScore: Double = 0.98,
    val depthScore: Double? = null
)

@Serializable
data class FaceStatusDto(
    val enrolled: Boolean,
    val staffProfileId: String? = null,
    val modelVersion: String? = null,
    val enrolledAt: String? = null,
    val embeddingDimensions: Int? = null,
    val enrollmentMethod: String? = null,
    val enrolledByUserId: String? = null,
    val enrolledByName: String? = null,
    val enrolledAtPremisesId: String? = null,
    val revokedAt: String? = null
)

@Serializable
data class FaceVerificationDto(
    val matched: Boolean,
    val similarity: Double,
    val threshold: Double
)

@Serializable
data class LeaveRequestCreate(
    val organizationId: String? = null,
    val ownerId: String? = null,
    val staffProfileId: String? = null,
    val shiftAssignmentId: String? = null,
    val shiftTemplateId: String? = null,
    val shiftName: String? = null,
    val startDate: String,
    val endDate: String,
    val type: String = "PAID",
    val reason: String
)

@Serializable
data class LeaveReviewRequest(
    val decision: String,
    val note: String? = null
)

@Serializable
data class LeaveDto(
    val id: String,
    val organizationId: String? = null,
    val staffProfileId: String,
    val shiftAssignmentId: String? = null,
    val shiftTemplateId: String? = null,
    val shiftName: String? = null,
    val staffName: String? = null,
    val staffCode: String? = null,
    val type: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val reason: String? = null,
    val reviewNote: String? = null,
    val requestedBy: String? = null,
    val reviewedBy: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String? = null
)

@Serializable
data class AttendanceEventDto(
    val id: String,
    val staffProfileId: String,
    val clockIn: String? = null,
    val clockOut: String? = null,
    val status: String,
    val workDate: String? = null,
    val premisesId: String? = null,
    val overtimeId: String? = null
)

@Serializable
data class DailyAttendanceDto(
    val id: String,
    val organisationId: String? = null,
    val premisesId: String,
    val staffId: String,
    val shiftAssignmentId: String,
    val workDate: String,
    val attendanceStatus: String,
    val clockInAt: String? = null,
    val clockOutAt: String? = null,
    val insidePingCount: Int = 0,
    val outsidePingCount: Int = 0,
    val paymentAmount: Double = 0.0,
    val statusUpdatedBy: String? = null,
    val remarks: String? = null,
    val staffName: String? = null,
    val staffCode: String? = null,
    val shiftName: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val halfDayThreshold: String? = null
)

@Serializable
data class CorrectDailyAttendanceRequest(
    val attendanceStatus: String,
    val remarks: String? = null,
    val overridePaymentAmount: Double? = null
)

@Serializable
data class ManualClockInResponse(
    val event: AttendanceEventDto,
    val exceptionId: String,
    val status: String
)

@Serializable
data class ResolveExceptionRequest(
    val correctedStatus: String,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ResolveExceptionResponse(
    val id: String,
    val status: String,
    val correctedStatus: String
)
