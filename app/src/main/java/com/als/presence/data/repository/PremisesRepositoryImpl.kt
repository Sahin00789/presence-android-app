package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.PremisesDao
import com.als.presence.data.local.PremisesEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.PremisesApiService
import com.als.presence.data.sync.SnapshotProjectionEngine
import com.als.presence.domain.repository.PremisesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremisesRepositoryImpl @Inject constructor(
    private val premisesApiService: PremisesApiService,
    private val premisesDao: PremisesDao,
    private val snapshotProjectionEngine: SnapshotProjectionEngine
) : PremisesRepository {

    fun observePremises(): Flow<List<PremisesDto>> {
        return premisesDao.observeAll().map { list -> list.map { it.toDto() } }
    }

    override suspend fun getPremises(): Resource<List<PremisesDto>> {
        val result = safeApiCall("Error loading premises") {
            premisesApiService.list()
        }
        if (result is Resource.Success && result.data != null) {
            val entities = result.data.map { PremisesEntity.fromDto(it) }
            premisesDao.upsertAll(entities)
        }
        return result
    }

    override suspend fun createPremises(request: CreatePremisesRequest): Resource<PremisesDto> {
        val result = safeApiCall("Error creating premises") {
            premisesApiService.create(request)
        }
        if (result is Resource.Success && result.data != null) {
            premisesDao.upsert(PremisesEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun updatePremises(premisesId: String, request: UpdatePremisesRequest): Resource<PremisesDto> {
        val result = safeApiCall("Error updating premises") {
            premisesApiService.update(premisesId, request)
        }
        if (result is Resource.Success && result.data != null) {
            premisesDao.upsert(PremisesEntity.fromDto(result.data))
        }
        return result
    }
}
