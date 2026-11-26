package com.als.presence.ui.dashboard.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.als.presence.core.designsystem.DarkBackground
import com.als.presence.core.designsystem.LightBackground

/**
 * Premium dashboard atmospheric background featuring soft role-tinted radial illumination,
 * subtle organic depth, and delicate technical micro-accents.
 *
 * Provides a modern, cohesive SaaS aesthetic across both Owner and Employee workspaces
 * while maintaining 100% legibility and high contrast for tabular and card data.
 */
@Composable
fun DashboardAtmosphericBackground(
    modifier: Modifier = Modifier,
    roleColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isDark) DarkBackground else LightBackground

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Base canvas fill
            drawRect(color = bgColor)

            // 2. Soft atmospheric top Aurora radial aura (role tinted)
            val topGlowCenter = Offset(width * 0.5f, height * 0.06f)
            val topGlowRadius = width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        roleColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                        roleColor.copy(alpha = if (isDark) 0.06f else 0.03f),
                        Color.Transparent
                    ),
                    center = topGlowCenter,
                    radius = topGlowRadius
                ),
                radius = topGlowRadius,
                center = topGlowCenter
            )

            // 3. Subtle bottom accent ambient glow
            val bottomGlowCenter = Offset(width * 0.88f, height * 0.92f)
            val bottomGlowRadius = width * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        roleColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                        Color.Transparent
                    ),
                    center = bottomGlowCenter,
                    radius = bottomGlowRadius
                ),
                radius = bottomGlowRadius,
                center = bottomGlowCenter
            )

            // 4. Subtle orbital tech ring in upper right quadrant
            val ringCenter = Offset(width * 0.85f, height * 0.14f)
            val ringColor = roleColor.copy(alpha = if (isDark) 0.08f else 0.05f)
            val dashedStroke = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            drawCircle(
                color = ringColor,
                radius = width * 0.24f,
                center = ringCenter,
                style = dashedStroke
            )
            drawCircle(
                color = ringColor.copy(alpha = if (isDark) 0.04f else 0.025f),
                radius = width * 0.42f,
                center = ringCenter,
                style = Stroke(width = 1f)
            )

            // 5. Delicate micro crosshairs in peripheral areas
            val crosshairColor = roleColor.copy(alpha = if (isDark) 0.14f else 0.09f)
            val markerSize = 6f
            val crosshairPositions = listOf(
                Offset(width * 0.08f, height * 0.12f),
                Offset(width * 0.92f, height * 0.26f),
                Offset(width * 0.12f, height * 0.78f)
            )
            crosshairPositions.forEach { pt ->
                drawLine(
                    color = crosshairColor,
                    start = Offset(pt.x - markerSize, pt.y),
                    end = Offset(pt.x + markerSize, pt.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = crosshairColor,
                    start = Offset(pt.x, pt.y - markerSize),
                    end = Offset(pt.x, pt.y + markerSize),
                    strokeWidth = 1.2f
                )
            }

            // 6. Subtle technical registration dots
            val dotColor = roleColor.copy(alpha = if (isDark) 0.12f else 0.07f)
            val dotPositions = listOf(
                Offset(width * 0.88f, height * 0.42f),
                Offset(width * 0.92f, height * 0.42f),
                Offset(width * 0.88f, height * 0.45f),
                Offset(width * 0.92f, height * 0.45f)
            )
            dotPositions.forEach { pt ->
                drawCircle(
                    color = dotColor,
                    radius = 1.5f,
                    center = pt
                )
            }
        }

        content()
    }
}
