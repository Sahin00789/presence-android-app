package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.data.model.OtpResponse
import com.als.presence.domain.model.Role
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, role: Role): Resource<OtpResponse> {
        return repository.requestOtp(email, role)
    }
}
