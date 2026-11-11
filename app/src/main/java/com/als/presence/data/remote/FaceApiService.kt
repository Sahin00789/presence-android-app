package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface FaceApiService {
    @GET("api/v1/biometrics/status")
    suspend fun getStatus(
        @Query("employeeProfileId") employeeProfileId: String? = null
    ): Response<BiometricsStatusDto>

    @POST("api/v1/biometrics/enroll")
    suspend fun enrollMultiPose(
        @Body request: EnrollMultiPoseRequest
    ): Response<EnrollMultiPoseResponseDto>

    @POST("api/v1/biometrics/verify")
    suspend fun verifyFace(
        @Body request: ClockInFaceRequest
    ): Response<FaceVerifyResponseDto>

    @GET("api/v1/face/templates")
    suspend fun list(): Response<List<FaceTemplateDto>>
}
