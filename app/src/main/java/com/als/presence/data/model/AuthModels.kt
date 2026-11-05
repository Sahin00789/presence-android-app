package com.als.presence.data.model

import com.als.presence.BuildConfig
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(
    val email: String,
    val role: String = "EMPLOYEE",
    val isRegistration: Boolean = false
)

@Serializable
data class OtpResponse(
    val sent: Boolean = true,
    val email: String = "",
    val expiresInSeconds: Int = 600,
    @SerialName("devOtp")
    val rawDevOtp: String? = null
) {
    val devOtp: String?
        get() = if (BuildConfig.DEBUG) rawDevOtp else null
}

@Serializable
data class OtpVerifyRequest(
    val email: String,
    val code: String,
    val role: String = "EMPLOYEE",
    val fullName: String? = null,
    val organizationName: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    val premisesName: String? = null,
    val premisesAddress: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radiusMeters: Int? = null,
    val deviceName: String? = null
)

@Serializable
data class GoogleExchangeRequest(
    /** Preferred on Android: the server verifies it without needing a client secret. */
    val idToken: String? = null,
    /** Fallback: server auth code, exchanged with the web client secret. */
    val code: String? = null,
    val redirectUri: String? = null,
    val deviceName: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceName: String? = null
)

@Serializable
data class GoogleOwnerRegistrationRequest(
    val idToken: String,
    val organizationName: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    val premisesName: String? = null,
    val premisesAddress: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radiusMeters: Int? = null,
    val deviceName: String? = null
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val organizationId: String,
    val organizationName: String,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val avatarUrl: String? = null,
    val organizationLogoUrl: String? = null
) {
    fun toDomainUser(): User {
        val userRole = try {
            Role.valueOf(role.uppercase())
        } catch (_: Exception) {
            Role.EMPLOYEE
        }
        return User(
            id = id,
            email = email,
            fullName = fullName,
            role = userRole,
            organizationId = organizationId,
            organizationName = organizationName,
            premisesId = premisesId,
            premisesName = premisesName,
            avatarUrl = avatarUrl,
            organizationLogoUrl = organizationLogoUrl
        )
    }
}

@Serializable
data class AuthResponse(
    val accessToken: String,
    val expiresIn: Int = 900,
    val refreshToken: String,
    val user: AuthUserDto
)
