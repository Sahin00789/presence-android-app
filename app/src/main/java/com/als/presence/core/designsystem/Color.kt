package com.als.presence.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Presence App Color Token Architecture.
 * Premium Fintech & Workforce Productivity Palette with Light and Dark mode specifications.
 */

// ---------------------------------------------------------------- LIGHT PALETTE
val LightBackground = Color(0xFFF5F6F8)        // Soft neutral canvas
val LightSurface = Color(0xFFFFFFFF)           // Primary white surface
val LightElevatedSurface = Color(0xFFF1F3F6)   // Secondary surface
val LightPrimaryText = Color(0xFF17202D)       // Main heading & primary text
val LightSecondaryText = Color(0xFF596579)     // Secondary body text
val LightMutedText = Color(0xFF8490A0)         // Muted micro labels
val LightPrimary = Color(0xFF4263EB)           // Modern Sapphire Accent
val LightBorder = Color(0xFFE2E8F0)            // Hairline border
val LightSuccess = Color(0xFF0D9488)           // Emerald Teal
val LightWarning = Color(0xFFD97706)           // Amber Gold
val LightError = Color(0xFFE11D48)             // Crimson Rose

val LightPrimaryContainer = Color(0xFFEFF6FF)
val LightOnPrimaryContainer = Color(0xFF1E40AF)
val LightSuccessContainer = Color(0xFFF0FDF4)
val LightOnSuccessContainer = Color(0xFF166534)
val LightWarningContainer = Color(0xFFFFFBEB)
val LightOnWarningContainer = Color(0xFF92400E)
val LightErrorContainer = Color(0xFFFFF1F2)
val LightOnErrorContainer = Color(0xFF9F1239)

// Light Mode Gradients
val LightBlueGradientStart = Color(0xFFDCEBFF)
val LightBlueGradientEnd = Color(0xFFE9E3FF)
val LightGreenGradientStart = Color(0xFFDDF3E5)
val LightGreenGradientEnd = Color(0xFFDDF2ED)
val LightAmberGradientStart = Color(0xFFFFF0D5)
val LightAmberGradientEnd = Color(0xFFFFE5CB)
val LightLavenderGradientStart = Color(0xFFE9E0FF)
val LightLavenderGradientEnd = Color(0xFFDDEAFF)
val LightTealGradientStart = Color(0xFFD8F3ED)
val LightTealGradientEnd = Color(0xFFDDF0FA)

// ---------------------------------------------------------------- DARK PALETTE
val DarkBackground = Color(0xFF101318)         // Deep dark canvas
val DarkSurface = Color(0xFF191E26)            // Primary dark surface
val DarkElevatedSurface = Color(0xFF222936)    // Secondary dark surface
val DarkPrimaryText = Color(0xFFF1F4F9)        // Main heading & primary dark text
val DarkSecondaryText = Color(0xFFA8B2C1)      // Secondary dark text
val DarkMutedText = Color(0xFF7D899A)          // Muted dark text
val DarkPrimary = Color(0xFF829AFF)            // Accent Blue/Lavender
val DarkBorder = Color(0xFF2B3444)             // Dark hairline border
val DarkSuccess = Color(0xFF34D399)            // Electric Emerald
val DarkWarning = Color(0xFFFBBF24)            // Radiant Amber
val DarkError = Color(0xFFFB7185)              // Bright Coral

val DarkPrimaryContainer = Color(0xFF172554)
val DarkOnPrimaryContainer = Color(0xFFBFDBFE)
val DarkSuccessContainer = Color(0xFF064E3B)
val DarkOnSuccessContainer = Color(0xFFA7F3D0)
val DarkWarningContainer = Color(0xFF451A03)
val DarkOnWarningContainer = Color(0xFFFDE68A)
val DarkErrorContainer = Color(0xFF4C0519)
val DarkOnErrorContainer = Color(0xFFFECDD3)

