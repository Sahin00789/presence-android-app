package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun checkServerHealth(): Boolean
    suspend fun requestOtp(email: String, role: String, isRegistration: Boolean = false): Resource<OtpResponse>
    suspend fun verifyOtp(
        email: String,
        otp: String,
        role: String = "STAFF",
        fullName: String? = null,
        organizationName: String? = null,
        timezone: String? = null,
        currency: String? = null,
        premisesName: String? = null,
        premisesAddress: String? = null,
        latitude: String? = null,
        longitude: String? = null,
        radiusMeters: Int? = null
    ): Resource<User>
    suspend fun googleExchange(
        idToken: String? = null,
        serverAuthCode: String? = null
    ): Resource<User>
    suspend fun registerOwner(
        idToken: String,
        orgName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radius: Int,
        premisesAddress: String? = null,
        latitude: String? = null,
        longitude: String? = null
    ): Resource<User>
    suspend fun getLoggedDevices(): Resource<List<LoggedDevice>>
    suspend fun revokeDevice(deviceId: String): Resource<Unit>
    suspend fun logout()
    suspend fun refreshToken(): Resource<User>
    suspend fun updateProfile(fullName: String? = null, phone: String? = null, avatarBase64: String? = null): Resource<User>
    fun observeCurrentUser(): Flow<User?>
    fun observeSessionChecking(): Flow<Boolean>
}
