package com.als.presence.ui.dashboard.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.location.GpsLocation
import com.als.presence.core.location.calculateDistanceMeters
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

/**
 * GpsAccuracyDialog continuously streams high-accuracy GPS fixes
 * until the accuracy is strictly under [targetAccuracyMeters] (default: 10m).
 *
 * Shows real-time accuracy progress, animated radar calibration, distance to
 * premises geofence, and auto-completes once accuracy < 10m is achieved.
 */
@Composable
fun GpsAccuracyDialog(
    targetAccuracyMeters: Float = 10f,
    premisesLatitude: Double? = null,
    premisesLongitude: Double? = null,
    premisesRadiusMeters: Int = 50,
    title: String = "Calibrating GPS Location",
    onLocationAcquired: (GpsLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val sysLocationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    }
    var isGpsEnabled by remember {
        mutableStateOf(sysLocationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: true)
    }

    var currentLocation by remember { mutableStateOf<GpsLocation?>(null) }
    var currentAccuracy by remember { mutableStateOf<Float?>(null) }
    var isTargetReached by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Timer to track how long we've been calibrating
    LaunchedEffect(Unit) {
        while (!isTargetReached) {
            delay(1000L)
            secondsElapsed++
        }
    }

    // Auto-advance with slight delay once target accuracy is reached
    LaunchedEffect(isTargetReached, currentLocation) {
        if (isTargetReached && currentLocation != null) {
            delay(650L) // Brief confirmation animation delay
            onLocationAcquired(currentLocation!!)
        }
    }

    // Continuous location updates using FusedLocationProviderClient
    DisposableEffect(hasPermission, isGpsEnabled) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@DisposableEffect onDispose {}
        }

        // Try getting last known location to bootstrap immediately
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && currentLocation == null) {
                    val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
                    val gps = GpsLocation(loc.latitude, loc.longitude, loc.accuracy, isMock)
                    currentLocation = gps
                    currentAccuracy = loc.accuracy
                    if (loc.accuracy <= targetAccuracyMeters) {
                        isTargetReached = true
                    }
                }
            }
        } catch (_: Exception) {}

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else @Suppress("DEPRECATION") loc.isFromMockProvider
                val gps = GpsLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy,
                    isMocked = isMock
                )
                currentLocation = gps
                currentAccuracy = loc.accuracy

                if (loc.accuracy <= targetAccuracyMeters) {
                    isTargetReached = true
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {}

        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: Exception) {}
        }
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    // Distance calculation
    val distanceToPremises = remember(currentLocation, premisesLatitude, premisesLongitude) {
        if (currentLocation != null && premisesLatitude != null && premisesLongitude != null) {
            calculateDistanceMeters(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                premisesLatitude,
                premisesLongitude
            )
        } else null
    }
    val isWithinGeofence = distanceToPremises?.let { it <= premisesRadiusMeters }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, PresenceDesign.Line),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Radar / Satellite Icon
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isTargetReached && isGpsEnabled && hasPermission) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(PresenceDesign.Brand.copy(alpha = pulseAlpha))
                        )
                    }

                    val iconBg by animateColorAsState(
                        targetValue = when {
                            !isGpsEnabled || !hasPermission -> PresenceDesign.DangerSoft
                            isTargetReached -> PresenceDesign.PositiveSoft
                            else -> PresenceDesign.BrandSoft
                        },
                        label = "icon_bg_color"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = when {
                            !isGpsEnabled || !hasPermission -> PresenceDesign.Danger
                            isTargetReached -> PresenceDesign.Positive
                            else -> PresenceDesign.Brand
                        },
                        label = "icon_tint_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                            .border(1.5.dp, iconTint.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !isGpsEnabled -> Icons.Default.LocationOff
                                !hasPermission -> Icons.Default.Warning
                                isTargetReached -> Icons.Default.CheckCircle
                                else -> Icons.Default.GpsFixed
                            },
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Title & Subtitle Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            !isGpsEnabled -> "Location (GPS) is Disabled"
                            !hasPermission -> "Location Permission Required"
                            isTargetReached -> "High-Accuracy GPS Acquired!"
                            else -> title
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when {
                            !isGpsEnabled -> "Please turn on Location in your device settings to allow GPS lock."
                            !hasPermission -> "Grant location access so the system can verify your work premises."
                            isTargetReached -> "Accuracy target reached! Proceeding to verification..."
                            else -> "Constantly streaming satellite signals until accuracy is under ${targetAccuracyMeters.toInt()}m."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = PresenceDesign.Muted,
                        textAlign = TextAlign.Center
                    )
                }

                // Accuracy Status Container
                if (hasPermission && isGpsEnabled) {
                    val acc = currentAccuracy
                    val containerBg = when {
                        isTargetReached -> PresenceDesign.PositiveSoft
                        acc != null && acc <= 20f -> PresenceDesign.CautionSoft
                        else -> PresenceDesign.DangerSoft
                    }
                    val containerBorder = when {
                        isTargetReached -> Color(0xFFA7F3D0)
                        acc != null && acc <= 20f -> Color(0xFFFDE68A)
                        else -> Color(0xFFFECACA)
                    }
                    val textColor = when {
                        isTargetReached -> PresenceDesign.Positive
                        acc != null && acc <= 20f -> PresenceDesign.CautionText
                        else -> PresenceDesign.DangerText
                    }

                    Surface(
                        color = containerBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, containerBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Current Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (acc != null) "±${acc.toInt()} meters" else "Acquiring fix...",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = textColor
                                    )
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, containerBorder)
                                ) {
                                    Text(
                                        text = "Target: < ${targetAccuracyMeters.toInt()} m",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Visual progress bar towards target
                            val progressFraction = if (acc != null) {
                                ((50f - acc) / (50f - targetAccuracyMeters)).coerceIn(0.08f, 1f)
                            } else 0.05f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(textColor)
                                )
                            }
                        }
                    }

                    // Optional Distance to Premises Row
                    if (distanceToPremises != null) {
                        Surface(
                            color = PresenceDesign.Workspace,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PresenceDesign.Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = PresenceDesign.Muted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Distance: ${distanceToPremises.toInt()} m",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PresenceDesign.Ink
                                    )
                                }

                                Text(
                                    text = if (isWithinGeofence == true) "Inside Boundary" else "Outside Boundary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWithinGeofence == true) PresenceDesign.Positive else PresenceDesign.DangerText
                                )
                            }
                        }
                    }

                    // Tips text
                    if (!isTargetReached) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = PresenceDesign.Brand
                            )
                            Text(
                                text = "Calibrating ($secondsElapsed s) • Keep device still or step near an open area.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                color = PresenceDesign.Muted
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        !isGpsEnabled -> {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PresenceDesign.Brand)
                            ) {
                                Text("Open Location Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        !hasPermission -> {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PresenceDesign.Brand)
                            ) {
                                Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        isTargetReached -> {
                            Button(
                                onClick = {
                                    currentLocation?.let(onLocationAcquired)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PresenceDesign.Positive)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Continue with ±${currentAccuracy?.toInt()}m Fix",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            // If calibration has run for at least 4 seconds and accuracy is <= 25m, allow manual continue
                            val acc = currentAccuracy
                            if (acc != null && acc <= 25f && secondsElapsed >= 4) {
                                Button(
                                    onClick = {
                                        currentLocation?.let(onLocationAcquired)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PresenceDesign.Brand)
                                ) {
                                    Text(
                                        text = "Use Current Fix (±${acc.toInt()}m)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PresenceDesign.Line)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PresenceDesign.Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
