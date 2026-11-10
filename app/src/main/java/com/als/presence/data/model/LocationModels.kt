package com.als.presence.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

object FlexibleNullableDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleNullableDouble", PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double? {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                if (element.content.equals("null", ignoreCase = true) || element.content.isBlank()) {
                    null
                } else {
                    element.doubleOrNull ?: element.content.toDoubleOrNull()
                }
            } else {
                null
            }
        } else {
            try {
                if (decoder.decodeNotNullMark()) decoder.decodeDouble() else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Serializable
data class GpsPingSummaryDto(
    val id: String = "",
    val employeeId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @Serializable(with = FlexibleNullableDoubleSerializer::class)
    val distanceMeters: Double? = null,
    @Serializable(with = FlexibleNullableDoubleSerializer::class)
    val accuracyMeters: Double? = null,
    val isOutside: Boolean = false,
    val pingTime: String = "",
    val checkTime: String = pingTime,
    val status: String = if (isOutside) "Outside" else "Inside"
)

@Serializable
data class GpsCountsDto(
    val inside: Int = 0,
    val outside: Int = 0,
    val missed: Int = 0
)

@Serializable
data class LocationStaffCardDto(
    val staffProfileId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val staffCode: String = "",
    val role: String = "EMPLOYEE",
    val avatarUrl: String? = null,
    val premisesId: String? = null,
    val premisesName: String = "",
    val attendanceStatus: String = "NOT_CLOCKED_IN",
    val locationStatus: String = "NOT_CLOCKED_IN",
    val dayOfWeek: String = "",
    val shiftName: String = "",
    val overtimeName: String? = null,
    val regularShiftStartTime: String? = null,
    val regularShiftEndTime: String? = null,
    val assignedStartTime: String? = null,
    val assignedEndTime: String? = null,
    val overtimeStartTime: String? = null,
    val overtimeEndTime: String? = null,
    val clockInTime: String? = null,
    val clockOutTime: String? = null,
    val overtimeClockInTime: String? = null,
    val overtimeClockOutTime: String? = null,
    val shiftStatus: String = "",
    val hasOvertimeToday: Boolean = false,
    val overtimeId: String? = null,
    val consecutiveFailures: Int = 0,
    val exceptionId: String? = null,
    val earnedAmountPaise: Long = 0L,
    val counts: GpsCountsDto = GpsCountsDto(),
    val latestPings: List<GpsPingSummaryDto> = emptyList()
) {
    val employeeId: String get() = staffProfileId
    val employeeName: String get() = fullName
    val employeeCode: String get() = staffCode
    val earnedAmountInr: Long get() = earnedAmountPaise
    val outsidePingCount: Int get() = if (counts.outside > 0) counts.outside else consecutiveFailures
}

@Serializable
data class LocationTodayPresenceResponseDto(
    val premisesId: String? = null,
    val premisesName: String? = null,
    val address: String? = null,
    val radiusMeters: Int? = 50,
    @Serializable(with = FlexibleNullableDoubleSerializer::class)
    val latitude: Double? = null,
    @Serializable(with = FlexibleNullableDoubleSerializer::class)
    val longitude: Double? = null,
    val summaryDate: String = "",
    val date: String = "",
    val totalActiveStaff: Int = 0,
    val totalClockedIn: Int = 0,
    val outsideGeofenceCount: Int = 0,
    val staffCards: List<LocationStaffCardDto> = emptyList(),
    val staff: List<LocationStaffCardDto> = emptyList()
) {
    val staffList: List<LocationStaffCardDto> get() = staff.ifEmpty { staffCards }
}

@Serializable
data class LocationSessionDto(
    val attendanceEventId: String? = null,
    val isActive: Boolean = false,
    val consecutiveFailures: Int = 0
)

@Serializable
data class LocationCheckRequestDto(
    val attendanceEventId: String? = null,
    val shiftAttendanceId: String? = null,
    val overtimeAttendanceId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val isMock: Boolean? = null,
    val timestamp: String? = null,
    val clientRequestId: String? = null
)

@Serializable
data class LocationCheckResultDto(
    val result: String = "OK",
    val consecutiveFailures: Int = 0,
    val exceptionCreated: Boolean = false
)

