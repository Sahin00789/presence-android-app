package com.als.presence.data.remote

import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AuthResponse
import com.als.presence.data.model.RefreshTokenRequest
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val json: Json
) : Authenticator {

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Do not attempt to refresh if the request was already a refresh call
        if (response.request.url.encodedPath.endsWith("/api/v1/auth/refresh")) {
            return null
        }

        // Avoid infinite refresh loops if credentials are fundamentally invalid
        if (responseCount(response) >= 2) {
            sessionManager.clear()
            return null
        }

        val currentRefreshToken = sessionManager.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            sessionManager.clear()
            return null
        }

        val client = OkHttpClient.Builder().build()
        val refreshRequestBody = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(refreshToken = currentRefreshToken)
        ).toRequestBody("application/json".toMediaType())

        val refreshUrl = "${PRESENCE_BASE_URL.trimEnd('/')}/api/v1/auth/refresh"
        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post(refreshRequestBody)
            .build()

        return try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string()
                if (!bodyString.isNullOrBlank()) {
                    val authResponse = json.decodeFromString(AuthResponse.serializer(), bodyString)
                    sessionManager.token = authResponse.accessToken
                    sessionManager.refreshToken = authResponse.refreshToken
                    sessionManager.organizationId = authResponse.user.organizationId
                    sessionManager.userId = authResponse.user.id
                    sessionManager.role = authResponse.user.role

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${authResponse.accessToken}")
                        .build()
                } else {
                    sessionManager.clear()
                    null
                }
            } else {
                sessionManager.clear()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
