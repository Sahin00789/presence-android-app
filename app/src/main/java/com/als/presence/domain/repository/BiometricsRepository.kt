package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.FaceStatusDto

interface BiometricsRepository {
    suspend fun getStaffFaceStatus(employeeId: String? = null): Resource<List<FaceStatusDto>>
    suspend fun enrollFaceSupervised(employeeId: String, poseName: String = "CENTER", embedding: List<Float> = emptyList()): Resource<FaceStatusDto>
    suspend fun revokeFaceEnrollment(employeeId: String): Resource<Unit>
}
