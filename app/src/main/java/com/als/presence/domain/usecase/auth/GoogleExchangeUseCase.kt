package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleExchangeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Either credential works; the server prefers [idToken] when both are present.
     * Keep them as distinct fields: an ID token sent as `code` is rejected by Google.
     */
    suspend operator fun invoke(
        idToken: String = "",
        serverAuthCode: String = ""
    ): Resource<User> = authRepository.googleExchange(
        idToken = idToken.ifBlank { null },
        serverAuthCode = serverAuthCode.ifBlank { null }
    )
}
