package com.als.presence.domain.usecase.owner

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterOwnerWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        idToken: String,
        organizationName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radiusMeters: Int
    ): Resource<User> {
        return repository.registerOwnerWithGoogle(
            idToken = idToken,
            organizationName = organizationName,
            timezone = timezone,
            currency = currency,
            premisesName = premisesName,
            radiusMeters = radiusMeters
        )
    }
}
