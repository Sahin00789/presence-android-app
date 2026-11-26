package com.als.presence.domain.usecase.auth

import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase for terminating current user session, clearing tokens, and revoking active device log.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /** Executes session logout and clears local session cache. */
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
