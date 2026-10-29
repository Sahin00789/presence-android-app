package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.remote.OvertimeApiService
import com.als.presence.domain.repository.OvertimeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OvertimeRepositoryImpl @Inject constructor(
    private val overtimeApiService: OvertimeApiService
) : OvertimeRepository {

    override suspend fun getOvertime(from: String?, to: String?, staffProfileId: String?, status: String?): Resource<List<OvertimeDto>> {
        return safeApiCall("Error loading overtime") {
            overtimeApiService.list(from, to, staffProfileId, status)
        }
    }

    override suspend fun createOvertime(request: CreateOvertimeRequest): Resource<OvertimeDto> {
        return safeApiCall("Error creating overtime") {
            overtimeApiService.create(request)
        }
    }

    override suspend fun cancelOvertime(overtimeId: String): Resource<Unit> {
        return try {
            overtimeApiService.cancel(overtimeId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e, e.localizedMessage ?: "Error cancelling overtime")
        }
    }
}
