package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FaceTemplateDto(
    val id: String = "",
    val employeeId: String = "",
    val poseName: String = "",
    val createdAt: String = ""
)

@Serializable
data class EnrollFaceRequest(
    val employeeId: String,
    val poseName: String,
    val embedding: List<Float>
)

@Serializable
data class LivenessEvidenceDto(
    val activeBlinkDetected: Boolean = true,
    val blinkCount: Int = 1,
    val passiveTextureScore: Double = 0.95,
    val passivePassed: Boolean = true,
    val capturedAt: String? = null
)

@Serializable
data class EnrollMultiPoseRequest(
    val employeeProfileId: String,
    val centerVector: List<Double>,
    val leftVector: List<Double>? = null,
    val rightVector: List<Double>? = null,
    val upVector: List<Double>? = null,
    val downVector: List<Double>? = null,
    val modelVersion: String = "sface-yunet-v1",
    val qualityScore: Double = 0.95,
    val premisesId: String? = null,
    val liveness: LivenessEvidenceDto = LivenessEvidenceDto()
)

@Serializable
data class EnrollMultiPoseResponseDto(
    val success: Boolean = false,
    val message: String? = null,
    val employeeProfileId: String? = null,
    val modelVersion: String? = null,
    val poseCount: Int = 5,
    val qualityScore: Double = 0.95,
    val livenessScore: Double = 0.95,
    val enrolledAt: String? = null
)

@Serializable
data class BiometricsStatusDto(
    val enrolled: Boolean = false,
    val employeeProfileId: String? = null,
    val enrolledAt: String? = null,
    val modelVersion: String? = null
)

@Serializable
data class ClockInFaceRequest(
    val liveVector: List<Double>,
    val liveness: LivenessEvidenceDto = LivenessEvidenceDto(),
    val premisesId: String? = null
)

@Serializable
data class FaceVerifyResponseDto(
    val success: Boolean = false,
    val matched: Boolean = false,
    val employeeProfileId: String? = null,
    val similarity: Double = 0.0,
    val threshold: Double = 0.363,
    val matchedPose: String? = null,
    val message: String? = null
)

