package com.als.presence.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen width categories to adapt UI scaling and layout responsiveness
 * across small, standard, and wide phone/tablet displays.
 */
enum class DisplayWidthTier {
    VERY_COMPACT, // < 360dp or high font scaling (budget/small phones like 320dp-359dp)
    COMPACT,      // 360dp - 411dp (standard modern smartphones)
    MEDIUM,       // 412dp - 599dp (large phablets)
    EXPANDED      // >= 600dp (Foldables unfolded, tablets)
}

/**
 * Holds display and device configuration metrics for adaptive Compose layouts.
 */
data class ScreenSizeInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val fontScale: Float,
    val widthTier: DisplayWidthTier,
    val isVeryCompact: Boolean,
    val isCompactOrSmaller: Boolean,
    val isTabletOrFoldable: Boolean,
    val horizontalScreenPadding: Dp,
    val cardContentPadding: Dp,
    val subCardPadding: Dp,
    val contentSpacing: Dp = if (isVeryCompact) 10.dp else 14.dp,
    val iconSizeMedium: Dp = if (isVeryCompact) 32.dp else 40.dp,
    val iconSizeSmall: Dp = if (isVeryCompact) 24.dp else 28.dp
)

/**
 * Global CompositionLocal to access the current screen's size, width tier,
 * and adaptive spacing tokens anywhere in the composable hierarchy.
 */
val LocalScreenSizeInfo = compositionLocalOf {
    ScreenSizeInfo(
        widthDp = 390.dp,
        heightDp = 844.dp,
        fontScale = 1.0f,
        widthTier = DisplayWidthTier.COMPACT,
        isVeryCompact = false,
        isCompactOrSmaller = true,
        isTabletOrFoldable = false,
        horizontalScreenPadding = 14.dp,
        cardContentPadding = 16.dp,
        subCardPadding = 12.dp
    )
}

/**
 * Detects current device display metrics, taking into account orientation,
 * screen width in dp, and user accessibility fontScale.
 */
@Composable
fun rememberScreenSizeInfo(): ScreenSizeInfo {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    return remember(configuration.screenWidthDp, configuration.screenHeightDp, density.fontScale) {
        val widthDp = configuration.screenWidthDp.dp
        val heightDp = configuration.screenHeightDp.dp
        val fontScale = density.fontScale

        // Effective width accounts for user accessibility fontScale crowding
        val effectiveWidthDp = (configuration.screenWidthDp / fontScale.coerceAtLeast(1.0f)).dp

        val widthTier = when {
            effectiveWidthDp < 360.dp || widthDp < 360.dp -> DisplayWidthTier.VERY_COMPACT
            widthDp < 412.dp -> DisplayWidthTier.COMPACT
            widthDp < 600.dp -> DisplayWidthTier.MEDIUM
            else -> DisplayWidthTier.EXPANDED
        }

        val isVeryCompact = widthTier == DisplayWidthTier.VERY_COMPACT
        val isCompactOrSmaller = widthTier == DisplayWidthTier.VERY_COMPACT || widthTier == DisplayWidthTier.COMPACT
        val isTabletOrFoldable = widthTier == DisplayWidthTier.EXPANDED

        ScreenSizeInfo(
            widthDp = widthDp,
            heightDp = heightDp,
            fontScale = fontScale,
            widthTier = widthTier,
            isVeryCompact = isVeryCompact,
            isCompactOrSmaller = isCompactOrSmaller,
            isTabletOrFoldable = isTabletOrFoldable,
            horizontalScreenPadding = when {
                isVeryCompact -> 10.dp
                widthDp < 390.dp -> 12.dp
                isTabletOrFoldable -> 24.dp
                else -> 14.dp
            },
            cardContentPadding = if (isVeryCompact) 12.dp else 16.dp,
            subCardPadding = if (isVeryCompact) 10.dp else 12.dp
        )
    }
}

/**
 * Provides [ScreenSizeInfo] down the Compose hierarchy via [LocalScreenSizeInfo].
 */
@Composable
fun ProvideScreenSizeInfo(content: @Composable () -> Unit) {
    val screenSizeInfo = rememberScreenSizeInfo()
    CompositionLocalProvider(
        LocalScreenSizeInfo provides screenSizeInfo,
        content = content
    )
}
