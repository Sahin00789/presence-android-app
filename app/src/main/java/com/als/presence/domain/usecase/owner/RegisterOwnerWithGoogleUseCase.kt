package com.als.presence.domain.usecase.owner

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterOwnerWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        idToken: String,
        orgName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radius: Int,
        premisesAddress: String? = null,
        latitude: String? = null,
        longitude: String? = null
    ): Resource<User> {
        return authRepository.registerOwner(
            idToken = idToken,
            orgName = orgName,
            timezone = timezone,
            currency = currency,
            premisesName = premisesName,
            radius = radius,
            premisesAddress = premisesAddress,
            latitude = latitude,
            longitude = longitude
        )
    }
}
