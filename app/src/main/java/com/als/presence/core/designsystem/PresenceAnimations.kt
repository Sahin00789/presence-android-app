package com.als.presence.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Presence Native Kotlin Compose Smooth Animation System.
 * Standardizes physics-based spring curves, press interactions,
 * shimmer loaders, pulsing beacons, and content transition specs.
 */
object PresenceAnimations {

    // ---------------------------------------------------------------- PHYSICS SPRING SPECS
    /** Bouncy spring spec for playful touch micro-interactions (buttons, chips). */
    val SpringBouncy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Smooth spring spec for card expansions and structural UI movements. */
    val SpringSmooth: AnimationSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium
    )

    /** Crisp stiff spring spec for immediate user feedback. */
    val SpringStiff: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // ---------------------------------------------------------------- DURATION SPECS
    val FadeQuick: AnimationSpec<Float> = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    val FadeStandard: AnimationSpec<Float> = tween(durationMillis = 350, easing = FastOutSlowInEasing)

    // ---------------------------------------------------------------- TRANSITION SPECS
    /** Standard vertical slide + fade enter transition for modals and tab content. */
    val SlideUpEnter: EnterTransition = slideInVertically(
        initialOffsetY = { 30 },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 300))

    val SlideDownExit: ExitTransition = slideOutVertically(
        targetOffsetY = { 30 },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 200))

    /** Content transition transform for tab and view switching. */
    fun tabTransition(): ContentTransform {
        return (fadeIn(animationSpec = tween(280)) + slideInVertically { 20 }).togetherWith(
            fadeOut(animationSpec = tween(200))
        )
    }
}

/**
 * Modifier extension for physics spring scale interaction on touch press.
 * Gives any clickable element a tactile, responsive native feel.
 */
fun Modifier.bounceClick(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && onClick != null) pressedScale else 1f,
        animationSpec = PresenceAnimations.SpringBouncy,
        label = "bounceClickScale"
    )

    this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Animated live beacon pulse modifier for active status indicators and live pings.
 */
fun Modifier.pulsingBeacon(
    minAlpha: Float = 0.35f,
    maxAlpha: Float = 1.0f,
    durationMillis: Int = 900
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulsingBeaconTransition")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )
    this.graphicsLayer { this.alpha = alpha }
}

/**
 * Shimmer effect modifier for skeleton loading components.
 */
@Composable
fun Modifier.shimmerLoading(
    baseColor: Color = PresenceDesign.NeutralSoft,
    highlightColor: Color = PresenceDesign.Hover
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerInfiniteTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 450f, translateAnim - 450f),
        end = Offset(translateAnim, translateAnim)
    )
    return this.background(brush)
}
