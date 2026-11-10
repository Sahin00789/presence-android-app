package com.als.presence.ui.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.RoleOwnerPrimary
import com.als.presence.core.designsystem.RoleStaffPrimary
import com.als.presence.data.model.GoogleRegistrationIdentity
import com.als.presence.data.model.GoogleSignInPurpose
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

enum class AuthFlowStep {
    ROLE_SELECTION,
    EMPLOYEE_EMAIL,
    EMPLOYEE_OTP,
    OWNER_TYPE_SELECTION,
    OWNER_EMAIL,
    OWNER_OTP,
    REG_EMAIL,
    REG_OTP,
    REG_ORG_DETAILS,
    REG_PREMISES,
    REG_SUCCESS
}

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

    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(context.getString(R.string.google_web_client_id), true)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    var authStep by remember { mutableStateOf(AuthFlowStep.ROLE_SELECTION) }

    // Inputs for Login
    var emailInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }

    // Inputs for Registration
    var regEmail by remember { mutableStateOf("") }
    var regOtp by remember { mutableStateOf("") }
    var regOwnerName by remember { mutableStateOf("") }
    var regOrgName by remember { mutableStateOf("") }
    var regTimezone by remember { mutableStateOf("Asia/Kolkata") }
    var regCurrency by remember { mutableStateOf("INR") }
    var regPremisesName by remember { mutableStateOf("Main Office") }
    var regStreetAddress by remember { mutableStateOf("") }
    var regRadius by remember { mutableStateOf("50") }
    var regLatitude by remember { mutableStateOf<String?>(null) }
    var regLongitude by remember { mutableStateOf<String?>(null) }

    var registeredUser by remember { mutableStateOf<User?>(null) }
    var googleSignInPurpose by remember { mutableStateOf(GoogleSignInPurpose.LOGIN) }
    var googleRegistrationIdentity by remember { mutableStateOf<GoogleRegistrationIdentity?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        if (intentData != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(intentData)
                    .getResult(ApiException::class.java)
                when (googleSignInPurpose) {
                    GoogleSignInPurpose.LOGIN -> {
                        val idToken = account.idToken
                        val code = account.serverAuthCode
                        if (idToken.isNullOrBlank() && code.isNullOrBlank()) {
                            isGoogleSigningIn = false
                            viewModel.showError("Google did not return credentials. Please try again.")
                        } else {
                            viewModel.loginWithGoogle(idToken = idToken, serverAuthCode = code)
                        }
                    }
                    GoogleSignInPurpose.OWNER_REGISTRATION -> {
                        isGoogleSigningIn = false
                        val idToken = account.idToken
                        if (idToken.isNullOrBlank() || account.email.isNullOrBlank()) {
                            viewModel.showError("Google did not return verified account details. Please try again.")
                        } else {
                            val identity = GoogleRegistrationIdentity(
                                idToken = idToken,
                                email = account.email.orEmpty(),
                                fullName = account.displayName.orEmpty()
                            )
                            googleRegistrationIdentity = identity
                            regEmail = identity.email
                            regOwnerName = identity.fullName
                            authStep = AuthFlowStep.REG_ORG_DETAILS
                        }
                    }
                }
            } catch (error: ApiException) {
                isGoogleSigningIn = false
                val statusCode = error.statusCode
                if (statusCode != 12501 && statusCode != 16) {
                    val friendlyMessage = when (statusCode) {
                        10 -> "Google Developer Error (10): Verify package name and SHA-1 fingerprint."
                        7 -> "Network Error: Google services unreachable. Please check your connection."
                        12500 -> "Google Play Services error. Please update Play Services."
                        else -> "Google sign-in failed (Code $statusCode)."
                    }
                    viewModel.showError(friendlyMessage)
                }
            } catch (error: Exception) {
                isGoogleSigningIn = false
                viewModel.showError("Google sign-in failed: ${error.localizedMessage ?: "Unknown error"}")
            }
        } else {
            isGoogleSigningIn = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.tryAutoLogin()
    }

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Loading) {
            isGoogleSigningIn = false
        }
        if (uiState is UiState.Success) {
            val user = (uiState as UiState.Success<User>).data
            if (authStep == AuthFlowStep.REG_PREMISES) {
                registeredUser = user
                authStep = AuthFlowStep.REG_SUCCESS
            } else if (authStep != AuthFlowStep.REG_SUCCESS) {
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
                if (purpose == GoogleSignInPurpose.OWNER_REGISTRATION) {
                    regEmail = identity.email
                    regOwnerName = identity.fullName
                    authStep = AuthFlowStep.REG_ORG_DETAILS
                }
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

    LaunchedEffect(otpState) {
        val otp = otpState
        if (otp != null) {
            when (authStep) {
                AuthFlowStep.EMPLOYEE_EMAIL -> {
                    authStep = AuthFlowStep.EMPLOYEE_OTP
                    otpCodeInput = ""
                    Toast.makeText(context, "Verification code sent to ${otp.email}", Toast.LENGTH_SHORT).show()
                }
                AuthFlowStep.OWNER_EMAIL -> {
                    authStep = AuthFlowStep.OWNER_OTP
                    otpCodeInput = ""
                    Toast.makeText(context, "Verification code sent to ${otp.email}", Toast.LENGTH_SHORT).show()
                }
                AuthFlowStep.REG_EMAIL -> {
                    authStep = AuthFlowStep.REG_OTP
                    regOtp = ""
                    Toast.makeText(context, "Registration code sent to ${otp.email}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    // Auto-update location when GPS is fetched in registration
    LaunchedEffect(gpsLocationState) {
        if (gpsLocationState is UiState.Success) {
            val location = (gpsLocationState as UiState.Success).data
            regLatitude = location.latitude.toString()
            regLongitude = location.longitude.toString()
        }
    }

    // Clean back navigation per flow step
    BackHandler(enabled = authStep != AuthFlowStep.ROLE_SELECTION && authStep != AuthFlowStep.REG_SUCCESS) {
        when (authStep) {
            AuthFlowStep.EMPLOYEE_EMAIL -> {
                viewModel.clearError()
                authStep = AuthFlowStep.ROLE_SELECTION
            }
            AuthFlowStep.EMPLOYEE_OTP -> {
                viewModel.clearError()
                authStep = AuthFlowStep.EMPLOYEE_EMAIL
            }
            AuthFlowStep.OWNER_TYPE_SELECTION -> {
                viewModel.clearError()
                authStep = AuthFlowStep.ROLE_SELECTION
            }
            AuthFlowStep.OWNER_EMAIL -> {
                viewModel.clearError()
                authStep = AuthFlowStep.OWNER_TYPE_SELECTION
            }
            AuthFlowStep.OWNER_OTP -> {
                viewModel.clearError()
                authStep = AuthFlowStep.OWNER_EMAIL
            }
            AuthFlowStep.REG_EMAIL -> {
                viewModel.clearError()
                authStep = AuthFlowStep.OWNER_TYPE_SELECTION
            }
            AuthFlowStep.REG_OTP -> {
                viewModel.clearError()
                authStep = AuthFlowStep.REG_EMAIL
            }
            AuthFlowStep.REG_ORG_DETAILS -> {
                viewModel.clearError()
                authStep = if (googleRegistrationIdentity != null) AuthFlowStep.REG_EMAIL else AuthFlowStep.REG_OTP
            }
            AuthFlowStep.REG_PREMISES -> {
                viewModel.clearError()
                authStep = AuthFlowStep.REG_ORG_DETAILS
            }
            else -> {}
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        AnimatedContent(
            targetState = authStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { -it / 3 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(animationSpec = tween(320)) { -it } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(320)) { it / 3 } + fadeOut(tween(220)))
                }
            },
            label = "AuthFlowTransition"
        ) { targetStep ->
            when (targetStep) {
                AuthFlowStep.ROLE_SELECTION -> {
                    ModernRoleSelectionScreen(
                        onSelectEmployee = {
                            viewModel.selectRole(Role.EMPLOYEE)
                            viewModel.clearError()
                            viewModel.resetOtp()
                            emailInput = ""
                            otpCodeInput = ""
                            authStep = AuthFlowStep.EMPLOYEE_EMAIL
                        },
                        onSelectOwner = {
                            viewModel.selectRole(Role.OWNER)
                            viewModel.clearError()
                            viewModel.resetOtp()
                            emailInput = ""
                            otpCodeInput = ""
                            authStep = AuthFlowStep.OWNER_TYPE_SELECTION
                        }
                    )
                }
                AuthFlowStep.EMPLOYEE_EMAIL -> {
                    ModernEmailScreen(
                        role = Role.EMPLOYEE,
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        uiState = uiState,
                        isGoogleSigningIn = isGoogleSigningIn,
                        onRequestOtp = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.EMPLOYEE, isRegistration = false)
                        },
                        onStartGoogle = { startGoogleSignIn(GoogleSignInPurpose.LOGIN) },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.ROLE_SELECTION
                        }
                    )
                }
                AuthFlowStep.EMPLOYEE_OTP -> {
                    ModernOtpScreen(
                        role = Role.EMPLOYEE,
                        email = emailInput,
                        otpCodeInput = otpCodeInput,
                        onOtpChange = { otpCodeInput = it },
                        otpState = otpState,
                        uiState = uiState,
                        primaryColor = RoleStaffPrimary,
                        onResendOtp = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.EMPLOYEE, isRegistration = false)
                        },
                        onVerify = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.verifyOtp(email = clean, code = otpCodeInput, role = Role.EMPLOYEE)
                        },
                        onChangeEmail = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.EMPLOYEE_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.EMPLOYEE_EMAIL
                        }
                    )
                }
                AuthFlowStep.OWNER_TYPE_SELECTION -> {
                    ModernOwnerTypeScreen(
                        onSelectExistingOwner = {
                            viewModel.clearError()
                            viewModel.resetOtp()
                            emailInput = ""
                            otpCodeInput = ""
                            authStep = AuthFlowStep.OWNER_EMAIL
                        },
                        onSelectNewOwner = {
                            viewModel.clearError()
                            viewModel.resetOtp()
                            regEmail = ""
                            regOtp = ""
                            regOwnerName = ""
                            regOrgName = ""
                            regPremisesName = "Main Office"
                            regStreetAddress = ""
                            googleRegistrationIdentity = null
                            authStep = AuthFlowStep.REG_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.ROLE_SELECTION
                        }
                    )
                }
                AuthFlowStep.OWNER_EMAIL -> {
                    ModernEmailScreen(
                        role = Role.OWNER,
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        uiState = uiState,
                        isGoogleSigningIn = isGoogleSigningIn,
                        onRequestOtp = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.OWNER, isRegistration = false)
                        },
                        onStartGoogle = { startGoogleSignIn(GoogleSignInPurpose.LOGIN) },
                        onGoToRegistration = {
                            viewModel.clearError()
                            regEmail = emailInput
                            regOtp = ""
                            regOwnerName = ""
                            regOrgName = ""
                            regPremisesName = "Main Office"
                            regStreetAddress = ""
                            googleRegistrationIdentity = null
                            authStep = AuthFlowStep.REG_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.OWNER_TYPE_SELECTION
                        }
                    )
                }
                AuthFlowStep.OWNER_OTP -> {
                    ModernOtpScreen(
                        role = Role.OWNER,
                        email = emailInput,
                        otpCodeInput = otpCodeInput,
                        onOtpChange = { otpCodeInput = it },
                        otpState = otpState,
                        uiState = uiState,
                        primaryColor = RoleOwnerPrimary,
                        onResendOtp = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.OWNER, isRegistration = false)
                        },
                        onVerify = {
                            val clean = emailInput.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.verifyOtp(email = clean, code = otpCodeInput, role = Role.OWNER)
                        },
                        onChangeEmail = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.OWNER_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.OWNER_EMAIL
                        }
                    )
                }

                // ==================== NEW OWNER REGISTRATION FLOW ====================
                AuthFlowStep.REG_EMAIL -> {
                    RegistrationEmailScreen(
                        emailInput = regEmail,
                        onEmailChange = { regEmail = it },
                        uiState = uiState,
                        isGoogleSigningIn = isGoogleSigningIn,
                        onRequestOtp = {
                            val clean = regEmail.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.OWNER, isRegistration = true)
                        },
                        onStartGoogle = { startGoogleSignIn(GoogleSignInPurpose.OWNER_REGISTRATION) },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.OWNER_TYPE_SELECTION
                        }
                    )
                }
                AuthFlowStep.REG_OTP -> {
                    RegistrationOtpScreen(
                        email = regEmail,
                        otpCodeInput = regOtp,
                        onOtpChange = { regOtp = it },
                        otpState = otpState,
                        onResendOtp = {
                            val clean = regEmail.replace(Regex("[\\s\\u00A0]+"), "").lowercase()
                            viewModel.requestOtp(clean, role = Role.OWNER, isRegistration = true)
                        },
                        onContinue = {
                            authStep = AuthFlowStep.REG_ORG_DETAILS
                        },
                        onChangeEmail = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.REG_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.REG_EMAIL
                        }
                    )
                }
                AuthFlowStep.REG_ORG_DETAILS -> {
                    RegistrationOrgDetailsScreen(
                        ownerName = regOwnerName,
                        onOwnerNameChange = { regOwnerName = it },
                        orgName = regOrgName,
                        onOrgNameChange = { regOrgName = it },
                        timezone = regTimezone,
                        onTimezoneChange = { regTimezone = it },
                        currency = regCurrency,
                        onCurrencyChange = { regCurrency = it },
                        onNext = {
                            authStep = AuthFlowStep.REG_PREMISES
                        },
                        onBack = {
                            authStep = if (googleRegistrationIdentity != null) AuthFlowStep.REG_EMAIL else AuthFlowStep.REG_OTP
                        }
                    )
                }
                AuthFlowStep.REG_PREMISES -> {
                    RegistrationPremisesScreen(
                        premisesName = regPremisesName,
                        onPremisesNameChange = { regPremisesName = it },
                        streetAddress = regStreetAddress,
                        onStreetAddressChange = { regStreetAddress = it },
                        radius = regRadius,
                        onRadiusChange = { regRadius = it },
                        latitude = regLatitude,
                        longitude = regLongitude,
                        gpsLocationState = gpsLocationState,
                        reverseGeocodeAddress = reverseGeocodeAddress,
                        uiState = uiState,
                        onFetchGps = { viewModel.fetchGpsLocation() },
                        onSubmit = {
                            val radiusInt = regRadius.toIntOrNull() ?: 50
                            viewModel.setRegistrationSuccessPending(true)
                            if (googleRegistrationIdentity != null) {
                                viewModel.registerOwnerWithGoogle(
                                    idToken = googleRegistrationIdentity!!.idToken,
                                    organizationName = regOrgName.trim(),
                                    timezone = regTimezone,
                                    currency = regCurrency,
                                    premisesName = regPremisesName.trim(),
                                    radiusMeters = radiusInt,
                                    premisesAddress = regStreetAddress.takeIf { it.isNotBlank() },
                                    latitude = regLatitude,
                                    longitude = regLongitude
                                )
                            } else {
                                viewModel.verifyOtp(
                                    email = regEmail.replace(Regex("[\\s\\u00A0]+"), "").lowercase(),
                                    code = regOtp.trim(),
                                    role = Role.OWNER,
                                    fullName = regOwnerName.trim(),
                                    organizationName = regOrgName.trim(),
                                    timezone = regTimezone,
                                    currency = regCurrency,
                                    premisesName = regPremisesName.trim(),
                                    premisesAddress = regStreetAddress.takeIf { it.isNotBlank() },
                                    latitude = regLatitude,
                                    longitude = regLongitude,
                                    radiusMeters = radiusInt
                                )
                            }
                        },
                        onBack = {
                            authStep = AuthFlowStep.REG_ORG_DETAILS
                        }
                    )
                }
                AuthFlowStep.REG_SUCCESS -> {
                    RegistrationSuccessScreen(
                        ownerName = regOwnerName,
                        orgName = regOrgName,
                        premisesName = regPremisesName,
                        email = if (googleRegistrationIdentity != null) googleRegistrationIdentity!!.email else regEmail,
                        onGoToDashboard = {
                            viewModel.setRegistrationSuccessPending(false)
                            val finalUser = registeredUser ?: (uiState as? UiState.Success<User>)?.data
                            if (finalUser != null) {
                                onLoginSuccess(finalUser)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Screen 1: Modern Full-Height Role Selection
 */
@Composable
private fun ModernRoleSelectionScreen(
    onSelectEmployee: () -> Unit,
    onSelectOwner: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF10B981),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090D16)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_presence),
                        contentDescription = "Presence Logo",
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PRESENCE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Text(
                    text = "Presence to Pagar • Attendance to Salary",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Choose your workspace",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select your role to access your dedicated dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Employee Card
            Surface(
                onClick = onSelectEmployee,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, Color(0xFF059669).copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF059669), Color(0xFF10B981))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "STAFF & WORKFORCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Employee",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Face & GPS clock-in attendance, view shift rosters, track real-time earnings and view monthly payslips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onSelectEmployee,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(
                            text = "Continue as Employee",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Owner Card
            Surface(
                onClick = onSelectOwner,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ADMIN & MANAGEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Business Owner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Oversee premises, manage workforce roster, configure shifts, lock monthly payroll ledgers and monitor attendance compliance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onSelectOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text(
                            text = "Continue as Owner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        FooterBrand(dark = true)
    }
}

/**
 * Screen 2E & 3O: Modern Full-Width Email Input Screen
 */
@Composable
private fun ModernEmailScreen(
    role: Role,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    uiState: UiState<User>,
    isGoogleSigningIn: Boolean,
    onRequestOtp: () -> Unit,
    onStartGoogle: () -> Unit,
    onGoToRegistration: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isOwner = role == Role.OWNER
    val primaryColor = if (isOwner) Color(0xFF6366F1) else Color(0xFF10B981)
    val badgeLabel = if (isOwner) "Owner Sign In" else "Employee Workspace"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = if (isOwner) "Sign in as Owner" else "Enter work email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOwner)
                    "Enter your business email to receive an instant verification code."
                else
                    "Enter the email registered by your employer to access your attendance workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                placeholder = { Text(if (isOwner) "owner@company.com" else "employee@company.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (emailInput.isNotBlank()) {
                        IconButton(onClick = { onEmailChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestOtp,
                enabled = uiState !is UiState.Loading && emailInput.replace(Regex("[\\s\\u00A0]+"), "").isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (uiState is UiState.Loading && !isGoogleSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sending Code...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(18.dp))

            val isGoogleAuthLoading = isGoogleSigningIn || (uiState is UiState.Loading && isGoogleSigningIn)

            Button(
                onClick = onStartGoogle,
                enabled = !isGoogleAuthLoading && uiState !is UiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                if (isGoogleAuthLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = primaryColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting Google...", fontWeight = FontWeight.Bold)
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            AuthErrorCard(
                uiState = uiState,
                isOwner = isOwner,
                onRegisterOrg = { onGoToRegistration?.invoke() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isOwner) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Employees must be registered and invited by an Organization Owner. If you do not have an account, please contact your business administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else if (onGoToRegistration != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFAF5FF),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New business owner?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                            Text(
                                text = "Register and configure a new organization",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7E22CE)
                            )
                        }
                        TextButton(onClick = onGoToRegistration) {
                            Text(
                                text = "Register →",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7E22CE)
                            )
                        }
                    }
                }
            }
        }

        FooterBrand()
    }
}

/**
 * Screen 2O: Owner Options ("Existing Owner" vs "New Owner")
 */
@Composable
private fun ModernOwnerTypeScreen(
    onSelectExistingOwner: () -> Unit,
    onSelectNewOwner: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Owner Portal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome, Business Owner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please select whether you want to sign in to your registered organization or create a new business account.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card 1: Existing Owner
            Surface(
                onClick = onSelectExistingOwner,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFFC7D2FE)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF4F46E5).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "ALREADY REGISTERED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Existing Owner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Sign in to access your organization dashboard, manage active premises, track employee clock-ins and finalize payroll.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onSelectExistingOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Sign In as Existing Owner", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: New Owner Registration
            Surface(
                onClick = onSelectNewOwner,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFAF5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "FAST 2-MIN SETUP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7E22CE),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New Owner Registration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Register a new business, configure your initial premises with GPS geofence radius, and set up your attendance system in minutes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onSelectNewOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                    ) {
                        Text("Register New Organization", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        FooterBrand()
    }
}

/**
 * Screen 3E & 4O: Modern 6-Cell OTP Verification Screen for Login
 */
@Composable
private fun ModernOtpScreen(
    role: Role,
    email: String,
    otpCodeInput: String,
    onOtpChange: (String) -> Unit,
    otpState: com.als.presence.data.model.OtpResponse?,
    uiState: UiState<User>,
    primaryColor: Color,
    onResendOtp: () -> Unit,
    onVerify: () -> Unit,
    onChangeEmail: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isOwner = role == Role.OWNER
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isOwner) "Owner Verification" else "Staff Verification",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "We sent a 6-digit code to ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onChangeEmail,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change email",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val devOtp = otpState?.devOtp
            if (!devOtp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = { onOtpChange(devOtp) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Dev Mode Code: $devOtp (Tap to fill)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 6-Cell Digit Pin Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = otpCodeInput,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit).take(6)
                        onOtpChange(digits)
                        if (digits.length == 6) {
                            onVerify()
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (otpCodeInput.length == 6) onVerify() }
                    ),
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val char = otpCodeInput.getOrNull(i)?.toString() ?: ""
                        val isFocused = otpCodeInput.length == i

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isFocused) 2.dp else 1.2.dp,
                                    color = when {
                                        isFocused -> primaryColor
                                        char.isNotEmpty() -> Color(0xFF0F172A)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onVerify,
                enabled = uiState !is UiState.Loading && otpCodeInput.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verifying...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Verify & Enter Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive code?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )

                TextButton(
                    onClick = onResendOtp,
                    enabled = uiState !is UiState.Loading
                ) {
                    Text(
                        text = "Resend Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            AuthErrorCard(uiState = uiState, isOwner = isOwner, onRegisterOrg = {})
        }

        FooterBrand()
    }
}

