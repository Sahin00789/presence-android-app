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
                val currentUser by authViewModel.currentUser.collectAsState()
                val isOnline by authViewModel.isOnline.collectAsState()
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(currentUser?.id) {
                    if (currentUser != null) {
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
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            val user = currentUser
                            if (user != null) {
                                DashboardScreen(
                                    user = user,
                                    onLogout = { authViewModel.logout() },
                                    isOnline = isOnline
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
