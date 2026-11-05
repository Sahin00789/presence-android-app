package com.als.presence.data.remote

import com.als.presence.data.local.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val json: Json,
    private val baseUrl: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.token

        // Only attach internal JWT authorization header for requests going to our backend host
        val requestHost = original.url.host
        val isBackendHost = try {
            val baseHost = baseUrl.toHttpUrlOrNull()?.host
            baseHost == null || requestHost.equals(baseHost, ignoreCase = true) || requestHost == "10.0.2.2" || requestHost == "localhost"
        } catch (_: Exception) {
            true
        }

        val requestBuilder = original.newBuilder()
        if (isBackendHost && !token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
