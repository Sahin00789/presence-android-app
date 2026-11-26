package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.graphicsLayer
import com.als.presence.core.common.UiState

data class SyncState(
    val isSyncing: Boolean = false,
    val onSyncClick: () -> Unit = {}
)

val LocalSyncState = staticCompositionLocalOf { SyncState() }

/**
 * TopBar Refresh Sync button that triggers Room DB synchronization from remote APIs.
 * Displays a continuous rotation animation while sync is in progress.
 */
@Composable
fun TopBarSyncRefreshButton(
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PresenceRefreshButton(
        onClick = onSyncClick,
        isRefreshing = isSyncing,
        contentDescription = "Sync Room Database",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Modern TopBar Server Icon component that indicates server connectivity status.
 * If server connection error occurs, the server icon is displayed with red alert styling and background.
 * When connected, it displays a healthy emerald status indicator.
 */
@Composable
fun TopBarServerIcon(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    isServerConnected: Boolean = true,
    onClick: () -> Unit = {}
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val hasServerErr = !isOnline || !isServerConnected
    val icon = if (hasServerErr) Icons.Default.CloudOff else Icons.Default.Dns
    val iconColor = if (hasServerErr) {
        if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444)
    } else {
        if (isDark) Color(0xFF34D399) else Color(0xFF0D9488)
    }
    val bgColor = if (hasServerErr) {
        if (isDark) Color(0xFF3B1219) else Color(0xFFFEF2F2)
    } else {
        if (isDark) Color(0xFF0D281E) else Color(0xFFE8F5E9)
    }
    val borderColor = if (hasServerErr) {
        if (isDark) Color(0xFF6B1B29) else Color(0xFFFCA5A5)
    } else {
        if (isDark) Color(0xFF1B5E43) else Color(0xFFA5D6A7)
    }
    val dotColor = iconColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (hasServerErr) "Server connection error. Tap to reload." else "Server connected and healthy. Tap to reload status.",
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 3.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

/**
 * Renders a [UiState] with consistent loading, error, empty and content states.
 * Every dashboard tab funnels through this so no screen can silently show stale
 * or placeholder data.
 */
@Composable
fun <T> AsyncContent(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onRetry: () -> Unit,
    isEmpty: (T) -> Boolean = { false },
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "Data will appear here as soon as it is recorded.",
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Idle, is UiState.Loading -> LoadingState(modifier = modifier, accent = accent)
        is UiState.Error -> ErrorState(
            message = state.message,
            modifier = modifier,
            onRetry = onRetry
        )
        is UiState.Success -> {
            if (isEmpty(state.data)) {
                EmptyState(
                    title = emptyTitle,
                    message = emptyMessage,
                    modifier = modifier
                )
            } else {
                content(state.data)
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading from server…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Could not reach the server",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

@Composable
fun StatusPill(status: String?, modifier: Modifier = Modifier) {
    val color = StatusStyle.color(status)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = StatusStyle.label(status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .border(
                0.75.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val initial = name.trim().take(1).ifBlank { "?" }.uppercase()
    val gradientColors = remember(initial) {
        val hash = kotlin.math.abs(initial.hashCode())
        when (hash % 5) {
            0 -> listOf(Color(0xFF2563EB), Color(0xFF7C3AED)) // Indigo -> Purple
            1 -> listOf(Color(0xFF059669), Color(0xFF0D9488)) // Emerald -> Teal
            2 -> listOf(Color(0xFFD97706), Color(0xFFEA580C)) // Amber -> Orange
            3 -> listOf(Color(0xFF0284C7), Color(0xFF2563EB)) // Sky -> Blue
            else -> listOf(Color(0xFF7C3AED), Color(0xFFDB2777)) // Violet -> Pink
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
fun InlineBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tone.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tone,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** Standard primary call-to-action used by the dashboards. */
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.Bold)
    }
}

/**
 * Lightweight transient banner for action results. Auto-dismisses so the
 * dashboards never accumulate stale confirmation text.
 */
@Composable
fun SectionLoading(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionError(text: String) {
    val isServerConnErr = text.contains("server", ignoreCase = true) ||
            text.contains("network", ignoreCase = true) ||
            text.contains("connect", ignoreCase = true) ||
            text.contains("timeout", ignoreCase = true)

    if (isServerConnErr) {
        Text(
            text = "Using cached offline data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun SectionEmpty(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun MessageSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    if (message.isNullOrBlank()) return

    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4_000)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 6.dp
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

