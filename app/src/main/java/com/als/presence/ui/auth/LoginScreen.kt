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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.als.presence.R
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PrimaryIndigo
import com.als.presence.core.designsystem.RoleManagerPrimary
import com.als.presence.core.designsystem.RoleOwnerPrimary
import com.als.presence.core.designsystem.RoleStaffPrimary
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class LoginStep {
    ENTER_EMAIL,
    ENTER_OTP
}

private enum class GoogleSignInPurpose {
    LOGIN,
    OWNER_REGISTRATION
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
    var selectedRole by remember { mutableStateOf(Role.STAFF) }
    var loginStep by remember { mutableStateOf(LoginStep.ENTER_EMAIL) }
    var showOwnerRegistrationWizard by remember { mutableStateOf(false) }
    var googleSignInPurpose by remember { mutableStateOf(GoogleSignInPurpose.LOGIN) }
    var googleRegistrationIdentity by remember { mutableStateOf<GoogleRegistrationIdentity?>(null) }

    val activeRoleColor = remember(selectedRole) {
        when (selectedRole) {
            Role.STAFF -> RoleStaffPrimary
            Role.MANAGER -> RoleManagerPrimary
            Role.OWNER -> RoleOwnerPrimary
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            when (googleSignInPurpose) {
                GoogleSignInPurpose.LOGIN -> {
                    val code = account.serverAuthCode
                    if (code.isNullOrBlank()) {
                        viewModel.showError("Google did not return an authorization code. Please try again.")
                    } else {
                        viewModel.loginWithGoogle(code)
                    }
                }
                GoogleSignInPurpose.OWNER_REGISTRATION -> {
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
            viewModel.showError("Google sign-in failed (${error.statusCode}). Please try again.")
        }
    }

    fun startGoogleSignIn(purpose: GoogleSignInPurpose) {
        googleSignInPurpose = purpose
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    // Synchronize OTP response state to Step 2
    if (otpState != null && loginStep == LoginStep.ENTER_EMAIL && !showOwnerRegistrationWizard) {
        loginStep = LoginStep.ENTER_OTP
        otpCodeInput = if (com.als.presence.BuildConfig.DEBUG) otpState?.devOtp.orEmpty() else ""
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF5F6FB)
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
            // 1. Centered Top Branding Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF12213F), Color(0xFF213665))
                            )
                        )
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // CENTERED LOGO EMBLEM
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            PrimaryIndigo,
                                            SecondaryEmerald,
                                            PrimaryIndigo.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_logo_presence),
                                    contentDescription = "Presence Logo",
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "PRESENCE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Staff Attendance • GPS Geofence • Salary Wallet",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCFD9F2),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Single Parent Login Card (Role Selector + EMAIL OTP TOP + GOOGLE SIGN-IN BOTTOM)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFE7E8F0)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ROLE SELECTOR INSIDE CARD
                    Text(
                        text = "Choose your workspace role",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF252943),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Select the account role you want to access.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF747895),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 10.dp),
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Role.entries.forEach { role ->
                            val isSelected = selectedRole == role
                            val roleColor = when (role) {
                                Role.STAFF -> RoleStaffPrimary
                                Role.MANAGER -> RoleManagerPrimary
                                Role.OWNER -> RoleOwnerPrimary
                            }
                            val roleIcon = when (role) {
                                Role.STAFF -> Icons.Default.Badge
                                Role.MANAGER -> Icons.Default.SupervisorAccount
                                Role.OWNER -> Icons.Default.AdminPanelSettings
                            }

                            val animatedRoleBg by animateColorAsState(
                                targetValue = if (isSelected) roleColor else MaterialTheme.colorScheme.surface,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "RoleBgColor"
                            )
                            val animatedRoleIconTint by animateColorAsState(
                                targetValue = if (isSelected) Color.White else roleColor,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "RoleIconTint"
                            )
                            val animatedRoleTextTint by animateColorAsState(
                                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "RoleTextTint"
                            )

                            Surface(
                                onClick = { selectedRole = role },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = animatedRoleBg,
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE7E8F0)),
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = roleIcon,
                                        contentDescription = role.name,
                                        tint = animatedRoleIconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (role) {
                                            Role.STAFF -> "Staff"
                                            Role.MANAGER -> "Manager"
                                            Role.OWNER -> "Owner"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = animatedRoleTextTint
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = when (selectedRole) {
                            Role.STAFF -> "Staff: Clock-in/out, shifts & salary wallet"
                            Role.MANAGER -> "Manager: Staff rosters, exceptions & payroll"
                            Role.OWNER -> "Owner: Full organization governance & premises"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color(0xFF747895),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE7E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // SECTION 2: EMAIL OTP SIGN IN (TOP)
                    Text(
                        text = if (loginStep == LoginStep.ENTER_OTP) "Check your inbox" else "Sign in as ${selectedRole.name.lowercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF171A34)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // STEP 1: Enter Email & Request OTP
                    AnimatedVisibility(
                        visible = loginStep == LoginStep.ENTER_EMAIL,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Work email") },
                                placeholder = { Text("e.g. name@company.com") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = activeRoleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.requestOtp(emailInput, role = selectedRole)
                                },
                                enabled = uiState !is UiState.Loading && emailInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeRoleColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState is UiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Send Email OTP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // STEP 2: Enter OTP Code
                    AnimatedVisibility(
                        visible = loginStep == LoginStep.ENTER_OTP,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                            modifier = Modifier.size(16.dp),
                                            tint = activeRoleColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = emailInput,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (com.als.presence.BuildConfig.DEBUG && !otpState?.devOtp.isNullOrEmpty()) {
                                otpState?.devOtp?.let { devOtp ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        color = Color(0xFFF1F5FF),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDCE5FF))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Development OTP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF596579)
                                            )
                                            Text(
                                                text = devOtp,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF263F78),
                                                letterSpacing = 2.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = otpCodeInput,
                                onValueChange = { value ->
                                    otpCodeInput = value.filter(Char::isDigit).take(6)
                                },
                                label = { Text("6-Digit OTP Code") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = activeRoleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        loginStep = LoginStep.ENTER_EMAIL
                                        viewModel.resetOtp()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = {
                                        viewModel.verifyOtp(
                                            email = emailInput,
                                            code = otpCodeInput,
                                            role = selectedRole
                                        )
                                    },
                                    enabled = uiState !is UiState.Loading && otpCodeInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activeRoleColor
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(42.dp),
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // DIVIDER FOR GOOGLE SIGN IN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE7E8F0))
                        Text(
                            text = "  or continue with Google  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9295AA)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE7E8F0))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SECTION 3: GOOGLE OAUTH SIGN IN BUTTON (BOTTOM)
                    Button(
                        onClick = { startGoogleSignIn(GoogleSignInPurpose.LOGIN) },
                        enabled = uiState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF202124),
                            disabledContainerColor = Color(0xFFF4F5F7),
                            disabledContentColor = Color(0xFF9295AA)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = activeRoleColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticating...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Error Banner
                    AnimatedVisibility(visible = uiState is UiState.Error) {
                        val errorMsg = (uiState as? UiState.Error)?.message ?: "Authentication failed"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (errorMsg.contains("not registered", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showOwnerRegistrationWizard = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Register Organization Now", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Compact Bottom Register Prompt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFDCDFFF)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFEEFF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(RoleOwnerPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = RoleOwnerPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Owner? Register business",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoleOwnerPrimary
                        )
                    }

                    Button(
                        onClick = { showOwnerRegistrationWizard = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoleOwnerPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Text(
                text = "Built by Arman Logical Systems",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
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
            googleIdentity = googleRegistrationIdentity,
            onDismiss = { showOwnerRegistrationWizard = false },
            onRequestOtp = { ownerEmail ->
                emailInput = ownerEmail
                viewModel.requestOtp(ownerEmail, role = Role.OWNER)
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
            onCompleteWithOtp = { ownerEmail, code, ownerName, orgName, timezone, currency, premisesName, radius ->
                viewModel.verifyOtp(
                    email = ownerEmail,
                    code = code,
                    role = Role.OWNER,
                    fullName = ownerName,
                    organizationName = orgName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    radiusMeters = radius
                )
                showOwnerRegistrationWizard = false
            },
            onCompleteWithGoogle = { idToken, orgName, timezone, currency, premisesName, radius ->
                viewModel.registerOwnerWithGoogle(
                    idToken = idToken,
                    organizationName = orgName,
                    timezone = timezone,
                    currency = currency,
                    premisesName = premisesName,
                    radiusMeters = radius
                )
                showOwnerRegistrationWizard = false
            }
        )
    }
}
