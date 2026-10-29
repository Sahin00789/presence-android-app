package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.LocationApiService
import com.als.presence.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationApiService: LocationApiService
) : LocationRepository {

    override suspend fun getTodayPresence(): Resource<LocationTodayPresenceResponseDto> {
        return safeApiCall("Error loading presence") {
            locationApiService.getToday()
        }
    }

    override suspend fun getTodayLocationPresence(premisesId: String?): Resource<LocationTodayPresenceResponseDto> {
        return getTodayPresence()
    }

    override suspend fun getActiveLocationSession(): Resource<LocationSessionDto> {
        return Resource.Success(LocationSessionDto(isActive = false))
    }

    override suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto> {
        return Resource.Success(LocationCheckResultDto(result = "OK", exceptionCreated = false))
    }

    override suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit> {
        return Resource.Success(Unit)
    }
}
