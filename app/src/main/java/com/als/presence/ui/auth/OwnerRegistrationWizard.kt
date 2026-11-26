package com.als.presence.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.GoogleRegistrationIdentity
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.model.PostOffice
import com.als.presence.ui.auth.components.AuthAtmosphericBackground
import com.als.presence.ui.auth.components.AuthStepProgress
import com.als.presence.ui.auth.components.GoogleSignInButton
import com.als.presence.ui.auth.components.OtpInputView
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceCard
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.common.components.PresenceSectionHeader

/**
 * Premium 4-Step Owner & Organisation Registration Wizard.
 * Step 1: Personal Details (Name, Phone, Email OTP/Google)
 * Step 2: Organisation Details (Organisation Name, Timezone, Currency)
 * Step 3: Premises Details & Geofence Location Setup (Premises Name, Address, GPS)
 * Step 4: Review & Finalize Creation
 */
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
    savedDraft: com.als.presence.data.local.OwnerRegistrationDraft? = null,
    onSaveDraft: (com.als.presence.data.local.OwnerRegistrationDraft) -> Unit = {},
    onClearDraft: () -> Unit = {},
    onDismiss: () -> Unit,
    onRequestOtp: (String) -> Unit,
    onResetOtp: () -> Unit = {},
    onStartGoogle: () -> Unit,
    onLookupPincode: (String) -> Unit = {},
    onFetchGpsLocation: () -> Unit = {},
    onGpsLocationAcquired: (GpsLocation) -> Unit = {},
    onRegistrationSuccessAcknowledged: () -> Unit = {},
    onCompleteWithOtp: (email: String, otp: String, ownerName: String, orgName: String, timezone: String, currency: String, premisesName: String, radius: Int, address: String?, latitude: String?, longitude: String?) -> Unit,
    onCompleteWithGoogle: (idToken: String, orgName: String, timezone: String, currency: String, premisesName: String, radius: Int, address: String?, latitude: String?, longitude: String?) -> Unit
) {
    var step by remember { mutableIntStateOf(savedDraft?.step ?: 1) }
    var verificationMethod by remember { mutableStateOf(savedDraft?.verificationMethod ?: "email") }
    var email by remember(initialEmail) { mutableStateOf(savedDraft?.email?.takeIf { it.isNotBlank() } ?: initialEmail) }
    var otp by remember { mutableStateOf(savedDraft?.verifiedOtp ?: "") }
    var isEmailVerified by remember { mutableStateOf(savedDraft?.verifiedOtp != null || savedDraft?.googleIdToken != null) }
    var ownerName by remember { mutableStateOf(savedDraft?.ownerName ?: "") }
    var phone by remember { mutableStateOf(savedDraft?.phone ?: "") }
    var organizationName by remember { mutableStateOf(savedDraft?.organizationName ?: "") }
    var organizationType by remember { mutableStateOf(savedDraft?.organizationType ?: "Technology & IT") }
    var timezone by remember { mutableStateOf("Asia/Kolkata") }
    var currency by remember { mutableStateOf("INR") }
    var premisesName by remember { mutableStateOf(savedDraft?.premisesName ?: "Main Headquarters") }
    var premisesAddress by remember { mutableStateOf(savedDraft?.premisesAddress ?: "") }
    var radiusMeters by remember { mutableIntStateOf(savedDraft?.radiusMeters ?: 50) }
    var latitude by remember { mutableStateOf<String?>(savedDraft?.latitude) }
    var longitude by remember { mutableStateOf<String?>(savedDraft?.longitude) }
    var googleIdToken by remember { mutableStateOf<String?>(savedDraft?.googleIdToken) }

    fun updateDraft(newStep: Int = step) {
        onSaveDraft(
            com.als.presence.data.local.OwnerRegistrationDraft(
                step = newStep,
                email = email,
                ownerName = ownerName,
                phone = phone,
                organizationName = organizationName,
                organizationType = organizationType,
                premisesName = premisesName,
                premisesAddress = premisesAddress,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                verificationMethod = verificationMethod,
                googleIdToken = googleIdToken,
                verifiedOtp = otp.takeIf { it.isNotBlank() }
            )
        )
    }

    val devOtp = otpResponse?.devOtp

    LaunchedEffect(googleIdentity) {
        if (googleIdentity != null) {
            verificationMethod = "google"
            googleIdToken = googleIdentity.idToken
            email = googleIdentity.email
            if (googleIdentity.fullName.isNotBlank() && ownerName.isBlank()) {
                ownerName = googleIdentity.fullName
            }
            isEmailVerified = true
            step = 2
            updateDraft(2)
        }
    }

    LaunchedEffect(gpsLocationState) {
        if (gpsLocationState is UiState.Success) {
            val loc = gpsLocationState.data
            latitude = loc.latitude.toString()
            longitude = loc.longitude.toString()
        }
    }

    LaunchedEffect(reverseGeocodeAddress) {
        if (reverseGeocodeAddress != null && reverseGeocodeAddress.streetAddress.isNotBlank()) {
            premisesAddress = reverseGeocodeAddress.streetAddress
        }
    }

    AuthAtmosphericBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Wizard Top Bar Header & Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = {
                            if (step > 1) step -= 1 else onDismiss()
                        }) {
                            Icon(
                                imageVector = if (step > 1) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            PresenceMicroLabel(text = "Step $step of 4")
                            Text(
                                text = when (step) {
                                    1 -> "Personal Information"
                                    2 -> "Organisation Profile"
                                    3 -> "Premises & Location"
                                    else -> "Review & Create"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_presence),
                            contentDescription = "Presence Logo",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Step Progress Indicator Bar
                AuthStepProgress(currentStep = step)
            }

            // 2. Step Card Container (24dp Radius Surface Card)
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "wizardStep"
            ) { currentStep ->
                PresenceCard(
                    elevation = 3.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                // STEP 1: PERSONAL DETAILS & EMAIL VERIFICATION
                                PresenceSectionHeader(
                                    title = "Owner Personal Details",
                                    subtitle = "Enter your full name, phone number, and verify business email"
                                )

                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    label = { Text("Full Name") },
                                    placeholder = { Text("John Doe") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    label = { Text("Mobile Phone Number") },
                                    placeholder = { Text("+91 9876543210") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    label = { Text("Business Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                if (verificationMethod == "email") {
                                    PresenceButton(
                                        text = "Send Verification OTP",
                                        onClick = {
                                            if (email.contains("@")) {
                                                onRequestOtp(email)
                                            }
                                        },
                                        variant = PresenceVariant.Primary,
                                        size = PresenceSize.Lg,
                                        loading = uiState is UiState.Loading,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (otpResponse != null) {
                                        OtpInputView(
                                            otpValue = otp,
                                            onOtpChange = { otp = it },
                                            onOtpSubmit = { digits ->
                                                otp = digits
                                            },
                                            onResendClick = { onRequestOtp(email) },
                                            devOtp = devOtp
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "OR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }

                                GoogleSignInButton(
                                    onClick = onStartGoogle,
                                    text = "Verify with Google SSO"
                                )

                                PresenceButton(
                                    text = "Next: Organisation Profile",
                                    onClick = {
                                        if (ownerName.isNotBlank() && email.isNotBlank()) {
                                            step = 2
                                        }
                                    },
                                    variant = PresenceVariant.Primary,
                                    size = PresenceSize.Lg,
                                    enabled = ownerName.isNotBlank() && email.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            2 -> {
                                // STEP 2: ORGANISATION DETAILS
                                PresenceSectionHeader(
                                    title = "Organisation Details",
                                    subtitle = "Name your organization, timezone, and accounting currency"
                                )

                                OutlinedTextField(
                                    value = organizationName,
                                    onValueChange = { organizationName = it },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    label = { Text("Organisation Legal Name") },
                                    placeholder = { Text("Acme Enterprises Ltd.") },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = timezone,
                                        onValueChange = { timezone = it },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        label = { Text("Timezone") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    OutlinedTextField(
                                        value = currency,
                                        onValueChange = { currency = it },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        label = { Text("Currency") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PresenceButton(
                                        text = "Back",
                                        onClick = { step = 1 },
                                        variant = PresenceVariant.Ghost,
                                        size = PresenceSize.Md,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PresenceButton(
                                        text = "Next: Premises",
                                        onClick = {
                                            if (organizationName.isNotBlank()) {
                                                step = 3
                                            }
                                        },
                                        variant = PresenceVariant.Primary,
                                        size = PresenceSize.Md,
                                        enabled = organizationName.isNotBlank(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            3 -> {
                                // STEP 3: PREMISES & GEOFENCE LOCATION SETUP
                                PresenceSectionHeader(
                                    title = "Premises & Geofence",
                                    subtitle = "Configure your first office premises and attendance geofence"
                                )

                                OutlinedTextField(
                                    value = premisesName,
                                    onValueChange = { premisesName = it },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    label = { Text("Premises / Office Name") },
                                    placeholder = { Text("Headquarters - Main Office") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedTextField(
                                    value = premisesAddress,
                                    onValueChange = { premisesAddress = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Street Address") },
                                    placeholder = { Text("123 Business Park, Sector 62") },
                                    singleLine = false,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                PresenceButton(
                                    text = "Auto-Detect Current GPS Location",
                                    onClick = onFetchGpsLocation,
                                    variant = PresenceVariant.Soft,
                                    size = PresenceSize.Sm,
                                    icon = Icons.Default.MyLocation,
                                    loading = gpsLocationState is UiState.Loading,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (latitude != null && longitude != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "GPS Coordinates Acquired: $latitude, $longitude",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PresenceButton(
                                        text = "Back",
                                        onClick = { step = 2 },
                                        variant = PresenceVariant.Ghost,
                                        size = PresenceSize.Md,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PresenceButton(
                                        text = "Next: Review",
                                        onClick = {
                                            if (premisesName.isNotBlank()) {
                                                step = 4
                                            }
                                        },
                                        variant = PresenceVariant.Primary,
                                        size = PresenceSize.Md,
                                        enabled = premisesName.isNotBlank(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            4 -> {
                                // STEP 4: REVIEW & FINALIZE CREATION
                                PresenceSectionHeader(
                                    title = "Review & Finalize",
                                    subtitle = "Confirm details and create your organization account"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PresenceMicroLabel(text = "Owner Administrator")
                                    Text(text = ownerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                                    PresenceMicroLabel(text = "Organisation Setup")
                                    Text(text = organizationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Premises: $premisesName (${radiusMeters}m geofence)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (premisesAddress.isNotBlank()) {
                                        Text(text = "Address: $premisesAddress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (uiState is UiState.Error) {
                                    Surface(
                                        color = PresenceDesign.DangerSoft,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.message,
                                            color = PresenceDesign.DangerText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PresenceButton(
                                        text = "Back",
                                        onClick = { step = 3 },
                                        variant = PresenceVariant.Ghost,
                                        size = PresenceSize.Lg,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PresenceButton(
                                        text = "Create Organisation",
                                        onClick = {
                                            if (verificationMethod == "google" && googleIdentity != null) {
                                                onCompleteWithGoogle(
                                                    googleIdentity.idToken,
                                                    organizationName,
                                                    timezone,
                                                    currency,
                                                    premisesName,
                                                    radiusMeters,
                                                    premisesAddress,
                                                    latitude,
                                                    longitude
                                                )
                                            } else {
                                                onCompleteWithOtp(
                                                    email,
                                                    otp,
                                                    ownerName,
                                                    organizationName,
                                                    timezone,
                                                    currency,
                                                    premisesName,
                                                    radiusMeters,
                                                    premisesAddress,
                                                    latitude,
                                                    longitude
                                                )
                                            }
                                        },
                                        variant = PresenceVariant.Primary,
                                        size = PresenceSize.Lg,
                                        loading = uiState is UiState.Loading,
                                        modifier = Modifier.weight(2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
