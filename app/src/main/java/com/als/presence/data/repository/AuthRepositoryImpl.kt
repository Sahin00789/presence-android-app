package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.*
import com.als.presence.data.remote.*
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val currentUserFlow = MutableStateFlow<User?>(null)

    override suspend fun requestOtp(email: String, role: String): Resource<OtpResponse> {
        return safeApiCall("Failed to request OTP") {
            authApiService.requestOtp(OtpRequest(email = email, role = role))
        }
    }

    override suspend fun verifyOtp(
        email: String,
        otp: String,
        role: String,
        fullName: String?,
        organizationName: String?,
        timezone: String?,
        currency: String?,
        premisesName: String?,
        premisesAddress: String?,
        latitude: String?,
        longitude: String?,
        radiusMeters: Int?
    ): Resource<User> {
        val result = safeApiCall("Failed to verify OTP") {
            authApiService.verifyOtp(
                OtpVerifyRequest(
                    email = email,
                    code = otp,
                    role = role,
                    fullName = fullName,
                    organizationName = organizationName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    premisesAddress = premisesAddress,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters
                )
            )
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                sessionManager.organizationId = authResponse.user.organizationId
                sessionManager.userId = authResponse.user.id
                sessionManager.role = authResponse.user.role
                val user = authResponse.user.toDomainUser()
                currentUserFlow.value = user
                Resource.Success(user)
            }
            is Resource.Error -> {
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun googleExchange(idToken: String): Resource<User> {
        val result = safeApiCall("Failed to authenticate with Google") {
            authApiService.googleExchange(GoogleExchangeRequest(code = idToken))
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                sessionManager.organizationId = authResponse.user.organizationId
                sessionManager.userId = authResponse.user.id
                sessionManager.role = authResponse.user.role
                val user = authResponse.user.toDomainUser()
                currentUserFlow.value = user
                Resource.Success(user)
            }
            is Resource.Error -> {
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun registerOwner(
        idToken: String,
        orgName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radius: Int,
        premisesAddress: String?,
        latitude: String?,
        longitude: String?
    ): Resource<User> {
        val result = safeApiCall("Failed to register owner") {
            authApiService.registerOwnerWithGoogle(
                GoogleOwnerRegistrationRequest(
                    idToken = idToken,
                    organizationName = orgName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    premisesAddress = premisesAddress,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radius
                )
            )
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                sessionManager.organizationId = authResponse.user.organizationId
                sessionManager.userId = authResponse.user.id
                sessionManager.role = authResponse.user.role
                val user = authResponse.user.toDomainUser()
                currentUserFlow.value = user
                Resource.Success(user)
            }
            is Resource.Error -> {
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getLoggedDevices(): Resource<List<LoggedDevice>> {
        return Resource.Success(emptyList())
    }

    override suspend fun revokeDevice(deviceId: String): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun logout() {
        sessionManager.clear()
        currentUserFlow.value = null
    }

    override suspend fun refreshToken(): Resource<User> {
        val currentRefreshToken = sessionManager.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            return Resource.Error(Exception("No refresh token stored"), "No refresh token stored")
        }
        val result = safeApiCall("Failed to refresh session") {
            authApiService.refreshToken(RefreshTokenRequest(refreshToken = currentRefreshToken))
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                sessionManager.organizationId = authResponse.user.organizationId
                sessionManager.userId = authResponse.user.id
                sessionManager.role = authResponse.user.role
                val user = authResponse.user.toDomainUser()
                currentUserFlow.value = user
                Resource.Success(user)
            }
            is Resource.Error -> {
                sessionManager.clear()
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun updateProfile(fullName: String?, phone: String?, avatarBase64: String?): Resource<User> {
        val current = currentUserFlow.value ?: User(id = "1", email = "user@example.com", fullName = fullName ?: "User", role = Role.STAFF, isActive = true)
        val updated = current.copy(fullName = fullName ?: current.fullName)
        currentUserFlow.value = updated
        return Resource.Success(updated)
    }

    override fun observeCurrentUser(): Flow<User?> = currentUserFlow.asStateFlow()
}
