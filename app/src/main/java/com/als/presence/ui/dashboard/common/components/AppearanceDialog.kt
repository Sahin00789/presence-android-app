package com.als.presence.ui.dashboard.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.designsystem.ThemeMode

@Composable
fun AppearanceDialog(
    currentMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose your preferred application theme:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThemeOptionRow(
                    label = "System Default",
                    description = "Follows your device system settings",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onSelectMode(ThemeMode.SYSTEM) }
                )

                ThemeOptionRow(
                    label = "Light Theme",
                    description = "Clean warm off-white surfaces",
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == ThemeMode.LIGHT,
                    onClick = { onSelectMode(ThemeMode.LIGHT) }
                )

                ThemeOptionRow(
                    label = "Dark Theme",
                    description = "Refined deep charcoal surfaces",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == ThemeMode.DARK,
                    onClick = { onSelectMode(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Brand
                )
            }
        },
        shape = RoundedCornerShape(PresenceDesign.RadiusLg),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) PresenceDesign.Brand else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = PresenceDesign.Brand)
        )
    }
}
