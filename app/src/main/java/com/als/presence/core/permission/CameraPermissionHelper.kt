package com.als.presence.core.permission

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object CameraPermissionUtil {
    fun hasCameraPermission(context: Context): Boolean {
        return AppPermissionUtil.hasCameraPermission(context)
    }
}

@Composable
fun rememberCameraPermissionState(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    return remember(context, onPermissionGranted, onPermissionDenied) {
        {
            if (CameraPermissionUtil.hasCameraPermission(context)) {
                onPermissionGranted()
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
