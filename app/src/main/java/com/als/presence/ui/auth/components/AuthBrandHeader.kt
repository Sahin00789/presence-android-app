package com.als.presence.ui.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Premium Brand Hero Header component for Presence Authentication.
 * Features the official Presence Vector Logo and typography.
 */
@Composable
fun AuthBrandHeader(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PresenceVerticalLogo(
            isDarkTheme = isDarkTheme
        )
    }
}
