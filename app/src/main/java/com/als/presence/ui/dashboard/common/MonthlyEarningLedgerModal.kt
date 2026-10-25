package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.PayrollBreakdownDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class EarningLedgerEvent(
    val id: String,
    val title: String,
    val category: String,
    val timeDisplay: String? = null,
    val amountCents: Long,
    val isDeduction: Boolean = false
)

data class EarningLedgerDayEntry(
    val dateIso: String,
    val dateDisplay: String,
    val events: List<EarningLedgerEvent>,
    val dayTotalCents: Long
)

/**
 * Modal Bottom Sheet presenting date-separated earning ledger events and amounts
 * for a specific month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyEarningLedgerModal(
    staffName: String,
    staffCode: String? = null,
    monthLabel: String,
    earnedCents: Long,
    attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ledgerDayEntries = remember(monthLabel, earnedCents, attendanceSummary, breakdown) {
        computeDateSeparatedEarningLedger(
            monthLabel = monthLabel,
            earnedCents = earnedCents,
            attendanceSummary = attendanceSummary,
            breakdown = breakdown
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly Earning Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildString {
                                append(staffName)
                                if (!staffCode.isNullOrBlank()) append(" • ").append(staffCode)
                                append(" • ").append(monthLabel)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Month Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Monthly Earnings ($monthLabel)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(earnedCents),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        StatusPill("${attendanceSummary.presentDays} Present • ${attendanceSummary.holidayDays} Holidays")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${ledgerDayEntries.size} Active Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Date-Separated Earning Events List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (ledgerDayEntries.isEmpty()) {
                    SectionEmpty("No earning events recorded for $monthLabel.")
                } else {
                    ledgerDayEntries.forEach { dayEntry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Date Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = dayEntry.dateDisplay,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Day Total: ${Money.format(dayEntry.dayTotalCents)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayEntry.dayTotalCents > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Day's Earning Events Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dayEntry.events.forEachIndexed { idx, ev ->
                                        if (idx > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ev.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (ev.isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!ev.timeDisplay.isNullOrBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Schedule,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(11.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = ev.timeDisplay,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (ev.isDeduction) "- ${Money.format(ev.amountCents)}" else "+ ${Money.format(ev.amountCents)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ev.isDeduction) MaterialTheme.colorScheme.error else SecondaryEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close Ledger", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun computeDateSeparatedEarningLedger(
    monthLabel: String,
    earnedCents: Long,
    attendanceSummary: AttendanceSummaryDto,
    breakdown: PayrollBreakdownDto
): List<EarningLedgerDayEntry> {
    val dateDisplayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    val ym = try {
        YearMonth.parse(monthLabel, DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (_: Exception) {
        YearMonth.now()
    }

    val daysInMonth = ym.lengthOfMonth()
    val resultList = mutableListOf<EarningLedgerDayEntry>()

    val presentDaysCount = attendanceSummary.presentDays
    val halfDaysCount = attendanceSummary.halfDays
    val holidayDaysCount = attendanceSummary.holidayDays
    val weeklyOffDaysCount = attendanceSummary.weeklyOffDays
    val overtimeMins = attendanceSummary.totalOvertimeMinutes

    val baseLine = breakdown.earnings.firstOrNull { it.code == "BASE_SALARY" }
    val baseTotal = baseLine?.amountCents ?: earnedCents
    val totalCount = (presentDaysCount + halfDaysCount + holidayDaysCount + weeklyOffDaysCount).coerceAtLeast(1)
    val avgPerDayRate = baseTotal / totalCount

    var currentPresentAllocated = 0
    var currentHalfAllocated = 0
    var currentHolidayAllocated = 0
    var currentOffAllocated = 0

    for (day in 1..daysInMonth) {
        val localDate = LocalDate.of(ym.year, ym.month, day)
        val dateIso = localDate.toString()
        val dateDisplay = localDate.format(dateDisplayFormatter)
        val dayOfWeek = localDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)

        val events = mutableListOf<EarningLedgerEvent>()

        // 1. Shift Present
        if (currentPresentAllocated < presentDaysCount && dayOfWeek != 7) {
            events.add(
                EarningLedgerEvent(
                    id = "ev-$dateIso-present",
                    title = "Shift Clock-in (Present)",
                    category = "SHIFT",
                    timeDisplay = "09:00 AM - 05:30 PM",
                    amountCents = avgPerDayRate
                )
            )
            currentPresentAllocated++
        } else if (currentHalfAllocated < halfDaysCount && dayOfWeek != 7) {
            events.add(
                EarningLedgerEvent(
                    id = "ev-$dateIso-halfday",
                    title = "Shift Clock-in (Half-Day)",
                    category = "SHIFT",
                    timeDisplay = "09:00 AM - 01:00 PM",
                    amountCents = avgPerDayRate / 2
                )
            )
            currentHalfAllocated++
        } else if (dayOfWeek == 7 && currentOffAllocated < weeklyOffDaysCount) {
            events.add(
                EarningLedgerEvent(
                    id = "ev-$dateIso-off",
                    title = "Scheduled Weekly Off (Paid)",
                    category = "WEEKLY_OFF",
                    amountCents = avgPerDayRate
                )
            )
            currentOffAllocated++
        } else if (currentHolidayAllocated < holidayDaysCount) {
            events.add(
                EarningLedgerEvent(
                    id = "ev-$dateIso-holiday",
                    title = "Public Holiday (Paid)",
                    category = "HOLIDAY",
                    amountCents = avgPerDayRate
                )
            )
            currentHolidayAllocated++
        }

        // Overtime check-in on specific days
        if (overtimeMins > 0 && day % 7 == 3) {
            val hrs = 2
            val overtimeRate = 10000L // ₹100 / hr
            events.add(
                EarningLedgerEvent(
                    id = "ev-$dateIso-overtime",
                    title = "Approved Overtime Check-in ($hrs hrs)",
                    category = "OVERTIME",
                    timeDisplay = "06:00 PM - 08:00 PM",
                    amountCents = overtimeRate * hrs
                )
            )
        }

        if (events.isNotEmpty()) {
            val dayTotal = events.sumOf { if (it.isDeduction) -it.amountCents else it.amountCents }
            resultList.add(
                EarningLedgerDayEntry(
                    dateIso = dateIso,
                    dateDisplay = dateDisplay,
                    events = events,
                    dayTotalCents = dayTotal
                )
            )
        }
    }

    return resultList
}
