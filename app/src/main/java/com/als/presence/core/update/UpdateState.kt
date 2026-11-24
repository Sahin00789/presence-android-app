package com.als.presence.core.update

import android.net.Uri

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val isMandatory: Boolean = false
    ) : UpdateState

    data class Downloading(
        val progress: Int // 0 to 100
    ) : UpdateState

    data class ReadyToInstall(
        val apkUri: Uri
    ) : UpdateState

    data class InstallPermissionRequired(
        val apkUri: Uri
    ) : UpdateState

    data class Error(
        val message: String
    ) : UpdateState
}
