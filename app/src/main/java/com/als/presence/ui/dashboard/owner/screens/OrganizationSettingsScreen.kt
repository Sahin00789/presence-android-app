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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.auth.AuthViewModel
import com.als.presence.ui.dashboard.common.EditProfileModal
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
import com.als.presence.ui.dashboard.owner.modals.EditOrganizationModal

@Composable
fun OrganizationSettingsScreen(
    user: User,
    onBack: () -> Unit,
    onEditProfileClick: (() -> Unit)? = null,
    onEditOrgClick: (() -> Unit)? = null,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var localShowProfileModal by remember { mutableStateOf(false) }
    var localShowOrgModal by remember { mutableStateOf(false) }

    val updateProfileState by authViewModel.updateProfileState.collectAsStateWithLifecycle()
    val updateOrgState by authViewModel.updateOrgState.collectAsStateWithLifecycle()

    val handleOpenProfile = {
        if (onEditProfileClick != null) {
            onEditProfileClick()
        } else {
            localShowProfileModal = true
        }
    }

    val handleOpenOrg = {
        if (onEditOrgClick != null) {
            onEditOrgClick()
        } else {
            localShowOrgModal = true
        }
    }

    PresenceSecondaryScaffold(
        crumb = "Account & Settings",
        title = "Organization Settings",
        onBack = onBack
    ) {
        PresencePageIntro(
            eyebrow = "Tenant & User Profile",
            title = "Organization Settings"
        )

        // 1. Personal Profile Section
        PresenceSectionTitle(
            title = "Personal Profile",
            action = {
                PresenceButton(
                    label = "Edit Profile",
                    onClick = { handleOpenProfile() },
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PresenceBadge(
                        label = user.role.name,
                        tone = when (user.role) {
                            Role.OWNER -> PresenceTone.Purple
                            Role.EMPLOYEE -> PresenceTone.Success
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                PresenceDetailRow(label = "Full Name", value = user.fullName)
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Email Address", value = user.email)
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Account Role", value = user.role.name)
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "User ID", value = user.id)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Organization Details Section
        PresenceSectionTitle(
            title = "Organization Details",
            description = "Business name, currency, time zone, and logo.",
            action = {
                if (user.role == Role.OWNER) {
                    PresenceButton(
                        label = "Edit Organization",
                        onClick = { handleOpenOrg() },
                        variant = PresenceVariant.Soft,
                        size = PresenceSize.Sm,
                        icon = Icons.Default.Edit
                    )
                }
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
                            text = user.organizationName.orEmpty().ifBlank { "Organization" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
                PresenceDetailRow(label = "Organization Name", value = user.organizationName.orEmpty().ifBlank { "N/A" })
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Time Zone", value = "Asia/Kolkata (IST)")
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Currency", value = "INR (₹)")
                Spacer(modifier = Modifier.height(8.dp))
                PresenceDetailRow(label = "Tenant ID", value = user.organizationId.orEmpty())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Security & Concurrency Rules Section
        PresenceSectionTitle(
            title = "Security & Access",
            description = "Authentication provider and session protection."
        )

        PresenceCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Single-Device Concurrency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active session enforced on 1 device at a time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Local Edit Profile Modal
    if (localShowProfileModal) {
        EditProfileModal(
            user = user,
            updateState = updateProfileState,
            onDismiss = { localShowProfileModal = false },
            onSaveProfile = { fullName, phone, avatarBase64 ->
                authViewModel.updateProfile(fullName, phone, avatarBase64)
            },
            onResetState = { authViewModel.resetUpdateProfileState() }
        )
    }

    // Local Edit Organization Modal
    if (localShowOrgModal) {
        EditOrganizationModal(
            user = user,
            updateOrgState = updateOrgState,
            onDismiss = { localShowOrgModal = false },
            onSaveOrg = { name, timezone, currency, logoBase64 ->
                authViewModel.updateOrganization(name, timezone, currency, logoBase64)
            },
            onResetState = { authViewModel.resetUpdateOrgState() }
        )
    }
}
