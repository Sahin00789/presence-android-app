package com.als.presence.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Errors & Organization
// ---------------------------------------------------------------------------

@Serializable
data class ApiErrorDto(
    val statusCode: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val timestamp: String? = null
)

@Serializable
data class OrganizationDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("logoUrl") val logoUrl: String? = null,
    @SerialName("timezone") val timezone: String,
    @SerialName("currency") val currency: String,
    @SerialName("createdAt") val createdAt: String
)

@Serializable
data class UpdateOrganizationRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("logoUrl") val logoUrl: String? = null,
    @SerialName("logo") val logo: String? = null
)

@Serializable
data class OrganizationSummaryDto(
    @SerialName("organization") val organization: OrganizationDto,
    @SerialName("memberCount") val memberCount: Int,
    @SerialName("staffCount") val staffCount: Int,
    @SerialName("managerCount") val managerCount: Int
)
