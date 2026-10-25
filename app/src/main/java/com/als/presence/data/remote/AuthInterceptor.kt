package com.als.presence.data.remote

import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AuthTokenResponse
import com.als.presence.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Attaches the access token to every request and transparently refreshes it
 * once on `401 Unauthorized`.
 *
 * If a 401 occurs and token refresh fails (or a retried request still returns 401),
 * `SessionManager.clearSession()` is called immediately to clear stored credentials
 * and redirect the user back to the Login screen.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val json: Json,
    private val baseUrl: String
) : Interceptor {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = runBlocking { sessionManager.getAccessToken() }

        val authorizedRequest = originalRequest.newBuilder().apply {
            if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
                header("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(authorizedRequest)

        if (response.code != 401) {
            return response
        }

        // Never try to refresh auth endpoints themselves.
        if (originalRequest.url.encodedPath.contains("/auth/")) {
            return response
        }

        val refreshedToken = synchronized(refreshLock) {
            val current = runBlocking { sessionManager.getAccessToken() }
            if (!current.isNullOrBlank() && current != token) {
                current
            } else {
                runBlocking { performRefresh() }
            }
        }

        if (refreshedToken.isNullOrBlank()) {
            // Unrecoverable 401: clear session to trigger automatic logout in UI
            runBlocking { sessionManager.clearSession() }
            return response
        }

        response.close()

        val retryRequest = authorizedRequest.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
            .build()

        val retryResponse = chain.proceed(retryRequest)
        if (retryResponse.code == 401) {
            // Retried request still 401: session is invalid
            runBlocking { sessionManager.clearSession() }
        }

        return retryResponse
    }

    private fun performRefresh(): String? {
        val refreshToken = runBlocking { sessionManager.getRefreshToken() } ?: run {
            runBlocking { sessionManager.clearSession() }
            return null
        }

        val payload = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(refreshToken = refreshToken)
        )

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/v1/auth/refresh")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    runBlocking { sessionManager.clearSession() }
                    return null
                }
                val body = response.body?.string() ?: return null
                val tokens = json.decodeFromString(AuthTokenResponse.serializer(), body)
                runBlocking {
                    sessionManager.updateTokens(tokens.accessToken, tokens.refreshToken)
                }
                tokens.accessToken
            }
        } catch (_: Exception) {
            null
        }
    }
}
