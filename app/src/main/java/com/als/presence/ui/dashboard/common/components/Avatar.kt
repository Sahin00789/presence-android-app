package com.als.presence.ui.dashboard.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * User Profile Avatar with Initials fallback & optional role border ring.
 */
@Composable
fun PresenceAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 40.dp,
    ringColor: Color? = null
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "?" }

    val gradientColors = remember(initials) {
        val hash = kotlin.math.abs(initials.hashCode())
        when (hash % 5) {
            0 -> listOf(Color(0xFF2563EB), Color(0xFF7C3AED)) // Indigo -> Purple
            1 -> listOf(Color(0xFF059669), Color(0xFF0D9488)) // Emerald -> Teal
            2 -> listOf(Color(0xFFD97706), Color(0xFFEA580C)) // Amber -> Orange
            3 -> listOf(Color(0xFF0284C7), Color(0xFF2563EB)) // Sky -> Blue
            else -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777)) // Violet -> Pink
        }
    }

    val baseModifier = modifier
        .size(size)
        .then(
            if (ringColor != null) Modifier.border(2.dp, ringColor, CircleShape)
            else Modifier
        )
        .clip(CircleShape)

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    } else {
        Box(
            modifier = baseModifier.background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
