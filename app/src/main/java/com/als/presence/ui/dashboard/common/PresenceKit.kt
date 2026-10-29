package com.als.presence.ui.dashboard.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign

/**
 * The Presence primitive kit, in Compose.
 *
 * The secondary screens are overlays rather than tabs, so they are built from these
 * pieces instead of the Material widgets: a hairline card on the workspace surface,
 * an uppercase micro-label above every value, and a soft-filled action button.
 */

/** The six badge tones Presence uses for state. */
enum class PresenceTone { Success, Warning, Danger, Info, Purple, Neutral }

/** Button treatments, mirroring Presence's `variants` map. */
enum class PresenceVariant { Primary, Secondary, Ghost, Soft, Danger, DangerSolid }

/** Button heights. The smallest is still a thumb-sized 40dp. */
enum class PresenceSize { Sm, Md, Lg }

private fun PresenceTone.softColors(): Pair<Color, Color> = when (this) {
    PresenceTone.Success -> PresenceDesign.PositiveSoft to PresenceDesign.Positive
    PresenceTone.Warning -> PresenceDesign.CautionSoft to PresenceDesign.CautionText
    PresenceTone.Danger -> PresenceDesign.DangerSoft to PresenceDesign.DangerText
    PresenceTone.Info -> PresenceDesign.BrandSoft to PresenceDesign.BrandDark
    PresenceTone.Purple -> PresenceDesign.GrapeSoft to PresenceDesign.GrapeText
    PresenceTone.Neutral -> PresenceDesign.NeutralSoft to PresenceDesign.NeutralText
}

/** Animated shimmer effect modifier for skeleton loading states. */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        PresenceDesign.NeutralSoft,
        PresenceDesign.Hover,
        PresenceDesign.NeutralSoft
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
    return this.background(brush)
}

/** Skeleton placeholder card with shimmer effect. */
@Composable
fun PresenceCardSkeleton(
    modifier: Modifier = Modifier
) {
    PresenceCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.5f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 20.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
        }
    }
}

/** A list of shimmer card skeletons for loading lists. */
@Composable
fun PresenceListSkeleton(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            PresenceCardSkeleton()
        }
    }
}

/** A pill with a soft fill and a saturated label, the way Presence marks state. */
@Composable
fun PresenceBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: PresenceTone = PresenceTone.Neutral,
    dot: Boolean = false
) {
    val (background, foreground) = tone.softColors()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(foreground)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            color = foreground
        )
    }
}

/** A round initials badge. */
@Composable
fun PresenceAvatar(
    name: String,
    modifier: Modifier = Modifier,
    tone: PresenceTone = PresenceTone.Info,
    size: Dp = 40.dp
) {
    val (background, foreground) = tone.softColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).ifBlank { "?" }.uppercase(),
            fontSize = (size.value / 3.2f).sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

/** The uppercase micro-label that sits above a value. */
@Composable
fun PresenceKicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PresenceDesign.LabelText
) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = color,
        modifier = modifier
    )
}

/** A hairline card on the workspace surface. */
@Composable
fun PresenceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PresenceDesign.RadiusCard))
            .background(PresenceDesign.Surface)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(PresenceDesign.RadiusCard))
    ) {
        content()
    }
}

/** A nested panel inside a card. */
@Composable
fun PresencePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PresenceDesign.RadiusPanel))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(PresenceDesign.RadiusPanel))
    ) {
        content()
    }
}

/** The page heading: an eyebrow, the title, a sentence of description, actions. */
@Composable
fun PresencePageIntro(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PresenceKicker(eyebrow)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                color = PresenceDesign.Ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = PresenceDesign.Muted
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

/** A section heading with an optional trailing action. */
@Composable
fun PresenceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = PresenceDesign.Ink
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = PresenceDesign.LabelText
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(10.dp))
            action()
        }
    }
}

/**
 * The inline label/value strip. It answers "how much of this is there" without
 * spending a row of panels on it.
 */
