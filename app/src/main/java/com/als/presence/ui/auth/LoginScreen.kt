package com.als.presence.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PrimaryIndigo
import com.als.presence.core.designsystem.RoleOwnerPrimary
import com.als.presence.core.designsystem.RoleStaffPrimary
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.GoogleRegistrationIdentity
import com.als.presence.data.model.GoogleSignInPurpose
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class LoginStep {
    ENTER_EMAIL,
    ENTER_OTP
}

data class RegistrationSuccessInfo(
    val email: String,
    val ownerName: String = "",
    val organizationName: String,
    val premisesName: String
)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val pincodeState by viewModel.pincodeState.collectAsState()
    val gpsLocationState by viewModel.gpsLocationState.collectAsState()
    val reverseGeocodeAddress by viewModel.reverseGeocodeState.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(context.getString(R.string.google_web_client_id), true)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    var emailInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var loginStep by remember { mutableStateOf(LoginStep.ENTER_EMAIL) }
    var showOwnerRegistrationWizard by remember { mutableStateOf(false) }
    var registrationSuccessInfo by remember { mutableStateOf<RegistrationSuccessInfo?>(null) }
    var googleSignInPurpose by remember { mutableStateOf(GoogleSignInPurpose.LOGIN) }
    var googleRegistrationIdentity by remember { mutableStateOf<GoogleRegistrationIdentity?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            isGoogleSigningIn = false
            return@rememberLauncherForActivityResult
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            when (googleSignInPurpose) {
                GoogleSignInPurpose.LOGIN -> {
                    val idToken = account.idToken
                    val code = account.serverAuthCode
                    if (idToken.isNullOrBlank() && code.isNullOrBlank()) {
                        isGoogleSigningIn = false
                        viewModel.showError("Google did not return a credential. Please try again.")
                    } else {
                        viewModel.loginWithGoogle(idToken = idToken, serverAuthCode = code)
                    }
                }
                GoogleSignInPurpose.OWNER_REGISTRATION -> {
                    isGoogleSigningIn = false
                    val idToken = account.idToken
                    if (idToken.isNullOrBlank() || account.email.isNullOrBlank()) {
                        viewModel.showError("Google did not return the verified account details. Please try again.")
                    } else {
                        googleRegistrationIdentity = GoogleRegistrationIdentity(
                            idToken = idToken,
                            email = account.email.orEmpty(),
                            fullName = account.displayName.orEmpty()
                        )
                    }
                }
            }
        } catch (error: ApiException) {
            isGoogleSigningIn = false
            val friendlyMessage = when (error.statusCode) {
                10 -> "Google Developer Error (10): Register your app's package name 'com.als.presence' and debug SHA-1 fingerprint in Google Cloud Console."
                7 -> "Google Network Error (7): Network unavailable or Google services unreachable. Please check your connection."
                12500 -> "Google Sign-In Error (12500): Google Play Services internal error. Ensure Google Play Services is updated."
                12501, 16 -> "Google sign-in was cancelled."
                else -> "Google sign-in failed (Code ${error.statusCode}). Please try again."
            }
            viewModel.showError(friendlyMessage)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.tryAutoLogin()
    }

    LaunchedEffect(selectedRole) {
        loginStep = LoginStep.ENTER_EMAIL
        otpCodeInput = ""
        isGoogleSigningIn = false
        viewModel.resetOtp()
        viewModel.clearError()
    }

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Loading) {
            isGoogleSigningIn = false
        }
        if (uiState is UiState.Success && !showOwnerRegistrationWizard) {
            val user = (uiState as UiState.Success<User>).data
            if (registrationSuccessInfo == null) {
                onLoginSuccess(user)
            }
        }
    }

    fun startGoogleSignIn(purpose: GoogleSignInPurpose) {
        isGoogleSigningIn = true
        googleSignInPurpose = purpose
        viewModel.launchGoogleSignIn(
            context = context,
            purpose = purpose,
            onRegistrationIdentity = { identity ->
                isGoogleSigningIn = false
                googleRegistrationIdentity = identity
            },
            onFallbackToLegacy = {
                try {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                } catch (_: Exception) {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            }
        )
    }

    // Synchronize OTP response state to Step 2
    if (otpState != null && loginStep == LoginStep.ENTER_EMAIL && !showOwnerRegistrationWizard) {
        loginStep = LoginStep.ENTER_OTP
        otpCodeInput = if (com.als.presence.BuildConfig.DEBUG) otpState?.devOtp.orEmpty() else ""
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF1F5FB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. Sleek Glassmorphic Hero Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3D66).copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B),
                                        Color(0xFF1E1B4B)
                                    )
                                )
                            )
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Metallic Glowing Logo Emblem
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                PrimaryIndigo,
                                                SecondaryEmerald,
                                                PrimaryIndigo.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF090D16)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_logo_presence),
                                        contentDescription = "Presence Logo",
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "PRESENCE",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 5.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = "Presence to Pagar • Attendance to Salary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Animated Segmented Workspace Switcher Tab Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isEmployee = selectedRole == Role.EMPLOYEE

                        val empBgColor by animateColorAsState(
                            targetValue = if (isEmployee) RoleStaffPrimary else Color.Transparent,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "EmpBg"
                        )
                        val empContentColor by animateColorAsState(
                            targetValue = if (isEmployee) Color.White else Color(0xFF475569),
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "EmpTxt"
                        )

                        Surface(
                            onClick = { viewModel.selectRole(Role.EMPLOYEE) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = empBgColor,
                            shadowElevation = if (isEmployee) 3.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = empContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Employee Login",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = empContentColor
                                )
                            }
                        }

                        val isOwner = selectedRole == Role.OWNER

                        val ownerBgColor by animateColorAsState(
                            targetValue = if (isOwner) RoleOwnerPrimary else Color.Transparent,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "OwnerBg"
                        )
                        val ownerContentColor by animateColorAsState(
                            targetValue = if (isOwner) Color.White else Color(0xFF475569),
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "OwnerTxt"
                        )

                        Surface(
                            onClick = { viewModel.selectRole(Role.OWNER) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = ownerBgColor,
                            shadowElevation = if (isOwner) 3.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = ownerContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Owner Login",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ownerContentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Modern Workspace Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedRole == Role.EMPLOYEE) {
                            EmployeeLoginContent(
                                emailInput = emailInput,
                                onEmailChange = { emailInput = it },
                                otpCodeInput = otpCodeInput,
                                onOtpCodeChange = { otpCodeInput = it },
                                loginStep = loginStep,
                                uiState = uiState,
                                otpState = otpState,
                                isGoogleSigningIn = isGoogleSigningIn,
                                onRequestOtp = { viewModel.requestOtp(emailInput.trim(), role = Role.EMPLOYEE) },
                                onVerifyOtp = { viewModel.verifyOtp(email = emailInput.trim(), code = otpCodeInput, role = Role.EMPLOYEE) },
                                onBackToEmail = {
                                    loginStep = LoginStep.ENTER_EMAIL
                                    viewModel.resetOtp()
                                },
                                onStartGoogle = { startGoogleSignIn(GoogleSignInPurpose.LOGIN) }
                            )
                        } else {
                            OwnerLoginContent(
                                emailInput = emailInput,
                                onEmailChange = { emailInput = it },
                                otpCodeInput = otpCodeInput,
                                onOtpCodeChange = { otpCodeInput = it },
                                loginStep = loginStep,
                                uiState = uiState,
                                otpState = otpState,
                                isGoogleSigningIn = isGoogleSigningIn,
                                onRequestOtp = { viewModel.requestOtp(emailInput.trim(), role = Role.OWNER) },
                                onVerifyOtp = { viewModel.verifyOtp(email = emailInput.trim(), code = otpCodeInput, role = Role.OWNER) },
                                onBackToEmail = {
                                    loginStep = LoginStep.ENTER_EMAIL
                                    viewModel.resetOtp()
                                },
                                onStartGoogle = { startGoogleSignIn(GoogleSignInPurpose.LOGIN) },
                                onRegisterOwner = { showOwnerRegistrationWizard = true }
                            )
                        }

                        // Modern Alert Card for Errors - Displays authoritative server error message
                        AnimatedVisibility(visible = uiState is UiState.Error) {
                            val errorState = uiState as? UiState.Error
                            val errorMsg = errorState?.message ?: "Authentication failed"
                            val errorCode = errorState?.code
                            val isNotRegisteredErr = errorMsg.contains("not registered", ignoreCase = true) ||
                                errorMsg.contains("NOT_REGISTERED", ignoreCase = true) ||
                                errorCode == "NOT_REGISTERED"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF1F2)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = errorMsg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF9F1239),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isNotRegisteredErr) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (selectedRole == Role.OWNER)
                                                    "Hint: Tap 'Register Organization' below to set up your business account."
                                                else
                                                    "Hint: Please ask your Organization Owner to add your email address to the team roster.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFBE123C),
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else if (!errorCode.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Code: $errorCode",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFBE123C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Modern Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Encrypted & Secured by Arman Logical Systems",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Modal Wizard: Owner Self-Registration
    if (showOwnerRegistrationWizard) {
        OwnerRegistrationWizard(
            initialEmail = emailInput,
            otpResponse = otpState,
            uiState = uiState,
            pincodeState = pincodeState,
            gpsLocationState = gpsLocationState,
            reverseGeocodeAddress = reverseGeocodeAddress,
            googleIdentity = googleRegistrationIdentity,
            onDismiss = { showOwnerRegistrationWizard = false },
            onRequestOtp = { ownerEmail ->
                emailInput = ownerEmail
                viewModel.requestOtp(ownerEmail, role = Role.OWNER, isRegistration = true)
            },
            onResetOtp = {
                viewModel.resetOtp()
            },
            onStartGoogle = {
                startGoogleSignIn(GoogleSignInPurpose.OWNER_REGISTRATION)
            },
            onLookupPincode = { pincode ->
                viewModel.lookupPincode(pincode)
            },
            onFetchGpsLocation = {
                viewModel.fetchGpsLocation()
            },
            onCompleteWithOtp = { ownerEmail, code, ownerName, orgName, timezone, currency, premisesName, radius, address, latitude, longitude ->
                registrationSuccessInfo = RegistrationSuccessInfo(
                    email = ownerEmail,
                    ownerName = ownerName,
                    organizationName = orgName,
                    premisesName = premisesName
                )
                viewModel.verifyOtp(
                    email = ownerEmail,
                    code = code,
                    role = Role.OWNER,
                    fullName = ownerName,
                    organizationName = orgName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    premisesAddress = address,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radius
                )
                showOwnerRegistrationWizard = false
            },
            onCompleteWithGoogle = { idToken, orgName, timezone, currency, premisesName, radius, address, latitude, longitude ->
                registrationSuccessInfo = RegistrationSuccessInfo(
                    email = googleRegistrationIdentity?.email.orEmpty(),
                    ownerName = googleRegistrationIdentity?.fullName.orEmpty(),
                    organizationName = orgName,
                    premisesName = premisesName
                )
                viewModel.registerOwnerWithGoogle(
                    idToken = idToken,
                    organizationName = orgName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    radiusMeters = radius,
                    premisesAddress = address,
                    latitude = latitude,
                    longitude = longitude
                )
                showOwnerRegistrationWizard = false
            }
        )
    }

    // Modal Dialog: Owner Registration Success Screen with Knowledge & Greeting
    if (registrationSuccessInfo != null) {
        OwnerRegistrationSuccessDialog(
            successInfo = registrationSuccessInfo!!,
            onGoToLogin = {
                val registeredEmail = registrationSuccessInfo!!.email
                registrationSuccessInfo = null
                viewModel.selectRole(Role.OWNER)
                emailInput = registeredEmail
                loginStep = LoginStep.ENTER_EMAIL
                viewModel.resetOtp()
                viewModel.clearError()
                val currentSuccess = uiState as? UiState.Success<User>
                if (currentSuccess != null) {
                    onLoginSuccess(currentSuccess.data)
                }
            }
        )
    }
}

