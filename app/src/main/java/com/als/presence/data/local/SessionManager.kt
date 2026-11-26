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

/**
 * Manages encrypted security credentials, session tokens, user metadata, and session revocation events.
 *
 * Uses AES256_GCM [EncryptedSharedPreferences] for encrypted key-value persistence.
 */
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

    /** Active JWT access token. */
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    /** Rotating refresh token. */
    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
        }

    /** Current organization ID claim. */
    var organizationId: String?
        get() = prefs.getString(KEY_ORG_ID, null)
        set(value) {
            prefs.edit().putString(KEY_ORG_ID, value).apply()
        }

    /** User role (OWNER or EMPLOYEE). */
    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) {
            prefs.edit().putString(KEY_ROLE, value).apply()
        }

    /** Authenticated User ID. */
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) {
            prefs.edit().putString(KEY_USER_ID, value).apply()
        }

    /** Saved login role preference for tab selection on sign-in. */
    var savedLoginRole: String?
        get() = prefs.getString(KEY_SAVED_LOGIN_ROLE, "EMPLOYEE")
        set(value) {
            prefs.edit().putString(KEY_SAVED_LOGIN_ROLE, value).apply()
        }

    /** Unique device session log ID. */
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

    /** Clears session tokens and triggers session revocation signal. */
    fun clear() {
        val lastSavedRole = savedLoginRole
        prefs.edit().clear().apply()
        if (lastSavedRole != null) {
            savedLoginRole = lastSavedRole
        }
        _sessionRevoked.tryEmit(Unit)
    }

    /**
     * Persists ongoing Owner Registration Wizard progress to survive configuration changes
     * and app backgrounding.
     */
    fun saveOwnerDraft(draft: OwnerRegistrationDraft) {
        prefs.edit()
            .putInt(KEY_DRAFT_STEP, draft.step)
            .putString(KEY_DRAFT_EMAIL, draft.email)
            .putString(KEY_DRAFT_OWNER_NAME, draft.ownerName)
            .putString(KEY_DRAFT_PHONE, draft.phone)
            .putString(KEY_DRAFT_ORG_NAME, draft.organizationName)
            .putString(KEY_DRAFT_ORG_TYPE, draft.organizationType)
            .putString(KEY_DRAFT_PREMISES_NAME, draft.premisesName)
            .putString(KEY_DRAFT_PREMISES_ADDR, draft.premisesAddress)
            .putString(KEY_DRAFT_LAT, draft.latitude)
            .putString(KEY_DRAFT_LNG, draft.longitude)
            .putInt(KEY_DRAFT_RADIUS, draft.radiusMeters)
            .putString(KEY_DRAFT_METHOD, draft.verificationMethod)
            .putString(KEY_DRAFT_GOOGLE_TOKEN, draft.googleIdToken)
            .putString(KEY_DRAFT_VERIFIED_OTP, draft.verifiedOtp)
            .apply()
    }

    /**
     * Retrieves saved Owner Registration Wizard progress, or null if no draft exists.
     */
    fun getOwnerDraft(): OwnerRegistrationDraft? {
        val email = prefs.getString(KEY_DRAFT_EMAIL, null)
        val orgName = prefs.getString(KEY_DRAFT_ORG_NAME, null)
        val ownerName = prefs.getString(KEY_DRAFT_OWNER_NAME, null)
        if (email.isNullOrBlank() && orgName.isNullOrBlank() && ownerName.isNullOrBlank()) {
            return null
        }
        return OwnerRegistrationDraft(
            step = prefs.getInt(KEY_DRAFT_STEP, 1),
            email = email.orEmpty(),
            ownerName = ownerName.orEmpty(),
            phone = prefs.getString(KEY_DRAFT_PHONE, "").orEmpty(),
            organizationName = orgName.orEmpty(),
            organizationType = prefs.getString(KEY_DRAFT_ORG_TYPE, "Technology & IT").orEmpty(),
            premisesName = prefs.getString(KEY_DRAFT_PREMISES_NAME, "Main Headquarters").orEmpty(),
            premisesAddress = prefs.getString(KEY_DRAFT_PREMISES_ADDR, "").orEmpty(),
            latitude = prefs.getString(KEY_DRAFT_LAT, null),
            longitude = prefs.getString(KEY_DRAFT_LNG, null),
            radiusMeters = prefs.getInt(KEY_DRAFT_RADIUS, 50),
            verificationMethod = prefs.getString(KEY_DRAFT_METHOD, "email").orEmpty(),
            googleIdToken = prefs.getString(KEY_DRAFT_GOOGLE_TOKEN, null),
            verifiedOtp = prefs.getString(KEY_DRAFT_VERIFIED_OTP, null)
        )
    }

    /** Clears saved Owner Registration Wizard draft on successful registration or explicit discard. */
    fun clearOwnerDraft() {
        prefs.edit()
            .remove(KEY_DRAFT_STEP)
            .remove(KEY_DRAFT_EMAIL)
            .remove(KEY_DRAFT_OWNER_NAME)
            .remove(KEY_DRAFT_PHONE)
            .remove(KEY_DRAFT_ORG_NAME)
            .remove(KEY_DRAFT_ORG_TYPE)
            .remove(KEY_DRAFT_PREMISES_NAME)
            .remove(KEY_DRAFT_PREMISES_ADDR)
            .remove(KEY_DRAFT_LAT)
            .remove(KEY_DRAFT_LNG)
            .remove(KEY_DRAFT_RADIUS)
            .remove(KEY_DRAFT_METHOD)
            .remove(KEY_DRAFT_GOOGLE_TOKEN)
            .remove(KEY_DRAFT_VERIFIED_OTP)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ORG_ID = "organization_id"
        private const val KEY_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SAVED_LOGIN_ROLE = "saved_login_role"
        private const val KEY_LOGGED_DEVICE_ID = "logged_device_id"

        private const val KEY_DRAFT_STEP = "owner_draft_step"
        private const val KEY_DRAFT_EMAIL = "owner_draft_email"
        private const val KEY_DRAFT_OWNER_NAME = "owner_draft_owner_name"
        private const val KEY_DRAFT_PHONE = "owner_draft_phone"
        private const val KEY_DRAFT_ORG_NAME = "owner_draft_org_name"
        private const val KEY_DRAFT_ORG_TYPE = "owner_draft_org_type"
        private const val KEY_DRAFT_PREMISES_NAME = "owner_draft_premises_name"
        private const val KEY_DRAFT_PREMISES_ADDR = "owner_draft_premises_addr"
        private const val KEY_DRAFT_LAT = "owner_draft_lat"
        private const val KEY_DRAFT_LNG = "owner_draft_lng"
        private const val KEY_DRAFT_RADIUS = "owner_draft_radius"
        private const val KEY_DRAFT_METHOD = "owner_draft_method"
        private const val KEY_DRAFT_GOOGLE_TOKEN = "owner_draft_google_token"
        private const val KEY_DRAFT_VERIFIED_OTP = "owner_draft_verified_otp"
    }
}

/** Data model representing a saved Owner Registration draft. */
data class OwnerRegistrationDraft(
    val step: Int = 1,
    val email: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val organizationName: String = "",
    val organizationType: String = "Technology & IT",
    val premisesName: String = "Main Headquarters",
    val premisesAddress: String = "",
    val latitude: String? = null,
    val longitude: String? = null,
    val radiusMeters: Int = 50,
    val verificationMethod: String = "email",
    val googleIdToken: String? = null,
    val verifiedOtp: String? = null
)
