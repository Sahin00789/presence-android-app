package com.als.presence.core.designsystem

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "presence_theme_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("app_theme_mode")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[themeKey] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = mode.name
        }
    }
}
