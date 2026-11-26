package com.als.presence.ui.auth.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.designsystem.AuthDarkAccent
import com.als.presence.core.designsystem.AuthDarkHeroEnd
import com.als.presence.core.designsystem.AuthDarkHeroStart
import com.als.presence.core.designsystem.AuthDarkPrimaryText
import com.als.presence.core.designsystem.AuthDarkSecondaryText
import com.als.presence.core.designsystem.AuthLightAccent
import com.als.presence.core.designsystem.AuthLightHeroEnd
import com.als.presence.core.designsystem.AuthLightHeroStart
import com.als.presence.core.designsystem.AuthLightPrimaryText
import com.als.presence.core.designsystem.AuthLightSecondaryText
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton

/**
 * Ultra-Premium Welcome Screen for Presence.
 * Implements Section A.1 requirements: Presence branding, atmospheric gradient hero,
 * geometric background decoration, "Get Started" CTA, and "Existing Account / Sign In" action.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) AuthDarkPrimaryText else AuthLightPrimaryText
    val secondaryTextColor = if (isDark) AuthDarkSecondaryText else AuthLightSecondaryText
    val accentColor = if (isDark) AuthDarkAccent else AuthLightAccent

    val heroGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(AuthDarkHeroStart, AuthDarkHeroEnd)
        } else {
            listOf(AuthLightHeroStart, AuthLightHeroEnd)
        }
    )

    AuthAtmosphericBackground(modifier = modifier, isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Atmospheric Hero Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Logo Symbol
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        accentColor.copy(alpha = 0.35f)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(heroGradient)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_presence),
                            contentDescription = stringResource(id = R.string.presence_brand_name),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Name & Tagline
                Text(
                    text = stringResource(id = R.string.presence_brand_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Workforce Attendance, Shifts & Salary Ledger",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Value Proposition Feature Pills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeaturePill(
                        icon = Icons.Default.Face,
                        title = "Face & GPS Geofence Verification",
                        subtitle = "High-accuracy attendance with on-device biometrics",
                        accentColor = accentColor
                    )
                    FeaturePill(
                        icon = Icons.Default.CalendarMonth,
                        title = "Dynamic Shifts & Live GPS Monitoring",
                        subtitle = "Premises rosters with active-shift location checks",
                        accentColor = Color(0xFF0D9488)
                    )
                    FeaturePill(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Append-Only Whole-INR Ledger",
                        subtitle = "Precise salary accruals and verified monthly payrolls",
                        accentColor = Color(0xFFD97706)
                    )
                }
            }

            // 2. Bottom Action Buttons & Enterprise Badge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Get Started Button (Primary)
                PresenceButton(
                    text = "Get Started",
                    onClick = onGetStarted,
                    variant = PresenceVariant.Primary,
                    size = PresenceSize.Lg,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )

                // Existing Account / Sign In Action (Secondary)
                PresenceButton(
                    text = "I Have an Account · Sign In",
                    onClick = onSignIn,
                    variant = PresenceVariant.Soft,
                    size = PresenceSize.Lg,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Single Device & Enterprise Security Micro Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Enterprise Multi-Tenant Security · Single Active Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
