package com.als.presence.ui.dashboard.common

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
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
                fontSize = 11.sp,
                fontWeight = if (uploadProgress == 100) FontWeight.SemiBold else FontWeight.Normal
            )
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
