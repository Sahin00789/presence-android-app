package com.als.presence.domain.usecase.device

import com.als.presence.core.common.Resource
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class RevokeDeviceUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(deviceId: String): Resource<Unit> {
        return authRepository.revokeDevice(deviceId)
    }
}
