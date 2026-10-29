package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleExchangeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String = "", redirectUri: String = "", idToken: String = ""): Resource<User> {
        val token = idToken.ifBlank { code }
        return authRepository.googleExchange(token)
    }
}
