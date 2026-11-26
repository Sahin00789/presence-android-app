package com.als.presence.ui.dashboard.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/** One tab in the workspace bottom bar. */
data class PresenceBottomItem(
    val label: String,
    val icon: ImageVector
)

/**
 * Modern Material 3 Bottom Navigation Bar for Android.
 * Features compact height, animated pill indicators, hairline top border, crisp surface container,
 * and high-contrast selection colors for Light and Dark modes.
 */
@Composable
fun PresenceBottomBar(
    items: List<PresenceBottomItem>,
    selectedIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAccent by animateColorAsState(
        targetValue = accent,
        animationSpec = tween(durationMillis = 350),
        label = "bottomBarAccent"
    )
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.94f else 0.97f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = animatedAccent,
                        selectedTextColor = animatedAccent,
                        indicatorColor = animatedAccent.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                )
            }
        }
    }
}
