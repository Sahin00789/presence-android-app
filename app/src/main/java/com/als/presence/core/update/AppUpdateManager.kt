package com.als.presence.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.als.presence.BuildConfig
import com.google.firebase.appdistribution.FirebaseAppDistribution
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/Sahin00789/presence-android-app/releases/latest"
        private const val FALLBACK_APK_URL =
            "https://github.com/Sahin00789/presence-android-app/releases/latest/download/presence.apk"
    }

    /**
     * Checks for updates from GitHub Releases API and Firebase App Distribution.
     */
    fun checkForUpdates(force: Boolean = false) {
        if (_updateState.value is UpdateState.Checking && !force) return

        _updateState.value = UpdateState.Checking

        // 1. Trigger Firebase App Distribution update check if available
        try {
            FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
                .addOnSuccessListener { release ->
                    if (release != null) {
                        Log.d(TAG, "Firebase App Distribution update available: ${release.displayVersion}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Firebase App Distribution check info: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase App Distribution SDK check skipped: ${e.message}")
        }

        // 2. Query GitHub Releases API for direct APK version checking
        scope.launch {
            try {
                val request = Request.Builder()
                    .url(GITHUB_RELEASES_API)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "presence-android-app")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "GitHub releases request failed code: ${response.code}")
                        _updateState.value = UpdateState.Idle
                        return@use
                    }

                    val responseBody = response.body?.string() ?: return@use
                    val jsonElement = json.parseToJsonElement(responseBody)
                    val jsonObject = jsonElement.jsonObject

                    val tagName = jsonObject["tag_name"]?.jsonPrimitive?.content ?: ""
                    val releaseNotes = jsonObject["body"]?.jsonPrimitive?.content ?: "New update available with performance & stability improvements."

                    var apkDownloadUrl = FALLBACK_APK_URL
                    val assets = jsonObject["assets"]?.jsonArray
                    assets?.forEach { asset ->
                        val assetObj = asset.jsonObject
                        val name = assetObj["name"]?.jsonPrimitive?.content ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content ?: FALLBACK_APK_URL
                        }
                    }

                    val currentVersion = BuildConfig.VERSION_NAME
                    val cleanTag = tagName.trimStart('v', 'V')

                    Log.d(TAG, "Current version: $currentVersion, Remote tag: $cleanTag")

                    if (isVersionNewer(cleanTag, currentVersion)) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            currentVersion = currentVersion,
                            latestVersion = cleanTag,
                            releaseNotes = releaseNotes,
                            downloadUrl = apkDownloadUrl
                        )
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for updates from GitHub", e)
                _updateState.value = UpdateState.Idle
            }
        }
    }

    /**
     * Downloads the APK file in background and updates progress state.
     */
    fun startDownload(downloadUrl: String) {
        if (_updateState.value is UpdateState.Downloading) return

        _updateState.value = UpdateState.Downloading(progress = 0)

        scope.launch {
            try {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "presence-android-app")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error("Failed to download update: HTTP ${response.code}")
                        return@use
                    }

                    val body = response.body ?: run {
                        _updateState.value = UpdateState.Error("Download response body was empty")
                        return@use
                    }

                    val updateDir = File(context.cacheDir, "updates")
                    if (!updateDir.exists()) {
                        updateDir.mkdirs()
                    }

                    val apkFile = File(updateDir, "presence_update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val contentLength = body.contentLength()
                    var bytesDownloaded = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(apkFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                                bytesDownloaded += read
                                if (contentLength > 0) {
                                    val progress = ((bytesDownloaded * 100) / contentLength).toInt()
                                    _updateState.value = UpdateState.Downloading(progress = progress.coerceIn(0, 100))
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                    Log.d(TAG, "Download completed. File saved at: ${apkFile.absolutePath}, Uri: $apkUri")
                    _updateState.value = UpdateState.ReadyToInstall(apkUri)

                    // Auto-trigger installation
                    withContext(Dispatchers.Main) {
                        installUpdate(apkUri)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download update APK", e)
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to download update")
            }
        }
    }

    /**
     * Triggers system package installer for the downloaded APK.
     */
    fun installUpdate(apkUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            _updateState.value = UpdateState.Error("Could not launch package installer: ${e.localizedMessage}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Semantic version comparison helper (e.g. 1.0.10 > 1.0.9).
     */
    private fun isVersionNewer(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        if (remote.equals(local, ignoreCase = true)) return false

        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
