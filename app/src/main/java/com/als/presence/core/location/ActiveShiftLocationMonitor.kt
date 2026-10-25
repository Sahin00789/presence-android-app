package com.als.presence.core.location

import com.als.presence.core.common.Resource
import com.als.presence.data.local.location.LocationCheckDao
import com.als.presence.data.local.location.LocationCheckEntity
import com.als.presence.domain.repository.OperationsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes one ~10-minute GPS monitoring check for the active shift.
 *
 * Each reading is written to a Room outbox before it is posted so a network
 * failure never drops a check. The outbox is drained on the next run.
 */
@Singleton
class ActiveShiftLocationMonitor @Inject constructor(
    private val repository: OperationsRepository,
    private val outbox: LocationCheckDao,
    private val locationManager: LocationManager
) {
    data class Outcome(
        val active: Boolean,
        val result: String? = null,
        val exceptionCreated: Boolean = false,
        val error: String? = null
    )

    private val mutex = Mutex()

    suspend fun runOnce(): Outcome = mutex.withLock {
        drainOutbox()

        val session = repository.getActiveLocationSession()
        val active = (session as? Resource.Success)?.data
        val eventId = active?.attendanceEventId
        if (active?.isActive != true || eventId.isNullOrBlank()) {
            return@withLock Outcome(active = false)
        }

        val fix = locationManager.getCurrentLocation()
            ?: return@withLock Outcome(active = true, error = "No GPS fix available")

        val entry = LocationCheckEntity(
            attendanceEventId = eventId,
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyMeters = fix.accuracyMeters.toDouble(),
            isMock = fix.isMocked,
            capturedAt = System.currentTimeMillis()
        )
        val id = outbox.enqueue(entry)
        val stored = entry.copy(id = id)

        val result = repository.submitLocationCheck(
            attendanceEventId = eventId,
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyMeters = fix.accuracyMeters.toDouble(),
            isMock = fix.isMocked,
            capturedAt = stored.capturedAt
        )

        when (result) {
            is Resource.Success -> {
                outbox.delete(id)
                Outcome(
                    active = true,
                    result = result.data.result,
                    exceptionCreated = result.data.exceptionCreated
                )
            }
            is Resource.Error -> {
                outbox.markFailed(id, result.message)
                Outcome(active = true, error = result.message)
            }
            Resource.Loading -> Outcome(active = true)
        }
    }

    private suspend fun drainOutbox() {
        outbox.pending().forEach { entry ->
            when (
                val result = repository.submitLocationCheck(
                    attendanceEventId = entry.attendanceEventId,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    accuracyMeters = entry.accuracyMeters,
                    isMock = entry.isMock,
                    capturedAt = entry.capturedAt
                )
            ) {
                is Resource.Success -> outbox.delete(entry.id)
                is Resource.Error -> outbox.markFailed(entry.id, result.message)
                Resource.Loading -> Unit
            }
        }
    }
}
