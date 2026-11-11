package com.als.presence.ui.auth

import android.location.Geocoder
import android.widget.Toast
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.GoogleRegistrationIdentity
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.model.PostOffice
import com.als.presence.ui.dashboard.common.GpsAccuracyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRegistrationWizard(
    initialEmail: String,
    otpResponse: OtpResponse?,
    uiState: UiState<*>,
    pincodeState: UiState<List<PostOffice>> = UiState.Idle,
    gpsLocationState: UiState<GpsLocation> = UiState.Idle,
    reverseGeocodeAddress: com.als.presence.core.location.ReverseGeocodedAddress? = null,
    googleIdentity: GoogleRegistrationIdentity?,
    onDismiss: () -> Unit,
    onRequestOtp: (String) -> Unit,
    onResetOtp: () -> Unit = {},
    onStartGoogle: () -> Unit,
    onLookupPincode: (String) -> Unit = {},
    onFetchGpsLocation: () -> Unit = {},
    onGpsLocationAcquired: (GpsLocation) -> Unit = {},
    onCompleteWithOtp: (email: String, otp: String, ownerName: String, orgName: String, timezone: String, currency: String, premisesName: String, radius: Int, address: String?, latitude: String?, longitude: String?) -> Unit,
    onCompleteWithGoogle: (idToken: String, orgName: String, timezone: String, currency: String, premisesName: String, radius: Int, address: String?, latitude: String?, longitude: String?) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var verificationMethod by remember { mutableStateOf("email") }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var otp by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var organizationName by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Kolkata") }
    var currency by remember { mutableStateOf("INR") }
    var premisesName by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("50") }

    var streetAddressInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    var capturedLat by remember { mutableStateOf<String?>(null) }
    var capturedLng by remember { mutableStateOf<String?>(null) }
    var showGpsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(otpResponse) {
        val resp = otpResponse
        if (resp != null) {
            Toast.makeText(context, "Verification code sent to ${resp.email}", Toast.LENGTH_SHORT).show()
        }
    }

    val requestLocationPermission = com.als.presence.core.location.rememberLocationPermissionState(
        onPermissionGranted = {
            showGpsDialog = true
        }
    )

    if (showGpsDialog) {
        GpsAccuracyDialog(
            title = "Calibrating Premises GPS",
            targetAccuracyMeters = 10f,
            onDismiss = { showGpsDialog = false },
            onLocationAcquired = { gps ->
                showGpsDialog = false
                capturedLat = gps.latitude.toString()
                capturedLng = gps.longitude.toString()
                onGpsLocationAcquired(gps)
            }
        )
    }

    val scrollState = rememberScrollState()
    val loading = uiState is UiState.Loading

    LaunchedEffect(reverseGeocodeAddress) {
        reverseGeocodeAddress?.let { rev ->
            if (rev.streetAddress.isNotBlank()) {
                streetAddressInput = rev.streetAddress
            }
        }
    }

    LaunchedEffect(gpsLocationState) {
        if (gpsLocationState is UiState.Success) {
            val gps = gpsLocationState.data
            capturedLat = gps.latitude.toString()
            capturedLng = gps.longitude.toString()
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(gps.latitude, gps.longitude, 1)
                    val first = addresses?.firstOrNull()
                    if (first != null) {
                        val addr = first.getAddressLine(0) ?: first.locality
                        if (!addr.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                if (streetAddressInput.isBlank()) {
                                    streetAddressInput = addr
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    androidx.activity.compose.BackHandler {
        if (step > 1) {
            step--
        } else {
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F6FB)
    ) {
            if (uiState is UiState.Success) {
                val registeredUser = (uiState as? UiState.Success<*>)?.data as? com.als.presence.domain.model.User
                RegistrationSuccessGreetScreen(
                    user = registeredUser,
                    organizationName = organizationName,
                    ownerName = googleIdentity?.fullName?.ifBlank { null } ?: ownerName,
                    email = googleIdentity?.email ?: email,
                    premisesName = premisesName,
                    onOkClick = onDismiss
                )
            } else {
                Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (step > 1) {
                                    step--
                                } else {
                                    onDismiss()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "PRESENCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF5265D8),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp
                                )
                                Text(
                                    "Create your workspace",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = Color(0xFFF1F3FF),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "$step / 3",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = Color(0xFF4659C7),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Verify", "Organization", "Premises").forEachIndexed { index, label ->
                                val itemStep = index + 1
                                val complete = itemStep < step
                                val active = itemStep == step
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = if (itemStep <= step) Color(0xFF5265D8) else Color(0xFFE6E8F0),
                                        shape = RoundedCornerShape(50)
                                    ) {}
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active || complete) Color(0xFF4659C7) else Color(0xFF8B90A3),
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = Color.White,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (step > 1) {
                                OutlinedButton(
                                    onClick = { step-- },
                                    enabled = !loading,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Back")
                                }
                            } else {
                                Text(
                                    "Your details stay secure",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (step < 3) {
                                Button(
                                    onClick = { step++ },
                                    enabled = !loading && when (step) {
                                        1 -> if (verificationMethod == "email") {
                                            otpResponse?.email?.equals(email.trim(), ignoreCase = true) == true &&
                                                otp.length == 6
                                        } else {
                                            googleIdentity != null
                                        }
                                        else -> (if (verificationMethod == "google") {
                                            googleIdentity?.fullName?.trim()?.length ?: 0
                                        } else {
                                            ownerName.trim().length
                                        }) >= 2 && organizationName.trim().length >= 2
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 22.dp,
                                        vertical = 14.dp
                                    )
                                ) {
                                    Text("Continue")
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val radiusMeters = radius.toIntOrNull() ?: 50
                                        val streetAddr = streetAddressInput.trim().ifBlank { null }

                                        if (verificationMethod == "email") {
                                            onCompleteWithOtp(
                                                email.trim(),
                                                otp,
                                                ownerName.trim(),
                                                organizationName.trim(),
                                                timezone.trim(),
                                                currency.trim(),
                                                premisesName.trim(),
                                                radiusMeters,
                                                streetAddr,
                                                capturedLat,
                                                capturedLng
                                            )
                                        } else {
                                            googleIdentity?.let {
                                                onCompleteWithGoogle(
                                                    it.idToken,
                                                    organizationName.trim(),
                                                    timezone.trim(),
                                                    currency.trim(),
                                                    premisesName.trim(),
                                                    radiusMeters,
                                                    streetAddr,
                                                    capturedLat,
                                                    capturedLng
                                                )
                                            }
                                        }
                                    },
                                    enabled = !loading &&
                                        premisesName.trim().length >= 2 &&
                                        (radius.toIntOrNull() ?: 0) in 1..10000,
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 14.dp
                                    )
                                ) {
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create organization")
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF5F6FB))
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    val stepIcon = when (step) {
                        1 -> Icons.Default.Email
                        2 -> Icons.Default.Business
                        else -> Icons.Default.LocationOn
                    }
                    val stepTitle = when (step) {
                        1 -> "Let’s verify it’s you"
                        2 -> "Tell us about your team"
                        else -> "Add your first workplace"
                    }
                    val stepDescription = when (step) {
                        1 -> "Use a work email or Google to securely verify the owner account."
                        2 -> "A few details help us tailor your workspace."
                        else -> "Set a name, pincode address and attendance boundary."
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            color = Color(0xFFE9ECFF),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp),
                                tint = Color(0xFF5265D8)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stepTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF20243A)
                            )
                            Text(
                                stepDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF74798D)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState is UiState.Error) {
                            val errorState = uiState as UiState.Error
                            val errorMsg = errorState.message.ifBlank { "An error occurred" }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = errorMsg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!errorState.code.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Error Code: ${errorState.code}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        when (step) {
                            1 -> {
                                Text(
                                    "Choose a verification method",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30344A)
                                )
                                VerificationMethodCard(
                                    title = "Continue with Google",
                                    description = "Quick and secure identity verification",
                                    selected = verificationMethod == "google",
                                    isGoogle = true,
                                    onClick = { verificationMethod = "google" }
                                )
                                VerificationMethodCard(
                                    title = "Verify with email",
                                    description = "We’ll send a one-time 6-digit code",
                                    selected = verificationMethod == "email",
                                    isGoogle = false,
                                    onClick = { verificationMethod = "email" }
                                )

                                Spacer(Modifier.height(4.dp))

                                if (verificationMethod == "email") {
                                    AnimatedContent(
                                        targetState = otpResponse != null,
                                        transitionSpec = {
                                            if (targetState) {
                                                (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { width -> width } + fadeIn(tween(350)))
                                                    .togetherWith(slideOutHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { width -> -width } + fadeOut(tween(250)))
                                            } else {
                                                (slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { width -> -width } + fadeIn(tween(350)))
                                                    .togetherWith(slideOutHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { width -> width } + fadeOut(tween(250)))
                                            }
                                        },
                                        label = "EmailToOtpSlide"
                                    ) { isCodeSent ->
                                        if (!isCodeSent) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = email,
                                                    onValueChange = { email = it },
                                                    label = { Text("Work email address") },
                                                    leadingIcon = { Icon(Icons.Default.Email, null) },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp)
                                                )

                                                Button(
                                                    onClick = { onRequestOtp(email.replace(Regex("[\\s\\u00A0]+"), "").lowercase()) },
                                                    enabled = !loading && email.replace(Regex("[\\s\\u00A0]+"), "").contains("@") && email.replace(Regex("[\\s\\u00A0]+"), "").contains("."),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp),
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    if (loading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Sending code...")
                                                    } else {
                                                        Text("Send verification code")
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(14.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFDCFCE7)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Email,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF15803D),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(12.dp))

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        text = "OTP Sent to Mail",
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF14532D)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF16A34A),
                                                                        modifier = Modifier.size(15.dp)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = otpResponse?.email ?: email,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF0F172A),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }

                                                            TextButton(
                                                                onClick = {
                                                                    otp = ""
                                                                    onResetOtp()
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Change Email",
                                                                    modifier = Modifier.size(14.dp),
                                                                    tint = Color(0xFF5265D8)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "Change",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF5265D8)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Text(
                                                            text = "We sent a 6-digit verification code to your email. Please check your inbox and spam folder.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF475569)
                                                        )

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        HorizontalDivider(color = Color(0xFFDCFCE7))

                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Didn't receive the email?",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF64748B)
                                                            )

                                                            TextButton(
                                                                onClick = { onRequestOtp(email.trim()) },
                                                                enabled = !loading,
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                            ) {
                                                                if (loading) {
                                                                    CircularProgressIndicator(
                                                                        modifier = Modifier.size(12.dp),
                                                                        color = Color(0xFF5265D8),
                                                                        strokeWidth = 1.5.dp
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                }
                                                                Text(
                                                                    text = "Resend Code",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (loading) Color(0xFF94A3B8) else Color(0xFF5265D8)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = otp,
                                                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                                                    label = { Text("6-digit verification code") },
                                                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF5265D8)) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    googleIdentity?.let {
                                        Surface(
                                            color = Color(0xFFEAF8F1),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "Google account selected\n${it.email}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                color = Color(0xFF28724F),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onStartGoogle,
                                        enabled = !loading,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color(0xFF5265D8),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Connecting to Google...",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(R.drawable.ic_google_logo),
                                                contentDescription = "Google",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (googleIdentity == null) "Choose a Google account" else "Choose another Google account",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                if (verificationMethod == "google") {
                                    googleIdentity?.let {
                                        Surface(
                                            color = Color(0xFFF3F5FF),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Icons.Default.Person, null, tint = Color(0xFF5265D8))
                                                Column {
                                                    Text(it.fullName, fontWeight = FontWeight.SemiBold)
                                                    Text(it.email, style = MaterialTheme.typography.bodySmall, color = Color(0xFF74798D))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = ownerName,
                                        onValueChange = { ownerName = it },
                                        label = { Text("Owner full name") },
                                        leadingIcon = { Icon(Icons.Default.Person, null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                Text(
                                    "Organization details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30344A)
                                )
                                OutlinedTextField(
                                    value = organizationName,
                                    onValueChange = { organizationName = it },
                                    label = { Text("Organization name") },
                                    leadingIcon = { Icon(Icons.Default.Business, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                OutlinedTextField(
                                    value = timezone,
                                    onValueChange = { timezone = it },
                                    label = { Text("Timezone") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            else -> {
                                Text(
                                    "Where does your team work?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30344A)
                                )

                                OutlinedTextField(
                                    value = premisesName,
                                    onValueChange = { premisesName = it },
                                    label = { Text("Premises name (e.g. Main HQ)") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedButton(
                                    onClick = requestLocationPermission,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Detect Location & Address via GPS", fontWeight = FontWeight.SemiBold)
                                }

                                when (gpsLocationState) {
                                    is UiState.Loading -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text("Acquiring GPS coordinates & address...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF74798D))
                                        }
                                    }
                                    is UiState.Success -> {
                                        val gps = gpsLocationState.data
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    "🎯 Location Fix: ${String.format(Locale.getDefault(), "%.6f", gps.latitude)}, ${String.format(Locale.getDefault(), "%.6f", gps.longitude)} (±${gps.accuracyMeters.toInt()}m)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                    is UiState.Error -> {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = gpsLocationState.message,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                TextButton(
                                                    onClick = requestLocationPermission,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text(
                                                        text = "Allow GPS",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }

                                OutlinedTextField(
                                    value = streetAddressInput,
                                    onValueChange = { streetAddressInput = it },
                                    label = { Text("Premises Address (Auto-filled by GPS or manual)") },
                                    leadingIcon = { Icon(Icons.Default.Home, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedTextField(
                                    value = radius,
                                    onValueChange = { radius = it.filter(Char::isDigit).take(5) },
                                    label = { Text("Geofence radius (metres)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Text(
                                    "You can update the geofence radius later in premises settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF74798D)
                                )
                                HorizontalDivider(color = Color(0xFFEAECF2))
                                Text(
                                    "Review your workspace",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30344A)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF7F8FC))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ReviewRow("Owner", googleIdentity?.fullName?.ifBlank { null } ?: ownerName.ifBlank { "Owner" })
                                    ReviewRow("Email", googleIdentity?.email ?: email)
                                    ReviewRow("Organization", organizationName.ifBlank { "Not set" })
                                    ReviewRow("Premises", "${premisesName.ifBlank { "Not set" }} · $radius m radius")
                                    if (streetAddressInput.isNotBlank()) {
                                        ReviewRow("Address", streetAddressInput)
                                    }
                                }
                                Text(
                                    "Your identity is verified securely when you create the organization.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationSuccessGreetScreen(
    user: com.als.presence.domain.model.User?,
    organizationName: String,
    ownerName: String,
    email: String,
    premisesName: String,
    onOkClick: () -> Unit
) {
    val displayOrg = user?.organizationName?.ifBlank { null } ?: organizationName.ifBlank { "My Organization" }
    val displayOwner = user?.fullName?.ifBlank { null } ?: ownerName.ifBlank { "Organization Owner" }
    val displayEmail = user?.email?.ifBlank { null } ?: email
    val displayPremises = user?.premisesName?.ifBlank { null } ?: premisesName.ifBlank { "Main Premises" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FB))
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF5265D8),
                            Color(0xFF10B981).copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Presence!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E293B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your organization setup is complete.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color(0xFF5265D8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Organization",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = displayOrg,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Owner Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = displayOwner,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = displayEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (displayPremises.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Initial Premises",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = displayPremises,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onOkClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5265D8)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "OK • Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VerificationMethodCard(
    title: String,
    description: String,
    selected: Boolean,
    isGoogle: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFF1F3FF) else Color.White,
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF5265D8) else Color(0xFFE3E6F0)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = if (selected) Color(0xFF5265D8) else Color(0xFFF5F6FB),
                shape = CircleShape
            ) {
                if (isGoogle) {
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = if (selected) Color.White else Color(0xFF676C83)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color(0xFF232842) else Color(0xFF3F445D)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF787D93)
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF747895)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20243A)
        )
    }
}
