package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign

/**
 * The frame every secondary screen sits in.
 *
 * A secondary screen is one the role kept out of the bottom row, so on a phone it
 * is reached through the hamburger rather than a tab. It is pushed as a full-bleed
 * overlay with its own header and back button; the bottom nav is hidden while it is
 * open. Backing out returns to the hamburger menu, so the user lands on the whole
 * navigation tree rather than the tab they happened to come from.
 */
@Composable
fun PresenceSecondaryScaffold(
    crumb: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PresenceDesign.Workspace)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PresenceDesign.Surface)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.width(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to navigation menu",
                    tint = PresenceDesign.NeutralText,
                    modifier = Modifier.height(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                PresenceKicker(crumb)
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = PresenceDesign.Ink,
                    maxLines = 1
                )
            }
            if (actions != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actions()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PresenceDesign.Line)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}
