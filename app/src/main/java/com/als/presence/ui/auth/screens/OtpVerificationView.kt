package com.als.presence.ui.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.AuthDarkAccent
import com.als.presence.core.designsystem.AuthDarkHeroEnd
import com.als.presence.core.designsystem.AuthDarkHeroStart
import com.als.presence.core.designsystem.AuthLightAccent
import com.als.presence.core.designsystem.AuthLightHeroEnd
import com.als.presence.core.designsystem.AuthLightHeroStart
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.domain.model.Role
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.auth.components.OtpInputView
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton
import kotlinx.coroutines.delay

/**
 * Dedicated, ultra-premium OTP Verification Screen.
 * Implements Section A.4:
 * - 6-digit OTP verification with active cell highlighting
 * - Automatic code autofill & submit
 * - Resend countdown timer
 * - Prominent "Change Email" action
 * - Expired, incorrect, and rate-limiting error feedback
 */
@Composable
fun OtpVerificationView(
    email: String,
    role: Role,
    otpValue: String,
    devOtp: String?,
    uiState: UiState<*>,
    rateLimitCountdown: Int,
    onOtpChange: (String) -> Unit,
    onOtpSubmit: (String) -> Unit,
    onResendOtp: () -> Unit,
    onChangeEmail: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val heroStart = if (isDark) AuthDarkHeroStart else AuthLightHeroStart
    val heroEnd = if (isDark) AuthDarkHeroEnd else AuthLightHeroEnd
    val accentColor = if (isDark) AuthDarkAccent else AuthLightAccent

    val heroGradient = Brush.verticalGradient(listOf(heroStart, heroEnd))

    // Local 60-second resend countdown timer
    var resendTimer by remember { mutableIntStateOf(60) }

    LaunchedEffect(resendTimer) {
        if (resendTimer > 0) {
            delay(1000L)
            resendTimer -= 1
        }
    }

    AuthAtmosphericBackground(modifier = modifier, isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. Central Verification Card Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Email Verification Icon Hero Badge
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.35f)),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(heroGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Headings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Check Your Email",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "We sent a 6-digit verification code to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Target Email Pill with Change Email Action
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .clickable(onClick = onChangeEmail),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change email",
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Rate Limit Alert Banner
                AnimatedVisibility(
                    visible = rateLimitCountdown > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Rate Limit Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    fontSize = 12.5.sp
                                )
                                Text(
                                    text = "Too many OTP requests. Please wait ${rateLimitCountdown}s before requesting a new code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                // Inline Error / Expired Banner
                if (uiState is UiState.Error) {
                    val errMsg = (uiState as UiState.Error).message
                    val isExpired = errMsg.contains("expired", ignoreCase = true)
                    Surface(
                        color = PresenceDesign.DangerSoft,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PresenceDesign.Danger.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PresenceDesign.Danger,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = if (isExpired) "Code Expired" else "Verification Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PresenceDesign.DangerText,
                                    fontSize = 12.5.sp
                                )
                                Text(
                                    text = if (isExpired) "This 6-digit code has expired. Please request a new code below." else errMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PresenceDesign.DangerText,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                // 6-digit OTP Box Input with Dev OTP auto-fill badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OtpInputView(
                            otpValue = otpValue,
                            onOtpChange = onOtpChange,
                            onOtpSubmit = onOtpSubmit,
                            onResendClick = {
                                resendTimer = 60
                                onResendOtp()
                            },
                            devOtp = devOtp,
                            isSubmitting = uiState is UiState.Loading,
                            resendSeconds = resendTimer
                        )
                    }
                }
            }

            // 3. Bottom Action Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresenceButton(
                    text = "Verify & Access Workspace",
                    onClick = {
                        if (otpValue.length == 6) {
                            onOtpSubmit(otpValue)
                        }
                    },
                    variant = PresenceVariant.Primary,
                    size = PresenceSize.Lg,
                    loading = uiState is UiState.Loading,
                    enabled = otpValue.length == 6 && uiState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                PresenceButton(
                    text = "Change Email Address",
                    onClick = onChangeEmail,
                    variant = PresenceVariant.Ghost,
                    size = PresenceSize.Md,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
