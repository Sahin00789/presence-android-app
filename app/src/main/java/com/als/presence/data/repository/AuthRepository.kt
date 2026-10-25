package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.OtpResponse
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun requestOtp(email: String, role: Role): Resource<OtpResponse>

    suspend fun verifyOtp(
        email: String,
        code: String,
        role: Role,
        fullName: String? = null,
        organizationName: String? = null,
        timezone: String? = null,
        currency: String? = null,
        premisesName: String? = null,
        radiusMeters: Int? = null
    ): Resource<User>

    suspend fun googleExchange(code: String, redirectUri: String? = null): Resource<User>
    suspend fun refreshSession(): Resource<User>
    suspend fun fetchCurrentUser(): Resource<User>
    fun observeCurrentUser(): Flow<User?>
    suspend fun logout(): Resource<Unit>
}