// =========================================================================
// ==================== NEW OWNER REGISTRATION SCREENS =====================
// =========================================================================

/**
 * Registration Step 1: Owner Email
 */
@Composable
private fun RegistrationEmailScreen(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    uiState: UiState<User>,
    isGoogleSigningIn: Boolean,
    onRequestOtp: () -> Unit,
    onStartGoogle: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Step 1 of 4 • Email",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Register Your Business",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your business email address to verify your owner account and start setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = onEmailChange,
                label = { Text("Business Email Address") },
                placeholder = { Text("e.g. owner@mybusiness.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(22.dp)
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7E22CE),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestOtp,
                enabled = uiState !is UiState.Loading && emailInput.replace(Regex("[\\s\\u00A0]+"), "").isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (uiState is UiState.Loading && !isGoogleSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sending Code...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(
                    text = "  or register with Google  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(modifier = Modifier.height(18.dp))

            val isGoogleAuthLoading = isGoogleSigningIn || (uiState is UiState.Loading && isGoogleSigningIn)

            Button(
                onClick = onStartGoogle,
                enabled = !isGoogleAuthLoading && uiState !is UiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                if (isGoogleAuthLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF7E22CE),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting Google...", fontWeight = FontWeight.Bold)
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            AuthErrorCard(uiState = uiState, isOwner = true, onRegisterOrg = {})
        }

        FooterBrand()
    }
}

/**
 * Registration Step 2: Owner OTP Verification
 */
@Composable
private fun RegistrationOtpScreen(
    email: String,
    otpCodeInput: String,
    onOtpChange: (String) -> Unit,
    otpState: com.als.presence.data.model.OtpResponse?,
    onResendOtp: () -> Unit,
    onContinue: () -> Unit,
    onChangeEmail: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Step 2 of 4 • Verify Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Verify Owner Email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Code sent to ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onChangeEmail,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change email",
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val devOtp = otpState?.devOtp
            if (!devOtp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = { onOtpChange(devOtp) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Dev Mode Code: $devOtp (Tap to fill)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = otpCodeInput,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit).take(6)
                        onOtpChange(digits)
                        if (digits.length == 6) {
                            onContinue()
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (otpCodeInput.length == 6) onContinue() }
                    ),
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val char = otpCodeInput.getOrNull(i)?.toString() ?: ""
                        val isFocused = otpCodeInput.length == i

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isFocused) 2.dp else 1.2.dp,
                                    color = when {
                                        isFocused -> Color(0xFF7E22CE)
                                        char.isNotEmpty() -> Color(0xFF0F172A)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onContinue,
                enabled = otpCodeInput.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Confirm & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive code?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )

                TextButton(onClick = onResendOtp) {
                    Text(
                        text = "Resend Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE)
                    )
                }
            }
        }

        FooterBrand()
    }
}

