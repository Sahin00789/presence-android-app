package com.als.presence.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.domain.model.Role

/**
 * Premium 2026 Role Selection Cards Component for Presence Auth.
 * Features distinctive Owner (Blue/Lavender) and Employee (Teal/Green) panel styling,
 * animated scale feedback, clear checkmark badges, and descriptive subtitles.
 */
@Composable
fun AuthRoleSelector(
    selectedRole: Role,
    onRoleSelected: (Role) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RoleCardPanel(
            title = "Organisation Owner",
            subtitle = "Setup organisation, manage premises geofences, roster shifts & approve payroll ledger.",
            icon = Icons.Default.BusinessCenter,
            isSelected = selectedRole == Role.OWNER,
            primaryColor = Color(0xFF4263EB),
            gradientStart = Color(0xFFDCEBFF),
            gradientEnd = Color(0xFFE9E3FF),
            darkGradientStart = Color(0xFF202E57),
            darkGradientEnd = Color(0xFF29244D),
            onClick = { onRoleSelected(Role.OWNER) }
        )

        RoleCardPanel(
            title = "Workforce Employee",
            subtitle = "Clock-in via Face + GPS verification, view shift schedules & track earned salary ledger.",
            icon = Icons.Default.PersonPin,
            isSelected = selectedRole == Role.EMPLOYEE,
            primaryColor = Color(0xFF0D9488),
            gradientStart = Color(0xFFDDF3E5),
            gradientEnd = Color(0xFFDDF2ED),
            darkGradientStart = Color(0xFF193B2A),
            darkGradientEnd = Color(0xFF173D39),
            onClick = { onRoleSelected(Role.EMPLOYEE) }
        )
    }
}

@Composable
private fun RoleCardPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    primaryColor: Color,
    gradientStart: Color,
    gradientEnd: Color,
    darkGradientStart: Color,
    darkGradientEnd: Color,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else if (isSelected) 1.01f else 1.0f,
        label = "rolePanelScale"
    )

    val activeGradient = Brush.horizontalGradient(
        colors = if (isDark) listOf(darkGradientStart, darkGradientEnd) else listOf(gradientStart, gradientEnd)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        label = "rolePanelBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) Modifier.background(activeGradient) else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Information
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
