package com.als.presence.ui.dashboard.owner.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.PresenceAvatar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceDetailRow
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant

@Composable
fun OrganizationSettingsScreen(
    user: User,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditOrgClick: () -> Unit
) {
    PresenceSecondaryScaffold(
        crumb = "Organization",
        title = "Organization & Owner",
        onBack = onBack
    ) {
        PresencePageIntro(
            eyebrow = "Tenant & Profile",
            title = "Owner & Organization Settings",
            description = "View your owner account details, organization configuration, time zone, currency, and branding logo."
        )

        PresenceMetaStrip(
            items = listOf(
                "Organization" to (user.organizationName ?: "Organization"),
                "Role" to user.role.name,
                "Currency" to "INR"
            )
        )

        // Owner Profile Section
        PresenceSectionTitle(
            title = "Owner Account",
            description = "Your personal profile and credentials.",
            action = {
                PresenceButton(
                    label = "Edit Profile",
                    onClick = onEditProfileClick,
                    variant = PresenceVariant.Soft,
                    size = PresenceSize.Sm,
                    icon = Icons.Default.Edit
                )
            }
        )

        PresenceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!user.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.fullName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PresenceAvatar(name = user.fullName, modifier = Modifier.size(56.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PresenceBadge(label = user.role.name, tone = PresenceTone.Purple)
                }
                Spacer(modifier = Modifier.height(14.dp))
                PresenceDetailRow(label = "Email Address", value = user.email)
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "User ID", value = user.id)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Organization Section
        PresenceSectionTitle(
            title = "Organization Details",
            description = "Business name, currency, time zone and logo.",
            action = {
                PresenceButton(
                    label = "Edit Organization",
                    onClick = onEditOrgClick,
                    variant = PresenceVariant.Soft,
                    size = PresenceSize.Sm,
                    icon = Icons.Default.Edit
                )
            }
        )

        PresenceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!user.organizationLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.organizationLogoUrl,
                            contentDescription = user.organizationName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.organizationName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Primary Business Tenant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PresenceBadge(label = "Active", tone = PresenceTone.Success, dot = true)
                }
                Spacer(modifier = Modifier.height(14.dp))
                PresenceDetailRow(label = "Time Zone", value = "Asia/Kolkata (IST)")
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Currency", value = "INR (₹)")
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Organization ID", value = user.organizationId.orEmpty())
            }
        }
    }
}
