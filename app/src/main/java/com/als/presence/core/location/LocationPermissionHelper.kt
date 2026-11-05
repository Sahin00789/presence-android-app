package com.als.presence.core.location

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.als.presence.core.permission.AppPermissionUtil

object LocationPermissionUtil {
    fun hasLocationPermission(context: Context): Boolean {
        return AppPermissionUtil.hasLocationPermission(context)
    }
}

@Composable
fun rememberLocationPermissionState(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    return remember(context, onPermissionGranted, onPermissionDenied) {
        {
            if (LocationPermissionUtil.hasLocationPermission(context)) {
                onPermissionGranted()
            } else {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}
