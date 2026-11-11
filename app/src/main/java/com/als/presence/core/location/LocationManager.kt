package com.als.presence.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.als.presence.data.remote.OlaMapsApiService
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

fun calculateDistanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

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

        // 1. Try Fused High Accuracy with a sub-timeout (e.g. 50% of timeoutMillis)
        var tokenSource = CancellationTokenSource()
        val highAccuracyTimeout = (timeoutMillis * 0.5).toLong().coerceAtLeast(2_000L)
        var freshLocation: Location? = withTimeoutOrNull(highAccuracyTimeout) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).awaitTask(tokenSource)
            } catch (_: Exception) {
                null
            }
        }

        // 2. Try Fused Balanced Power Accuracy if High Accuracy returned null
        if (freshLocation == null) {
            tokenSource = CancellationTokenSource()
            val balancedTimeout = (timeoutMillis * 0.3).toLong().coerceAtLeast(1_500L)
            freshLocation = withTimeoutOrNull(balancedTimeout) {
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        tokenSource.token
                    ).awaitTask(tokenSource)
                } catch (_: Exception) {
                    null
                }
            }
        }

        // 3. Try Fused Last Location
        var locationToUse = freshLocation ?: run {
            try {
                fusedLocationClient.lastLocation.awaitTask()
            } catch (_: Exception) {
                null
            }
        }

        // 4. Try System LocationManager providers (GPS, Network, Passive)
        if (locationToUse == null) {
            val providers = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER,
                android.location.LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                try {
                    val loc = sysLocationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        locationToUse = loc
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        // 5. Fallback for Emulator / Debug environments
        if (locationToUse == null && (isEmulator() || com.als.presence.BuildConfig.DEBUG)) {
            return LocationResult.Success(
                GpsLocation(
                    latitude = 37.4220936,
                    longitude = -122.083922,
                    accuracyMeters = 5.0f,
                    isMocked = true
                )
            )
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

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    suspend fun getCurrentLocation(timeoutMillis: Long = 10_000L): GpsLocation? {
        return when (val res = getCurrentLocationResult(timeoutMillis)) {
            is LocationResult.Success -> res.location
            is LocationResult.Error -> null
        }
    }

    /**
     * Continuously emits high-accuracy GPS fixes at the specified interval.
     * Uses FusedLocationProviderClient with PRIORITY_HIGH_ACCURACY.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 1000L): Flow<GpsLocation> = callbackFlow {
        if (!LocationPermissionUtil.hasLocationPermission(context)) {
            close(IllegalStateException("Location permission is not granted. Please allow location access in your device settings."))
            return@callbackFlow
        }

        val sysLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val isGpsEnabled = sysLocationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
        if (!isGpsEnabled) {
            close(IllegalStateException("Location services (GPS) are turned off. Please enable Location in your device settings."))
            return@callbackFlow
        }

        // Emit cached last location if available immediately to bootstrap
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        loc.isMock
                    } else {
                        @Suppress("DEPRECATION")
                        loc.isFromMockProvider
                    }
                    trySend(
                        GpsLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = loc.accuracy,
                            isMocked = isMock
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                val loc = result.lastLocation ?: return
                val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    loc.isMock
                } else {
                    @Suppress("DEPRECATION")
                    loc.isFromMockProvider
                }
                trySend(
                    GpsLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        isMocked = isMock
                    )
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: Exception) {}
        }
    }

    /**
     * Awaits until a location fix with accuracy <= [targetAccuracyMeters] is acquired,
     * or returns the best acquired fix before [timeoutMillis].
     */
    suspend fun awaitAccurateLocation(
        targetAccuracyMeters: Float = 10f,
        timeoutMillis: Long = 15_000L
    ): GpsLocation? = withTimeoutOrNull(timeoutMillis) {
        var bestFix: GpsLocation? = null
        try {
            getLocationUpdates(intervalMs = 1000L).collect { fix ->
                if (bestFix == null || fix.accuracyMeters < bestFix!!.accuracyMeters) {
                    bestFix = fix
                }
                if (fix.accuracyMeters <= targetAccuracyMeters) {
                    throw CancellationException("Target accuracy achieved")
                }
            }
        } catch (e: CancellationException) {
            if (e.message == "Target accuracy achieved") {
                return@withTimeoutOrNull bestFix
            }
        } catch (_: Exception) {}
        bestFix
    } ?: getCurrentLocation()

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        olaApiKey: String = com.als.presence.BuildConfig.OLA_MAPS_API_KEY
    ): ReverseGeocodedAddress? = withContext(Dispatchers.IO) {
        // 1. Try Ola Maps API if API Key is supplied or available
        if (olaApiKey.isNotBlank()) {
            try {
                val requestId = java.util.UUID.randomUUID().toString()
                val response = olaMapsApiService.reverseGeocode(
                    latlng = "$latitude,$longitude",
                    apiKey = olaApiKey,
                    requestId = requestId
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val result = body?.results?.firstOrNull() ?: body?.result
                    if (result != null) {
                        val components = result.addressComponents
                        val pincode = components.firstOrNull { it.types.contains("postal_code") }?.longName.orEmpty()
                        val state = components.firstOrNull { it.types.contains("administrative_area_level_1") }?.longName.orEmpty()
                        val district = components.firstOrNull { it.types.contains("administrative_area_level_2") }?.longName
                            ?: components.firstOrNull { it.types.contains("locality") }?.longName.orEmpty()
                        val block = components.firstOrNull { it.types.contains("sublocality") || it.types.contains("sublocality_level_1") }?.longName
                            ?: components.firstOrNull { it.types.contains("locality") }?.longName.orEmpty()

                        val formattedStreet = result.formattedAddress.ifBlank { result.name.orEmpty() }
                        if (formattedStreet.isNotBlank()) {
                            return@withContext ReverseGeocodedAddress(
                                streetAddress = formattedStreet,
                                pincode = pincode,
                                state = state,
                                district = district,
                                block = block
                            )
                        }
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
