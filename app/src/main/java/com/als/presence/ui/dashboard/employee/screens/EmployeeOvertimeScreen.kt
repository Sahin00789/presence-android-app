package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.OvertimeDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill

/**
 * Compact Employee Overtime Screen.
 * Displays personal scheduled overtime, completed overtime sessions,
 * agreed payment rates, and earning status for the employee.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeOvertimeScreen(
    user: User,
    onBack: () -> Unit,
    viewModel: EmployeeAttendanceViewModel = hiltViewModel()
) {
    val overtimeState by viewModel.overtime.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadOvertime()
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Scheduled, 1 = Completed, 2 = All History

    val allOvertime = (overtimeState as? UiState.Success)?.data.orEmpty()

    val scheduledList = remember(allOvertime) {
        allOvertime.filter { it.status == "APPROVED" || it.status == "IN_PROGRESS" || it.status == "REQUESTED" }
    }
    val completedList = remember(allOvertime) {
        allOvertime.filter { it.status == "COMPLETED" }
    }

    val totalEarnedPaise = remember(completedList) {
        completedList.sumOf { it.paymentInr }
    }
    val totalPlannedHours = remember(allOvertime) {
        allOvertime.sumOf { it.plannedMinutes } / 60.0
    }

    PresenceSecondaryScaffold(
        crumb = "PERSONAL WORKSPACE",
        title = "Assigned Overtimes",
        onBack = onBack,
        onRefresh = viewModel::loadOvertime
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Compact Summary Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Overtime Summary (${allOvertime.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        PresenceBadge(
                            label = "${scheduledList.size} Scheduled",
                            tone = PresenceTone.Success
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresenceStatTile(
                            label = "Scheduled Hours",
                            value = String.format(java.util.Locale.US, "%.1f hrs", totalPlannedHours),
                            modifier = Modifier.weight(1f)
                        )
                        PresenceStatTile(
                            label = "Completed Pay",
                            value = Money.format(totalEarnedPaise),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                indicator = {},
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 0) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Scheduled (${scheduledList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 1) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Completed (${completedList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 2) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "All (${allOvertime.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Tab Content List
            when (overtimeState) {
                is UiState.Loading, is UiState.Idle -> SectionLoading("Loading your overtime schedule…")
                is UiState.Error -> SectionError((overtimeState as UiState.Error).message)
                is UiState.Success -> {
                    val displayList = when (selectedTab) {
                        0 -> scheduledList
                        1 -> completedList
                        else -> allOvertime
                    }

                    if (displayList.isEmpty()) {
                        SectionEmpty(
                            when (selectedTab) {
                                0 -> "No scheduled overtime assigned for you."
                                1 -> "No completed overtime sessions recorded yet."
                                else -> "No overtime records found."
                            }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayList.forEach { entry ->
                                CompactEmployeeOvertimeCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactEmployeeOvertimeCard(entry: OvertimeDto) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = Dates.formatDate(entry.workDate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(entry.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = Dates.formatWindow(entry.startTime, entry.endTime),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${Dates.formatMinutes(entry.plannedMinutes)} • ${Money.format(entry.paymentInr)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!entry.reason.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = entry.reason,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
