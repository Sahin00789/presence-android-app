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
            locationApiService.getTodayLocationPresence()
        }
    }

    override suspend fun getTodayLocationPresence(premisesId: String?): Resource<LocationTodayPresenceResponseDto> {
        return safeApiCall("Error loading presence") {
            locationApiService.getTodayLocationPresence(premisesId = premisesId)
        }
    }

    override suspend fun getActiveLocationSession(): Resource<LocationSessionDto> {
        return safeApiCall("Error loading active location session") {
            locationApiService.getActiveLocationSession()
        }
    }

    override suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto> {
        return safeApiCall("Error submitting location check") {
            locationApiService.submitLocationCheck(
                LocationCheckRequestDto(
                    attendanceEventId = attendanceEventId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    isMock = isMock
                )
            )
        }
    }

    override suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit> {
        return Resource.Success(Unit)
    }
}

