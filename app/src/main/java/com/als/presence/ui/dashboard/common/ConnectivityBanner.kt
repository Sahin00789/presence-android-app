package com.als.presence.ui.dashboard.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * WhatsApp/Facebook-style network status banner that slides down right below the top bar
 * when the device loses internet connection or cannot reach the backend server,
 * and shows a brief "Back online" green banner when connection is restored before sliding away.
 */
@Composable
fun ConnectivityBanner(
    isOnline: Boolean,
    isServerConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    var wasOffline by remember { mutableStateOf(false) }
    var showBackOnline by remember { mutableStateOf(false) }

    val hasConnectionIssue = !isOnline || !isServerConnected

    LaunchedEffect(hasConnectionIssue) {
        if (hasConnectionIssue) {
            wasOffline = true
            showBackOnline = false
        } else if (wasOffline) {
            showBackOnline = true
            delay(2500) // Display "Back online" banner for 2.5s then slide out
            showBackOnline = false
            wasOffline = false
        }
    }

    val isVisible = hasConnectionIssue || showBackOnline

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        val backgroundColor = if (hasConnectionIssue) {
            Color(0xFFD32F2F) // Caution Red / Amber background
        } else {
            Color(0xFF2E7D32) // Success Green background
        }

        val icon = when {
            !isOnline -> Icons.Default.WifiOff
            !isServerConnected -> Icons.Default.CloudOff
            else -> Icons.Default.Wifi
        }

        val message = when {
            !isOnline -> "No internet connection. Waiting for network…"
            !isServerConnected -> "Unable to connect to server. Retrying…"
            else -> "Back online"
        }

        Surface(
            color = backgroundColor,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}
