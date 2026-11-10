package com.als.presence.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "presence_secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.values().first(),
                EncryptedSharedPreferences.PrefValueEncryptionScheme.values().first()
            )
        } catch (_: Exception) {
            context.getSharedPreferences("presence_session_fallback", Context.MODE_PRIVATE)
        }
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
        }

    var organizationId: String?
        get() = prefs.getString(KEY_ORG_ID, null)
        set(value) {
            prefs.edit().putString(KEY_ORG_ID, value).apply()
        }

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) {
            prefs.edit().putString(KEY_ROLE, value).apply()
        }

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) {
            prefs.edit().putString(KEY_USER_ID, value).apply()
        }

    var savedLoginRole: String?
        get() = prefs.getString(KEY_SAVED_LOGIN_ROLE, "EMPLOYEE")
        set(value) {
            prefs.edit().putString(KEY_SAVED_LOGIN_ROLE, value).apply()
        }

    var loggedDeviceId: String?
        get() = prefs.getString(KEY_LOGGED_DEVICE_ID, null)
        set(value) {
            prefs.edit().putString(KEY_LOGGED_DEVICE_ID, value).apply()
        }

    /**
     * Emits whenever the stored session is wiped, regardless of whether it was
     * an explicit logout or a rejected/expired refresh token. Observers use this
     * to drive the UI back to the login screen.
     */
    private val _sessionRevoked = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionRevoked: SharedFlow<Unit> = _sessionRevoked.asSharedFlow()

    fun clear() {
        val lastSavedRole = savedLoginRole
        prefs.edit().clear().apply()
        if (lastSavedRole != null) {
            savedLoginRole = lastSavedRole
        }
        // tryEmit never suspends, so this is safe from the OkHttp authenticator
        // thread. The buffer keeps the signal even if the collector is busy.
        _sessionRevoked.tryEmit(Unit)
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ORG_ID = "organization_id"
        private const val KEY_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SAVED_LOGIN_ROLE = "saved_login_role"
        private const val KEY_LOGGED_DEVICE_ID = "logged_device_id"
    }
}