/**
 * Registration Step 3: Owner & Organization Details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationOrgDetailsScreen(
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    orgName: String,
    onOrgNameChange: (String) -> Unit,
    timezone: String,
    onTimezoneChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var timezoneExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val timezoneOptions = listOf("Asia/Kolkata", "UTC", "Asia/Dubai", "Europe/London", "America/New_York", "Asia/Singapore")
    val currencyOptions = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Step 3 of 4 • Organization",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Organization Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tell us your name and the name of your organization.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                label = { Text("Owner Full Name") },
                placeholder = { Text("e.g. John Doe") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7E22CE),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = orgName,
                onValueChange = onOrgNameChange,
                label = { Text("Organization Name") },
                placeholder = { Text("e.g. Acme Enterprises") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7E22CE),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Timezone Dropdown
                ExposedDropdownMenuBox(
                    expanded = timezoneExpanded,
                    onExpandedChange = { timezoneExpanded = it },
                    modifier = Modifier.weight(1.3f)
                ) {
                    OutlinedTextField(
                        value = timezone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Timezone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7E22CE),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = timezoneExpanded,
                        onDismissRequest = { timezoneExpanded = false }
                    ) {
                        timezoneOptions.forEach { tz ->
                            DropdownMenuItem(
                                text = { Text(tz) },
                                onClick = {
                                    onTimezoneChange(tz)
                                    timezoneExpanded = false
                                }
                            )
                        }
                    }
                }

                // Currency Dropdown
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7E22CE),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencyOptions.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur) },
                                onClick = {
                                    onCurrencyChange(cur)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onNext,
                enabled = ownerName.trim().isNotBlank() && orgName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Next: Setup Premises", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        FooterBrand()
    }
}

/**
 * Registration Step 4: Premises & Geofence Setup
 */
