package com.als.presence.ui.auth.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.domain.model.Role
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.auth.components.AuthRoleSelector
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton

/**
 * Role Selection Screen for Presence Auth.
 * Heading: "How will you use Presence?"
 * Distinctive Owner (Blue/Lavender) and Employee (Teal/Green) panels with smooth selection.
 */
@Composable
fun RoleSelectionScreen(
    selectedRole: Role,
    onRoleSelected: (Role) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthAtmosphericBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_presence),
                            contentDescription = "Presence Logo",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Presence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(48.dp)) // Balance back button width
            }

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        text = "How will you use Presence?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select your workspace role. You can manage your organisation as an Owner or clock-in as an Employee.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                AuthRoleSelector(
                    selectedRole = selectedRole,
                    onRoleSelected = onRoleSelected
                )
            }

            // Bottom Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                PresenceButton(
                    text = if (selectedRole == Role.OWNER) "Continue as Owner" else "Continue as Employee",
                    onClick = onContinue,
                    variant = PresenceVariant.Primary,
                    size = PresenceSize.Lg,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
