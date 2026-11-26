package com.als.presence.ui.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.domain.model.Role
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.auth.components.AuthRoleSelector
import com.als.presence.ui.auth.components.GoogleSignInButton
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceCard
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel

/**
 * Premium Authentication Screen for Owner and Employee roles.
 *
 * Implements:
 * - Section A.2: Role selection remains visible before authentication
 * - Section A.3: Email OTP and Google Sign-In methods
 * - Section A.4: Email input, rate limiting feedback
 * - Section C: Educational employee invitation info; never displays employee registration button
 */
@Composable
fun AuthEmailScreen(
    role: Role,
    onRoleChange: (Role) -> Unit,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onStartRegistration: () -> Unit,
    onOpenActivationGuide: () -> Unit,
    onBack: () -> Unit,
    uiState: UiState<*>,
    rateLimitCountdown: Int = 0,
    isSendingOtp: Boolean,
    isGoogleSigningIn: Boolean,
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo_presence),
                                contentDescription = "Presence Logo",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Presence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(38.dp))
            }

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (role == Role.OWNER)
                            "Manage organisation rosters, geofenced premises & approve ledger."
                        else
                            "Clock in with Face + GPS, track shifts & view your earned salary ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                // 1. Role Selection (Visible before authentication)
                PresenceMicroLabel(text = "Choose Your Role Context")
                AuthRoleSelector(
                    selectedRole = role,
                    onRoleSelected = onRoleChange
                )

                // 2. Main Auth Card (Email OTP & Google SSO)
                PresenceCard(
                    elevation = 2.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PresenceMicroLabel(text = "Authenticate with Business Email")

                        // Email Input Field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address") },
                            placeholder = { Text("name@workplace.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            isError = uiState is UiState.Error,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Rate Limit Warning Banner
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
                                        text = "Rate limit active. Please wait ${rateLimitCountdown}s before requesting code.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        // Error Banner / Contextual Prompt
                        if (uiState is UiState.Error) {
                            val errMsg = uiState.message
                            val isUnregisteredOwner = role == Role.OWNER && (
                                errMsg.contains("unregistered", ignoreCase = true) ||
                                errMsg.contains("not found", ignoreCase = true) ||
                                errMsg.contains("account required", ignoreCase = true)
                            )
                            val isUninvitedEmp = role == Role.EMPLOYEE && (
                                errMsg.contains("unregistered", ignoreCase = true) ||
                                errMsg.contains("not registered", ignoreCase = true) ||
                                errMsg.contains("invitation", ignoreCase = true) ||
                                errMsg.contains("account required", ignoreCase = true)
                            )

                            Surface(
                                color = if (isUninvitedEmp) PresenceDesign.CautionSoft else PresenceDesign.DangerSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isUninvitedEmp)
                                            "Your employee account has not been created yet. Please contact your organisation Owner to receive an invitation."
                                        else if (isUnregisteredOwner)
                                            "No Owner account exists for this email address."
                                        else errMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUninvitedEmp) PresenceDesign.CautionText else PresenceDesign.DangerText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )

                                    if (isUnregisteredOwner) {
                                        PresenceButton(
                                            text = "Start Owner Registration",
                                            onClick = onStartRegistration,
                                            variant = PresenceVariant.Primary,
                                            size = PresenceSize.Sm,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Continue with Email OTP Button
                        PresenceButton(
                            text = "Continue with Email OTP",
                            onClick = onSendOtp,
                            variant = PresenceVariant.Primary,
                            size = PresenceSize.Lg,
                            loading = isSendingOtp,
                            enabled = rateLimitCountdown == 0 && !isSendingOtp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "OR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Google SSO Button
                        GoogleSignInButton(
                            onClick = onGoogleSignIn,
                            loading = isGoogleSigningIn,
                            text = "Continue with Google"
                        )
                    }
                }

                // 3. Bottom Contextual Card (Owner Registration vs Employee Activation Guide)
                if (role == Role.OWNER) {
                    PresenceCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        elevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "New Organisation? Register as Owner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "Setup your business organisation, configure verified premises geofences, and invite workforce employees.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                            PresenceButton(
                                text = "Register New Organisation",
                                onClick = onStartRegistration,
                                variant = PresenceVariant.Primary,
                                size = PresenceSize.Sm,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Employee Context Notice: Never display an employee registration button!
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Employee Workplace Sign-In",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Employees cannot self-register. Accounts are created and invited exclusively by your Organisation Owner.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                            PresenceButton(
                                text = "How Employee Activation Works",
                                onClick = onOpenActivationGuide,
                                variant = PresenceVariant.Ghost,
                                size = PresenceSize.Sm,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
