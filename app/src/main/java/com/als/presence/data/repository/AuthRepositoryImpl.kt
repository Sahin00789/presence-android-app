package com.als.presence.data.repository

import android.os.Build
import com.als.presence.core.common.Resource
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.GoogleExchangeRequest
import com.als.presence.data.model.GoogleOwnerRegistrationRequest
import com.als.presence.data.model.OtpRequest
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.model.OtpVerifyRequest
import com.als.presence.data.model.RefreshTokenRequest
import com.als.presence.data.model.UpdateProfileRequest
import com.als.presence.data.remote.AuthApiService
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val currentDeviceName: String get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    private val currentDeviceType: String get() = "MOBILE"
    private val currentOsName: String get() = "Android ${Build.VERSION.RELEASE}"

    override suspend fun requestOtp(email: String, role: Role): Resource<OtpResponse> {
        return try {
            val response = apiService.requestOtp(OtpRequest(email = email, role = role.name))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Failed to send OTP request" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = sanitizeErrorMessage(e, "Network error sending OTP request")
            )
        }
    }

    override suspend fun verifyOtp(
        email: String,
        code: String,
        role: Role,
        fullName: String?,
        organizationName: String?,
        timezone: String?,
        currency: String?,
        premisesName: String?,
        radiusMeters: Int?
    ): Resource<User> {
        return try {
            val response = apiService.verifyOtp(
                OtpVerifyRequest(
                    email = email,
                    code = code,
                    role = role.name,
                    fullName = fullName,
                    organizationName = organizationName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    radiusMeters = radiusMeters,
                    deviceName = currentDeviceName,
                    deviceType = currentDeviceType,
                    osName = currentOsName
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userDto = body.user
                )
                Resource.Success(body.user.toDomain())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "OTP verification failed" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Network error during OTP verification"
            )
        }
    }

    override suspend fun googleExchange(code: String, redirectUri: String?): Resource<User> {
        return try {
            val response = apiService.googleExchange(
                GoogleExchangeRequest(
                    code = code,
                    redirectUri = redirectUri,
                    deviceName = currentDeviceName,
                    deviceType = currentDeviceType,
                    osName = currentOsName
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userDto = body.user
                )
                Resource.Success(body.user.toDomain())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Google authentication failed" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Network error during Google login"
            )
        }
    }

    override suspend fun registerOwnerWithGoogle(
        idToken: String,
        organizationName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radiusMeters: Int
    ): Resource<User> {
        return try {
            val response = apiService.registerOwnerWithGoogle(
                GoogleOwnerRegistrationRequest(
                    idToken = idToken,
                    organizationName = organizationName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    radiusMeters = radiusMeters,
                    deviceName = currentDeviceName,
                    deviceType = currentDeviceType,
                    osName = currentOsName
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userDto = body.user
                )
                Resource.Success(body.user.toDomain())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Owner registration failed" }
                Resource.Error(Exception(errorMsg), errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e, e.localizedMessage ?: "Network error during owner registration")
        }
    }

    override suspend fun refreshSession(): Resource<User> {
        return try {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSession()
                return Resource.Error(Exception("No refresh token stored"), "No refresh token available")
            }

            val response = apiService.refreshToken(
                RefreshTokenRequest(
                    refreshToken = refreshToken,
                    deviceName = currentDeviceName,
                    deviceType = currentDeviceType,
                    osName = currentOsName
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userDto = body.user
                )
                Resource.Success(body.user.toDomain())
            } else {
                sessionManager.clearSession()
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: "Session expired. Please log in again."
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Failed to refresh session"
            )
        }
    }

    override suspend fun fetchCurrentUser(): Resource<User> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body() != null) {
                val userDto = response.body()!!
                sessionManager.updateUser(userDto)
                Resource.Success(userDto.toDomain())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Failed to fetch user profile" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Error fetching current user profile"
            )
        }
    }

    override suspend fun updateProfile(
        fullName: String?,
        phone: String?,
        avatarBase64: String?
    ): Resource<User> {
        return try {
            val request = UpdateProfileRequest(
                fullName = fullName,
                phone = phone,
                avatar = avatarBase64
            )
            val response = apiService.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                val userDto = response.body()!!
                sessionManager.updateUser(userDto)
                Resource.Success(userDto.toDomain())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Failed to update profile" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Error updating profile"
            )
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return sessionManager.userFlow
    }

    override suspend fun getLoggedDevices(): Resource<List<LoggedDevice>> {
        return try {
            val response = apiService.getLoggedDevices()
            if (response.isSuccessful && response.body() != null) {
                val devices = response.body()!!.devices.map { it.toDomain() }
                Resource.Success(devices)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Failed to fetch logged devices" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Error fetching logged devices"
            )
        }
    }

    override suspend fun revokeDevice(deviceId: String): Resource<Unit> {
        return try {
            val response = apiService.revokeDevice(deviceId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: response.message().ifBlank { "Failed to revoke device session" }
                Resource.Error(
                    exception = Exception(errorMsg),
                    message = errorMsg
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                exception = e,
                message = e.localizedMessage ?: "Error revoking device session"
            )
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // Ignore network errors on logout to ensure local session is cleared
            }
            sessionManager.clearSession()
            Resource.Success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Resource.Error(e, "Error clearing local session")
        }
    }

    private fun sanitizeErrorMessage(e: Throwable, fallback: String): String {
        val message = e.localizedMessage ?: e.message ?: ""
        return when {
            e is ConnectException || message.contains("Failed to connect", ignoreCase = true) ->
                "Unable to connect to server. Please check your connection."
            e is UnknownHostException || message.contains("Unable to resolve host", ignoreCase = true) ->
                "Unable to reach server. Please check your network."
            e is SocketTimeoutException || message.contains("timeout", ignoreCase = true) ->
                "Server connection timed out. Retrying…"
            e is IOException || message.contains("IOException", ignoreCase = true) ->
                "Network connection error. Please try again."
            message.isNotBlank() && !message.contains("10.0.2.2") && !message.contains("http://") && !message.contains("https://") ->
                message
            else -> fallback
        }
    }

    private fun extractErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            json.optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
