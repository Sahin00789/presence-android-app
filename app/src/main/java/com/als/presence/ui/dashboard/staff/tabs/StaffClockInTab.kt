package com.als.presence.ui.dashboard.staff.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.FaceCaptureDialog
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.staff.viewmodels.StaffClockInViewModel

/**
 * Staff "Clock-In" tab. Every action posts to the server — nothing is simulated
 * locally. GPS comes from the fused location provider, the face step is the
 * on-device capture pipeline and the server re-validates the geofence.
 */
@Composable
fun StaffClockInTab(
    user: User,
    state: StaffClockInViewModel.ClockInUiState,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onClockIn: (overtimeId: String?) -> Unit,
    onClockOut: () -> Unit,
    engine: FaceEmbeddingEngine,
    onFaceCaptured: (FaceEmbeddingEngine.Embedding) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val active = state.activeEvent
    var showCapture by remember { mutableStateOf(false) }
    var clockInAfterCapture by remember { mutableStateOf(false) }
    var pendingOvertimeId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Hello, ${user.fullName.substringBefore(' ')}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "STAFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = user.organizationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            if (active != null) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = when {
                        active?.overtimeId != null -> "Overtime in progress"
                        active != null -> "On shift since ${Dates.formatTime(active.clockIn)}"
                        else -> "Ready to clock in"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                val fix = state.location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (fix != null) {
                            val accuracy = fix.accuracyMeters.toInt()
                            "GPS fix • ±${accuracy}m accuracy"
                        } else {
                            "No GPS fix yet — tap Locate"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.faceEnrolled) {
                            "Face enrolled • server re-validates every clock-in"
                        } else {
                            "Face verification required"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!state.faceEnrolled) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Face not enrolled. Please contact your Manager/Owner to get enrolled at the premises.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                }

                if (active == null) {
                    Button(
                        onClick = {
                            clockInAfterCapture = true
                            pendingOvertimeId = null
                            showCapture = true
                        },
                        enabled = !state.isSubmitting && state.faceEnrolled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (state.faceEnrolled) {
                                "Verify Face & Clock In"
                            } else {
                                "Face enrollment required"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onClockOut,
                        enabled = !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clock Out", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onLocate,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSubmitting
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Locate me")
                }
            }
        }

        if (state.error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (state.openOvertime.isNotEmpty()) {
            SectionHeader(title = "Approved overtime")
            state.openOvertime.forEach { entry ->
                OvertimeCheckInCard(
                    entry = entry,
                    enabled = active == null && !state.isSubmitting && state.faceEnrolled,
                    onCheckIn = {
                        clockInAfterCapture = true
                        pendingOvertimeId = entry.id
                        showCapture = true
                    }
                )
            }
        }

        SectionHeader(
            title = "Today",
            trailing = {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh today",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        when {
            state.isLoading -> Text(
                text = "Loading today's attendance…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.todayRecords.isEmpty() -> Text(
                text = "No attendance recorded today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> state.todayRecords.forEach { record -> TodayRow(record) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showCapture) {
        FaceCaptureDialog(
            engine = engine,
            mode = "VERIFY",
            onCapturedMulti = { _ -> },
            onCapturedSingle = { embedding ->
                onFaceCaptured(embedding)
                showCapture = false
                if (clockInAfterCapture) {
                    clockInAfterCapture = false
                    onClockIn(pendingOvertimeId)
                    pendingOvertimeId = null
                }
            },
            onDismiss = {
                showCapture = false
                clockInAfterCapture = false
                pendingOvertimeId = null
            }
        )
    }
}

@Composable
private fun OvertimeCheckInCard(
    entry: OvertimeDto,
    enabled: Boolean,
    onCheckIn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Dates.formatDate(entry.workDate),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(entry.status)
            }
            KeyValueRow(
                label = "Window",
                value = Dates.formatWindow(entry.startTime, entry.endTime)
            )
            KeyValueRow(label = "Agreed payment", value = Money.format(entry.paymentCents))
            entry.reason?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Button(
                onClick = onCheckIn,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Check in for overtime", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TodayRow(record: AttendanceRecordDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.overtimeId != null) {
                        Icons.Default.LockClock
                    } else {
                        Icons.Default.Fingerprint
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.overtimeId != null) "Overtime session" else "Shift session",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Dates.formatTime(record.clockIn)} → ${Dates.formatTime(record.clockOut)} • ${Dates.formatMinutes(record.workedMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(record.status)
        }
    }
}

/** Structural alias so the tab can accept the view-model state without importing it. */
typealias StaffClockInViewModelState = StaffClockInViewModel.ClockInUiState
