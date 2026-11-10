package com.als.presence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.als.presence.core.designsystem.PresenceTheme
import com.als.presence.core.permission.RequestAppPermissionsOnOpen
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.data.remote.PRESENCE_BASE_URL
import com.als.presence.ui.auth.AuthViewModel
import com.als.presence.ui.auth.LoginScreen
import com.als.presence.ui.dashboard.DashboardScreen
import com.als.presence.ui.splash.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var eventsWebSocketService: EventsWebSocketService

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PresenceTheme {
                // Request missing permissions (Location, Camera, Notifications) when app opens
                RequestAppPermissionsOnOpen()

                val currentUser by authViewModel.currentUser.collectAsState()
                val isRegistrationSuccessPending by authViewModel.isRegistrationSuccessPending.collectAsState()
                val isOnline by authViewModel.isOnline.collectAsState()
                val isServerConnected by authViewModel.isServerConnected.collectAsState()
                val isSessionChecking by authViewModel.isSessionChecking.collectAsState()
                val isSyncing by authViewModel.isSyncing.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                var splashTimeElapsed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    splashTimeElapsed = true
                }

                val showSplash = isSessionChecking || !splashTimeElapsed

                LaunchedEffect(currentUser?.id) {
                    if (currentUser != null && !isRegistrationSuccessPending) {
                        eventsWebSocketService.start(PRESENCE_BASE_URL)
                    } else {
                        eventsWebSocketService.stop()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 500),
                        label = "SplashCrossfade"
                    ) { isSplashing ->
                        if (isSplashing) {
                            SplashScreen(
                                onSplashFinished = { /* Driven by showSplash state */ }
                            )
                        } else {
                            val user = currentUser
                            if (user != null && !isRegistrationSuccessPending) {
                                DashboardScreen(
                                    user = user,
                                    onLogout = { authViewModel.logout() },
                                    isOnline = isOnline,
                                    isServerConnected = isServerConnected,
                                    onCheckServerHealth = { authViewModel.checkServerHealth() },
                                    isSyncing = isSyncing,
                                    onSyncClick = { authViewModel.syncRoomDatabase(context) }
                                )
                            } else {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = { /* Handled by StateFlow */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