@Composable
fun PresenceMetaStrip(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PresenceDesign.RadiusControl))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(PresenceDesign.RadiusControl))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    color = PresenceDesign.LabelText
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.1).sp,
                    color = PresenceDesign.Ink
                )
            }
        }
    }
}

/** A single number with its label, trend indicator, and optional click action. */
@Composable
fun PresenceStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    trend: String? = null,
    tone: PresenceTone = PresenceTone.Info,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresenceKicker(label)
            if (trend != null) {
                PresenceBadge(label = trend, tone = tone)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = PresenceDesign.Ink
        )
        if (hint != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hint,
                fontSize = 10.5.sp,
                color = PresenceDesign.LabelText
            )
        }
    }
}

/** One label/value pair on its own hairline row. */
@Composable
fun PresenceDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PresenceDesign.Ink
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.Line, RoundedCornerShape(9.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = PresenceDesign.LabelText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

/** The action button. */
@Composable
fun PresenceButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PresenceVariant = PresenceVariant.Primary,
    size: PresenceSize = PresenceSize.Md,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val height = when (size) {
        PresenceSize.Sm -> 38.dp
        PresenceSize.Md -> 42.dp
        PresenceSize.Lg -> 48.dp
    }
    val (background, foreground, border) = when (variant) {
        PresenceVariant.Primary -> Triple(PresenceDesign.Brand, Color.White, null)
        PresenceVariant.Secondary -> Triple(PresenceDesign.Surface, PresenceDesign.NavyLight, BorderStroke(1.dp, PresenceDesign.InputBorder))
        PresenceVariant.Ghost -> Triple(Color.Transparent, PresenceDesign.NeutralText, null)
        PresenceVariant.Soft -> Triple(PresenceDesign.BrandSoft, PresenceDesign.BrandDark, null)
        PresenceVariant.Danger -> Triple(PresenceDesign.DangerSoft, PresenceDesign.DangerText, null)
        PresenceVariant.DangerSolid -> Triple(PresenceDesign.DangerText, Color.White, null)
    }

    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (border != null) Modifier.border(border, RoundedCornerShape(9.dp)) else Modifier
            )
            .background(if (enabled && !loading) background else background.copy(alpha = 0.55f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = foreground
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

/** A square icon-only action. */
@Composable
fun PresenceIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PresenceDesign.NeutralText
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

/** Filter/Selection Chip for premises, tabs, and status toggles. */
@Composable
fun PresenceFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) PresenceDesign.Brand else PresenceDesign.Muted
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = PresenceDesign.Line,
            selectedBorderColor = PresenceDesign.Brand,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        ),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = PresenceDesign.Surface,
            labelColor = PresenceDesign.Ink,
            selectedContainerColor = PresenceDesign.BrandSoft,
            selectedLabelColor = PresenceDesign.BrandDark
        )
    )
}

/** The dashed empty state. */
@Composable
fun PresenceEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PresenceDesign.RadiusCard))
            .background(PresenceDesign.Panel)
            .border(1.dp, PresenceDesign.InputBorder, RoundedCornerShape(PresenceDesign.RadiusCard))
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PresenceDesign.BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PresenceDesign.Brand,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = PresenceDesign.Ink
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = PresenceDesign.LabelText,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

/** The loading state with circular spinner and shimmer skeleton cards below. */
@Composable
fun PresenceLoading(
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = PresenceDesign.Brand
            )
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PresenceDesign.Ink
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        PresenceListSkeleton(count = 3)
    }
}

/** The failure state, with a retry that keeps the page usable. */
@Composable
fun PresenceErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = PresenceDesign.Danger,
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Could not reach the server",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PresenceDesign.Ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = PresenceDesign.LabelText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        PresenceButton(
            label = "Retry",
            onClick = onRetry,
            variant = PresenceVariant.Secondary,
            size = PresenceSize.Sm,
            icon = Icons.Default.Refresh
        )
    }
}
