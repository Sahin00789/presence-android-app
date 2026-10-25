package com.als.presence.domain.model

data class LoggedDevice(
    val id: String,
    val userId: String,
    val sessionJti: String,
    val deviceName: String? = null,
    val deviceType: String? = null,
    val osName: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val isActive: Boolean,
    val isCurrentDevice: Boolean = false,
    val lastActiveAt: String,
    val createdAt: String,
    val revokedAt: String? = null
)
