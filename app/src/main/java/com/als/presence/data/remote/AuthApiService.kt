package com.als.presence.data.remote

import com.als.presence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest): Response<OtpResponse>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<AuthResponse>

    @POST("api/v1/auth/google/exchange")
    suspend fun googleExchange(@Body request: GoogleExchangeRequest): Response<AuthResponse>

    @POST("api/v1/auth/google/owner-registration")
    suspend fun registerOwnerWithGoogle(@Body request: GoogleOwnerRegistrationRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<AuthUserDto>
}
