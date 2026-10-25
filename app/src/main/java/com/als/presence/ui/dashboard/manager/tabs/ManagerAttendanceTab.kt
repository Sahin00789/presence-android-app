package com.als.presence.ui.dashboard.manager.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.AttendanceRegisterTab
import com.als.presence.ui.dashboard.staff.tabs.StaffClockInTab
import com.als.presence.ui.dashboard.staff.viewmodels.StaffClockInViewModel

/**
 * Manager "Attendance" tab — Handles both the Manager's own personal attendance and Staff attendance.
 * Uses a segmented control: [ My Attendance | Staff Attendance ]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAttendanceTab(
    user: User,
    recordsState: UiState<List<AttendanceRecordDto>>,
    exceptionsState: UiState<List<AttendanceExceptionDto>>,
    isSubmitting: Boolean,
    clockState: StaffClockInViewModel.ClockInUiState,
    onRefresh: () -> Unit,
    onResolve: (exceptionId: String, status: String, reason: String) -> Unit,
    onClockIn: (overtimeId: String?) -> Unit,
    onClockOut: () -> Unit,
    onLocate: () -> Unit,
    engine: FaceEmbeddingEngine,
    onFaceCaptured: (FaceEmbeddingEngine.Embedding) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = My Attendance, 1 = Staff Attendance

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Top Segmented Control: My Attendance | Staff Attendance
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedSegment == 0,
                        onClick = { selectedSegment = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(
                            text = "My Attendance",
                            fontWeight = if (selectedSegment == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    SegmentedButton(
                        selected = selectedSegment == 1,
                        onClick = { selectedSegment = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(
                            text = "Staff Attendance",
                            fontWeight = if (selectedSegment == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tab Content based on segmented control
        when (selectedSegment) {
            0 -> {
                StaffClockInTab(
                    user = user,
                    state = clockState,
                    onRefresh = onRefresh,
                    onLocate = onLocate,
                    onClockIn = onClockIn,
                    onClockOut = onClockOut,
                    engine = engine,
                    onFaceCaptured = onFaceCaptured
                )
            }
            1 -> {
                AttendanceRegisterTab(
                    recordsState = recordsState,
                    exceptionsState = exceptionsState,
                    isSubmitting = isSubmitting,
                    onRefresh = onRefresh,
                    onResolve = onResolve
                )
            }
        }
    }
}
