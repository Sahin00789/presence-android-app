package com.als.presence.ui.dashboard.owner.modals

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.als.presence.ui.dashboard.common.PhotoUploadBox
import com.als.presence.ui.dashboard.common.processImageUriWithProgress
import com.als.presence.core.common.UiState
import com.als.presence.data.model.OrganizationDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.uriToBase64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrganizationModal(
    user: User,
    updateOrgState: UiState<OrganizationDto>,
    onDismiss: () -> Unit,
    onSaveOrg: (name: String?, timezone: String?, currency: String?, logoBase64: String?) -> Unit,
    onResetState: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var orgName by remember { mutableStateOf(user.organizationName.orEmpty()) }
    var timezone by remember { mutableStateOf("Asia/Kolkata") }
    var currency by remember { mutableStateOf("INR") }
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var logoBase64 by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLogoUri = uri
            coroutineScope.launch {
                isUploading = true
                uploadProgress = 0
                val b64 = processImageUriWithProgress(context, uri) { progress ->
                    uploadProgress = progress
                }
                logoBase64 = b64
                isUploading = false
            }
        }
    }

    LaunchedEffect(updateOrgState) {
        if (updateOrgState is UiState.Success) {
            Toast.makeText(context, "Organization details updated!", Toast.LENGTH_SHORT).show()
            onResetState()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Organization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logo Picker Section with Cloudinary percentage loader
            PhotoUploadBox(
                model = selectedLogoUri ?: user.organizationLogoUrl,
                isUploading = isUploading,
                uploadProgress = uploadProgress,
                onSelectImage = { galleryLauncher.launch("image/*") },
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(18.dp),
                placeholderText = user.organizationName.orEmpty().take(1).uppercase().ifBlank { "O" },
                helperText = "Tap box to upload company logo"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Organization Name Input
            OutlinedTextField(
                value = orgName,
                onValueChange = { orgName = it },
                label = { Text("Organization Name") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Business, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timezone Input
            OutlinedTextField(
                value = timezone,
                onValueChange = { timezone = it },
                label = { Text("Timezone (e.g. Asia/Kolkata)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Currency Input
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase() },
                label = { Text("Currency Code (e.g. INR, USD)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (updateOrgState is UiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = updateOrgState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveOrg(
                        orgName.trim().ifBlank { null },
                        timezone.trim().ifBlank { null },
                        currency.trim().ifBlank { null },
                        logoBase64
                    )
                },
                enabled = updateOrgState !is UiState.Loading && orgName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (updateOrgState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Organization Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
