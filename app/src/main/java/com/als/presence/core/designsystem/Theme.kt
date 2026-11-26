package com.als.presence.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSuccess,
    onSecondary = Color.White,
    secondaryContainer = LightSuccessContainer,
    onSecondaryContainer = LightOnSuccessContainer,
    tertiary = LightWarning,
    onTertiary = Color.White,
    tertiaryContainer = LightWarningContainer,
    onTertiaryContainer = LightOnWarningContainer,
    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = LightSecondaryText,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightElevatedSurface,
    outline = LightBorder,
    outlineVariant = LightBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF0B1220),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSuccess,
    onSecondary = Color(0xFF0B1220),
    secondaryContainer = DarkSuccessContainer,
    onSecondaryContainer = DarkOnSuccessContainer,
    tertiary = DarkWarning,
    onTertiary = Color(0xFF0B1220),
    tertiaryContainer = DarkWarningContainer,
    onTertiaryContainer = DarkOnWarningContainer,
    error = DarkError,
    onError = Color(0xFF0B1220),
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkSecondaryText,
    surfaceContainerLow = DarkBackground,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevatedSurface,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun PresenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val screenSizeInfo = rememberScreenSizeInfo()

    CompositionLocalProvider(
        LocalScreenSizeInfo provides screenSizeInfo
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
