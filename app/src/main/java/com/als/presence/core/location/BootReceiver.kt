package com.als.presence.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * BroadcastReceiver triggered on system boot or app update.
 * Restarts 10-minute background location monitoring if an active shift session exists.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && action == Intent.ACTION_LOCKED_BOOT_COMPLETED)
        ) {
            Log.d("BootReceiver", "Boot or package update detected ($action). Initializing background location monitor...")
            try {
                ShiftLocationService.start(context)
                LocationMonitorScheduler.schedule(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to launch location background service on boot: ${e.message}")
            }
        }
    }
}
