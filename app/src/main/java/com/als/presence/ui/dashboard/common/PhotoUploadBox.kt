package com.als.presence.ui.dashboard.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reusable Photo Upload Box component featuring progress percentage loader overlay,
 * Cloudinary branding, smooth transitions, and success state.
 */
@Composable
fun PhotoUploadBox(
    model: Any?,
    isUploading: Boolean,
    uploadProgress: Int,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    placeholderText: String? = null,
    helperText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (uploadProgress == 100) Color(0xFF10B981) else borderColor, shape)
                .clickable(enabled = !isUploading, onClick = onSelectImage)
        ) {
            // Background Image / Placeholder
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "Photo Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop
                )
            } else if (!placeholderText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = placeholderText.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Uploading Overlay with Circular Loader and Percentage
            androidx.compose.animation.AnimatedVisibility(
                visible = isUploading || (uploadProgress in 1..99),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (uploadProgress.coerceIn(0, 100)) / 100f },
                                modifier = Modifier.size(44.dp),
                                color = Color(0xFF38BDF8),
                                trackColor = Color.White.copy(alpha = 0.25f),
                                strokeWidth = 3.5.dp
                            )
                            Text(
                                text = "$uploadProgress%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cloudinary",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Upload Success Badge (100%)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isUploading && uploadProgress == 100,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xBB000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Uploaded",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Uploaded!",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }

            // Camera Edit Badge Icon on Bottom Right (when not uploading or completed)
            if (!isUploading && uploadProgress != 100) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload Photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!helperText.isNullOrBlank()) {
            Text(
                text = if (isUploading) "Uploading... $uploadProgress%" else if (uploadProgress == 100) "Uploaded to Cloudinary" else helperText,
                style = MaterialTheme.typography.labelSmall,
                color = if (uploadProgress == 100) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = if (uploadProgress == 100) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Dedicated Photo Upload Modal for selecting from gallery or taking a photo with camera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoUploadModal(
    selectedImageUri: Uri?,
    currentAvatarUrl: String?,
    placeholderText: String? = null,
    onDismiss: () -> Unit,
    onPhotoSelected: (Uri?, String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempImageUri by remember { mutableStateOf<Uri?>(selectedImageUri) }
    var tempBase64 by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tempImageUri = uri
            coroutineScope.launch {
                isUploading = true
                uploadProgress = 0
                val b64 = processImageUriWithProgress(context, uri) { progress ->
                    uploadProgress = progress
                }
                tempBase64 = b64
                isUploading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                isUploading = true
                uploadProgress = 0
                val b64 = processBitmapWithProgress(bitmap) { progress ->
                    uploadProgress = progress
                }
                tempBase64 = b64
                tempImageUri = null // Base64 used directly for camera bitmap
                isUploading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Upload Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Choose gallery image or take a quick photo",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF3F4F6))

            // Main Large Photo Preview Box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .border(3.dp, Color(0xFF2563EB), CircleShape)
            ) {
                val previewModel = tempImageUri ?: currentAvatarUrl
                if (previewModel != null) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = "Photo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!placeholderText.isNullOrBlank()) {
                    Text(
                        text = placeholderText.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(54.dp)
                    )
                }

                // Uploading Loader
                androidx.compose.animation.AnimatedVisibility(
                    visible = isUploading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = { (uploadProgress.coerceIn(0, 100)) / 100f },
                                modifier = Modifier.size(40.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 3.5.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$uploadProgress%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Option Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gallery Option Card
                Surface(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Browse photos stored on your device",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Camera Option Card
                Surface(
                    onClick = { cameraLauncher.launch() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF15803D)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Take Photo with Camera",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Capture a quick portrait photo now",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                if (tempImageUri != null || !currentAvatarUrl.isNullOrBlank() || tempBase64 != null) {
                    TextButton(
                        onClick = {
                            tempImageUri = null
                            tempBase64 = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Remove Photo",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = Color(0xFF4B5563))
                }

                Button(
                    onClick = {
                        onPhotoSelected(tempImageUri, tempBase64)
                        onDismiss()
                    },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Photo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reads an image Uri, calculates byte stream conversion progress (0-100%),
 * and returns base64 encoded image string.
 */
suspend fun processImageUriWithProgress(
    context: Context,
    uri: Uri,
    onProgress: (Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        onProgress(10)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val totalBytes = inputStream.available().coerceAtLeast(1)
        val buffer = ByteArray(8192)
        val outputStream = ByteArrayOutputStream()
        var bytesRead: Int
        var readSoFar = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            readSoFar += bytesRead
            val percent = 10 + ((readSoFar.toDouble() / totalBytes.toDouble()) * 40).toInt().coerceIn(0, 40)
            onProgress(percent)
        }
        inputStream.close()
        val bytes = outputStream.toByteArray()

        onProgress(55)
        delay(60)
        onProgress(75)

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        onProgress(90)
        delay(60)
        onProgress(100)

        base64
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Encodes a Bitmap directly to Base64 with progress updates.
 */
suspend fun processBitmapWithProgress(
    bitmap: Bitmap,
    onProgress: (Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        onProgress(20)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        onProgress(60)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        onProgress(100)
        base64
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
