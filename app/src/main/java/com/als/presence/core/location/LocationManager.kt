package com.als.presence.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.als.presence.data.remote.OlaMapsApiService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isMocked: Boolean = false
)

data class ReverseGeocodedAddress(
    val streetAddress: String = "",
    val pincode: String = "",
    val state: String = "",
    val district: String = "",
    val block: String = ""
)

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val olaMapsApiService: OlaMapsApiService
) {
    sealed class LocationResult {
        data class Success(val location: GpsLocation) : LocationResult()
        data class Error(val message: String) : LocationResult()
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationResult(timeoutMillis: Long = 10_000L): LocationResult {
        if (!LocationPermissionUtil.hasLocationPermission(context)) {
            return LocationResult.Error("Location permission is not granted. Please allow location access in your device settings.")
        }

        val sysLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val isGpsEnabled = sysLocationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
        if (!isGpsEnabled) {
            return LocationResult.Error("Location services (GPS) are turned off. Please enable Location in your device settings.")
        }

        val cancellationTokenSource = CancellationTokenSource()
        val freshLocation: Location? = withTimeoutOrNull(timeoutMillis) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).awaitTask(cancellationTokenSource)
            } catch (_: Exception) {
                null
            }
        }

        val locationToUse = freshLocation ?: run {
            try {
                fusedLocationClient.lastLocation.awaitTask()
            } catch (_: Exception) {
                null
            }
        }

        return if (locationToUse != null) {
            val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationToUse.isMock
            } else {
                @Suppress("DEPRECATION")
                locationToUse.isFromMockProvider
            }
            LocationResult.Success(
                GpsLocation(
                    latitude = locationToUse.latitude,
                    longitude = locationToUse.longitude,
                    accuracyMeters = locationToUse.accuracy,
                    isMocked = isMock
                )
            )
        } else {
            LocationResult.Error("Unable to acquire a GPS location fix. Please make sure you are in an open area and try again.")
        }
    }

    suspend fun getCurrentLocation(timeoutMillis: Long = 10_000L): GpsLocation? {
        return when (val res = getCurrentLocationResult(timeoutMillis)) {
            is LocationResult.Success -> res.location
            is LocationResult.Error -> null
        }
    }

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        olaApiKey: String = com.als.presence.BuildConfig.OLA_MAPS_API_KEY
    ): ReverseGeocodedAddress? = withContext(Dispatchers.IO) {
        // 1. Try Ola Maps API if API Key is supplied or available
        if (olaApiKey.isNotBlank()) {
            try {
                val response = olaMapsApiService.reverseGeocode("$latitude,$longitude", olaApiKey)
                if (response.isSuccessful) {
                    val result = response.body()?.results?.firstOrNull()
                    if (result != null) {
                        val components = result.addressComponents
                        val pincode = components.firstOrNull { it.types.contains("postal_code") }?.longName.orEmpty()
                        val state = components.firstOrNull { it.types.contains("administrative_area_level_1") }?.longName.orEmpty()
                        val district = components.firstOrNull { it.types.contains("administrative_area_level_2") }?.longName
                            ?: components.firstOrNull { it.types.contains("locality") }?.longName.orEmpty()
                        val block = components.firstOrNull { it.types.contains("sublocality") || it.types.contains("sublocality_level_1") }?.longName
                            ?: components.firstOrNull { it.types.contains("locality") }?.longName.orEmpty()

                        return@withContext ReverseGeocodedAddress(
                            streetAddress = result.formattedAddress.ifBlank { result.name.orEmpty() },
                            pincode = pincode,
                            state = state,
                            district = district,
                            block = block
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back to Android system Geocoder
            }
        }

        // 2. Fallback to native Android Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }
                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
            }

            val addr = addresses.firstOrNull() ?: return@withContext null
            val street = addr.getAddressLine(0) ?: buildString {
                if (!addr.featureName.isNullOrBlank()) append(addr.featureName)
                if (!addr.thoroughfare.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(addr.thoroughfare)
                }
                if (!addr.subLocality.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(addr.subLocality)
                }
            }
            val pincode = addr.postalCode.orEmpty()
            val state = addr.adminArea.orEmpty()
            val district = addr.subAdminArea.orEmpty().ifBlank { addr.locality.orEmpty() }
            val block = addr.subLocality.orEmpty().ifBlank { addr.locality.orEmpty() }

            ReverseGeocodedAddress(
                streetAddress = street,
                pincode = pincode,
                state = state,
                district = district,
                block = block
            )
        } catch (_: Exception) {
            null
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(cancellationTokenSource: CancellationTokenSource? = null): T? =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancellationTokenSource?.cancel()
        }
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
