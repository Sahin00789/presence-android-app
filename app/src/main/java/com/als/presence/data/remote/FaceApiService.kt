package com.als.presence.data.remote

import com.als.presence.data.model.EnrollMultiPoseRequest
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.FaceVerificationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Face templates are created only through the supervised endpoint, which is
 * restricted to Owners and Managers.
 */
interface FaceApiService {
    @POST("api/v1/biometrics/enroll")
    suspend fun enrollSupervised(@Body request: EnrollMultiPoseRequest): Response<FaceStatusDto>

    @GET("api/v1/face/status")
    suspend fun status(): Response<FaceStatusDto>

    @GET("api/v1/face/staff/{staffProfileId}/status")
    suspend fun staffStatus(@Path("staffProfileId") staffProfileId: String): Response<FaceStatusDto>

    @DELETE("api/v1/face/enrollment/{staffProfileId}")
    suspend fun revoke(@Path("staffProfileId") staffProfileId: String): Response<Unit>
}
