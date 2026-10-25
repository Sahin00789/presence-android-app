package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleExchangeUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(code: String, redirectUri: String? = null): Resource<User> {
        return repository.googleExchange(code, redirectUri)
    }
}
