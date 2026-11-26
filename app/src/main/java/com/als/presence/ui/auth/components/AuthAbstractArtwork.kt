package com.als.presence.ui.auth.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Modern 2026 Abstract 3D-Inspired Graphic Component.
 * Draws vibrant, glassmorphic 3D spheres, glowing orbital rings, and dynamic floating nodes
 * that symbolize workforce visibility, time, and connected organization structure.
 */
@Composable
fun AuthAbstractArtwork(
    modifier: Modifier = Modifier,
    heightDp: Int = 220,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val transition = rememberInfiniteTransition(label = "artworkAnim")

    val floatY by transition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rotateDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotateDeg"
    )

    val sphere1Colors = if (isDark) {
        listOf(Color(0xFF829AFF), Color(0xFF4263EB), Color(0xFF1E2654))
    } else {
        listOf(Color(0xFF6B8AFF), Color(0xFF4263EB), Color(0xFF93ACFF))
    }

    val sphere2Colors = if (isDark) {
        listOf(Color(0xFF34D399), Color(0xFF0D9488), Color(0xFF064E3B))
    } else {
        listOf(Color(0xFF2DD4BF), Color(0xFF0D9488), Color(0xFFA7F3D0))
    }

    val sphere3Colors = if (isDark) {
        listOf(Color(0xFFA78BFA), Color(0xFF7C3AED), Color(0xFF4C1D95))
    } else {
        listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6), Color(0xFFDDD6FE))
    }

    val orbitColor = if (isDark) Color(0xFF829AFF).copy(alpha = 0.35f) else Color(0xFF4263EB).copy(alpha = 0.25f)
    val glowShadow = if (isDark) Color(0xFF000000).copy(alpha = 0.4f) else Color(0xFF17202D).copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f + floatY

            if (width <= 0 || height <= 0) return@Canvas

            // 1. Shadow ambient ellipse under central artwork
            drawOval(
                color = glowShadow,
                topLeft = Offset(centerX - width * 0.3f, height * 0.78f),
                size = Size(width * 0.6f, height * 0.15f)
            )

            // 2. Main Outer Orbital Ring
            val orbitRadiusX = width * 0.38f
            val orbitRadiusY = height * 0.24f
            val orbitPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        centerX - orbitRadiusX,
                        centerY - orbitRadiusY,
                        centerX + orbitRadiusX,
                        centerY + orbitRadiusY
                    )
                )
            }
            drawPath(
                path = orbitPath,
                color = orbitColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Secondary angled ring
            val angleOrbitPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        centerX - orbitRadiusX * 0.7f,
                        centerY - orbitRadiusY * 1.2f,
                        centerX + orbitRadiusX * 0.7f,
                        centerY + orbitRadiusY * 1.2f
                    )
                )
            }
            drawPath(
                path = angleOrbitPath,
                color = orbitColor.copy(alpha = 0.2f),
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Central Primary 3D Sphere (Owner/HQ Node)
            val sphere1Radius = height * 0.26f
            val sphere1Center = Offset(centerX - width * 0.06f, centerY - height * 0.05f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = sphere1Colors,
                    center = sphere1Center - Offset(sphere1Radius * 0.35f, sphere1Radius * 0.35f),
                    radius = sphere1Radius * 1.2f
                ),
                center = sphere1Center,
                radius = sphere1Radius
            )

            // Sphere specular light highlight curve
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                center = sphere1Center - Offset(sphere1Radius * 0.4f, sphere1Radius * 0.4f),
                radius = sphere1Radius * 0.18f
            )

            // 4. Secondary Teal 3D Sphere (Employee/Workforce Node)
            val sphere2Radius = height * 0.18f
            val sphere2Center = Offset(centerX + width * 0.22f, centerY + height * 0.12f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = sphere2Colors,
                    center = sphere2Center - Offset(sphere2Radius * 0.3f, sphere2Radius * 0.3f),
                    radius = sphere2Radius * 1.2f
                ),
                center = sphere2Center,
                radius = sphere2Radius
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                center = sphere2Center - Offset(sphere2Radius * 0.35f, sphere2Radius * 0.35f),
                radius = sphere2Radius * 0.15f
            )

            // 5. Tertiary Lavender Sphere (Earnings/Ledger Node)
            val sphere3Radius = height * 0.12f
            val sphere3Center = Offset(centerX - width * 0.25f, centerY + height * 0.18f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = sphere3Colors,
                    center = sphere3Center - Offset(sphere3Radius * 0.3f, sphere3Radius * 0.3f),
                    radius = sphere3Radius * 1.2f
                ),
                center = sphere3Center,
                radius = sphere3Radius
            )

            // 6. Floating micro accent nodes along orbit
            val node1Pos = Offset(centerX + orbitRadiusX * 0.85f, centerY - orbitRadiusY * 0.4f)
            drawCircle(
                color = if (isDark) Color(0xFF829AFF) else Color(0xFF4263EB),
                center = node1Pos,
                radius = 6.dp.toPx()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                center = node1Pos,
                radius = 2.dp.toPx()
            )

            val node2Pos = Offset(centerX - orbitRadiusX * 0.7f, centerY - orbitRadiusY * 0.6f)
            drawCircle(
                color = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488),
                center = node2Pos,
                radius = 5.dp.toPx()
            )
        }
    }
}
