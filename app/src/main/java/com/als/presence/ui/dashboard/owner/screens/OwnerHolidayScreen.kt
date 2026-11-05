package com.als.presence.ui.dashboard.owner.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MessageSnackbar
import com.als.presence.ui.dashboard.common.PresenceBadge
import com.als.presence.ui.dashboard.common.PresenceButton
import com.als.presence.ui.dashboard.common.PresenceCard
import com.als.presence.ui.dashboard.common.PresenceDialog
import com.als.presence.ui.dashboard.common.PresenceEmptyState
import com.als.presence.ui.dashboard.common.PresenceFieldGroup
import com.als.presence.ui.dashboard.common.PresenceIconButton
import com.als.presence.ui.dashboard.common.PresenceMetaStrip
import com.als.presence.ui.dashboard.common.PresencePageIntro
import com.als.presence.ui.dashboard.common.PresenceSecondaryScaffold
import com.als.presence.ui.dashboard.common.PresenceSectionTitle
import com.als.presence.ui.dashboard.common.PresenceSize
import com.als.presence.ui.dashboard.common.PresenceTextField
import com.als.presence.ui.dashboard.common.PresenceTone
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/**
 * Owner Holiday Management Screen.
 * Provides premises-level public holiday configuration and management for organization Owners.
 */
@Composable
fun OwnerHolidayScreen(
    onBack: () -> Unit,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    val premisesState by viewModel.premisesState.collectAsStateWithLifecycle()
    val message by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) activeMessage = message
    }

    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HolidayDto?>(null) }

    val holidays = emptyList<HolidayDto>()
    val premisesList = (premisesState as? UiState.Success)?.data.orEmpty()

    Box {
        PresenceSecondaryScaffold(
            crumb = "SCHEDULING CONSOLE",
            title = "Premises Holiday Management",
            onBack = onBack,
            onRefresh = { viewModel.refresh() }
        ) {
            PresencePageIntro(
                eyebrow = "Calendar Management",
                title = "Manage Premises Holidays",
                description = "Add or remove premises public holidays. Configured holidays retain full daily pay in payroll engine."
            )

            PresenceMetaStrip(
                items = listOf(
                    "Holidays" to holidays.size.toString(),
                    "Upcoming" to holidays.count { it.date >= Dates.todayIso() }.toString()
                )
            )

            PresenceSectionTitle(
                title = "Published Premises Holidays",
                description = "Sorted by calendar date for active premises.",
                action = {
                    PresenceButton(
                        label = "Add Holiday",
                        onClick = { showCreate = true },
                        variant = PresenceVariant.Soft,
                        size = PresenceSize.Sm,
                        icon = Icons.Default.EventAvailable,
                        loading = isSubmitting
                    )
                }
            )

            if (holidays.isEmpty()) {
                PresenceEmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = "No holidays configured yet",
                    description = "Tap 'Add Holiday' to define premises public holidays for your organization."
                )
            } else {
                holidays.forEach { holiday ->
                    val premisesName = premisesList.find { it.id == holiday.premisesId }?.name
                    HolidayCard(
                        holiday = holiday,
                        premisesName = premisesName,
                        onDelete = { deleteTarget = holiday }
                    )
                }
            }
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }

    if (showCreate) {
        CreateHolidayDialog(
            premisesList = premisesList,
            initialPremisesId = premisesList.firstOrNull()?.id,
            onDismiss = { showCreate = false },
            onConfirm = { name, date, premisesId ->
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
                deleteTarget = null
            },
            confirmVariant = PresenceVariant.DangerSolid
        ) {
            Text(
                text = "Remove “${target.name}” (${Dates.formatDate(target.date)}) from the calendar? Existing attendance records will remain unchanged.",
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
    premisesName: String?,
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
                    if (!premisesName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = PresenceDesign.MutedLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = premisesName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PresenceDesign.Brand
                        )
                    }
                }
            }
            PresenceBadge(
                label = "Paid Holiday",
                tone = PresenceTone.Success,
                dot = true
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateHolidayDialog(
    premisesList: List<PremisesDto>,
    initialPremisesId: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, date: String, premisesId: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.todayIso()) }
    var selectedPremisesId by remember(premisesList, initialPremisesId) {
        mutableStateOf(initialPremisesId ?: premisesList.firstOrNull()?.id.orEmpty())
    }

    val calendar = remember(date) {
        java.util.Calendar.getInstance().apply {
            if (date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                try {
                    val parts = date.split("-")
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                } catch (_: Exception) {}
            }
        }
    }

    val showDatePicker = {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    val valid = name.trim().length >= 2 &&
            date.trim().matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) &&
            selectedPremisesId.isNotBlank()

    PresenceDialog(
        eyebrow = "Scheduling",
        title = "Add holiday",
        onDismiss = onDismiss,
        confirmLabel = "Add holiday",
        onConfirm = { onConfirm(name.trim(), date.trim(), selectedPremisesId) },
        confirmEnabled = valid
    ) {
        PresenceFieldGroup(title = "Premises") {
            if (premisesList.isEmpty()) {
                Text(
                    text = "No premises available. Please create a premises first.",
                    fontSize = 12.sp,
                    color = PresenceDesign.DangerText
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(premisesList) { p ->
                        FilterChip(
                            selected = selectedPremisesId == p.id,
                            onClick = { selectedPremisesId = p.id },
                            label = { Text(p.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PresenceFieldGroup(title = "Details") {
            PresenceTextField(value = name, onValueChange = { name = it }, label = "Holiday name")
            PresenceTextField(
                value = date,
                onValueChange = { date = it },
                label = "Date (YYYY-MM-DD)",
                trailingIcon = {
                    IconButton(onClick = { showDatePicker() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick date from calendar",
                            tint = PresenceDesign.Brand
                        )
                    }
                }
            )
        }
    }
}
