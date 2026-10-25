package com.als.presence.data.model

import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("fullName") val fullName: String,
    @SerialName("role") val role: String,
    @SerialName("organizationId") val organizationId: String,
    @SerialName("organizationName") val organizationName: String,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("organizationLogoUrl") val organizationLogoUrl: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        fullName = fullName,
        role = Role.fromString(role),
        organizationId = organizationId,
        organizationName = organizationName,
        avatarUrl = avatarUrl,
        organizationLogoUrl = organizationLogoUrl
    )
}

@Serializable
data class UpdateProfileRequest(
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null
)

@Serializable
data class AuthTokenResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiresIn") val expiresIn: Long,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("user") val user: UserDto
)

@Serializable
data class OtpRequest(
    @SerialName("email") val email: String,
    @SerialName("role") val role: String
)

@Serializable
data class OtpResponse(
    @SerialName("sent") val sent: Boolean,
    @SerialName("email") val email: String,
    @SerialName("expiresInSeconds") val expiresInSeconds: Long,
    @SerialName("devOtp") val devOtp: String? = null
)

@Serializable
data class OtpVerifyRequest(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("role") val role: String,
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("organizationName") val organizationName: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("premisesName") val premisesName: String? = null,
    @SerialName("radiusMeters") val radiusMeters: Int? = null,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("deviceType") val deviceType: String? = null,
    @SerialName("osName") val osName: String? = null
)

@Serializable
data class GoogleExchangeRequest(
    @SerialName("code") val code: String,
    @SerialName("redirectUri") val redirectUri: String? = null,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("deviceType") val deviceType: String? = null,
    @SerialName("osName") val osName: String? = null
)

@Serializable
data class GoogleOwnerRegistrationRequest(
    @SerialName("idToken") val idToken: String,
    @SerialName("organizationName") val organizationName: String,
    @SerialName("timezone") val timezone: String,
    @SerialName("currency") val currency: String,
    @SerialName("premisesName") val premisesName: String,
    @SerialName("radiusMeters") val radiusMeters: Int,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("deviceType") val deviceType: String? = null,
    @SerialName("osName") val osName: String? = null
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("deviceType") val deviceType: String? = null,
    @SerialName("osName") val osName: String? = null
)

@Serializable
data class LogoutResponse(
    @SerialName("loggedOut") val loggedOut: Boolean,
    @SerialName("revokedSessions") val revokedSessions: Int? = null
)

@Serializable
data class LoggedDeviceDto(
    @SerialName("id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("sessionJti") val sessionJti: String,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("deviceType") val deviceType: String? = null,
    @SerialName("osName") val osName: String? = null,
    @SerialName("ipAddress") val ipAddress: String? = null,
    @SerialName("userAgent") val userAgent: String? = null,
    @SerialName("isActive") val isActive: Boolean,
    @SerialName("isCurrentDevice") val isCurrentDevice: Boolean = false,
    @SerialName("lastActiveAt") val lastActiveAt: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("revokedAt") val revokedAt: String? = null
) {
    fun toDomain(): LoggedDevice = LoggedDevice(
        id = id,
        userId = userId,
        sessionJti = sessionJti,
        deviceName = deviceName,
        deviceType = deviceType,
        osName = osName,
        ipAddress = ipAddress,
        userAgent = userAgent,
        isActive = isActive,
        isCurrentDevice = isCurrentDevice,
        lastActiveAt = lastActiveAt,
        createdAt = createdAt,
        revokedAt = revokedAt
    )
}

@Serializable
data class LoggedDevicesResponseDto(
    @SerialName("devices") val devices: List<LoggedDeviceDto>
)
