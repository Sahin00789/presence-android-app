package com.als.presence.data.repository

import android.os.Build
import com.als.presence.core.common.Resource
import com.als.presence.core.network.NetworkMonitor
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.SessionManager
import com.als.presence.data.local.UserDao
import com.als.presence.data.local.UserEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.*
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.di.ApplicationScope
import com.als.presence.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AuthRepository {

    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val isSessionCheckingFlow = MutableStateFlow(true)

    init {
        // The OkHttp authenticator wipes the session when a refresh is rejected
        // (401/403). Keep the in-memory user and Room DB in sync so the UI falls back
        // to the login screen instead of showing a stale dashboard.
        applicationScope.launch {
            sessionManager.sessionRevoked.collect {
                userDao.clear()
                currentUserFlow.value = null
            }
        }

        // Live network listener: When the device reconnects online, re-sync the latest user
        // profile & organization details from the server and persist them to Room DB.
        applicationScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && !sessionManager.refreshToken.isNullOrBlank()) {
                    syncUserProfile()
                }
            }
        }

        // Auto-check and restore session on app launch
        applicationScope.launch {
            checkAndRestoreSession()
        }
    }

    private suspend fun syncUserProfile() {
        if (sessionManager.userId == null) return
        try {
            val response = authApiService.getCurrentUser()
            if (response.isSuccessful) {
                val authUserDto = response.body()
                if (authUserDto != null) {
                    val user = authUserDto.toDomainUser()
                    saveUserToLocalCache(user)
                }
            } else if (response.code() != 401 && response.code() != 403) {
                // If getCurrentUser fails due to expired access token, try refreshToken()
                refreshToken()
            }
        } catch (e: Exception) {
            android.util.Log.w("AuthRepo", "Failed to sync user profile: ${e.localizedMessage}")
        }
    }

    private suspend fun saveUserToLocalCache(user: User) {
        sessionManager.token = sessionManager.token
        sessionManager.refreshToken = sessionManager.refreshToken
        sessionManager.organizationId = user.organizationId
        sessionManager.userId = user.id
        sessionManager.role = user.role.name

        userDao.insertUser(UserEntity.fromDomain(user))
        currentUserFlow.value = user
    }

    private suspend fun checkAndRestoreSession() {
        isSessionCheckingFlow.value = true
        val savedRefreshToken = sessionManager.refreshToken
        val savedUserId = sessionManager.userId
        val savedRole = sessionManager.role
        val savedOrgId = sessionManager.organizationId

        // 1. Immediately load locally cached User & Organization details from Room DB
        if (!savedUserId.isNullOrBlank()) {
            val cachedUserEntity = userDao.getUserById(savedUserId)
            if (cachedUserEntity != null) {
                currentUserFlow.value = cachedUserEntity.toDomain()
            } else if (!savedRole.isNullOrBlank()) {
                val roleEnum = runCatching { Role.valueOf(savedRole) }.getOrDefault(Role.EMPLOYEE)
                currentUserFlow.value = User(
                    id = savedUserId,
                    email = "",
                    fullName = "User",
                    role = roleEnum,
                    organizationId = savedOrgId ?: "",
                    isActive = true
                )
            }
        }

        // 2. Validate session token with backend if refresh token exists
        if (!savedRefreshToken.isNullOrBlank()) {
            when (val res = refreshToken()) {
                is Resource.Success -> {
                    // Session successfully validated & fresh user saved to Room DB
                }
                is Resource.Error -> {
                    if (res.isAuthFailure()) {
                        userDao.clear()
                        sessionManager.clear()
                        currentUserFlow.value = null
                    }
                    // If offline error (non-auth failure), keep the Room DB cached user active
                }
                is Resource.Loading -> Unit
            }
        } else {
            userDao.clear()
            currentUserFlow.value = null
        }
        isSessionCheckingFlow.value = false
    }

    private fun cleanEmail(email: String): String {
        return email.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
    }

    override suspend fun requestOtp(email: String, role: String, isRegistration: Boolean): Resource<OtpResponse> {
        val sanitized = cleanEmail(email)
        return safeApiCall("Failed to request OTP") {
            authApiService.requestOtp(OtpRequest(email = sanitized, role = role, isRegistration = isRegistration))
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
        val sanitized = cleanEmail(email)
        val result = safeApiCall("Failed to verify OTP") {
            authApiService.verifyOtp(
                OtpVerifyRequest(
                    email = sanitized,
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
                val user = authResponse.user.toDomainUser()
                saveUserToLocalCache(user)
                Resource.Success(user)
            }
            is Resource.Error -> {
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun googleExchange(
        idToken: String?,
        serverAuthCode: String?
    ): Resource<User> {
        val result = safeApiCall("Failed to authenticate with Google") {
            authApiService.googleExchange(
                GoogleExchangeRequest(
                    idToken = idToken?.takeIf { it.isNotBlank() },
                    code = serverAuthCode?.takeIf { it.isNotBlank() },
                    deviceName = Build.MODEL
                )
            )
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                val user = authResponse.user.toDomainUser()
                saveUserToLocalCache(user)
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
                    radiusMeters = radius,
                    deviceName = Build.MODEL
                )
            )
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                val user = authResponse.user.toDomainUser()
                saveUserToLocalCache(user)
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
        try {
            authApiService.logout()
        } catch (_: Exception) {
            // Offline or already-expired token
        }
        userDao.clear()
        sessionManager.clear()
        currentUserFlow.value = null
    }

    override suspend fun refreshToken(): Resource<User> {
        val currentRefreshToken = sessionManager.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            userDao.clear()
            sessionManager.clear()
            return Resource.Error(Exception("No refresh token stored"), "No refresh token stored")
        }
        val result = safeApiCall("Failed to refresh session") {
            authApiService.refreshToken(
                RefreshTokenRequest(refreshToken = currentRefreshToken, deviceName = Build.MODEL)
            )
        }
        return when (result) {
            is Resource.Success -> {
                val authResponse = result.data
                sessionManager.token = authResponse.accessToken
                sessionManager.refreshToken = authResponse.refreshToken
                val user = authResponse.user.toDomainUser()
                saveUserToLocalCache(user)
                Resource.Success(user)
            }
            is Resource.Error -> {
                if (result.isAuthFailure()) {
                    userDao.clear()
                    sessionManager.clear()
                    currentUserFlow.value = null
                }
                Resource.Error(result.exception, result.message, result.code)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    private fun Resource.Error.isAuthFailure(): Boolean {
        if (code in AUTH_FAILURE_CODES) return true
        val httpCode = (exception as? HttpException)?.code()
        return httpCode == 401 || httpCode == 403
    }

    override suspend fun updateProfile(fullName: String?, phone: String?, avatarBase64: String?): Resource<User> {
        val current = currentUserFlow.value ?: User(id = "1", email = "user@example.com", fullName = fullName ?: "User", role = Role.EMPLOYEE, isActive = true)
        val updated = current.copy(
            fullName = fullName ?: current.fullName,
            phone = phone ?: current.phone
        )
        saveUserToLocalCache(updated)
        return Resource.Success(updated)
    }

    override suspend fun checkServerHealth(): Boolean {
        return try {
            val response = authApiService.checkHealth()
            if (response.isSuccessful) {
                true
            } else {
                val userResponse = authApiService.getCurrentUser()
                userResponse.isSuccessful || userResponse.code() in listOf(401, 403)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Health check primary failed: ${e.localizedMessage}")
            try {
                val userResponse = authApiService.getCurrentUser()
                userResponse.isSuccessful || userResponse.code() in listOf(401, 403)
            } catch (e2: Exception) {
                android.util.Log.e("AuthRepo", "Health check fallback failed: ${e2.localizedMessage}")
                false
            }
        }
    }

    override fun observeCurrentUser(): Flow<User?> = currentUserFlow.asStateFlow()

    override fun observeSessionChecking(): Flow<Boolean> = isSessionCheckingFlow.asStateFlow()

    private companion object {
        val AUTH_FAILURE_CODES = setOf(
            "INVALID_REFRESH_TOKEN",
            "REFRESH_TOKEN_REUSE",
            "SESSION_EXPIRED",
            "SESSION_REVOKED",
            "SESSION_NOT_FOUND",
            "DEVICE_REVOKED",
            "ACCOUNT_DEACTIVATED",
            "NO_MEMBERSHIP",
            "UNAUTHORIZED"
        )
    }
}
