package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.PaymentRecordDto
import com.als.presence.data.model.PayrollBreakdownDto
import com.als.presence.data.model.StaffEarningLedgerDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class EarningLedgerEvent(
    val id: String,
    val title: String,
    val category: String,
    val timeDisplay: String? = null,
    val amountPaise: Long,
    val isDeduction: Boolean = false,
    val runningBalancePaise: Long = 0L
)

data class EarningLedgerDayEntry(
    val dateIso: String,
    val dateDisplay: String,
    val events: List<EarningLedgerEvent>,
    val dayTotalPaise: Long
)

/**
 * Modal Bottom Sheet presenting date-separated earning ledger events, summary counts,
 * and optional Record Salary Payment capability for a specific month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyEarningLedgerModal(
    staffName: String,
    staffCode: String? = null,
    monthLabel: String,
    earnedPaise: Long,
    paidPaise: Long = 0L,
    attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    paymentHistory: List<PaymentRecordDto> = emptyList(),
    entries: List<StaffEarningLedgerDto> = emptyList(),
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onRecordPayment: ((amountPaise: Long, paymentMethod: String, reference: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPayModal by remember { mutableStateOf(false) }

    val duePaise = (earnedPaise - paidPaise).coerceAtLeast(0L)

    val ledgerDayEntries = remember(monthLabel, earnedPaise, attendanceSummary, breakdown, entries) {
        computeDateSeparatedEarningLedger(
            monthLabel = monthLabel,
            earnedPaise = earnedPaise,
            attendanceSummary = attendanceSummary,
            breakdown = breakdown,
            rawEntries = entries
        )
    }

    val presentCount = attendanceSummary.presentDays.coerceAtLeast(attendanceSummary.presentShiftsCount)
    val halfDayCount = attendanceSummary.halfDays.coerceAtLeast(attendanceSummary.halfDayShiftsCount)
    val leaveCount = attendanceSummary.leaveDays.coerceAtLeast(attendanceSummary.leaveShiftsCount)
    val absentCount = attendanceSummary.absentDays.coerceAtLeast(attendanceSummary.absentShiftsCount)
    val holidayCount = attendanceSummary.holidayDays.coerceAtLeast(attendanceSummary.holidayCount)
    val offDayCount = attendanceSummary.weeklyOffDays.coerceAtLeast(attendanceSummary.offDayCount)
    val overtimeMins = attendanceSummary.totalOvertimeMinutes

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
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Month Financial & Attendance Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Financial Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(earnedPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(paidPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(duePaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Attendance Summary Counts Grid
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Attendance Summary Counts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryCountChip(label = "Present", count = "$presentCount days", color = SecondaryEmerald, modifier = Modifier.weight(1f))
                            SummaryCountChip(label = "Half-Day", count = "$halfDayCount days", color = TertiaryAmber, modifier = Modifier.weight(1f))
                            SummaryCountChip(label = "Leave", count = "$leaveCount days", color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryCountChip(label = "Absent", count = "$absentCount days", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                            SummaryCountChip(label = "Holidays", count = "$holidayCount days", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                            SummaryCountChip(label = "Off-Days", count = "$offDayCount days", color = Color(0xFF64748B), modifier = Modifier.weight(1f))
                        }

                        if (overtimeMins > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SummaryCountChip(
                                    label = "Overtime",
                                    count = "${overtimeMins / 60} hrs (${overtimeMins % 60} mins)",
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Date-Separated Earning Events List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ledgerDayEntries.isEmpty()) {
                    SectionEmpty("No daily earning events recorded for $monthLabel.")
                } else {
                    ledgerDayEntries.forEach { dayEntry ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Date Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = dayEntry.dateDisplay,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Day Total: ${Money.format(dayEntry.dayTotalPaise)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = if (dayEntry.dayTotalPaise > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Day's Earning Events Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    dayEntry.events.forEachIndexed { idx, ev ->
                                        if (idx > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ev.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!ev.timeDisplay.isNullOrBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Schedule,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(10.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = ev.timeDisplay,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "+ ${Money.format(ev.amountPaise)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SecondaryEmerald
                                                )
                                                if (ev.runningBalancePaise > 0) {
                                                    Text(
                                                        text = "Bal: ${Money.format(ev.runningBalancePaise)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onRecordPayment != null) {
                    Button(
                        onClick = { showPayModal = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Salary Payment", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = if (onRecordPayment != null) Modifier else Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showPayModal) {
        PayStaffModal(
            staffName = staffName,
            staffCode = staffCode,
            monthLabel = monthLabel,
            earnedPaise = earnedPaise,
            paidPaise = paidPaise,
            breakdown = breakdown,
            attendanceSummary = attendanceSummary,
            paymentHistory = paymentHistory,
            earningLedgerEntries = entries,
            isSubmitting = isSubmitting,
            onDismiss = { showPayModal = false },
            onConfirmPay = { amountPaise, paymentMethod, reference ->
                onRecordPayment?.invoke(amountPaise, paymentMethod, reference)
                showPayModal = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun SummaryCountChip(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun computeDateSeparatedEarningLedger(
    monthLabel: String,
    earnedPaise: Long,
    attendanceSummary: AttendanceSummaryDto,
    breakdown: PayrollBreakdownDto,
    rawEntries: List<StaffEarningLedgerDto>
): List<EarningLedgerDayEntry> {
    val dateDisplayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    if (rawEntries.isNotEmpty()) {
        // Sort chronologically ascending to compute running balance correctly
        val sortedEntries = rawEntries.sortedBy { it.earningDate }
        var runningAccumulator = 0L

        val eventsWithBalance = sortedEntries.map { item ->
            val amt = item.amountInr
            runningAccumulator += amt
            val label = when (item.type.uppercase()) {
                "SHIFT" -> "Shift Attendance"
                "HOLIDAY" -> "Public Holiday (Paid)"
                "OFF_DAY" -> "Scheduled Weekly Off (Paid)"
                "OVERTIME" -> "Approved Overtime"
                else -> item.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
            Triple(
                item.earningDate,
                EarningLedgerEvent(
                    id = item.id.ifBlank { "ev-${item.earningDate}-${item.type}" },
                    title = label,
                    category = item.type,
                    timeDisplay = null,
                    amountPaise = amt,
                    isDeduction = false,
                    runningBalancePaise = runningAccumulator
                ),
                amt
            )
        }

        val groupedByDate = eventsWithBalance.groupBy { it.first }
        val resultList = mutableListOf<EarningLedgerDayEntry>()

        for ((dateIso, triples) in groupedByDate.entries.sortedByDescending { it.key }) {
            val localDate = runCatching { LocalDate.parse(dateIso) }.getOrNull()
            val dateDisplay = localDate?.format(dateDisplayFormatter) ?: dateIso
            val events = triples.map { it.second }
            val dayTotal = triples.sumOf { it.third }

            resultList.add(
                EarningLedgerDayEntry(
                    dateIso = dateIso,
                    dateDisplay = dateDisplay,
                    events = events,
                    dayTotalPaise = dayTotal
                )
            )
        }

        return resultList
    }

    // Fallback: If raw daily entries are not loaded, generate category breakdown summary for monthLabel
    if (earnedPaise <= 0L && breakdown.earnings.isEmpty() && attendanceSummary.presentDays == 0) {
        return emptyList()
    }

    val syntheticEvents = mutableListOf<EarningLedgerEvent>()

    // 1. Present Days
    if (attendanceSummary.presentDays > 0 || (breakdown.earnings.isEmpty() && earnedPaise > 0)) {
        val count = attendanceSummary.presentDays.coerceAtLeast(1)
        val baseLine = breakdown.earnings.firstOrNull { it.code in listOf("BASE_SALARY", "SHIFT", "PRESENT", "PRESENT_PAY") }
        val otherEarningsSum = breakdown.earnings.filter { it.code !in listOf("BASE_SALARY", "SHIFT", "PRESENT", "PRESENT_PAY") }.sumOf { it.amountInr }
        val amount = baseLine?.amountInr ?: if (breakdown.earnings.isEmpty()) earnedPaise else (earnedPaise - otherEarningsSum).coerceAtLeast(0L)

        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-present",
                title = "Shift Clock-in ($count present days)",
                category = "SHIFT",
                timeDisplay = "$count shift days accrued",
                amountPaise = amount
            )
        )
    }

    // 2. Half Days
    if (attendanceSummary.halfDays > 0) {
        val count = attendanceSummary.halfDays
        val halfDayLine = breakdown.earnings.firstOrNull { it.code in listOf("HALF_DAY", "HALF_DAY_PAY") }
        val amount = halfDayLine?.amountInr ?: 0L
        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-halfday",
                title = "Half-Day Shifts ($count days)",
                category = "SHIFT",
                timeDisplay = "$count half-day shifts",
                amountPaise = amount
            )
        )
    }

    // 3. Paid Holidays
    if (attendanceSummary.holidayDays > 0) {
        val count = attendanceSummary.holidayDays
        val line = breakdown.earnings.firstOrNull { it.code in listOf("HOLIDAY", "HOLIDAY_PAY", "PAID_HOLIDAY") }
        val amount = line?.amountInr ?: 0L
        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-holiday",
                title = "Public Holidays ($count days)",
                category = "HOLIDAY",
                timeDisplay = "$count paid holidays",
                amountPaise = amount
            )
        )
    }

    // 4. Weekly Offs
    if (attendanceSummary.weeklyOffDays > 0) {
        val count = attendanceSummary.weeklyOffDays
        val line = breakdown.earnings.firstOrNull { it.code in listOf("OFF_DAY", "WEEKLY_OFF", "WEEKLY_OFF_PAY") }
        val amount = line?.amountInr ?: 0L
        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-offday",
                title = "Scheduled Weekly Offs ($count days)",
                category = "OFF_DAY",
                timeDisplay = "$count off-days",
                amountPaise = amount
            )
        )
    }

    // 5. Paid Leave
    if (attendanceSummary.leaveDays > 0) {
        val count = attendanceSummary.leaveDays
        val line = breakdown.earnings.firstOrNull { it.code in listOf("PAID_LEAVE", "LEAVE") }
        val amount = line?.amountInr ?: 0L
        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-leave",
                title = "Paid Leave ($count days)",
                category = "PAID_LEAVE",
                timeDisplay = "$count leave days",
                amountPaise = amount
            )
        )
    }

    // 5b. Absent Days
    if (attendanceSummary.absentDays > 0) {
        val count = attendanceSummary.absentDays
        val absentLine = breakdown.earnings.firstOrNull { it.code in listOf("ABSENT", "ABSENT_PAY") }
        val amount = absentLine?.amountInr ?: 0L
        if (amount != 0L || absentLine != null) {
            syntheticEvents.add(
                EarningLedgerEvent(
                    id = "synth-absent",
                    title = "Absent Shift Pay ($count days)",
                    category = "ABSENT",
                    timeDisplay = "$count absent days",
                    amountPaise = amount
                )
            )
        }
    }

    // 6. Overtime
    val otMins = attendanceSummary.totalOvertimeMinutes.toInt()
    val otLine = breakdown.earnings.firstOrNull { it.code in listOf("OVERTIME", "COMPLETED_OVERTIME") }
    if (otMins > 0 || (otLine != null && otLine.amountInr > 0)) {
        val hrs = if (otMins > 0) "${otMins / 60} hrs" else "Approved"
        val amount = otLine?.amountInr ?: 0L
        syntheticEvents.add(
            EarningLedgerEvent(
                id = "synth-overtime",
                title = "Approved Overtime ($hrs)",
                category = "OVERTIME",
                timeDisplay = hrs,
                amountPaise = amount
            )
        )
    }

    if (syntheticEvents.isEmpty()) {
        return emptyList()
    }

    var synthRunning = 0L
    val syntheticWithBalance = syntheticEvents.map { ev ->
        synthRunning += ev.amountPaise
        ev.copy(runningBalancePaise = synthRunning)
    }

    val total = syntheticWithBalance.sumOf { it.amountPaise }
    return listOf(
        EarningLedgerDayEntry(
            dateIso = monthLabel,
            dateDisplay = "Monthly Summary ($monthLabel)",
            events = syntheticWithBalance,
            dayTotalPaise = total
        )
    )
}
