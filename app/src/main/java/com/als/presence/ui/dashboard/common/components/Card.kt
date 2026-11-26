package com.als.presence.ui.dashboard.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.GradientPalette
import com.als.presence.core.designsystem.PresenceAnimations
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.ui.dashboard.common.PresenceTone

/**
 * Core Presence Card Primitive.
 * Features hairline borders, organic corner radii, surface container colors,
 * physics-based press animation, and optional gradient surfaces.
 */
@Composable
fun PresenceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusLg),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    borderWidth: Dp = PresenceDesign.BorderHairline,
    elevation: Dp = 1.5.dp,
    backgroundBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.982f else 1f,
        animationSpec = PresenceAnimations.SpringSmooth,
        label = "cardSpringScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBrush != null) Color.Transparent else containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interactionSource
    ) {
        if (backgroundBrush != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush),
                content = content
            )
        } else {
            content()
        }
    }
}

/**
 * 1. Aurora Card (Family A):
 * Multi-stop linear & radial gradients, translucent decorative glowing shapes.
 * Reserved for main hero cards (e.g. Clock In Hero, Attendance Hero).
 */
@Composable
fun AuroraCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusXl),
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = PresenceDesign.gradientBrush(GradientPalette.Aurora)
    val glowColor = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.15f) else Color(0xFF818CF8).copy(alpha = 0.22f)
    val circleGlow = if (isDark) Color(0xFFC084FC).copy(alpha = 0.10f) else Color(0xFF38BDF8).copy(alpha = 0.18f)

    PresenceCard(
        modifier = modifier.drawBehind {
            // Draw soft radial light bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.2f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.85f, size.height * 0.2f)
            )
            // Draw translucent decorative ring
            drawCircle(
                color = circleGlow,
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.95f, size.height * 0.9f),
                style = Stroke(width = 32.dp.toPx())
            )
        },
        shape = shape,
        backgroundBrush = bgBrush,
        borderColor = borderColor,
        borderWidth = 1.25.dp,
        elevation = 3.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * 2. Gradient Metric Card / Grain Card (Family B):
 * Pastel gradient backgrounds, subtle micro-dots texture, minimal shadows.
 * Suitable for earnings, financial stats, and metric tiles.
 */
@Composable
fun GradientMetricCard(
    palette: GradientPalette,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusLg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgBrush = PresenceDesign.gradientBrush(palette)
    val dotColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.035f)

    PresenceCard(
        modifier = modifier.drawBehind {
            // Subtle fine noise / dot grid background
            val step = 14.dp.toPx()
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(
                        color = dotColor,
                        radius = 1.25.dp.toPx(),
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        },
        shape = shape,
        backgroundBrush = bgBrush,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        borderWidth = 1.dp,
        elevation = 1.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * 3. Geometric Activity Card (Family C):
 * Curved abstract shapes and diagonal forms behind content.
 * Suitable for shift schedule and overtime cards.
 */
@Composable
fun GeometricActivityCard(
    modifier: Modifier = Modifier,
    palette: GradientPalette = GradientPalette.Amber,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusLg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgBrush = PresenceDesign.gradientBrush(palette)
    val shapeColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.30f)

    PresenceCard(
        modifier = modifier.drawBehind {
            // Abstract geometric wave path behind content
            val path = Path().apply {
                moveTo(size.width * 0.4f, 0f)
                cubicTo(
                    size.width * 0.6f, size.height * 0.3f,
                    size.width * 0.3f, size.height * 0.7f,
                    size.width, size.height * 0.9f
                )
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path = path, color = shapeColor)
        },
        shape = shape,
        backgroundBrush = bgBrush,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        borderWidth = 1.dp,
        elevation = 1.5.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * 4. Glass Overlay / Glass Card (Family D):
 * Frosted translucent appearance, reserved for floating controls, verification overlays,
 * and selected interactive elements.
 */
@Composable
fun GlassOverlay(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusLg),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassColor)
            .border(1.dp, borderColor, shape)
            .drawBehind {
                // Subtle top hairline highlight
                drawLine(
                    color = Color.White.copy(alpha = if (isDark) 0.12f else 0.6f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * 5. Atmospheric Dark / Textured Card (Family E):
 * Deep colored surfaces, subtle radial lighting, translucent borders.
 */
@Composable
fun TexturedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PresenceDesign.RadiusLg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = PresenceDesign.gradientBrush(GradientPalette.Dark)
    val highlightColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.08f) else Color(0xFF60A5FA).copy(alpha = 0.12f)

    PresenceCard(
        modifier = modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(highlightColor, Color.Transparent),
                    center = Offset(size.width * 0.9f, 0f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.9f, 0f)
            )
        },
        shape = shape,
        backgroundBrush = bgBrush,
        borderColor = if (isDark) Color(0xFF2B3444) else Color(0xFFD1D5DB),
        borderWidth = 1.dp,
        elevation = 2.dp,
        onClick = onClick,
        content = content
    )
}

/**
 * Refined Progress Indicator with animated gradient fill, rounded glowing cap, and step ticks.
 */
@Composable
fun PresenceProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    gradient: Brush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progressAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(CircleShape)
                .background(gradient)
        )
    }
}

/**
 * Verification Card for Face ID and GPS verification states.
 * Features live status badge, animated indicator glow, and step verification breakdown.
 */
@Composable
fun VerificationCard(
    title: String,
    subtitle: String,
    tone: PresenceTone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    showPulse: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    AuroraCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            PresenceBadge(
                text = if (tone == PresenceTone.Success) "Verified" else "Pending",
                tone = tone,
                showLiveBeacon = showPulse
            )
        }
    }
}
