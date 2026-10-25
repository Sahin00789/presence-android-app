package com.als.presence.ui.auth

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.remote.PostOffice

private val INDIAN_STATES = listOf(
    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
    "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
    "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
    "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
    "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
    "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
    "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
)

data class GoogleRegistrationIdentity(
    val idToken: String,
    val email: String,
    val fullName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRegistrationWizard(
    initialEmail: String,
    otpResponse: OtpResponse?,
    uiState: UiState<*>,
    pincodeState: UiState<List<PostOffice>> = UiState.Idle,
    gpsLocationState: UiState<GpsLocation> = UiState.Idle,
    googleIdentity: GoogleRegistrationIdentity?,
    onDismiss: () -> Unit,
    onRequestOtp: (String) -> Unit,
    onResetOtp: () -> Unit = {},
    onStartGoogle: () -> Unit,
    onLookupPincode: (String) -> Unit = {},
    onFetchGpsLocation: () -> Unit = {},
    onCompleteWithOtp: (String, String, String, String, String, String, String, Int) -> Unit,
    onCompleteWithGoogle: (String, String, String, String, String, Int) -> Unit
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

    // ADDRESS DROPDOWNS & MANUAL FIELDS
    var pincodeInput by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("") }
    var stateDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDistrict by remember { mutableStateOf("") }
    var districtDropdownExpanded by remember { mutableStateOf(false) }
    var selectedBlock by remember { mutableStateOf("") }
    var blockDropdownExpanded by remember { mutableStateOf(false) }
    var selectedNearestPo by remember { mutableStateOf("") }
    var poDropdownExpanded by remember { mutableStateOf(false) }

    var streetAddressInput by remember { mutableStateOf("") }
    var landmarkInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val loading = uiState is UiState.Loading

    // Auto-update dropdown selections strictly from Pincode lookup response
    LaunchedEffect(pincodeState) {
        if (pincodeState is UiState.Success) {
            val offices = pincodeState.data
            val first = offices.firstOrNull()
            if (first != null) {
                selectedNearestPo = first.name.orEmpty()
                selectedBlock = first.division.orEmpty().ifBlank { first.region.orEmpty() }
                selectedDistrict = first.district.orEmpty()
                selectedState = first.state.orEmpty()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F6FB)
        ) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close registration"
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
                                        val fullPremisesName = buildString {
                                            append(premisesName.trim())
                                            if (selectedNearestPo.isNotBlank()) {
                                                append(", $selectedNearestPo")
                                            }
                                        }
                                        if (verificationMethod == "email") {
                                            onCompleteWithOtp(
                                                email.trim(),
                                                otp,
                                                ownerName.trim(),
                                                organizationName.trim(),
                                                timezone.trim(),
                                                currency.trim(),
                                                fullPremisesName,
                                                radiusMeters
                                            )
                                        } else {
                                            googleIdentity?.let {
                                                onCompleteWithGoogle(
                                                    it.idToken,
                                                    organizationName.trim(),
                                                    timezone.trim(),
                                                    currency.trim(),
                                                    fullPremisesName,
                                                    radiusMeters
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
                                                    onClick = { onRequestOtp(email.trim()) },
                                                    enabled = !loading && email.contains("@") && email.contains("."),
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
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF2FF)),
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFDCE2FF))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Email,
                                                                contentDescription = null,
                                                                tint = Color(0xFF5265D8),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text("Verification sent to:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF74798D))
                                                                Text(otpResponse?.email ?: email, fontWeight = FontWeight.Bold, color = Color(0xFF20243A), style = MaterialTheme.typography.bodyMedium)
                                                            }
                                                        }

                                                        TextButton(
                                                            onClick = {
                                                                otp = ""
                                                                onResetOtp()
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = Color(0xFF5265D8)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Change Email", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF5265D8))
                                                        }
                                                    }
                                                }

                                                if (com.als.presence.BuildConfig.DEBUG && !otpResponse?.devOtp.isNullOrEmpty()) {
                                                    otpResponse?.devOtp?.let { devCode ->
                                                        Surface(
                                                            color = Color(0xFFE8F5E9),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text("Development OTP Code:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                                                                Text(devCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20), letterSpacing = 2.sp)
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

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Didn't receive code?", style = MaterialTheme.typography.labelMedium, color = Color(0xFF74798D))
                                                    TextButton(
                                                        onClick = { onRequestOtp(email.trim()) },
                                                        enabled = !loading
                                                    ) {
                                                        Text("Resend Code", fontWeight = FontWeight.Bold, color = Color(0xFF5265D8))
                                                    }
                                                }
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
                                        Text(if (googleIdentity == null) "Choose a Google account" else "Choose another Google account")
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
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = timezone,
                                        onValueChange = { timezone = it },
                                        label = { Text("Timezone") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    OutlinedTextField(
                                        value = currency,
                                        onValueChange = { currency = it },
                                        label = { Text("Currency") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
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

                                // PINCODE INPUT
                                OutlinedTextField(
                                    value = pincodeInput,
                                    onValueChange = { value ->
                                        val digits = value.filter(Char::isDigit).take(6)
                                        pincodeInput = digits
                                        if (digits.length == 6) {
                                            onLookupPincode(digits)
                                        }
                                    },
                                    label = { Text("6-Digit Indian Pincode") },
                                    leadingIcon = { Icon(Icons.Default.PinDrop, null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                if (pincodeState is UiState.Loading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Fetching postal area details...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF74798D))
                                    }
                                }

                                // 1. STATE DROPDOWN
                                ExposedDropdownMenuBox(
                                    expanded = stateDropdownExpanded,
                                    onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedState,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("State (Dropdown)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = stateDropdownExpanded,
                                        onDismissRequest = { stateDropdownExpanded = false }
                                    ) {
                                        val statesList = if (pincodeState is UiState.Success) {
                                            val pincodeStates = pincodeState.data.mapNotNull { it.state }.filter { it.isNotBlank() }.distinct()
                                            pincodeStates.ifEmpty { INDIAN_STATES }
                                        } else {
                                            INDIAN_STATES
                                        }
                                        statesList.forEach { stateName ->
                                            DropdownMenuItem(
                                                text = { Text(stateName, fontWeight = FontWeight.SemiBold) },
                                                onClick = {
                                                    selectedState = stateName
                                                    stateDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. DISTRICT DROPDOWN
                                ExposedDropdownMenuBox(
                                    expanded = districtDropdownExpanded,
                                    onExpandedChange = { districtDropdownExpanded = !districtDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedDistrict,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("District (Dropdown)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = districtDropdownExpanded,
                                        onDismissRequest = { districtDropdownExpanded = false }
                                    ) {
                                        val districtsList = if (pincodeState is UiState.Success) {
                                            pincodeState.data.mapNotNull { it.district }.distinct()
                                        } else emptyList()

                                        if (districtsList.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(if (selectedDistrict.isNotBlank()) selectedDistrict else "Enter 6-Digit Pincode to populate Districts", color = Color(0xFF74798D)) },
                                                onClick = { districtDropdownExpanded = false }
                                            )
                                        } else {
                                            districtsList.forEach { distName ->
                                                DropdownMenuItem(
                                                    text = { Text(distName, fontWeight = FontWeight.SemiBold) },
                                                    onClick = {
                                                        selectedDistrict = distName
                                                        districtDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. BLOCK / TEHSIL DROPDOWN
                                ExposedDropdownMenuBox(
                                    expanded = blockDropdownExpanded,
                                    onExpandedChange = { blockDropdownExpanded = !blockDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedBlock,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Block / Tehsil / Division (Dropdown)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = blockDropdownExpanded,
                                        onDismissRequest = { blockDropdownExpanded = false }
                                    ) {
                                        val blocksList = if (pincodeState is UiState.Success) {
                                            pincodeState.data.mapNotNull { it.division ?: it.region }.distinct()
                                        } else emptyList()

                                        if (blocksList.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(if (selectedBlock.isNotBlank()) selectedBlock else "Enter 6-Digit Pincode to populate Blocks", color = Color(0xFF74798D)) },
                                                onClick = { blockDropdownExpanded = false }
                                            )
                                        } else {
                                            blocksList.forEach { blockName ->
                                                DropdownMenuItem(
                                                    text = { Text(blockName, fontWeight = FontWeight.SemiBold) },
                                                    onClick = {
                                                        selectedBlock = blockName
                                                        blockDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. NEAREST POST OFFICE DROPDOWN
                                ExposedDropdownMenuBox(
                                    expanded = poDropdownExpanded,
                                    onExpandedChange = { poDropdownExpanded = !poDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedNearestPo,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Nearest Post Office (Dropdown)") },
                                        leadingIcon = { Icon(Icons.Default.Place, null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = poDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = poDropdownExpanded,
                                        onDismissRequest = { poDropdownExpanded = false }
                                    ) {
                                        val poList = if (pincodeState is UiState.Success) {
                                            pincodeState.data.mapNotNull { it.name }.distinct()
                                        } else emptyList()

                                        if (poList.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(if (selectedNearestPo.isNotBlank()) selectedNearestPo else "Enter 6-Digit Pincode to populate Post Offices", color = Color(0xFF74798D)) },
                                                onClick = { poDropdownExpanded = false }
                                            )
                                        } else {
                                            poList.forEach { poName ->
                                                DropdownMenuItem(
                                                    text = { Text(poName, fontWeight = FontWeight.SemiBold) },
                                                    onClick = {
                                                        selectedNearestPo = poName
                                                        poDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // MANUAL ENTRY STREET ADDRESS & LANDMARK
                                OutlinedTextField(
                                    value = streetAddressInput,
                                    onValueChange = { streetAddressInput = it },
                                    label = { Text("Building / Street / Door No (Manual Entry)") },
                                    leadingIcon = { Icon(Icons.Default.Home, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedTextField(
                                    value = landmarkInput,
                                    onValueChange = { landmarkInput = it },
                                    label = { Text("Landmark (Optional Manual Entry)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                HorizontalDivider(color = Color(0xFFEAECF2))

                                // DETECT ACTUAL GPS COORDINATES SECTION
                                OutlinedButton(
                                    onClick = onFetchGpsLocation,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Detect Actual GPS Coordinates", fontWeight = FontWeight.SemiBold)
                                }

                                when (gpsLocationState) {
                                    is UiState.Loading -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text("Acquiring high-accuracy GPS coordinates...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF74798D))
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
                                                    "🎯 GPS Captured: ${String.format("%.6f", gps.latitude)}, ${String.format("%.6f", gps.longitude)}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    "Accuracy: ±${gps.accuracyMeters.toInt()}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF388E3C)
                                                )
                                            }
                                        }
                                    }
                                    is UiState.Error -> {
                                        Text(gpsLocationState.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> {}
                                }

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
                                    "You can update the geofence later in premises settings.",
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
                                    if (selectedNearestPo.isNotBlank() || selectedDistrict.isNotBlank()) {
                                        ReviewRow("Location", "$selectedNearestPo, $selectedBlock, $selectedDistrict $selectedState")
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
