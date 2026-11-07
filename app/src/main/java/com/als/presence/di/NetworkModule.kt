package com.als.presence.di

import com.als.presence.data.local.SessionManager
import com.als.presence.data.remote.*
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
    private val DEFAULT_BASE_URL = PRESENCE_BASE_URL

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
        tokenAuthenticator: TokenAuthenticator,
        json: Json
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager, json, DEFAULT_BASE_URL))
            .authenticator(tokenAuthenticator)
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
    fun provideEmployeeApiService(retrofit: Retrofit): EmployeeApiService =
        retrofit.create(EmployeeApiService::class.java)

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
    fun provideHolidayApiService(retrofit: Retrofit): HolidayApiService = retrofit.create(HolidayApiService::class.java)

    @Provides
    @Singleton
    fun provideShiftsApiService(retrofit: Retrofit): ShiftsApiService = retrofit.create(ShiftsApiService::class.java)

    @Provides
    @Singleton
    fun provideOlaMapsApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): com.als.presence.data.remote.OlaMapsApiService {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.olamaps.io/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(com.als.presence.data.remote.OlaMapsApiService::class.java)
    }
}
