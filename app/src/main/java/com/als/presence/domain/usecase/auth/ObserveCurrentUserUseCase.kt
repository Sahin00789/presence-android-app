package com.als.presence.domain.usecase.auth

import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return repository.observeCurrentUser()
    }
}
