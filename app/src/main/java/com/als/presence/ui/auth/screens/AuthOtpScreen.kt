package com.als.presence.ui.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.auth.components.OtpInputView
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceCard

/**
 * Dedicated Full-Screen OTP Verification Screen.
 * Features email badge with "Change email" action, interactive 6-digit OTP input with
 * active cell highlighting, dev OTP tap-to-fill helper, resend timer, rate limit feedback,
 * and explicit expired/invalid error states.
 */
@Composable
fun AuthOtpScreen(
    email: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onChangeEmail: () -> Unit,
    uiState: UiState<*>,
    devOtp: String?,
    rateLimitCountdown: Int = 0,
    isVerifying: Boolean,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onChangeEmail) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "OTP Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                }
            }

            // Central OTP Card Container
            PresenceCard(
                elevation = 3.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Lock Icon Badge
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Headline & Email Chip
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Enter Verification Code",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "We sent a 6-digit OTP code to:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(onClick = onChangeEmail)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Rate Limit Banner
                    AnimatedVisibility(
                        visible = rateLimitCountdown > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Rate limit active. Please wait ${rateLimitCountdown}s before requesting a new OTP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    // Error Banner (Invalid/Expired OTP)
                    if (uiState is UiState.Error) {
                        val errMsg = uiState.message
                        val isExpired = errMsg.contains("expired", ignoreCase = true)
                        Surface(
                            color = PresenceDesign.DangerSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = PresenceDesign.Danger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isExpired) "Code has expired. Tap 'Resend OTP' below." else errMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PresenceDesign.DangerText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Interactive 6-Digit OTP Box View with automatic autofill
                    OtpInputView(
                        otpValue = otpCode,
                        onOtpChange = onOtpChange,
                        onOtpSubmit = onVerifyOtp,
                        onResendClick = onResendOtp,
                        devOtp = devOtp,
                        isSubmitting = isVerifying
                    )

                    // Verify Button CTA
                    PresenceButton(
                        text = "Verify & Access Workspace",
                        onClick = {
                            if (otpCode.length == 6) {
                                onVerifyOtp(otpCode)
                            }
                        },
                        variant = PresenceVariant.Primary,
                        size = PresenceSize.Lg,
                        loading = isVerifying,
                        enabled = otpCode.length == 6 && !isVerifying,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom Helper Text
            Text(
                text = "Didn't receive code? Please check your spam or junk folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
