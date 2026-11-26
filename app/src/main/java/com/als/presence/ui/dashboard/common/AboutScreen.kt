package com.als.presence.ui.dashboard.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.domain.model.User
import com.als.presence.ui.auth.AuthViewModel

@Composable
fun AboutScreen(
    user: User,
    onBack: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var showLicensesModal by remember { mutableStateOf(false) }

    val appVersion = "1.0.11"
    val buildNumber = "1011"
    val environment = "Production"

    val handleCopyDiagnostics = {
        val diagInfo = """
            --- PRESENCE DIAGNOSTICS ---
            App: Presence v$appVersion ($buildNumber) | Env: $environment
            Org: ${user.organizationName ?: "N/A"} (${user.organizationId ?: "N/A"})
            Role: ${user.role.name} | User ID: ${user.id}
            Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
            Engine: ONNX Mobile v1.17+ | DB: Room SQLite
        """.trimIndent()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Presence Diagnostics", diagInfo)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    PresenceSecondaryScaffold(
        crumb = "System & Info",
        title = "About Presence",
        onBack = onBack
    ) {
        // 1. Compact Header Card
        PresenceCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PresenceDesign.BrandSoft)
                        .border(1.dp, PresenceDesign.Brand.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_presence),
                        contentDescription = "Presence Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Presence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PresenceBadge(
                            label = "v$appVersion",
                            tone = PresenceTone.Info
                        )
                    }
                    Text(
                        text = "Enterprise Workforce & Biometric System",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PresenceButton(
                    label = "Updates",
                    onClick = { authViewModel.checkForAppUpdates() },
                    variant = PresenceVariant.Primary,
                    size = PresenceSize.Sm,
                    icon = Icons.Default.SystemUpdate
                )
            }
        }

        // 2. Compact Developer & Support Section
        PresenceCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = PresenceDesign.Brand,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Developer Support",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Sahin00789",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactContactChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Email,
                        label = "sahin401099@gmail.com",
                        actionLabel = "Email",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:sahin401099@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Presence App Support Request")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Developer Email", "sahin401099@gmail.com"))
                                Toast.makeText(context, "Email copied: sahin401099@gmail.com", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    CompactContactChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "+919563401099",
                        actionLabel = "WhatsApp",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/919563401099?text=Hello%20Developer,%20I%20need%20support%20with%20Presence%20app")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("WhatsApp Number", "+919563401099"))
                                Toast.makeText(context, "WhatsApp copied: +919563401099", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // 3. Compact Device & System Specs Card
        PresenceCard {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device & Architecture Specs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PresenceButton(
                        label = "Copy Specs",
                        onClick = { handleCopyDiagnostics() },
                        variant = PresenceVariant.Ghost,
                        size = PresenceSize.Sm,
                        icon = Icons.Default.ContentCopy
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                CompactSpecRow(label = "Hardware", value = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}")
                CompactSpecRow(label = "Android OS", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                CompactSpecRow(label = "Biometric Engine", value = "ONNX Runtime Mobile v1.17+")
                CompactSpecRow(label = "GPS Engine", value = "Google Fused Location API")
                CompactSpecRow(label = "Database & Security", value = "Room SQLite • Android Keystore")
                CompactSpecRow(label = "Tenant / Role", value = "${user.organizationName ?: "N/A"} (${user.role.name})")
            }
        }

        // 4. Compact Footer (Licenses & Copyright)
        PresenceCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "© 2025 ALS Workforce & Sahin00789",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Licenses",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Brand,
                    modifier = Modifier.clickable { showLicensesModal = true }
                )
            }
        }
    }

    if (showLicensesModal) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesModal = false })
    }
}

@Composable
private fun CompactContactChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PresenceDesign.Brand,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactSpecRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    val libraries = listOf(
        "Jetpack Compose & Material 3" to "Apache License 2.0 (Google LLC)",
        "ONNX Runtime Mobile" to "MIT License (Microsoft Corp)",
        "Hilt Dependency Injection" to "Apache License 2.0 (Google LLC)",
        "Retrofit 2 & OkHttp 4" to "Apache License 2.0 (Square, Inc.)",
        "Room DB & Coroutines" to "Apache License 2.0 (Google & JetBrains)",
        "Google Play Services Location" to "Google Play Services License",
        "Fastify 5 & PostgreSQL Drizzle" to "MIT License (Fastify & Drizzle)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PresenceDesign.BrandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = PresenceDesign.Brand,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Open Source Libraries",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Presence is built with the following open-source frameworks and SDKs:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                libraries.forEach { (name, license) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = license,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Brand
                )
            }
        },
        shape = RoundedCornerShape(PresenceDesign.RadiusLg),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
