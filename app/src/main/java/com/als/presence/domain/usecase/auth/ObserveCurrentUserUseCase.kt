package com.als.presence.domain.usecase.auth

import com.als.presence.domain.model.User
import com.als.presence.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to observe reactive state flow of current logged-in user.
 */
class ObserveCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /** Returns cold Flow emitting [User] state updates or `null` when logged out. */
    operator fun invoke(): Flow<User?> = authRepository.observeCurrentUser()
}
