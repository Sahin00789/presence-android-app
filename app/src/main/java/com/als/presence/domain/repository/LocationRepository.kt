package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface LocationRepository {
    suspend fun getTodayPresence(): Resource<LocationTodayPresenceResponseDto>
    suspend fun getTodayLocationPresence(premisesId: String? = null): Resource<LocationTodayPresenceResponseDto> = getTodayPresence()
    suspend fun getActiveLocationSession(): Resource<LocationSessionDto>
    suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto>
    suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit>
}
