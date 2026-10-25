package com.als.presence.ui.dashboard.common

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.als.presence.core.face.FaceEmbeddingEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Full-screen CameraX capture flow. It produces a numeric embedding only:
 * the frame is never persisted or transmitted.
 */
@Composable
fun FaceCaptureDialog(
    engine: FaceEmbeddingEngine,
    mode: String = "VERIFY",
    onCapturedSingle: (FaceEmbeddingEngine.Embedding) -> Unit = {},
    onCapturedMulti: (Map<String, FaceEmbeddingEngine.Embedding>) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // Re-check permission when activity/screen resumes (e.g. returning from app settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val targetPoses = if (mode == "ENROLL") listOf("CENTER", "LEFT", "RIGHT", "UP", "DOWN") else listOf("CENTER")
    var currentPoseIndex by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("Position your face in the frame.") }
    val accumulatedEmbeddings = remember { mutableMapOf<String, FaceEmbeddingEngine.Embedding>() }
    
    // Automatically trigger callback if all poses are captured
    LaunchedEffect(currentPoseIndex) {
        if (currentPoseIndex >= targetPoses.size) {
            delay(500)
            if (mode == "ENROLL") {
                onCapturedMulti(accumulatedEmbeddings)
            } else {
                accumulatedEmbeddings["CENTER"]?.let { onCapturedSingle(it) }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(26.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (mode == "ENROLL") "Enrol your face" else "Verify your face",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (mode == "ENROLL") {
                    LinearProgressIndicator(
                        progress = { currentPoseIndex / targetPoses.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                    Text(
                        text = "$currentPoseIndex of ${targetPoses.size} poses captured",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Centre your face in the frame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(40.dp))
                            Text("Camera permission is required", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }) { Text("Grant camera access") }
                        }
                    }
                } else {
                    // Pass the auto-capture states down to the preview
                    FaceCameraPreview(
                        engine = engine,
                        targetPose = if (currentPoseIndex < targetPoses.size) targetPoses[currentPoseIndex] else "CENTER",
                        isDone = currentPoseIndex >= targetPoses.size,
                        onSuccess = { embedding, pose -> 
                            accumulatedEmbeddings[pose] = embedding
                            currentPoseIndex++
                            if (currentPoseIndex < targetPoses.size) {
                                feedbackMessage = "Great! Now look ${targetPoses[currentPoseIndex]}"
                            } else {
                                feedbackMessage = "All poses captured successfully!"
                            }
                        },
                        onFeedback = { feedbackMessage = it }
                    )
                }

                // Dynamic Instruction Guide
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackMessage,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun FaceCameraPreview(
    engine: FaceEmbeddingEngine,
    targetPose: String,
    isDone: Boolean,
    onSuccess: (FaceEmbeddingEngine.Embedding, String) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }

    val currentTargetPose by rememberUpdatedState(targetPose)
    val currentIsDone by rememberUpdatedState(isDone)
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnFeedback by rememberUpdatedState(onFeedback)

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    
                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    if (isCapturing || currentIsDone) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    
                    isCapturing = true
                    val rawBitmap = imageProxy.toBitmap()
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    imageProxy.close() // Close early to allow CameraX to recycle buffers

                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        if (rotated !== rawBitmap) rawBitmap.recycle()
                        rotated
                    } else {
                        rawBitmap
                    }
                    
                    val poseToCapture = currentTargetPose
                    scope.launch {
                        try {
                            val embedding = engine.captureSpecificPose(rotatedBitmap, poseToCapture)
                            currentOnSuccess(embedding, poseToCapture)
                        } catch (e: Exception) {
                            val message = e.message ?: "Adjust your face."
                            if (message.isNotBlank()) {
                                currentOnFeedback(message)
                            }
                        } finally {
                            if (!rotatedBitmap.isRecycled) {
                                rotatedBitmap.recycle()
                            }
                            delay(400) // Delay before next capture attempt
                            isCapturing = false
                        }
                    }
                }
                
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analysis
                )
            } catch (error: Exception) {
                currentOnFeedback("Camera unavailable: ${error.message ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            analyzerExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
    }
}
