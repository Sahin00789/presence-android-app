package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.PremisesApiService
import com.als.presence.domain.repository.PremisesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremisesRepositoryImpl @Inject constructor(
    private val premisesApiService: PremisesApiService
) : PremisesRepository {

    override suspend fun getPremises(): Resource<List<PremisesDto>> {
        return safeApiCall("Error loading premises") {
            premisesApiService.list()
        }
    }

    override suspend fun createPremises(request: CreatePremisesRequest): Resource<PremisesDto> {
        return safeApiCall("Error creating premises") {
            premisesApiService.create(request)
        }
    }

    override suspend fun updatePremises(premisesId: String, request: UpdatePremisesRequest): Resource<PremisesDto> {
        return safeApiCall("Error updating premises") {
            premisesApiService.update(premisesId, request)
        }
    }
}
