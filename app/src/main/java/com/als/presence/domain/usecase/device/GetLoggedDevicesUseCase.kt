package com.als.presence.domain.usecase.device

import com.als.presence.core.common.Resource
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.repository.AuthRepository
import javax.inject.Inject

class GetLoggedDevicesUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<List<LoggedDevice>> {
        return authRepository.getLoggedDevices()
    }
}
