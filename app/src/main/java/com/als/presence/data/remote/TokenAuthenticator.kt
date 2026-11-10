package com.als.presence.data.remote

import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AuthResponse
import com.als.presence.data.model.RefreshTokenRequest
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    /**
     * Dedicated client for the refresh call. It intentionally has no
     * [AuthInterceptor]/[Authenticator] attached, so a failing refresh can never
     * recursively trigger another refresh.
     */
    private val refreshClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Do not attempt to refresh if the request was already a refresh call
        if (response.request.url.encodedPath.endsWith("/api/v1/auth/refresh")) {
            return null
        }

        // Never send our refresh token to a third-party host.
        if (!isPresenceRequest(response.request)) {
            return null
        }

        // Avoid infinite refresh loops if credentials are fundamentally invalid
        if (responseCount(response) >= 2) {
            sessionManager.clear()
            return null
        }

        val failedAccessToken = response.request
            .header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val currentAccessToken = sessionManager.token

        // Double-checked locking: while this call waited on the monitor another
        // request may already have rotated the session. Reuse the new access
        // token instead of rotating (and revoking) the refresh token again.
        if (!failedAccessToken.isNullOrBlank() &&
            !currentAccessToken.isNullOrBlank() &&
            currentAccessToken != failedAccessToken
        ) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentAccessToken")
                .build()
        }

        val currentRefreshToken = sessionManager.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            sessionManager.clear()
            return null
        }

        val refreshRequestBody = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(
                refreshToken = currentRefreshToken,
                loggedDeviceId = sessionManager.loggedDeviceId
            )
        ).toRequestBody("application/json".toMediaType())

        val refreshUrl = "${PRESENCE_BASE_URL.trimEnd('/')}/api/v1/auth/refresh"
        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post(refreshRequestBody)
            .build()

        return try {
            refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    // Only a definitive auth rejection should wipe the local
                    // session. A 5xx or a flaky network must not log the user out.
                    if (refreshResponse.code == 400 ||
                        refreshResponse.code == 401 ||
                        refreshResponse.code == 403
                    ) {
                        sessionManager.clear()
                    }
                    return null
                }

                val bodyString = refreshResponse.body?.string()
                if (bodyString.isNullOrBlank()) {
                    sessionManager.clear()
                    return null
                }

                val authResponse = json.decodeFromString(AuthResponse.serializer(), bodyString)
                persistSession(authResponse)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${authResponse.accessToken}")
                    .build()
            }
        } catch (_: Exception) {
            // Network failure: keep the refresh token so the next request retries.
            null
        }
    }

    private fun persistSession(authResponse: AuthResponse) {
        sessionManager.token = authResponse.accessToken
        sessionManager.refreshToken = authResponse.refreshToken
        if (!authResponse.loggedDeviceId.isNullOrBlank()) {
            sessionManager.loggedDeviceId = authResponse.loggedDeviceId
        }
        sessionManager.organizationId = authResponse.user.organizationId
        sessionManager.userId = authResponse.user.id
        sessionManager.role = authResponse.user.role
    }

    private fun isPresenceRequest(request: Request): Boolean {
        val baseHost = PRESENCE_BASE_URL.toHttpUrlOrNull()?.host ?: return true
        val requestHost = request.url.host
        return requestHost.equals(baseHost, ignoreCase = true) ||
            requestHost == "10.0.2.2" ||
            requestHost == "localhost"
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
