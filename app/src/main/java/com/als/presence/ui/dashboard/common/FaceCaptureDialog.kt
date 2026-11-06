package com.als.presence.ui.dashboard.common

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.als.presence.core.location.GpsLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * CameraX Face Verification & Enrollment Modal.
 * Security Feature:
 * Continuously fetches fresh high-accuracy GPS coordinates every 2s while modal is open,
 * ensuring clock-in security does not depend on stale or previously cached location fixes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceCaptureDialog(
    engine: FaceEmbeddingEngine,
    mode: String = "VERIFY",
    premisesName: String? = null,
    premisesLatitude: Double? = null,
    premisesLongitude: Double? = null,
    distanceMeters: Int? = null,
    accuracyMeters: Int? = null,
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

    // Continuous Live GPS fetching inside modal for clock-in security
    var liveLocation by remember { mutableStateOf<GpsLocation?>(null) }
    var liveDistanceMeters by remember { mutableStateOf<Int?>(distanceMeters) }
    var liveAccuracyMeters by remember { mutableStateOf<Int?>(accuracyMeters) }
    var isFetchingGps by remember { mutableStateOf(true) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        while (true) {
            isFetchingGps = true
            try {
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            liveLocation = GpsLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracyMeters = loc.accuracy,
                                isMocked = loc.isFromMockProvider
                            )
                            liveAccuracyMeters = loc.accuracy.toInt()
                            if (premisesLatitude != null && premisesLongitude != null) {
                                val dist = calculateDistanceMeters(loc.latitude, loc.longitude, premisesLatitude, premisesLongitude)
                                liveDistanceMeters = dist.toInt()
                            }
                        }
                        isFetchingGps = false
                    }
                }
            } catch (_: Exception) {
                isFetchingGps = false
            }
            delay(2000L) // Continuous live GPS refresh every 2s for clock in security
        }
    }

    val targetPoses = listOf("CENTER")
    var currentPoseIndex by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("Position your face in the camera.") }
    var isSuccess by remember { mutableStateOf(false) }

    val accumulatedEmbeddings = remember { mutableMapOf<String, FaceEmbeddingEngine.Embedding>() }

    LaunchedEffect(currentPoseIndex) {
        if (currentPoseIndex >= targetPoses.size) {
            isSuccess = true
            feedbackMessage = "Clock-In Verified Successfully!"
            delay(1000)
            if (mode == "ENROLL") {
                onCapturedMulti(accumulatedEmbeddings)
            } else {
                accumulatedEmbeddings["CENTER"]?.let { onCapturedSingle(it) }
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (mode == "ENROLL") "Enroll Face ID" else "Face & GPS Verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Live GPS & Premises Distance Status Strip inside Modal
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = premisesName ?: "Assigned Premises",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isFetchingGps && liveDistanceMeters == null) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            }
                            if (liveDistanceMeters != null) {
                                Text(
                                    text = "${liveDistanceMeters}m away",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (liveAccuracyMeters != null) {
                                Text(
                                    text = "±${liveAccuracyMeters}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (mode == "ENROLL") {
                    LinearProgressIndicator(
                        progress = { currentPoseIndex / targetPoses.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                }

                // Big Full Rounded Circular Face Camera Container (No Frame Guard)
                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("Camera permission required", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        FaceCameraPreview(
                            engine = engine,
                            targetPose = if (currentPoseIndex < targetPoses.size) targetPoses[currentPoseIndex] else "CENTER",
                            isDone = currentPoseIndex >= targetPoses.size,
                            onSuccess = { embedding, pose ->
                                accumulatedEmbeddings[pose] = embedding
                                currentPoseIndex++
                                if (currentPoseIndex < targetPoses.size) {
                                    feedbackMessage = "Hold steady... Look ${targetPoses[currentPoseIndex]}"
                                } else {
                                    feedbackMessage = "Face Captured!"
                                }
                            },
                            onFeedback = { feedbackMessage = it }
                        )

                        if (isSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x9910B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    }
                }

                // Feedback Banner
                Surface(
                    color = if (isSuccess) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackMessage,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onPrimaryContainer
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
                    .setTargetResolution(android.util.Size(640, 480))
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
                    imageProxy.close()

                    val matrix = android.graphics.Matrix().apply {
                        postRotate(rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                    )
                    if (rotatedBitmap != rawBitmap) {
                        rawBitmap.recycle()
                    }

                    val poseToCapture = currentTargetPose
                    scope.launch {
                        try {
                            val embedding = engine.captureSpecificPose(rotatedBitmap, poseToCapture)
                            currentOnSuccess(embedding, poseToCapture)
                        } catch (e: Exception) {
                            val message = e.message ?: "Adjust your face in the camera."
                            if (message.isNotBlank()) {
                                currentOnFeedback(message)
                            }
                        } finally {
                            if (!rotatedBitmap.isRecycled) {
                                rotatedBitmap.recycle()
                            }
                            delay(400)
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

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun calculateDistanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}
