package com.als.presence.domain.usecase.auth

import com.als.presence.core.common.Resource
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.logout()
    }
}
