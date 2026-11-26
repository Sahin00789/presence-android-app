package com.als.presence.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing an authenticated user in the system.
 *
 * Can represent either an [Role.OWNER] (organization administrator) or an [Role.EMPLOYEE] (workforce staff).
 *
 * @property id Unique user identifier.
 * @property email Authenticated user's email address.
 * @property fullName Display name of the user.
 * @property role Operational role ([Role.OWNER] or [Role.EMPLOYEE]).
 * @property isActive Account active flag.
 * @property avatarUrl Optional profile picture URL.
 * @property phone Optional phone number.
 * @property organizationName Organization name, if associated.
 * @property organizationLogoUrl Organization branding logo URL.
 * @property organizationId Organization ID for multi-tenant data scoping.
 * @property premisesId Assigned premises ID for employees.
 * @property premisesName Assigned premises name.
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: Role,
    val isActive: Boolean = true,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val organizationName: String? = null,
    val organizationLogoUrl: String? = null,
    val organizationId: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null
)
