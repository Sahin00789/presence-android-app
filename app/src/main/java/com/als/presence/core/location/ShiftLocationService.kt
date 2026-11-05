package com.als.presence.core.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.als.presence.MainActivity
import com.als.presence.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that performs the active-shift location check roughly
 * every 10 minutes. WorkManager runs a 15-minute fallback so monitoring resumes
 * even when the foreground service is killed by the OS.
 */
@AndroidEntryPoint
class ShiftLocationService : Service() {

    companion object {
        private const val CHANNEL_ID = "shift_location_monitoring"
        private const val NOTIFICATION_ID = 4101
        private const val CHECK_INTERVAL_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ShiftLocationService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShiftLocationService::class.java))
        }
    }

    @Inject lateinit var monitor: ActiveShiftLocationMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loop?.isActive == true) return START_STICKY
        loop = scope.launch {
            while (isActive) {
                val outcome = monitor.runOnce()
                if (!outcome.active) {
                    stopSelf()
                    break
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active shift location checks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Periodic GPS checks while a shift is active"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_logo_presence)
        .setContentTitle("Shift location monitoring active")
        .setContentText("Checking your location every ~10 minutes during your shift.")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()
}
