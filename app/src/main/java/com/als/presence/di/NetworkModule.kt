package com.als.presence.di

import com.als.presence.data.local.SessionManager
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.data.remote.AuthApiService
import com.als.presence.data.remote.AuthInterceptor
import com.als.presence.data.remote.FaceApiService
import com.als.presence.data.remote.InvitationApiService
import com.als.presence.data.remote.LeaveApiService
import com.als.presence.data.remote.LocationApiService
import com.als.presence.data.remote.OvertimeApiService
import com.als.presence.data.remote.PRESENCE_BASE_URL
import com.als.presence.data.remote.OrganizationApiService
import com.als.presence.data.remote.PayrollApiService
import com.als.presence.data.remote.PostalApiService
import com.als.presence.data.remote.PremisesApiService
import com.als.presence.data.remote.ShiftApiService
import com.als.presence.data.remote.StaffApiService
import com.als.presence.data.remote.WalletApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * `10.0.2.2` is the host loopback as seen from the Android emulator. Override
     * it for a physical device by pointing it at the machine's LAN address.
     */
    private const val DEFAULT_BASE_URL = PRESENCE_BASE_URL

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        json: Json
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager, json, DEFAULT_BASE_URL))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideAttendanceApiService(retrofit: Retrofit): AttendanceApiService =
        retrofit.create(AttendanceApiService::class.java)

    @Provides
    @Singleton
    fun provideOvertimeApiService(retrofit: Retrofit): OvertimeApiService =
        retrofit.create(OvertimeApiService::class.java)

    @Provides
    @Singleton
    fun provideStaffApiService(retrofit: Retrofit): StaffApiService =
        retrofit.create(StaffApiService::class.java)

    @Provides
    @Singleton
    fun provideInvitationApiService(retrofit: Retrofit): InvitationApiService =
        retrofit.create(InvitationApiService::class.java)

    @Provides
    @Singleton
    fun providePremisesApiService(retrofit: Retrofit): PremisesApiService =
        retrofit.create(PremisesApiService::class.java)

    @Provides
    @Singleton
    fun provideOrganizationApiService(retrofit: Retrofit): OrganizationApiService =
        retrofit.create(OrganizationApiService::class.java)

    @Provides
    @Singleton
    fun provideShiftApiService(retrofit: Retrofit): ShiftApiService =
        retrofit.create(ShiftApiService::class.java)

    @Provides
    @Singleton
    fun provideWalletApiService(retrofit: Retrofit): WalletApiService =
        retrofit.create(WalletApiService::class.java)

    @Provides
    @Singleton
    fun providePayrollApiService(retrofit: Retrofit): PayrollApiService =
        retrofit.create(PayrollApiService::class.java)

    @Provides
    @Singleton
    fun provideFaceApiService(retrofit: Retrofit): FaceApiService =
        retrofit.create(FaceApiService::class.java)

    @Provides
    @Singleton
    fun provideLeaveApiService(retrofit: Retrofit): LeaveApiService =
        retrofit.create(LeaveApiService::class.java)

    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService =
        retrofit.create(LocationApiService::class.java)

    @Provides
    @Singleton
    fun providePostalApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): PostalApiService {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.postalpincode.in/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(PostalApiService::class.java)
    }
}
