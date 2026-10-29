package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.OrganizationDto

interface OrganizationRepository {
    suspend fun updateOrganization(name: String? = null, timezone: String? = null, currency: String? = null, logoBase64: String? = null): Resource<OrganizationDto>
}
