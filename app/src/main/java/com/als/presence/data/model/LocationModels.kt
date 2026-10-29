package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsPingSummaryDto(
    val id: String = "",
    val employeeId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val isOutside: Boolean = false,
    val pingTime: String = ""
) {
    val status: String get() = if (isOutside) "Outside" else "Inside"
    val checkTime: String get() = pingTime
}

@Serializable
data class GpsCountsDto(
    val inside: Int = 0,
    val outside: Int = 0,
    val missed: Int = 0
)

@Serializable
data class LocationStaffCardDto(
    val employeeId: String = "",
    val employeeName: String = "",
    val employeeCode: String = "",
    val role: String = "STAFF",
    val premisesName: String = "",
    val attendanceStatus: String = "PRESENT",
    val clockInTime: String? = null,
    val clockOutTime: String? = null,
    val outsidePingCount: Int = 0,
    val locationStatus: String = if (outsidePingCount > 0) "Outside" else "Inside",
    val exceptionId: String? = null,
    val dayOfWeek: String = "",
    val shiftName: String = "",
    val regularShiftStartTime: String = "",
    val regularShiftEndTime: String = "",
    val hasOvertimeToday: Boolean = false,
    val overtimeId: String? = null,
    val overtimeName: String? = null,
    val overtimeStartTime: String? = null,
    val overtimeEndTime: String? = null,
    val overtimeClockInTime: String? = null,
    val overtimeClockOutTime: String? = null,
    val latestPings: List<GpsPingSummaryDto> = emptyList(),
    val counts: GpsCountsDto = GpsCountsDto(outside = outsidePingCount)
) {
    val fullName: String get() = employeeName
    val staffCode: String get() = employeeCode
}

@Serializable
data class LocationTodayPresenceResponseDto(
    val premisesId: String? = null,
    val premisesName: String? = null,
    val address: String? = null,
    val radiusMeters: Int? = 50,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val summaryDate: String = "",
    val totalActiveStaff: Int = 0,
    val totalClockedIn: Int = 0,
    val outsideGeofenceCount: Int = 0,
    val staffCards: List<LocationStaffCardDto> = emptyList()
) {
    val staff: List<LocationStaffCardDto> get() = staffCards
}

@Serializable
data class LocationSessionDto(
    val isActive: Boolean = false,
    val attendanceEventId: String? = null
)

@Serializable
data class LocationCheckResultDto(
    val result: String = "OK",
    val exceptionCreated: Boolean = false
)
