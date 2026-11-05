package com.als.presence.ui.dashboard.employee.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.OvertimeDto
import com.als.presence.domain.model.User
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceStatTile
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeAttendanceViewModel

/**
 * Employee Overtime Screen.
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Summary Header Tile
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Overtime Summary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${allOvertime.size} Assigned Session${if (allOvertime.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Scheduled (${scheduledList.size})",
                        style = MaterialTheme.typography.titleSmall,
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
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Completed (${completedList.size})",
                        style = MaterialTheme.typography.titleSmall,
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
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "All (${allOvertime.size})",
                        style = MaterialTheme.typography.titleSmall,
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
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayList.forEach { entry ->
                                EmployeeOvertimeCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeOvertimeCard(entry: OvertimeDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = Dates.formatDate(entry.workDate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PresenceDesign.Ink
                    )
                }
                StatusPill(entry.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Time Window",
                        style = MaterialTheme.typography.labelSmall,
                        color = PresenceDesign.LabelText
                    )
                    Text(
                        text = Dates.formatWindow(entry.startTime, entry.endTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Agreed Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = PresenceDesign.LabelText
                    )
                    Text(
                        text = Money.format(entry.paymentInr),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            KeyValueRow(label = "Planned Duration", value = Dates.formatMinutes(entry.plannedMinutes))
            if (entry.workedMinutes > 0) {
                KeyValueRow(label = "Worked Duration", value = Dates.formatMinutes(entry.workedMinutes))
            }

            if (!entry.reason.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = entry.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Guidance Footnote Box
            val footnoteText = when (entry.status) {
                "APPROVED" -> "Scheduled by Owner • Clock in on assigned premises during your scheduled window."
                "IN_PROGRESS" -> "Active overtime session currently in progress."
                "COMPLETED" -> "Completed overtime • Earnings added to your salary ledger."
                "CANCELLED" -> "Cancelled by Owner • No clock-in required."
                else -> "Assigned Overtime Record"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (entry.status) {
                            "APPROVED", "IN_PROGRESS" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (entry.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (entry.status == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = footnoteText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
