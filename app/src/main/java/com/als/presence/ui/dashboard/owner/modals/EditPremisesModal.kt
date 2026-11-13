package com.als.presence.ui.dashboard.owner.modals

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.PremisesDto
import com.als.presence.ui.dashboard.common.GpsAccuracyDialog
import com.als.presence.ui.dashboard.owner.viewmodels.PremisesLocationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPremisesModal(
    premises: PremisesDto,
    locationState: PremisesLocationState? = null,
    onFetchGpsAndAddress: () -> Unit = {},
    onGpsLocationAcquired: (GpsLocation) -> Unit = {},
    onDismiss: () -> Unit,
    onUpdatePremises: (
        premisesId: String,
        name: String,
        latitude: String,
        longitude: String,
        radiusMeters: Int,
        isActive: Boolean,
        address: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var premisesName by remember(premises) { mutableStateOf(premises.name) }
    var address by remember(premises) { mutableStateOf(premises.address.orEmpty()) }
    var latitude by remember(premises) { mutableStateOf(premises.latitude.toString()) }
    var longitude by remember(premises) { mutableStateOf(premises.longitude.toString()) }
    var radiusMeters by remember(premises) { mutableStateOf(premises.radiusMeters.toString()) }
    var isActive by remember(premises) { mutableStateOf(premises.isActive) }
    var showGpsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationState) {
        if (locationState != null && !locationState.isLoading && locationState.error == null) {
            if (locationState.latitude != 0.0) latitude = locationState.latitude.toString()
            if (locationState.longitude != 0.0) longitude = locationState.longitude.toString()
            locationState.address?.let { rev ->
                if (rev.streetAddress.isNotBlank()) address = rev.streetAddress
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Geofenced Premises",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val requestLocationPermission = com.als.presence.core.location.rememberLocationPermissionState(
                onPermissionGranted = { showGpsDialog = true }
            )

            if (showGpsDialog) {
                GpsAccuracyDialog(
                    title = "Calibrating Premises GPS",
                    targetAccuracyMeters = 10f,
                    onDismiss = { showGpsDialog = false },
                    onLocationAcquired = { gps ->
                        showGpsDialog = false
                        latitude = gps.latitude.toString()
                        longitude = gps.longitude.toString()
                        onGpsLocationAcquired(gps)
                    }
                )
            }

            OutlinedButton(
                onClick = requestLocationPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (locationState?.isLoading == true) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acquiring GPS location & address...")
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-fill Coordinates & Address via GPS", fontWeight = FontWeight.SemiBold)
                }
            }

            if (locationState?.error != null) {
                Text(
                    text = locationState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (latitude != "0.0") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(12.dp)
                ) {
                    val accuracyText = locationState?.accuracyMeters?.let { " (±${it.toInt()}m)" } ?: ""
                    Text(
                        text = "🎯 GPS Fix: Lat $latitude · Lng $longitude$accuracyText",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedTextField(
                value = premisesName,
                onValueChange = { premisesName = it },
                label = { Text("Premises Name") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Premises Address") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = radiusMeters,
                onValueChange = { radiusMeters = it.filter { ch -> ch.isDigit() } },
                label = { Text("Geofence Radius (metres)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Premises Active Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }

            Button(
                onClick = {
                    onUpdatePremises(
                        premises.id,
                        premisesName.trim(),
                        latitude,
                        longitude,
                        radiusMeters.toIntOrNull() ?: 50,
                        isActive,
                        address.trim().ifBlank { null }
                    )
                },
                enabled = premisesName.isNotBlank() && latitude != "0.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
