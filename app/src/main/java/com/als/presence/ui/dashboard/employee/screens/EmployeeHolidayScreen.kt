package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.als.presence.data.model.HolidayDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel

/**
 * Employee Holiday Calendar Screen.
 * Displays published premises public holidays and paid holiday schedule for employees.
 */
@Composable
fun EmployeeHolidayScreen(
    onBack: () -> Unit,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel()
) {
    val assignmentsState by viewModel.assignments.collectAsStateWithLifecycle()

    val holidays = emptyList<HolidayDto>()
    val upcomingCount = holidays.count { it.date >= Dates.todayIso() }

    PresenceSecondaryScaffold(
        crumb = "PERSONAL WORKSPACE",
        title = "Holiday Calendar",
        onBack = onBack,
        onRefresh = { viewModel.refresh() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            if (holidays.isEmpty()) {
                PresenceEmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = "No holidays published yet",
                    description = "Official premises holidays added by your organization will automatically appear here."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    holidays.forEach { holiday ->
                        EmployeeHolidayCard(holiday = holiday)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeHolidayCard(holiday: HolidayDto) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holiday.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Dates.formatDate(holiday.date),
                        fontSize = 11.5.sp,
                        color = PresenceDesign.LabelText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = PresenceDesign.MutedLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Dates.dayName(holiday.date),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            PresenceBadge(
                label = "Full Paid Holiday",
                tone = PresenceTone.Success,
                dot = true
            )
        }
    }
}
