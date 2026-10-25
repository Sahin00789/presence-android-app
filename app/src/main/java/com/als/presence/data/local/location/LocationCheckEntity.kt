package com.als.presence.data.local.location

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline outbox for periodic active-shift location pings.
 *
 * A ping is written here first and only removed once the backend acknowledged it,
 * so a temporary loss of connectivity never silently drops a monitoring check
 * (`docs/LOCATION_MONITORING.md` §2, `docs/TESTING.md`).
 */
@Entity(tableName = "location_check_outbox")
data class LocationCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attendanceEventId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val isMock: Boolean,
    val capturedAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null
)
