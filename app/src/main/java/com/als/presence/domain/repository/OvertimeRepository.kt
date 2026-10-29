package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.OvertimeDto

interface OvertimeRepository {
    suspend fun getOvertime(from: String? = null, to: String? = null, staffProfileId: String? = null, status: String? = null): Resource<List<OvertimeDto>>
    suspend fun createOvertime(request: CreateOvertimeRequest): Resource<OvertimeDto>
    suspend fun cancelOvertime(overtimeId: String): Resource<Unit>
}
