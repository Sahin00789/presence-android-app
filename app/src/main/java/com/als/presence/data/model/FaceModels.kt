package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FaceTemplateDto(
    val id: String,
    val employeeId: String,
    val poseName: String,
    val createdAt: String
)

@Serializable
data class EnrollFaceRequest(
    val employeeId: String,
    val poseName: String,
    val embedding: List<Float>
)
