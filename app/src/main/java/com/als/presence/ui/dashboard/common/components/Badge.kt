package com.als.presence.ui.dashboard.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.ui.dashboard.common.PresenceTone

@Composable
private fun PresenceTone.colors(): Pair<Color, Color> = when (this) {
    PresenceTone.Success -> PresenceDesign.PositiveSoft to PresenceDesign.Positive
    PresenceTone.Warning -> PresenceDesign.CautionSoft to PresenceDesign.CautionText
    PresenceTone.Danger -> PresenceDesign.DangerSoft to PresenceDesign.DangerText
    PresenceTone.Info -> PresenceDesign.BrandSoft to PresenceDesign.BrandDark
    PresenceTone.Purple -> PresenceDesign.GrapeSoft to PresenceDesign.GrapeText
    PresenceTone.Neutral -> PresenceDesign.NeutralSoft to PresenceDesign.NeutralText
}

/**
 * Modern Status Chip / Badge component.
 * Displays state indicator chips with high contrast, animated color transitions, optional live beacon, and soft fill.
 */
@Composable
fun PresenceBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: PresenceTone = PresenceTone.Neutral,
    icon: ImageVector? = null,
    showLiveBeacon: Boolean = false
) {
    val (targetBg, targetFg) = tone.colors()

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 300),
        label = "badgeBgColor"
    )
    val animatedFg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = tween(durationMillis = 300),
        label = "badgeFgColor"
    )

    val beaconAlpha = if (showLiveBeacon) {
        val transition = rememberInfiniteTransition(label = "badgeBeacon")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "beaconPulse"
        )
        alpha
    } else 1.0f

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBg)
            .border(0.75.dp, animatedFg.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showLiveBeacon) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = beaconAlpha }
                    .clip(CircleShape)
                    .background(animatedFg)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedFg,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            color = animatedFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Alias for StatusBadge requested by design system specs. */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: PresenceTone = PresenceTone.Neutral,
    icon: ImageVector? = null,
    showLiveBeacon: Boolean = false
) {
    PresenceBadge(
        text = text,
        modifier = modifier,
        tone = tone,
        icon = icon,
        showLiveBeacon = showLiveBeacon
    )
}
