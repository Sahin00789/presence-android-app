package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
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
    ): Resource<User> {
        return authRepository.verifyOtp(
            email = email,
            otp = otp,
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
    }
}
