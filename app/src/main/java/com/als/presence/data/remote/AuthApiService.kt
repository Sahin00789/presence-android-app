package com.als.presence.data.remote

import com.als.presence.data.model.AuthTokenResponse
import com.als.presence.data.model.GoogleExchangeRequest
import com.als.presence.data.model.GoogleOwnerRegistrationRequest
import com.als.presence.data.model.LoggedDevicesResponseDto
import com.als.presence.data.model.LogoutResponse
import com.als.presence.data.model.OtpRequest
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.model.OtpVerifyRequest
import com.als.presence.data.model.RefreshTokenRequest
import com.als.presence.data.model.UpdateProfileRequest
import com.als.presence.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {

    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(
        @Body request: OtpRequest
    ): Response<OtpResponse>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(
        @Body request: OtpVerifyRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/google/exchange")
    suspend fun googleExchange(
        @Body request: GoogleExchangeRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/google/owner-registration")
    suspend fun registerOwnerWithGoogle(
        @Body request: GoogleOwnerRegistrationRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthTokenResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<UserDto>

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<UserDto>

    @POST("api/v1/auth/me/avatar")
    suspend fun uploadAvatar(
        @Body request: Map<String, String>
    ): Response<UserDto>

    @GET("api/v1/auth/devices")
    suspend fun getLoggedDevices(): Response<LoggedDevicesResponseDto>

    @DELETE("api/v1/auth/devices/{deviceId}")
    suspend fun revokeDevice(
        @Path("deviceId") deviceId: String
    ): Response<LogoutResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<LogoutResponse>
}
