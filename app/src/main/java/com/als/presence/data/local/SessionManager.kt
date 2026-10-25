package com.als.presence.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.als.presence.data.model.UserDto
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "presence_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userRoleKey = stringPreferencesKey("user_role")
    private val orgIdKey = stringPreferencesKey("org_id")
    private val orgNameKey = stringPreferencesKey("org_name")
    private val userAvatarUrlKey = stringPreferencesKey("user_avatar_url")
    private val orgLogoUrlKey = stringPreferencesKey("org_logo_url")

    val userFlow: Flow<User?> = context.dataStore.data.map { preferences ->
        val id = preferences[userIdKey] ?: return@map null
        val email = preferences[userEmailKey] ?: ""
        val name = preferences[userNameKey] ?: ""
        val roleStr = preferences[userRoleKey] ?: Role.STAFF.name
        val orgId = preferences[orgIdKey] ?: ""
        val orgName = preferences[orgNameKey] ?: ""
        val avatarUrl = preferences[userAvatarUrlKey]
        val orgLogoUrl = preferences[orgLogoUrlKey]

        User(
            id = id,
            email = email,
            fullName = name,
            role = Role.fromString(roleStr),
            organizationId = orgId,
            organizationName = orgName,
            avatarUrl = avatarUrl,
            organizationLogoUrl = orgLogoUrl
        )
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }

    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[refreshTokenKey]
    }

    suspend fun getAccessToken(): String? = accessTokenFlow.firstOrNull()

    suspend fun getRefreshToken(): String? = refreshTokenFlow.firstOrNull()

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userDto: UserDto
    ) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
            preferences[userIdKey] = userDto.id
            preferences[userEmailKey] = userDto.email
            preferences[userNameKey] = userDto.fullName
            preferences[userRoleKey] = userDto.role
            preferences[orgIdKey] = userDto.organizationId
            preferences[orgNameKey] = userDto.organizationName
            if (userDto.avatarUrl != null) {
                preferences[userAvatarUrlKey] = userDto.avatarUrl
            } else {
                preferences.remove(userAvatarUrlKey)
            }
            if (userDto.organizationLogoUrl != null) {
                preferences[orgLogoUrlKey] = userDto.organizationLogoUrl
            } else {
                preferences.remove(orgLogoUrlKey)
            }
        }
    }

    suspend fun updateUser(userDto: UserDto) {
        context.dataStore.edit { preferences ->
            preferences[userIdKey] = userDto.id
            preferences[userEmailKey] = userDto.email
            preferences[userNameKey] = userDto.fullName
            preferences[userRoleKey] = userDto.role
            preferences[orgIdKey] = userDto.organizationId
            preferences[orgNameKey] = userDto.organizationName
            if (userDto.avatarUrl != null) {
                preferences[userAvatarUrlKey] = userDto.avatarUrl
            } else {
                preferences.remove(userAvatarUrlKey)
            }
            if (userDto.organizationLogoUrl != null) {
                preferences[orgLogoUrlKey] = userDto.organizationLogoUrl
            } else {
                preferences.remove(orgLogoUrlKey)
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
