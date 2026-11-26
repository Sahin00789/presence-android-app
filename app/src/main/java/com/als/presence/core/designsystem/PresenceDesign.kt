package com.als.presence.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GradientPalette { Blue, Green, Amber, Lavender, Teal, Aurora, Dark }

/**
 * Centralized Presence Design System Tokens.
 * Dynamically resolves theme colors via [MaterialTheme.colorScheme] getters,
 * ensuring seamless Light and Dark mode UI responsiveness.
 */
object PresenceDesign {

    // ---------------------------------------------------------------- RAW PALETTE TOKENS
    val LightBg = LightBackground
    val LightSurf = LightSurface
    val LightElevated = LightElevatedSurface
    val LightTextPrimary = LightPrimaryText
    val LightTextSecondary = LightSecondaryText
    val LightBrandPrimary = LightPrimary
    val LightBorderLine = LightBorder

    val DarkBg = DarkBackground
    val DarkSurf = DarkSurface
    val DarkElevated = DarkElevatedSurface
    val DarkTextPrimary = DarkPrimaryText
    val DarkTextSecondary = DarkSecondaryText
    val DarkBrandPrimary = DarkPrimary
    val DarkBorderLine = DarkBorder

    // ---------------------------------------------------------------- DYNAMIC THEMED COLOR TOKENS
    val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val Navy: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val NavyLight: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val MutedLight: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val Line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val Workspace: Color @Composable get() = MaterialTheme.colorScheme.background

    val Brand: Color @Composable get() = MaterialTheme.colorScheme.primary
    val BrandDark: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
    val BrandSoft: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer

    val Positive: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val PositiveSoft: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer

    val Caution: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val CautionSoft: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
    val CautionText: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryContainer

    val Grape: Color = Color(0xFF8B5CF6)
    val GrapeSoft: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val GrapeText: Color @Composable get() = MaterialTheme.colorScheme.primary

    val Danger: Color @Composable get() = MaterialTheme.colorScheme.error
    val DangerSoft: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
    val DangerText: Color @Composable get() = MaterialTheme.colorScheme.onErrorContainer

    val Panel: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val NeutralSoft: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val NeutralText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val BodyText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val LabelText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val InputBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val DividerSoft: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val Hover: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    // ---------------------------------------------------------------- GRADIENT BRUSH HELPER
    @Composable
    fun gradientColors(palette: GradientPalette): Pair<Color, Color> {
        val isDark = isSystemInDarkTheme()
        return when (palette) {
            GradientPalette.Blue -> if (isDark) DarkBlueGradientStart to DarkBlueGradientEnd else LightBlueGradientStart to LightBlueGradientEnd
            GradientPalette.Green -> if (isDark) DarkGreenGradientStart to DarkGreenGradientEnd else LightGreenGradientStart to LightGreenGradientEnd
            GradientPalette.Amber -> if (isDark) DarkAmberGradientStart to DarkAmberGradientEnd else LightAmberGradientStart to LightAmberGradientEnd
            GradientPalette.Lavender -> if (isDark) DarkLavenderGradientStart to DarkLavenderGradientEnd else LightLavenderGradientStart to LightLavenderGradientEnd
            GradientPalette.Teal -> if (isDark) DarkTealGradientStart to DarkTealGradientEnd else LightTealGradientStart to LightTealGradientEnd
            GradientPalette.Aurora -> if (isDark) Color(0xFF1E2640) to Color(0xFF2C204A) else Color(0xFFDCEBFF) to Color(0xFFE9E3FF)
            GradientPalette.Dark -> if (isDark) Color(0xFF141923) to Color(0xFF1D2432) else Color(0xFF1E2638) to Color(0xFF151C2A)
        }
    }

    @Composable
    fun gradientBrush(palette: GradientPalette): Brush {
        val (start, end) = gradientColors(palette)
        return Brush.linearGradient(listOf(start, end))
    }

    @Composable
    fun textContrastColor(palette: GradientPalette): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark || palette == GradientPalette.Dark) DarkPrimaryText else LightPrimaryText
    }

    @Composable
    fun textSecondaryContrastColor(palette: GradientPalette): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark || palette == GradientPalette.Dark) DarkSecondaryText else LightSecondaryText
    }

    // ----------------------------------------------------------- 4dp GRID SPACING SCALE
    val MainHorizontalPadding: Dp = 20.dp
    val SectionSpacing: Dp = 24.dp
    val ComponentSpacingMedium: Dp = 16.dp
    val ComponentSpacingSmall: Dp = 12.dp
    val CardPadding: Dp = 16.dp

    val Space2: Dp = 2.dp
    val Space4: Dp = 4.dp
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space32: Dp = 32.dp
    val Space40: Dp = 40.dp
    val Space48: Dp = 48.dp

    // ---------------------------------------------------------------- CORNER RADII
    val RadiusSmallControl: Dp = 10.dp
    val RadiusStandardCard: Dp = 16.dp
    val RadiusLargePanel: Dp = 20.dp

    val RadiusXs: Dp = 4.dp
    val RadiusSm: Dp = 8.dp
    val RadiusMd: Dp = 12.dp
    val RadiusLg: Dp = 16.dp
    val RadiusXl: Dp = 20.dp
    val RadiusPill: Dp = 999.dp

    val RadiusCard: Dp = 16.dp
    val RadiusPanel: Dp = 16.dp
    val RadiusControl: Dp = 10.dp

    // ---------------------------------------------------------------- ELEVATION & BORDERS
    val CardElevation: Dp = 2.dp
    val SurfaceElevation: Dp = 1.dp
    val ModalElevation: Dp = 8.dp

    val BorderHairline: Dp = 1.dp
    val BorderFocused: Dp = 2.dp

    // ---------------------------------------------------------------- TOUCH TARGETS
    val MinimumTouchTarget: Dp = 48.dp
    val DefaultButtonHeight: Dp = 48.dp
    val SmallButtonHeight: Dp = 38.dp
}

