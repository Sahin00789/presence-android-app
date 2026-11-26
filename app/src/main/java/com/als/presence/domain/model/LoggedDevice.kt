package com.als.presence.domain.model

/**
 * Model representing an active device session logged into the account.
 *
 * @property id Unique session/device log ID.
 * @property deviceName Human-readable name or user-agent of the device.
 * @property lastActiveAt ISO timestamp of last active session communication.
 * @property isCurrent Flag indicating if this session matches the current device.
 */
data class LoggedDevice(
    val id: String,
    val deviceName: String? = null,
    val lastActiveAt: String? = null,
    val isCurrent: Boolean = false
)
