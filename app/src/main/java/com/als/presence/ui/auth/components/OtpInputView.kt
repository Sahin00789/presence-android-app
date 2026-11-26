package com.als.presence.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import kotlinx.coroutines.delay

/**
 * 6-digit PIN Box Input View with active cell highlighting,
 * tap-to-fill Dev OTP badge, and resend countdown timer.
 */
@Composable
fun OtpInputView(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    onOtpSubmit: (String) -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
    devOtp: String? = null,
    isSubmitting: Boolean = false,
    resendSeconds: Int = 60
) {
    var timerSeconds by remember { mutableIntStateOf(resendSeconds) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Dev Mode Helper Badge (Tap-To-Fill)
        if (!devOtp.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val cleanDevOtp = devOtp.filter { it.isDigit() }.take(6)
                        if (cleanDevOtp.isNotBlank()) {
                            onOtpChange(cleanDevOtp)
                            onOtpSubmit(cleanDevOtp)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DEV OTP: $devOtp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "(Tap to Auto-Fill)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Hidden BasicTextField overlaid on 6 OTP Cell Boxes
        BasicTextField(
            value = otpValue,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(6)
                onOtpChange(digits)
                if (digits.length == 6) {
                    onOtpSubmit(digits)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val char = otpValue.getOrNull(index)?.toString() ?: ""
                        val isFocused = (otpValue.length == index) || (index == 5 && otpValue.length == 6)
                        val isFilled = char.isNotEmpty()

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isFocused -> MaterialTheme.colorScheme.primary
                                isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "otpBorderColor"
                        )

                        val cellBgColor by animateColorAsState(
                            targetValue = when {
                                isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                isFilled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            label = "otpCellBgColor"
                        )

                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(cellBgColor)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }
        )

        // Resend Timer Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (timerSeconds > 0) {
                Text(
                    text = "Resend OTP in ${timerSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Didn't receive the code? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        timerSeconds = resendSeconds
                        onResendClick()
                    }
                )
            }
        }
    }
}
