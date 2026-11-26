package com.als.presence.ui.auth.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.als.presence.core.designsystem.AuthDarkAccent
import com.als.presence.core.designsystem.AuthDarkBackground
import com.als.presence.core.designsystem.AuthDarkHeroEnd
import com.als.presence.core.designsystem.AuthDarkHeroStart
import com.als.presence.core.designsystem.AuthLightAccent
import com.als.presence.core.designsystem.AuthLightBackground
import com.als.presence.core.designsystem.AuthLightHeroEnd
import com.als.presence.core.designsystem.AuthLightHeroStart

/**
 * Premium atmospheric canvas background featuring soft radial light, aurora gradient,
 * and subtle abstract geometric decoration (concentric orbital rings, grid dots, and floating accents).
 */
@Composable
fun AuthAtmosphericBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isDark) AuthDarkBackground else AuthLightBackground
    val heroStart = if (isDark) AuthDarkHeroStart else AuthLightHeroStart
    val heroEnd = if (isDark) AuthDarkHeroEnd else AuthLightHeroEnd
    val accent = if (isDark) AuthDarkAccent else AuthLightAccent

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Solid canvas fill
            drawRect(color = bgColor)

            // 2. Soft atmospheric top Aurora radial glow
            val radialGlowCenter = Offset(width * 0.5f, height * 0.15f)
            val glowRadius = width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        heroStart.copy(alpha = if (isDark) 0.55f else 0.70f),
                        heroEnd.copy(alpha = if (isDark) 0.25f else 0.40f),
                        Color.Transparent
                    ),
                    center = radialGlowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = radialGlowCenter
            )

            // 3. Subtle bottom accent ambient glow
            val bottomGlowCenter = Offset(width * 0.85f, height * 0.90f)
            val bottomGlowRadius = width * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isDark) 0.12f else 0.08f),
                        Color.Transparent
                    ),
                    center = bottomGlowCenter,
                    radius = bottomGlowRadius
                ),
                radius = bottomGlowRadius,
                center = bottomGlowCenter
            )

            // 4. Abstract geometric decoration: delicate concentric orbital rings
            val ringCenter = Offset(width * 0.80f, height * 0.18f)
            val ringColor = accent.copy(alpha = if (isDark) 0.10f else 0.07f)
            val dashedStroke = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            )

            drawCircle(
                color = ringColor,
                radius = width * 0.28f,
                center = ringCenter,
                style = dashedStroke
            )
            drawCircle(
                color = ringColor.copy(alpha = if (isDark) 0.06f else 0.04f),
                radius = width * 0.48f,
                center = ringCenter,
                style = Stroke(width = 1f)
            )

            // 5. Subtle micro crosshair markers in hero area
            val markerColor = accent.copy(alpha = if (isDark) 0.18f else 0.12f)
            val markerSize = 8f
            val crosshairPositions = listOf(
                Offset(width * 0.12f, height * 0.12f),
                Offset(width * 0.88f, height * 0.28f),
                Offset(width * 0.15f, height * 0.45f)
            )
            crosshairPositions.forEach { pt ->
                drawLine(
                    color = markerColor,
                    start = Offset(pt.x - markerSize, pt.y),
                    end = Offset(pt.x + markerSize, pt.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = markerColor,
                    start = Offset(pt.x, pt.y - markerSize),
                    end = Offset(pt.x, pt.y + markerSize),
                    strokeWidth = 1.5f
                )
            }
        }

        content()
    }
}
