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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.PremisesDto
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.owner.OwnerWorkforceViewModel
import com.als.presence.ui.dashboard.owner.modals.EditPremisesModal

/**
 * "Manage Premises" management screen. Lists the geofenced premises of the
 * organization with exact coordinates, radius, and reverse-geocoded address.
 */
@Composable
fun PremisesManagementScreen(
    onBack: () -> Unit,
    canCreate: Boolean,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val premisesState by viewModel.premises.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val locationAddressState by viewModel.locationAddressState.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) activeMessage = message
    }

    var editTarget by remember { mutableStateOf<PremisesDto?>(null) }

    val premises = (premisesState as? UiState.Success)?.data.orEmpty()

    Box {
        PresenceSecondaryScaffold(
            crumb = "Workforce",
            title = "Manage Premises",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            PresencePageIntro(
                eyebrow = "Geofencing",
                title = "Premises Management",
                description = "Configure work locations, addresses, and geofence radii for automated GPS clock-in verification."
            )

            PresenceMetaStrip(
                items = listOf(
                    "Premises" to premises.size.toString(),
                    "Active" to premises.count { it.isActive }.toString(),
                    "Default Radius" to "50 m"
                )
            )

            PresenceSectionTitle(
                title = "Geofenced locations",
                description = "Active work premises enforced during clock-in."
            )

            when (val state = premisesState) {
                is UiState.Idle, is UiState.Loading -> PresenceLoading("Loading the geofence centres")
                is UiState.Error -> PresenceErrorState(message = state.message, onRetry = viewModel::refresh)
                is UiState.Success -> {
                    if (premises.isEmpty()) {
                        PresenceEmptyState(
                            icon = Icons.Default.Business,
                            title = "No premises configured",
                            description = "Existing work locations will appear here for geofence editing."
                        )
                    } else {
                        premises.forEach { p ->
                            PremisesManagementCard(
                                premises = p,
                                canEdit = canCreate,
                                onEdit = { editTarget = p }
                            )
                        }
                    }
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    editTarget?.let { target ->
        EditPremisesModal(
            premises = target,
            locationState = locationAddressState,
            onFetchGpsAndAddress = viewModel::fetchCurrentGpsAndAddress,
            onDismiss = {
                editTarget = null
                viewModel.clearLocationAddressState()
            },
            onUpdatePremises = { id, name, lat, lng, radius, isActive, address ->
                viewModel.updatePremises(id, name, lat, lng, radius, isActive, address)
                editTarget = null
                viewModel.clearLocationAddressState()
            }
        )
    }
}

@Composable
private fun PremisesManagementCard(
    premises: PremisesDto,
    canEdit: Boolean = false,
    onEdit: () -> Unit = {}
) {
    val nameOnly = premises.name.split(",", limit = 2)[0].trim()
    val formattedAddress = premises.address?.ifBlank { null }
        ?: premises.name.split(",", limit = 2).getOrNull(1)?.trim()
        ?: "Address not specified"

    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Badge Icon + Name + Badges + Edit
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PresenceDesign.BrandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = PresenceDesign.Brand,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameOnly,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PresenceBadge(
                            label = "Geofence: ${premises.radiusMeters} m",
                            tone = PresenceTone.Neutral
                        )
                        PresenceBadge(
                            label = if (premises.isActive) "Active" else "Inactive",
                            tone = if (premises.isActive) PresenceTone.Success else PresenceTone.Warning,
                            dot = true
                        )
                    }
                }
                if (canEdit) {
                    PresenceIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit premises ${premises.name}",
                        onClick = onEdit
                    )
                }
            }

            // Address Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PresenceDesign.NeutralSoft)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                        tint = PresenceDesign.Brand
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedAddress,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PresenceDesign.Ink,
                        lineHeight = 18.sp
                    )
                }
            }

            // Coordinates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PresenceDesign.Brand
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lat: ${premises.latitude} · Lng: ${premises.longitude}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PresenceDesign.LabelText
                    )
                }
            }
        }
    }
}
