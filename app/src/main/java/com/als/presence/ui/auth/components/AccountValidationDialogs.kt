package com.als.presence.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.domain.model.Role
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton

/**
 * Dialog shown when authenticated role differs from selected login role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleMismatchDialog(
    selectedRole: Role,
    actualRole: Role,
    onSwitchRoleAndContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    ValidationAlertTemplate(
        icon = Icons.Default.SwapHoriz,
        iconTint = PresenceDesign.Brand,
        title = "Role Context Mismatch",
        message = "You selected the ${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }} context, but your account is registered as an ${actualRole.name.lowercase().replaceFirstChar { it.uppercase() }}.\n\nWould you like to switch to the ${actualRole.name.lowercase().replaceFirstChar { it.uppercase() }} workspace?",
        primaryButtonText = "Switch to ${actualRole.name.lowercase().replaceFirstChar { it.uppercase() }}",
        onPrimaryClick = onSwitchRoleAndContinue,
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss
    )
}

/**
 * Dialog shown when an unregistered email attempts to sign in as Employee.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnregisteredEmployeeDialog(
    email: String,
    onDismiss: () -> Unit
) {
    ValidationAlertTemplate(
        icon = Icons.Default.ErrorOutline,
        iconTint = Color(0xFFD97706),
        title = "Account Not Registered",
        message = "No employee record was found for '$email'.\n\nEmployees cannot self-register. Please contact your Organisation Owner or Admin to invite you to the workspace.",
        primaryButtonText = "Contact Owner",
        onPrimaryClick = onDismiss,
        secondaryButtonText = "Dismiss",
        onSecondaryClick = onDismiss
    )
}

/**
 * Dialog shown when an unregistered email attempts to sign in as Owner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnregisteredOwnerDialog(
    email: String,
    onStartRegistration: () -> Unit,
    onDismiss: () -> Unit
) {
    ValidationAlertTemplate(
        icon = Icons.Default.Business,
        iconTint = PresenceDesign.Brand,
        title = "No Owner Account Found",
        message = "No registered organisation owner account exists for '$email'.\n\nWould you like to register a new business organisation now?",
        primaryButtonText = "Register Organisation",
        onPrimaryClick = onStartRegistration,
        secondaryButtonText = "Try Another Email",
        onSecondaryClick = onDismiss
    )
}

/**
 * Dialog shown when account is deactivated on backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeactivatedDialog(
    onDismiss: () -> Unit
) {
    ValidationAlertTemplate(
        icon = Icons.Default.Block,
        iconTint = PresenceDesign.Danger,
        title = "Account Deactivated",
        message = "This account has been deactivated by the organisation administrator. Access to presence attendance and rosters is suspended.",
        primaryButtonText = "Understood",
        onPrimaryClick = onDismiss
    )
}

/**
 * Dialog shown when employee account has no assigned premises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePendingPremisesDialog(
    onDismiss: () -> Unit
) {
    ValidationAlertTemplate(
        icon = Icons.Default.LocationOff,
        iconTint = Color(0xFFD97706),
        title = "Premises Assignment Pending",
        message = "Your employee profile is active, but you have not yet been assigned to a workplace premises by your Organisation Owner.\n\nPlease contact your Owner to assign your work premises before clocking in.",
        primaryButtonText = "Understood",
        onPrimaryClick = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValidationAlertTemplate(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    BasicAlertDialog(
        onDismissRequest = { (onSecondaryClick ?: onPrimaryClick).invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    fontSize = 13.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (secondaryButtonText != null && onSecondaryClick != null) {
                        PresenceButton(
                            text = secondaryButtonText,
                            onClick = onSecondaryClick,
                            variant = PresenceVariant.Ghost,
                            size = PresenceSize.Md,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PresenceButton(
                        text = primaryButtonText,
                        onClick = onPrimaryClick,
                        variant = PresenceVariant.Primary,
                        size = PresenceSize.Md,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
