package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        code: String,
        role: Role,
        fullName: String? = null,
        organizationName: String? = null,
        timezone: String? = null,
        currency: String? = null,
        premisesName: String? = null,
        radiusMeters: Int? = null
    ): Resource<User> {
        return repository.verifyOtp(
            email = email,
            code = code,
            role = role,
            fullName = fullName,
            organizationName = organizationName,
            timezone = timezone,
            currency = currency,
            premisesName = premisesName,
            radiusMeters = radiusMeters
        )
    }
}
