package com.als.presence.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the Presence app.
 *
 * The secondary screens of this app are full-bleed overlays reached from the
 * hamburger, so they are painted on the Presence workspace surface rather than on
 * the Material canvas: hairline borders, near-flat cards, uppercase micro-labels
 * and a single blue brand accent. Keeping the values here instead of inline means
 * a screen can be re-skinned without hunting through every composable.
 */
object PresenceDesign {

    // ---------------------------------------------------------------- palette
    val Ink = Color(0xFF1C2B42)
    val Navy = Color(0xFF132541)
    val NavyLight = Color(0xFF1C365B)
    val Muted = Color(0xFF7D8AA0)
    val MutedLight = Color(0xFFA3ADBD)
    val Line = Color(0xFFE7EBF2)
    val Surface = Color(0xFFFFFFFF)
    val Workspace = Color(0xFFF6F8FC)

    val Brand = Color(0xFF3C6DF0)
    val BrandDark = Color(0xFF2D59D2)
    val BrandSoft = Color(0xFFEDF3FF)

    val Positive = Color(0xFF22A06B)
    val PositiveSoft = Color(0xFFEAF8F1)

    val Caution = Color(0xFFED9B40)
    val CautionSoft = Color(0xFFFFF5E8)
    val CautionText = Color(0xFFC07C1F)

    val Grape = Color(0xFF8667E8)
    val GrapeSoft = Color(0xFFF1EFFF)
    val GrapeText = Color(0xFF6F4FD6)

    val Danger = Color(0xFFDD5B67)
    val DangerSoft = Color(0xFFFFF0F1)
    val DangerText = Color(0xFFC44855)

    // --------------------------------------------------------------- surfaces
    /** The tint every nested panel sits on inside a card. */
    val Panel = Color(0xFFFBFCFE)
    val NeutralSoft = Color(0xFFF1F4F9)
    val NeutralText = Color(0xFF66758C)
    val BodyText = Color(0xFF536177)
    val LabelText = Color(0xFF8B97AA)
    val InputBorder = Color(0xFFDFE5EE)
    val DividerSoft = Color(0xFFEDF0F5)
    val Hover = Color(0xFFF4F7FC)

    // ----------------------------------------------------------------- radii
    val RadiusCard = 13.dp
    val RadiusPanel = 11.dp
    val RadiusControl = 10.dp
    val RadiusPill = 9.dp

    // ---------------------------------------------------------------- shadows
    /** `0 1px 2px rgba(24,40,72,.03), 0 5px 18px rgba(24,40,72,.035)` */
    val CardElevation = 2.dp
}
