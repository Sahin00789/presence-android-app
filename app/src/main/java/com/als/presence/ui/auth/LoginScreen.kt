package com.als.presence.ui.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.data.model.GoogleRegistrationIdentity
import com.als.presence.data.model.GoogleSignInPurpose
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.ui.auth.components.AccountDeactivatedDialog
import com.als.presence.ui.auth.components.EmployeeActivationDialog
import com.als.presence.ui.auth.components.EmployeePendingPremisesDialog
import com.als.presence.ui.auth.components.RoleMismatchDialog
import com.als.presence.ui.auth.components.UnregisteredEmployeeDialog
import com.als.presence.ui.auth.components.UnregisteredOwnerDialog
import com.als.presence.ui.auth.screens.AuthEmailScreen
import com.als.presence.ui.auth.screens.AuthOtpScreen
import com.als.presence.ui.auth.screens.RegistrationSuccessScreen
import com.als.presence.ui.auth.screens.RoleSelectionScreen
import com.als.presence.ui.auth.screens.WelcomeScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class AuthFlowStep {
    WELCOME,
    ROLE_SELECTION,
    LOGIN_EMAIL,
    OTP_VERIFICATION,
    OWNER_REGISTRATION_WIZARD,
    REGISTRATION_SUCCESS
}

/**
 * Award-Winning Presence Authentication Host Screen.
 * Orchestrates Welcome, Role Selection, Email OTP, Google SSO (Android Credential Manager),
 * Owner Registration Wizard (with persistent draft restoration), Employee Activation Guide,
 * and Backend Account Validation dialogs.
 */
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
    val rateLimitCountdown by viewModel.rateLimitCountdown.collectAsState()
    val roleMismatch by viewModel.roleMismatch.collectAsState()
    val employeePendingPremises by viewModel.employeePendingPremises.collectAsState()

    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(context.getString(R.string.google_web_client_id), true)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    var selectedRole by remember { mutableStateOf(viewModel.selectedRole.value) }
    var authStep by remember { mutableStateOf(AuthFlowStep.WELCOME) }
    var emailInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var googleRegistrationIdentity by remember { mutableStateOf<GoogleRegistrationIdentity?>(null) }
    var showEmployeeActivationGuide by remember { mutableStateOf(false) }
    var showUnregisteredEmployeeDialog by remember { mutableStateOf(false) }
    var showUnregisteredOwnerDialog by remember { mutableStateOf(false) }
    var showDeactivatedDialog by remember { mutableStateOf(false) }

    // Last registered details for success screen
    var registeredOrgName by remember { mutableStateOf("") }
    var registeredOwnerName by remember { mutableStateOf("") }
    var registeredPremisesName by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        if (intentData != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(intentData)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                val code = account.serverAuthCode
                if (idToken.isNullOrBlank() && code.isNullOrBlank()) {
                    isGoogleSigningIn = false
                    viewModel.showError("Google did not return credentials. Please try again.")
                } else {
                    viewModel.loginWithGoogle(idToken = idToken, serverAuthCode = code)
                }
            } catch (error: ApiException) {
                isGoogleSigningIn = false
                viewModel.showError("Google sign-in failed (Code ${error.statusCode}).")
            }
        } else {
            isGoogleSigningIn = false
        }
    }

    // Modern Google Sign-In with Android Credential Manager as primary API
    fun initiateGoogleSignIn(purpose: GoogleSignInPurpose = GoogleSignInPurpose.LOGIN) {
        viewModel.clearError()
        isGoogleSigningIn = true
        viewModel.launchGoogleSignIn(
            context = context,
            purpose = purpose,
            onRegistrationIdentity = { identity ->
                isGoogleSigningIn = false
                googleRegistrationIdentity = identity
            },
            onFallbackToLegacy = {
                // Fallback to GoogleSignInClient when Credential Manager is unavailable
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )
    }

    // Back handler for multi-step navigation
    BackHandler(enabled = authStep != AuthFlowStep.WELCOME) {
        viewModel.clearError()
        authStep = when (authStep) {
            AuthFlowStep.ROLE_SELECTION -> AuthFlowStep.WELCOME
            AuthFlowStep.LOGIN_EMAIL -> AuthFlowStep.ROLE_SELECTION
            AuthFlowStep.OTP_VERIFICATION -> AuthFlowStep.LOGIN_EMAIL
            AuthFlowStep.OWNER_REGISTRATION_WIZARD -> AuthFlowStep.LOGIN_EMAIL
            AuthFlowStep.REGISTRATION_SUCCESS -> AuthFlowStep.WELCOME
            AuthFlowStep.WELCOME -> AuthFlowStep.WELCOME
        }
    }

    // Transition to OTP verification only when OTP is successfully sent
    LaunchedEffect(otpState) {
        if (otpState != null && (authStep == AuthFlowStep.LOGIN_EMAIL || authStep == AuthFlowStep.ROLE_SELECTION)) {
            authStep = AuthFlowStep.OTP_VERIFICATION
        }
    }

    // Handle authentication UI state responses & backend error code mapping
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success<*> -> {
                val data = (uiState as UiState.Success<*>).data
                if (data is User) {
                    isGoogleSigningIn = false
                    if (registeredOrgName.isNotBlank()) {
                        viewModel.setRegistrationSuccessPending(true)
                        authStep = AuthFlowStep.REGISTRATION_SUCCESS
                    } else {
                        onLoginSuccess(data)
                    }
                }
            }
            is UiState.Error -> {
                isGoogleSigningIn = false
                val code = (uiState as UiState.Error).code
                val msg = (uiState as UiState.Error).message
                when {
                    code == "NOT_REGISTERED_EMPLOYEE" || (selectedRole == Role.EMPLOYEE && msg.contains("not registered", ignoreCase = true)) -> {
                        showUnregisteredEmployeeDialog = true
                    }
                    code == "NOT_REGISTERED_OWNER" || (selectedRole == Role.OWNER && msg.contains("not registered", ignoreCase = true)) -> {
                        showUnregisteredOwnerDialog = true
                    }
                    code == "ACCOUNT_DEACTIVATED" || msg.contains("deactivated", ignoreCase = true) -> {
                        showDeactivatedDialog = true
                    }
                }
            }
            else -> {}
        }
    }

    val devOtp = otpState?.devOtp

    // ------------------------------------------------ DIALOGS
    if (showEmployeeActivationGuide) {
        EmployeeActivationDialog(
            onDismiss = { showEmployeeActivationGuide = false }
        )
    }

    if (showUnregisteredEmployeeDialog) {
        UnregisteredEmployeeDialog(
            email = emailInput,
            onDismiss = {
                showUnregisteredEmployeeDialog = false
                viewModel.clearError()
            }
        )
    }

    if (showUnregisteredOwnerDialog) {
        UnregisteredOwnerDialog(
            email = emailInput,
            onStartRegistration = {
                showUnregisteredOwnerDialog = false
                viewModel.clearError()
                authStep = AuthFlowStep.OWNER_REGISTRATION_WIZARD
            },
            onDismiss = {
                showUnregisteredOwnerDialog = false
                viewModel.clearError()
            }
        )
    }

    if (showDeactivatedDialog) {
        AccountDeactivatedDialog(
            onDismiss = {
                showDeactivatedDialog = false
                viewModel.clearError()
            }
        )
    }

    if (employeePendingPremises) {
        EmployeePendingPremisesDialog(
            onDismiss = {
                viewModel.dismissPendingPremises()
                viewModel.logout()
            }
        )
    }

    val currentMismatch = roleMismatch
    if (currentMismatch != null) {
        RoleMismatchDialog(
            selectedRole = currentMismatch.first,
            actualRole = currentMismatch.second,
            onSwitchRoleAndContinue = {
                selectedRole = currentMismatch.second
                viewModel.selectRole(currentMismatch.second)
                viewModel.clearRoleMismatch()
                val user = viewModel.currentUser.value
                if (user != null) {
                    onLoginSuccess(user)
                }
            },
            onDismiss = {
                viewModel.clearRoleMismatch()
                viewModel.logout()
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = authStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authScreenStep"
        ) { currentStep ->
            when (currentStep) {
                AuthFlowStep.WELCOME -> {
                    WelcomeScreen(
                        onGetStarted = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.ROLE_SELECTION
                        },
                        onSignIn = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.ROLE_SELECTION
                        }
                    )
                }

                AuthFlowStep.ROLE_SELECTION -> {
                    RoleSelectionScreen(
                        selectedRole = selectedRole,
                        onRoleSelected = { role ->
                            selectedRole = role
                            viewModel.selectRole(role)
                            viewModel.clearError()
                        },
                        onContinue = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.LOGIN_EMAIL
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.WELCOME
                        }
                    )
                }

                AuthFlowStep.LOGIN_EMAIL -> {
                    AuthEmailScreen(
                        role = selectedRole,
                        onRoleChange = { role ->
                            selectedRole = role
                            viewModel.selectRole(role)
                            viewModel.clearError()
                        },
                        emailInput = emailInput,
                        onEmailChange = {
                            emailInput = it
                            if (uiState is UiState.Error) viewModel.clearError()
                        },
                        onSendOtp = {
                            if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                viewModel.clearError()
                                viewModel.requestOtp(emailInput, role = selectedRole)
                            } else {
                                viewModel.showError("Please enter a valid email address.")
                            }
                        },
                        onGoogleSignIn = {
                            initiateGoogleSignIn(GoogleSignInPurpose.LOGIN)
                        },
                        onStartRegistration = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.OWNER_REGISTRATION_WIZARD
                        },
                        onOpenActivationGuide = {
                            showEmployeeActivationGuide = true
                        },
                        onBack = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.ROLE_SELECTION
                        },
                        uiState = uiState,
                        rateLimitCountdown = rateLimitCountdown,
                        isSendingOtp = uiState is UiState.Loading,
                        isGoogleSigningIn = isGoogleSigningIn
                    )
                }

                AuthFlowStep.OTP_VERIFICATION -> {
                    AuthOtpScreen(
                        email = emailInput,
                        otpCode = otpCodeInput,
                        onOtpChange = { otpCodeInput = it },
                        onVerifyOtp = { digits ->
                            viewModel.verifyOtp(emailInput, digits, selectedRole)
                        },
                        onResendOtp = {
                            viewModel.requestOtp(emailInput, role = selectedRole)
                        },
                        onChangeEmail = {
                            viewModel.clearError()
                            viewModel.resetOtp()
                            authStep = AuthFlowStep.LOGIN_EMAIL
                        },
                        uiState = uiState,
                        devOtp = devOtp,
                        rateLimitCountdown = rateLimitCountdown,
                        isVerifying = uiState is UiState.Loading
                    )
                }

                AuthFlowStep.OWNER_REGISTRATION_WIZARD -> {
                    OwnerRegistrationWizard(
                        initialEmail = emailInput,
                        otpResponse = otpState,
                        uiState = uiState,
                        pincodeState = pincodeState,
                        gpsLocationState = gpsLocationState,
                        reverseGeocodeAddress = reverseGeocodeAddress,
                        googleIdentity = googleRegistrationIdentity,
                        onDismiss = {
                            viewModel.clearError()
                            authStep = AuthFlowStep.LOGIN_EMAIL
                        },
                        onRequestOtp = { email ->
                            emailInput = email
                            viewModel.requestOtp(email, role = Role.OWNER, isRegistration = true)
                        },
                        onStartGoogle = {
                            initiateGoogleSignIn(GoogleSignInPurpose.OWNER_REGISTRATION)
                        },
                        onLookupPincode = { code -> viewModel.lookupPincode(code) },
                        onFetchGpsLocation = { viewModel.fetchGpsLocation() },
                        onGpsLocationAcquired = { loc -> viewModel.updateGpsLocation(loc) },
                        onCompleteWithOtp = { email, otp, name, org, tz, curr, prem, radius, addr, lat, lng ->
                            registeredOrgName = org
                            registeredOwnerName = name
                            registeredPremisesName = prem
                            viewModel.verifyOtp(
                                email = email,
                                code = otp,
                                role = Role.OWNER,
                                fullName = name,
                                organizationName = org,
                                timezone = tz,
                                currency = curr,
                                premisesName = prem,
                                premisesAddress = addr,
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = radius
                            )
                        },
                        onCompleteWithGoogle = { token, org, tz, curr, prem, radius, addr, lat, lng ->
                            registeredOrgName = org
                            registeredOwnerName = "Owner"
                            registeredPremisesName = prem
                            viewModel.registerOwnerWithGoogle(
                                idToken = token,
                                organizationName = org,
                                timezone = tz,
                                currency = curr,
                                premisesName = prem,
                                radiusMeters = radius,
                                premisesAddress = addr,
                                latitude = lat,
                                longitude = lng
                            )
                        }
                    )
                }

                AuthFlowStep.REGISTRATION_SUCCESS -> {
                    RegistrationSuccessScreen(
                        organizationName = registeredOrgName,
                        ownerName = registeredOwnerName,
                        premisesName = registeredPremisesName,
                        onEnterWorkspace = {
                            viewModel.setRegistrationSuccessPending(false)
                            val current = viewModel.currentUser.value
                            if (current != null) {
                                onLoginSuccess(current)
                            }
                        }
                    )
                }
            }
        }
    }
}
