package com.als.presence.ui.dashboard.manager.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.HolidayDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceDialog
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceErrorState
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceLoading
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.manager.ManagerShiftsViewModel

/**
 * "Manage Holidays" management screen — the organization's public holiday
 * calendar straight from the scheduling backend.
 *
 * Staff members may open this in a read-only capacity; only owners and managers
 * can add or remove entries, matching the server's role guards.
 */
@Composable
fun HolidayManagementScreen(
    onBack: () -> Unit,
    canManage: Boolean,
    viewModel: ManagerShiftsViewModel = hiltViewModel()
) {
    val holidaysState by viewModel.holidays.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) activeMessage = message
    }

    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HolidayDto?>(null) }

    val holidays = (holidaysState as? UiState.Success)?.data.orEmpty()

    Box {
        PresenceSecondaryScaffold(
            crumb = "Scheduling",
            title = "Holidays",
            onBack = onBack,
            actions = {
                PresenceIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh holidays",
                    onClick = { viewModel.loadHolidays() }
                )
            }
        ) {
            PresencePageIntro(
                eyebrow = "Calendar",
                title = if (canManage) "Manage Holidays" else "Holiday Calendar",
                description = if (canManage) {
                    "Add or remove organisation holidays. Paid holidays keep the full daily rate."
                } else {
                    "The published company holiday calendar for this organisation."
                }
            )

            PresenceMetaStrip(
                items = listOf(
                    "Holidays" to holidays.size.toString(),
                    "Paid" to holidays.count { it.isPaid }.toString(),
                    "Upcoming" to holidays.count { it.date >= Dates.todayIso() }.toString()
                )
            )

            if (canManage) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    PresenceButton(
                        label = "Add holiday",
                        onClick = { showCreate = true },
                        variant = PresenceVariant.Soft,
                        size = PresenceSize.Sm,
                        icon = Icons.Default.EventAvailable,
                        loading = isSubmitting
                    )
                }
            }

            PresenceSectionTitle(
                title = "Calendar",
                description = "Sorted by date as published by the organisation."
            )

            when (val state = holidaysState) {
                is UiState.Idle, is UiState.Loading -> PresenceLoading("Loading holidays")
                is UiState.Error -> PresenceErrorState(message = state.message, onRetry = { viewModel.loadHolidays() })
                is UiState.Success -> {
                    if (holidays.isEmpty()) {
                        PresenceEmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = "No holidays recorded yet",
                            description = "Once an owner or manager adds a holiday it will show up here for everyone."
                        )
                    } else {
                        holidays.forEach { holiday ->
                            HolidayCard(
                                holiday = holiday,
                                canManage = canManage,
                                onDelete = { deleteTarget = holiday }
                            )
                        }
                    }
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showCreate) {
        CreateHolidayDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name, date, isPaid ->
                viewModel.createHoliday(name, date, isPaid)
                showCreate = false
            }
        )
    }

    deleteTarget?.let { target ->
        PresenceDialog(
            eyebrow = "Scheduling",
            title = "Remove holiday",
            onDismiss = { deleteTarget = null },
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.deleteHoliday(target.id)
                deleteTarget = null
            },
            confirmVariant = PresenceVariant.DangerSolid
        ) {
            Text(
                text = "Remove “${target.name}” (${Dates.formatDate(target.date)}) from the organisation calendar? Existing attendance records are not changed.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = PresenceDesign.BodyText
            )
        }
    }
}

@Composable
private fun HolidayCard(
    holiday: HolidayDto,
    canManage: Boolean,
    onDelete: () -> Unit
) {
    PresenceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holiday.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Dates.formatDate(holiday.date),
                        fontSize = 11.sp,
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
                        fontSize = 11.sp,
                        color = PresenceDesign.LabelText
                    )
                }
            }
            PresenceBadge(
                label = if (holiday.isPaid) "Paid" else "Unpaid",
                tone = if (holiday.isPaid) PresenceTone.Success else PresenceTone.Warning,
                dot = true
            )
            if (canManage) {
                Spacer(modifier = Modifier.width(6.dp))
                PresenceIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Remove ${holiday.name}",
                    onClick = onDelete,
                    tint = PresenceDesign.DangerText
                )
            }
        }
    }
}

@Composable
private fun CreateHolidayDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, date: String, isPaid: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.todayIso()) }
    var isPaid by remember { mutableStateOf(true) }

    val valid = name.trim().length >= 2 && date.trim().matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))

    PresenceDialog(
        eyebrow = "Scheduling",
        title = "Add holiday",
        onDismiss = onDismiss,
        confirmLabel = "Add holiday",
        onConfirm = { onConfirm(name.trim(), date.trim(), isPaid) },
        confirmEnabled = valid
    ) {
        PresenceFieldGroup(title = "Details") {
            PresenceTextField(value = name, onValueChange = { name = it }, label = "Holiday name")
            PresenceTextField(value = date, onValueChange = { date = it }, label = "Date (YYYY-MM-DD)")
        }
        PresenceFieldGroup(
            title = "Pay policy",
            hint = "Paid holidays keep the full daily rate for those covered by the calendar."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paid holiday",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PresenceDesign.Ink
                    )
                    Text(
                        text = "Applies to the organisation holiday list",
                        fontSize = 10.5.sp,
                        color = PresenceDesign.LabelText
                    )
                }
                Switch(checked = isPaid, onCheckedChange = { isPaid = it })
            }
        }
    }
}
