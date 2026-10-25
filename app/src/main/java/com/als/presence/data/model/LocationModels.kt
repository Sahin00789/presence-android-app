package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsPingSummaryDto(
    val id: String,
    val checkTime: String,
    val distanceMeters: Double? = null,
    val accuracyMeters: Double? = null,
    val status: String
)

@Serializable
data class GpsCountsDto(
    val inside: Int = 0,
    val outside: Int = 0,
    val missed: Int = 0
)

@Serializable
data class LocationStaffCardDto(
    val staffProfileId: String,
    val userId: String,
    val fullName: String,
    val role: String = "STAFF",
    val staffCode: String,
    val avatarUrl: String? = null,
    val premisesId: String? = null,
    val premisesName: String,
    val attendanceStatus: String,
    val locationStatus: String,
    val dayOfWeek: String = "Today",
    val shiftName: String = "Regular Shift",
    val overtimeName: String? = null,
    val regularShiftStartTime: String? = null,
    val regularShiftEndTime: String? = null,
    val overtimeStartTime: String? = null,
    val overtimeEndTime: String? = null,
    val clockInTime: String? = null,
    val clockOutTime: String? = null,
    val overtimeClockInTime: String? = null,
    val overtimeClockOutTime: String? = null,
    val shiftStatus: String,
    val hasOvertimeToday: Boolean = false,
    val consecutiveFailures: Int = 0,
    val exceptionId: String? = null,
    val counts: GpsCountsDto = GpsCountsDto(),
    val latestPings: List<GpsPingSummaryDto> = emptyList()
)

@Serializable
data class LocationTodayPresenceResponseDto(
    val premisesId: String? = null,
    val premisesName: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radiusMeters: Int? = null,
    val date: String,
    val staff: List<LocationStaffCardDto> = emptyList()
)

@Serializable
data class AssignStaffPremisesRequest(
    val premisesId: String? = null
)
