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
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import com.als.presence.ui.dashboard.owner.modals.EditPremisesModal
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
import com.als.presence.ui.dashboard.common.PresenceDetailRow
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.owner.OwnerWorkforceViewModel
import com.als.presence.ui.dashboard.owner.modals.AddPremisesModal

/**
 * "Manage Premises" management screen. Lists the geofenced premises of the
 * organization with the exact coordinates and radius the backend enforces.
 */
@Composable
fun PremisesManagementScreen(
    onBack: () -> Unit,
    canCreate: Boolean,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val premisesState by viewModel.premises.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) activeMessage = message
    }

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<PremisesDto?>(null) }

    val premises = (premisesState as? UiState.Success)?.data.orEmpty()

    Box {
        PresenceSecondaryScaffold(
            crumb = "Workforce",
            title = "Manage Premises",
            onBack = onBack,
            actions = {
                PresenceIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh premises",
                    onClick = viewModel::refresh
                )
            }
        ) {
            PresencePageIntro(
                eyebrow = "Geofencing",
                title = "Premises Management",
                description = "Configure work locations and geofence radii for automated GPS clock-in verification."
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
                description = "Active work premises enforced during clock-in.",
                action = {
                    if (canCreate) {
                        PresenceButton(
                            label = "Add premises",
                            onClick = { showAdd = true },
                            variant = PresenceVariant.Soft,
                            size = PresenceSize.Sm,
                            icon = Icons.Default.AddLocation,
                            loading = isSubmitting
                        )
                    }
                }
            )

            when (val state = premisesState) {
                is UiState.Idle, is UiState.Loading -> PresenceLoading("Loading the geofence centres")
                is UiState.Error -> PresenceErrorState(message = state.message, onRetry = viewModel::refresh)
                is UiState.Success -> {
                    if (premises.isEmpty()) {
                        PresenceEmptyState(
                            icon = Icons.Default.Business,
                            title = "No premises configured",
                            description = "Add a work location with latitude, longitude and radius so staff can clock in within the geofence."
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

    if (showAdd) {
        AddPremisesModal(
            onDismiss = { showAdd = false },
            onAddPremises = { name, lat, lng, radius ->
                viewModel.addPremises(name, lat, lng, radius)
                showAdd = false
            }
        )
    }

    editTarget?.let { target ->
        EditPremisesModal(
            premises = target,
            onDismiss = { editTarget = null },
            onUpdatePremises = { id, name, lat, lng, radius, isActive ->
                viewModel.updatePremises(id, name, lat, lng, radius, isActive)
                editTarget = null
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
    val parts = premises.name.split(",", limit = 2)
    val nameOnly = parts[0].trim()
    val addressOnly = if (parts.size > 1) parts[1].trim() else null

    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PresenceDesign.BrandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = PresenceDesign.Brand,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameOnly,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                    if (addressOnly != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = PresenceDesign.Brand
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = addressOnly,
                                fontSize = 12.sp,
                                color = PresenceDesign.LabelText
                            )
                        }
                    }
                }
                PresenceBadge(
                    label = if (premises.isActive) "Active" else "Inactive",
                    tone = if (premises.isActive) PresenceTone.Success else PresenceTone.Warning,
                    dot = true
                )
                if (canEdit) {
                    Spacer(modifier = Modifier.width(4.dp))
                    PresenceIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit premises ${premises.name}",
                        onClick = onEdit
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PresenceDetailRow(label = "Latitude", value = premises.latitude)
                PresenceDetailRow(label = "Longitude", value = premises.longitude)
                PresenceDetailRow(
                    label = "Geofence radius",
                    value = "${premises.radiusMeters} m",
                    valueColor = PresenceDesign.BrandDark
                )
            }
        }
    }
}
