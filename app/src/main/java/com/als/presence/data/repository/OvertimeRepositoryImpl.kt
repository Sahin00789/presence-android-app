package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.OvertimeDao
import com.als.presence.data.local.OvertimeEntity
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.remote.OvertimeApiService
import com.als.presence.domain.repository.OvertimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OvertimeRepositoryImpl @Inject constructor(
    private val overtimeApiService: OvertimeApiService,
    private val overtimeDao: OvertimeDao
) : OvertimeRepository {

    fun observeOvertime(): Flow<List<OvertimeDto>> {
        return overtimeDao.observeAll().map { list -> list.map { it.toDto() } }
    }

    override suspend fun getOvertime(from: String?, to: String?, staffProfileId: String?, status: String?): Resource<List<OvertimeDto>> {
        val result = safeApiCall("Error loading overtime") {
            overtimeApiService.list(from, to, staffProfileId, status)
        }
        if (result is Resource.Success && result.data != null) {
            val entities = result.data.map { OvertimeEntity.fromDto(it) }
            overtimeDao.clear()
            overtimeDao.upsertAll(entities)
            return Resource.Success(result.data)
        }
        val cached = overtimeDao.getAll()
        val filtered = cached.filter { entry ->
            (staffProfileId.isNullOrBlank() || entry.staffProfileId == staffProfileId) &&
            (status.isNullOrBlank() || entry.status.equals(status, ignoreCase = true)) &&
            (from.isNullOrBlank() || entry.workDate >= from) &&
            (to.isNullOrBlank() || entry.workDate <= to)
        }
        return if (filtered.isNotEmpty()) {
            Resource.Success(filtered.map { it.toDto() })
        } else if (result is Resource.Error) {
            result
        } else {
            Resource.Success(emptyList())
        }
    }

    override suspend fun createOvertime(request: CreateOvertimeRequest): Resource<OvertimeDto> {
        val result = safeApiCall("Error creating overtime") {
            overtimeApiService.create(request)
        }
        if (result is Resource.Success && result.data != null) {
            overtimeDao.upsert(OvertimeEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun cancelOvertime(overtimeId: String): Resource<Unit> {
        val result = safeApiCall("Error cancelling overtime") {
            overtimeApiService.cancel(overtimeId)
        }
        if (result is Resource.Success) {
            overtimeDao.markDeleted(overtimeId)
        }
        return when (result) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(result.exception, result.message)
            Resource.Loading -> Resource.Loading
        }
    }
}
