package com.als.presence.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.als.presence.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GoogleAuthResult {
    data class Success(
        val idToken: String,
        val email: String? = null,
        val displayName: String? = null
    ) : GoogleAuthResult

    data object Cancelled : GoogleAuthResult

    data object FallbackToLegacy : GoogleAuthResult

    data class Error(val message: String) : GoogleAuthResult
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Obtains a Google ID Token using Android Credential Manager API.
     * Signals FallbackToLegacy if Credential Manager is unavailable or produces no credential.
     */
    suspend fun getGoogleIdToken(activityContext: Context): GoogleAuthResult {
        val webClientId = activityContext.getString(R.string.google_web_client_id)
        if (webClientId.isBlank()) {
            return GoogleAuthResult.Error("Google Web Client ID is not configured.")
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleAuthResult.Success(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName
                )
            } else {
                Log.w("GoogleAuthManager", "Unexpected credential type '${credential.type}', falling back to legacy Google Sign-In")
                GoogleAuthResult.FallbackToLegacy
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleAuthManager", "Google Sign-In was cancelled by user")
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuthManager", "CredentialManager failed (${e.javaClass.simpleName}: ${e.message}), falling back to legacy Google Sign-In")
            GoogleAuthResult.FallbackToLegacy
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("GoogleAuthManager", "Failed to parse Google ID Token", e)
            GoogleAuthResult.Error("Invalid Google ID Token format.")
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "Unexpected error in CredentialManager (${e.javaClass.simpleName}: ${e.message}), falling back to legacy Google Sign-In", e)
            GoogleAuthResult.FallbackToLegacy
        }
    }
}
