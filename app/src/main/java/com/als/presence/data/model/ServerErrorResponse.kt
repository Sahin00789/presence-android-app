package com.als.presence.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Standard error response model returned by the backend Fastify server.
 */
@Serializable
data class ServerErrorResponse(
    val status: String? = null,
    val statusCode: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val details: JsonElement? = null,
    val path: String? = null,
    val method: String? = null,
    val timestamp: String? = null
)
