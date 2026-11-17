package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.FaceVerifyResponseDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeBiometricViewModel

/**
 * Employee Check Biometric Screen.
 * Allows employees to check their facial biometrics registration status,
 * inspect 128-D SFace neural template details, and run live camera verification tests.
 */
@Composable
fun EmployeeBiometricScreen(
    onBack: () -> Unit,
    viewModel: EmployeeBiometricViewModel = hiltViewModel()
) {
    val faceStatusState by viewModel.faceStatus.collectAsStateWithLifecycle()
    val verifyResult by viewModel.verifyResult.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showCaptureDialog by remember { mutableStateOf(false) }

    PresenceSecondaryScaffold(
        crumb = "PERSONAL WORKSPACE",
        title = "Check Biometric",
        subtitle = "Verify registration status & test live face recognition",
        onBack = onBack,
        onRefresh = { viewModel.loadStatus() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            when (val state = faceStatusState) {
                is UiState.Idle, is UiState.Loading -> {
                    PresenceCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                                Text(
                                    text = "Checking biometric enrollment status...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    PresenceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Failed to load biometric status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { viewModel.loadStatus() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val status = state.data

                    // Main Status Card
                    BiometricsStatusCard(
                        status = status,
                        onStartTest = { showCaptureDialog = true }
                    )

                    // Live Verification Test Result Card
                    if (isVerifying) {
                        VerifyingProgressCard()
                    } else if (verifyResult != null) {
                        VerifyResultCard(
                            result = verifyResult!!,
                            onClear = { viewModel.clearVerifyResult() },
                            onTestAgain = {
                                viewModel.clearVerifyResult()
                                showCaptureDialog = true
                            }
                        )
                    } else if (errorMessage != null) {
                        VerifyErrorCard(
                            message = errorMessage!!,
                            onClear = { viewModel.clearVerifyResult() }
                        )
                    }

                    // System & Security Info Card
                    BiometricsInfoCard()
                }
            }
        }
    }

    // Camera Capture Modal for Live Face Verification Test
    if (showCaptureDialog) {
        FaceCaptureDialog(
            engine = viewModel.faceEmbeddingEngine,
            mode = "VERIFY",
            onCapturedSingle = { embedding ->
                viewModel.testFaceVerification(embedding)
                showCaptureDialog = false
            },
            onDismiss = { showCaptureDialog = false }
        )
    }
}

@Composable
private fun BiometricsStatusCard(
    status: FaceStatusDto,
    onStartTest: () -> Unit
) {
    val isEnrolled = status.enrolled

    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnrolled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnrolled) Icons.Default.Verified else Icons.Default.Face,
                            contentDescription = null,
                            tint = if (isEnrolled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Facial Biometrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PresenceDesign.Ink
                        )
                        Text(
                            text = if (isEnrolled) "Multi-Pose SFace Template Active" else "No Multi-Pose Template Found",
                            style = MaterialTheme.typography.bodySmall,
                            color = PresenceDesign.LabelText
                        )
                    }
                }

                PresenceBadge(
                    label = if (isEnrolled) "Enrolled" else "Not Enrolled",
                    tone = if (isEnrolled) PresenceTone.Success else PresenceTone.Warning,
                    dot = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Stat Tiles Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresenceStatTile(
                    label = "Model Version",
                    value = status.modelVersion ?: "sface-yunet-v1",
                    modifier = Modifier.weight(1f)
                )
                PresenceStatTile(
                    label = "Vector Size",
                    value = "${status.embeddingDimensions ?: 128}-D Vector",
                    modifier = Modifier.weight(1f)
                )
            }

            if (isEnrolled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresenceStatTile(
                        label = "Enrolled Date",
                        value = status.enrolledAt?.let { Dates.formatDate(it) } ?: "Active",
                        modifier = Modifier.weight(1f)
                    )
                    PresenceStatTile(
                        label = "Liveness Check",
                        value = "Active (Blink)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isEnrolled) {
                Button(
                    onClick = onStartTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Biometric Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Please ask your Administrator or Owner to enroll your multi-pose facial biometrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifyingProgressCard() {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Text(
                    text = "Matching live face vector with server template...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun VerifyResultCard(
    result: FaceVerifyResponseDto,
    onClear: () -> Unit,
    onTestAgain: () -> Unit
) {
    val matched = result.matched
    val similarityPercent = (result.similarity * 100).toInt()
    val thresholdPercent = (result.threshold * 100).toInt()

    val borderColor = if (matched) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)

    PresenceCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (matched) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (matched) "Verification Match Success!" else "Verification Match Failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                PresenceBadge(
                    label = if (matched) "MATCHED" else "NO MATCH",
                    tone = if (matched) PresenceTone.Success else PresenceTone.Danger,
                    dot = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresenceStatTile(
                    label = "Similarity Score",
                    value = "$similarityPercent%",
                    modifier = Modifier.weight(1f)
                )
                PresenceStatTile(
                    label = "Threshold Cutoff",
                    value = "$thresholdPercent%",
                    modifier = Modifier.weight(1f)
                )
                if (result.matchedPose != null) {
                    PresenceStatTile(
                        label = "Matched Pose",
                        value = result.matchedPose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!result.message.isNullOrBlank()) {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.LabelText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = onTestAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Again")
                }
            }
        }
    }
}

@Composable
private fun VerifyErrorCard(
    message: String,
    onClear: () -> Unit
) {
    PresenceCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verification Error",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun BiometricsInfoCard() {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Biometric Security & Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Text(
                text = "• On-device ONNX runtime extracts 128-dimensional numerical feature vectors during camera alignment.\n" +
                    "• Raw facial photos are immediately purged from device memory and are NEVER stored or transmitted.\n" +
                    "• Active eye-blink and passive texture variance liveness checks prevent spoofing with photos or videos.\n" +
                    "• Biometric templates are used solely for clock-in attendance verification across assigned premises.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = PresenceDesign.LabelText,
                lineHeight = 18.sp
            )
        }
    }
}
