package com.als.presence.domain.usecase.location

import com.als.presence.core.common.Resource
import com.als.presence.core.location.GpsLocation
import com.als.presence.core.location.LocationManager
import javax.inject.Inject

/**
 * UseCase to obtain single-shot high-accuracy GPS coordinates via Fused Location Provider.
 */
class GetGpsLocationUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    /** Retrieves current device GPS coordinates. */
    suspend operator fun invoke(): Resource<GpsLocation> {
        return when (val result = locationManager.getCurrentLocationResult()) {
            is LocationManager.LocationResult.Success -> Resource.Success(result.location)
            is LocationManager.LocationResult.Error -> Resource.Error(
                Exception(result.message),
                result.message
            )
        }
    }
}
