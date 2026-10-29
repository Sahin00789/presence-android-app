package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface PremisesRepository {
    suspend fun getPremises(): Resource<List<PremisesDto>>
    suspend fun createPremises(request: CreatePremisesRequest): Resource<PremisesDto>
    suspend fun updatePremises(premisesId: String, request: UpdatePremisesRequest): Resource<PremisesDto>
}