@Composable
private fun RegistrationPremisesScreen(
    premisesName: String,
    onPremisesNameChange: (String) -> Unit,
    streetAddress: String,
    onStreetAddressChange: (String) -> Unit,
    radius: String,
    onRadiusChange: (String) -> Unit,
    latitude: String?,
    longitude: String?,
    gpsLocationState: UiState<*>,
    reverseGeocodeAddress: com.als.presence.core.location.ReverseGeocodedAddress?,
    uiState: UiState<User>,
    onFetchGps: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Step 4 of 4 • Premises & Geofence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Primary Premises Setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure your work location where employees will verify attendance.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = premisesName,
                onValueChange = onPremisesNameChange,
                label = { Text("Premises / Branch Name") },
                placeholder = { Text("e.g. Main Outlet, Head Office") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7E22CE),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = streetAddress,
                onValueChange = onStreetAddressChange,
                label = { Text("Street Address (Optional)") },
                placeholder = { Text("e.g. 123 MG Road, Bengaluru") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7E22CE),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // GPS Geofence Location Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GPS Geofence Center",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (latitude != null && longitude != null)
                                    "Lat: $latitude, Lng: $longitude"
                                else
                                    "No GPS coordinates captured yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (latitude != null) Color(0xFF15803D) else Color(0xFF64748B)
                            )
                        }

                        OutlinedButton(
                            onClick = onFetchGps,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF7E22CE))
                        ) {
                            if (gpsLocationState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF7E22CE),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF7E22CE),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (latitude != null) "Re-capture" else "Capture GPS",
                                    color = Color(0xFF7E22CE),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (reverseGeocodeAddress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val formattedAddr = listOf(
                            reverseGeocodeAddress.streetAddress,
                            reverseGeocodeAddress.district,
                            reverseGeocodeAddress.state,
                            reverseGeocodeAddress.pincode
                        ).filter { it.isNotBlank() }.joinToString(", ")
                        if (formattedAddr.isNotBlank()) {
                            Text(
                                text = "📍 $formattedAddr",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Allowed Clock-in Radius: ${radius}m",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50", "100", "200", "500").forEach { r ->
                            val isSelected = radius == r
                            Surface(
                                onClick = { onRadiusChange(r) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF7E22CE) else Color(0xFFF1F5F9),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "${r}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            AuthErrorCard(uiState = uiState, isOwner = true, onRegisterOrg = {})

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSubmit,
                enabled = uiState !is UiState.Loading && premisesName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Creating Organization...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Create Organization & Finish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        FooterBrand()
    }
}

/**
 * Registration Step 5: Full-Bleed Registration Success Screen with Big Dashboard Button
 */
@Composable
private fun RegistrationSuccessScreen(
    ownerName: String,
    orgName: String,
    premisesName: String,
    email: String,
    onGoToDashboard: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669).copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome, ${if (ownerName.isNotBlank()) ownerName else "Partner"}! 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🎉 Organization Registered Successfully",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF34D399),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReviewRowDark(label = "Organization", value = orgName)
                    ReviewRowDark(label = "Owner Email", value = email)
                    ReviewRowDark(label = "Initial Premises", value = premisesName)
                    ReviewRowDark(label = "Status", value = "Active & Configured")
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quick Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Start Steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC)
                    )

                    HorizontalDivider(color = Color(0xFF334155))

                    QuickGuideStepDark(
                        icon = Icons.Default.Group,
                        title = "1. Add Employees",
                        description = "Invite your employees by email in the Workforce directory."
                    )

                    QuickGuideStepDark(
                        icon = Icons.Default.Schedule,
                        title = "2. Configure Shifts",
                        description = "Define working hours, weekly offs, and assign shifts."
                    )

                    QuickGuideStepDark(
                        icon = Icons.Default.Payments,
                        title = "3. Automated Ledgers & Payroll",
                        description = "Attendance automatically records daily earnings and monthly payslips."
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Big prominent "Go to Dashboard" button!
        Button(
            onClick = onGoToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Go to Owner Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun QuickGuideStepDark(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = Color(0xFF6366F1).copy(alpha = 0.2f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFA5B4FC),
                modifier = Modifier
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ReviewRowDark(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AuthErrorCard(
    uiState: UiState<User>,
    isOwner: Boolean,
    onRegisterOrg: () -> Unit
) {
    AnimatedVisibility(visible = uiState is UiState.Error) {
        val errorState = uiState as? UiState.Error
        val errorMsg = errorState?.message ?: "Authentication failed"
        val errorCode = errorState?.code
        val isNotRegistered = errorMsg.contains("not registered", ignoreCase = true) ||
            errorMsg.contains("NOT_REGISTERED", ignoreCase = true) ||
            errorCode == "NOT_REGISTERED"

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF1F2),
            border = BorderStroke(1.dp, Color(0xFFFECDD3))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9F1239),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isNotRegistered && isOwner) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRegisterOrg,
                        colors = ButtonDefaults.buttonColors(containerColor = RoleOwnerPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Register as New Owner",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterBrand(dark: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (dark) Color(0xFF64748B) else Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Encrypted & Secured by Arman Logical Systems",
            style = MaterialTheme.typography.labelSmall,
            color = if (dark) Color(0xFF64748B) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
