package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.data.model.OtpResponse
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * UseCase for requesting a 6-digit Email OTP token.
 */
class RequestOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /** Requests OTP token dispatch for given email and role. */
    suspend operator fun invoke(email: String, role: String, isRegistration: Boolean = false): Resource<OtpResponse> {
        return authRepository.requestOtp(email, role, isRegistration)
    }
}