@Composable
private fun EmployeeLoginContent(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    otpCodeInput: String,
    onOtpCodeChange: (String) -> Unit,
    loginStep: LoginStep,
    uiState: UiState<User>,
    otpState: com.als.presence.data.model.OtpResponse?,
    isGoogleSigningIn: Boolean,
    onRequestOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onBackToEmail: () -> Unit,
    onStartGoogle: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Employee Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoleStaffPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = RoleStaffPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Employee Portal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoleStaffPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Staff",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoleStaffPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Clock-in attendance, rosters & salary wallet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(16.dp))

        // Email OTP Section
        if (loginStep == LoginStep.ENTER_EMAIL) {
            OutlinedTextField(
                value = emailInput,
                onValueChange = onEmailChange,
                label = { Text("Employee work email") },
                placeholder = { Text("e.g. employee@company.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = RoleStaffPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { if (emailInput.isNotBlank()) onRequestOtp() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoleStaffPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestOtp,
                enabled = uiState !is UiState.Loading && emailInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoleStaffPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending Code...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Email OTP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // STEP 2: ENTER OTP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            modifier = Modifier.size(18.dp),
                            tint = RoleStaffPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OTP sent to:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = emailInput,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    TextButton(
                        onClick = onBackToEmail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(14.dp),
                            tint = RoleStaffPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoleStaffPrimary
                        )
                    }
                }
            }

            if (com.als.presence.BuildConfig.DEBUG && !otpState?.devOtp.isNullOrEmpty()) {
                otpState?.devOtp?.let { devOtp ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dev OTP Code:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D4ED8)
                            )
                            Text(
                                text = devOtp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E40AF),
                                letterSpacing = 3.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = otpCodeInput,
                onValueChange = { value ->
                    onOtpCodeChange(value.filter(Char::isDigit).take(6))
                },
                label = { Text("6-Digit OTP Code") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RoleStaffPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (otpCodeInput.isNotBlank()) onVerifyOtp() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoleStaffPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToEmail,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onVerifyOtp,
                    enabled = uiState !is UiState.Loading && otpCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoleStaffPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Verify & Login", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Divider & Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            Text(
                text = "  or sign in with Google  ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        }

        Spacer(modifier = Modifier.height(14.dp))

        val isGoogleAuthLoading = isGoogleSigningIn || (uiState is UiState.Loading && isGoogleSigningIn)

        Button(
            onClick = onStartGoogle,
            enabled = !isGoogleAuthLoading && uiState !is UiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isGoogleAuthLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = RoleStaffPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Signing in with Google...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Employee Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Employee accounts are invited directly by your Organization Owner. Ask your Administrator if you haven't received an invitation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun OwnerLoginContent(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    otpCodeInput: String,
    onOtpCodeChange: (String) -> Unit,
    loginStep: LoginStep,
    uiState: UiState<User>,
    otpState: com.als.presence.data.model.OtpResponse?,
    isGoogleSigningIn: Boolean,
    onRequestOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onBackToEmail: () -> Unit,
    onStartGoogle: () -> Unit,
    onRegisterOwner: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Owner Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoleOwnerPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = RoleOwnerPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Owner Governance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoleOwnerPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoleOwnerPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Organization governance, premises & payroll",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(16.dp))

        // Email OTP Section
        if (loginStep == LoginStep.ENTER_EMAIL) {
            OutlinedTextField(
                value = emailInput,
                onValueChange = onEmailChange,
                label = { Text("Owner business email") },
                placeholder = { Text("e.g. owner@company.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = RoleOwnerPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { if (emailInput.isNotBlank()) onRequestOtp() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoleOwnerPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestOtp,
                enabled = uiState !is UiState.Loading && emailInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoleOwnerPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending Code...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Email OTP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // STEP 2: ENTER OTP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            modifier = Modifier.size(18.dp),
                            tint = RoleOwnerPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OTP sent to:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = emailInput,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    TextButton(
                        onClick = onBackToEmail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(14.dp),
                            tint = RoleOwnerPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoleOwnerPrimary
                        )
                    }
                }
            }

            if (com.als.presence.BuildConfig.DEBUG && !otpState?.devOtp.isNullOrEmpty()) {
                otpState?.devOtp?.let { devOtp ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color(0xFFF5F3FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dev OTP Code:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6D28D9)
                            )
                            Text(
                                text = devOtp,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF5B21B6),
                                letterSpacing = 3.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = otpCodeInput,
                onValueChange = { value ->
                    onOtpCodeChange(value.filter(Char::isDigit).take(6))
                },
                label = { Text("6-Digit OTP Code") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RoleOwnerPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (otpCodeInput.isNotBlank()) onVerifyOtp() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoleOwnerPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToEmail,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onVerifyOtp,
                    enabled = uiState !is UiState.Loading && otpCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoleOwnerPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Verify & Login", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Divider & Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            Text(
                text = "  or sign in with Google  ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        }

        Spacer(modifier = Modifier.height(14.dp))

        val isGoogleAuthLoading = isGoogleSigningIn || (uiState is UiState.Loading && isGoogleSigningIn)

        Button(
            onClick = onStartGoogle,
            enabled = !isGoogleAuthLoading && uiState !is UiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isGoogleAuthLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = RoleOwnerPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Signing in with Google...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Prominent Registration Card for New Owners
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RoleOwnerPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = RoleOwnerPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Owner? Register Business",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoleOwnerPrimary
                        )
                        Text(
                            text = "Configure your organization & premises in 2 mins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B21A8),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRegisterOwner,
                    colors = ButtonDefaults.buttonColors(containerColor = RoleOwnerPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Register Organization Now",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog presented after an Owner completes registration, providing a warm greeting,
 * organization summary, quick-start guide, and a prominent "Go to Owner Login" button.
 */
@Composable
private fun OwnerRegistrationSuccessDialog(
    successInfo: RegistrationSuccessInfo,
    onGoToLogin: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Warm Greeting
                Text(
                    text = if (successInfo.ownerName.isNotBlank()) "Welcome, ${successInfo.ownerName}! 👋" else "Welcome to PRESENCE! 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "🎉 Organization Registered Successfully!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Organization Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReviewRow(label = "Organization", value = successInfo.organizationName)
                        ReviewRow(label = "Owner Email", value = successInfo.email)
                        ReviewRow(label = "Initial Premises", value = successInfo.premisesName)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Start Knowledge Guide for Owners
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = RoleOwnerPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to use PRESENCE (Quick Start)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoleOwnerPrimary
                            )
                        }

                        HorizontalDivider(color = Color(0xFFDDD6FE))

                        QuickGuideStep(
                            icon = Icons.Default.Group,
                            title = "1. Add Employees",
                            description = "Go to Workforce in your Owner Dashboard to invite employees by email."
                        )

                        QuickGuideStep(
                            icon = Icons.Default.Schedule,
                            title = "2. Configure Shifts",
                            description = "Define premises shifts and assign staff members to work rosters."
                        )

                        QuickGuideStep(
                            icon = Icons.Default.Payments,
                            title = "3. Automated Ledgers & Payroll",
                            description = "GPS geofence + ONNX face verification automatically calculates earnings & payslips."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary "Go to Owner Login" Button
                Button(
                    onClick = onGoToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoleOwnerPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go to Owner Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGuideStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = RoleOwnerPrimary.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RoleOwnerPrimary,
                modifier = Modifier
                    .padding(5.dp)
                    .size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E1065)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5B21B6),
                fontSize = 11.sp
            )
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