// Dark Mode Gradients
val DarkBlueGradientStart = Color(0xFF202E57)
val DarkBlueGradientEnd = Color(0xFF29244D)
val DarkGreenGradientStart = Color(0xFF193B2A)
val DarkGreenGradientEnd = Color(0xFF173D39)
val DarkAmberGradientStart = Color(0xFF45321D)
val DarkAmberGradientEnd = Color(0xFF493025)
val DarkLavenderGradientStart = Color(0xFF352652)
val DarkLavenderGradientEnd = Color(0xFF20365B)
val DarkTealGradientStart = Color(0xFF173D39)
val DarkTealGradientEnd = Color(0xFF19394D)

// ---------------------------------------------------------------- LEGACY COMPATIBILITY MAPPINGS
val PrimaryIndigo = LightPrimary
val PrimaryIndigoDark = LightPrimary
val PrimaryIndigoLight = DarkPrimary
val PrimaryContainerIndigo = LightPrimaryContainer
val OnPrimaryContainerIndigo = LightOnPrimaryContainer

val SecondaryEmerald = LightSuccess
val SecondaryEmeraldDark = LightSuccess
val SecondaryContainerEmerald = LightSuccessContainer
val OnSecondaryContainerEmerald = LightOnSuccessContainer

val TertiaryAmber = LightWarning
val TertiaryContainerAmber = LightWarningContainer
val OnTertiaryContainerAmber = LightOnWarningContainer

val DangerRose = LightError
val DangerContainerRose = LightErrorContainer
val OnDangerContainerRose = LightOnErrorContainer

val SurfaceLight = LightSurface
val SurfaceVariantLight = LightElevatedSurface
val SurfaceContainerLowLight = LightBackground
val SurfaceContainerLight = LightElevatedSurface
val SurfaceContainerHighLight = LightBorder
val BackgroundLight = LightBackground
val OnSurfaceDark = LightPrimaryText
val OnSurfaceVariantSlate = LightSecondaryText
val OutlineSlate = LightBorder
val OutlineVariantSlate = LightBorder

val BackgroundDarkVal = DarkBackground
val SurfaceDarkVal = DarkSurface
val SurfaceVariantDarkVal = DarkElevatedSurface
val SurfaceContainerLowDarkVal = DarkBackground
val SurfaceContainerDarkVal = DarkSurface
val SurfaceContainerHighDarkVal = DarkElevatedSurface
val OnSurfaceLightVal = DarkPrimaryText
val OnSurfaceVariantLightSlateVal = DarkSecondaryText
val OutlineDarkSlateVal = DarkBorder
val OutlineVariantDarkSlateVal = DarkBorder

val RoleOwnerPrimary = LightPrimary
val RoleOwnerContainer = LightPrimaryContainer
val RoleManagerPrimary = LightSuccess
val RoleManagerContainer = LightSuccessContainer
val RoleStaffPrimary = LightWarning
val RoleStaffContainer = LightWarningContainer

// ---------------------------------------------------------------- AUTH REDESIGN 2026 SPECIFICATION TOKENS
val AuthLightBackground = Color(0xFFF5F6FA)
val AuthLightSurface = Color(0xFFFFFFFF)
val AuthLightPrimaryText = Color(0xFF17202D)
val AuthLightSecondaryText = Color(0xFF596579)
val AuthLightAccent = Color(0xFF4263EB)
val AuthLightHeroStart = Color(0xFFDCEBFF)
val AuthLightHeroEnd = Color(0xFFE9E3FF)

val AuthDarkBackground = Color(0xFF101318)
val AuthDarkSurface = Color(0xFF191E26)
val AuthDarkPrimaryText = Color(0xFFF1F4F9)
val AuthDarkSecondaryText = Color(0xFFA8B2C1)
val AuthDarkAccent = Color(0xFF829AFF)
val AuthDarkHeroStart = Color(0xFF202E57)
val AuthDarkHeroEnd = Color(0xFF29244D)


