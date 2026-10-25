package com.als.presence.domain.usecase.location

import com.als.presence.core.common.Resource
import com.als.presence.core.location.GpsLocation
import com.als.presence.core.location.LocationManager
import javax.inject.Inject

class GetGpsLocationUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(): Resource<GpsLocation> {
        val location = locationManager.getCurrentLocation()
        return if (location != null) {
            Resource.Success(location)
        } else {
            val msg = "Unable to fetch GPS location. Ensure location permission and GPS are enabled."
            Resource.Error(IllegalStateException(msg), msg)
        }
    }
}
