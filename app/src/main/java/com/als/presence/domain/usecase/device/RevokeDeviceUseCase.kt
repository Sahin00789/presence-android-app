package com.als.presence.domain.usecase.device

import com.als.presence.core.common.Resource
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase to remotely revoke and invalidate an active device session.
 */
class RevokeDeviceUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /** Revokes device session specified by [deviceId]. */
    suspend operator fun invoke(deviceId: String): Resource<Unit> {
        return authRepository.revokeDevice(deviceId)
    }
}
