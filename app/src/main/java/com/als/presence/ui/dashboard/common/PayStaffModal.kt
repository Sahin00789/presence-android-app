package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.PaymentRecordDto
import com.als.presence.data.model.PayrollBreakdownDto

private data class SalaryBillLineItem(
    val label: String,
    val countLabel: String,
    val lineTotalCents: Long,
    val isDeduction: Boolean = false
)

/**
 * Modal Bottom Sheet to disburse/record salary payment for an employee.
 * Features an itemized monthly salary bill statement table (Attendance Line Item, Count, Line Total Amount),
 * payment history, partial/multiple payment entry, selectable payment method, and optional reference notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayStaffModal(
    staffName: String,
    staffCode: String? = null,
    monthLabel: String? = null,
    earnedCents: Long,
    paidCents: Long,
    breakdown: PayrollBreakdownDto = PayrollBreakdownDto(),
    attendanceSummary: AttendanceSummaryDto = AttendanceSummaryDto(),
    paymentHistory: List<PaymentRecordDto> = emptyList(),
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmPay: (amountCents: Long, paymentMethod: String, reference: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dueCents = (earnedCents - paidCents).coerceAtLeast(0L)
    val defaultAmount = if (dueCents > 0) (dueCents / 100).toString() else if (earnedCents > 0) (earnedCents / 100).toString() else ""

    var amountInput by remember(defaultAmount) { mutableStateOf(defaultAmount) }
    var selectedMethod by remember { mutableStateOf("BANK_TRANSFER") }
    var referenceInput by remember { mutableStateOf("") }

    val parsedCents = Money.parseToCents(amountInput) ?: 0L

    val paymentMethods = listOf(
        "BANK_TRANSFER" to "Bank Transfer",
        "UPI" to "UPI / GPay / PhonePe",
        "CASH" to "Cash",
        "CHEQUE" to "Cheque"
    )

    // Construct Itemized Bill Statement Line Items
    val billItems = remember(earnedCents, breakdown, attendanceSummary) {
        buildList {
            // 1. Present Days
            if (attendanceSummary.presentDays > 0 || (breakdown.earnings.isEmpty() && earnedCents > 0)) {
                val count = attendanceSummary.presentDays.coerceAtLeast(1)
                val baseLine = breakdown.earnings.firstOrNull { it.code == "BASE_SALARY" }
                val amount = baseLine?.amountCents ?: earnedCents
                add(
                    SalaryBillLineItem(
                        label = "Present Days",
                        countLabel = "$count days",
                        lineTotalCents = amount
                    )
                )
            }

            // 2. Half-Days
            if (attendanceSummary.halfDays > 0) {
                val count = attendanceSummary.halfDays
                val halfDayLine = breakdown.earnings.firstOrNull { it.code == "HALF_DAY" }
                val amount = halfDayLine?.amountCents ?: 0L
                add(
                    SalaryBillLineItem(
                        label = "Half-Days",
                        countLabel = "$count days",
                        lineTotalCents = amount
                    )
                )
            }

            // 3. Paid Holidays
            if (attendanceSummary.holidayDays > 0) {
                val count = attendanceSummary.holidayDays
                val line = breakdown.earnings.firstOrNull { it.code == "HOLIDAY_PAY" }
                val amount = line?.amountCents ?: 0L
                add(
                    SalaryBillLineItem(
                        label = "Paid Holidays",
                        countLabel = "$count days",
                        lineTotalCents = amount
                    )
                )
            }

            // 4. Weekly Offs
            if (attendanceSummary.weeklyOffDays > 0) {
                val count = attendanceSummary.weeklyOffDays
                val line = breakdown.earnings.firstOrNull { it.code == "WEEKLY_OFF_PAY" }
                val amount = line?.amountCents ?: 0L
                add(
                    SalaryBillLineItem(
                        label = "Weekly Offs",
                        countLabel = "$count days",
                        lineTotalCents = amount
                    )
                )
            }

            // 5. Paid Leave
            val leaveDaysCount = attendanceSummary.leaveDays
            if (leaveDaysCount > 0) {
                val line = breakdown.earnings.firstOrNull { it.code == "PAID_LEAVE" }
                val amount = line?.amountCents ?: 0L
                add(
                    SalaryBillLineItem(
                        label = "Paid Leave",
                        countLabel = "$leaveDaysCount days",
                        lineTotalCents = amount
                    )
                )
            }

            // 6. Completed Overtime
            val overtimeMins = attendanceSummary.totalOvertimeMinutes.toInt()
            val overtimeLine = breakdown.earnings.firstOrNull { it.code == "OVERTIME" || it.code == "COMPLETED_OVERTIME" }
            if (overtimeMins > 0 || (overtimeLine != null && overtimeLine.amountCents > 0)) {
                val hrs = if (overtimeMins > 0) "${overtimeMins / 60} hrs" else "Approved"
                val amount = overtimeLine?.amountCents ?: 0L
                add(
                    SalaryBillLineItem(
                        label = "Approved Overtime",
                        countLabel = hrs,
                        lineTotalCents = amount
                    )
                )
            }

            // 7. Other Earnings Lines
            breakdown.earnings.filter { lineItem ->
                lineItem.code !in listOf("BASE_SALARY", "HALF_DAY", "HOLIDAY_PAY", "WEEKLY_OFF_PAY", "PAID_LEAVE", "OVERTIME", "COMPLETED_OVERTIME")
            }.forEach { lineItem ->
                add(
                    SalaryBillLineItem(
                        label = lineItem.label,
                        countLabel = "Earning",
                        lineTotalCents = lineItem.amountCents
                    )
                )
            }

            // 8. Deductions
            breakdown.deductions.forEach { lineItem ->
                add(
                    SalaryBillLineItem(
                        label = lineItem.label,
                        countLabel = "Deduction",
                        lineTotalCents = lineItem.amountCents,
                        isDeduction = true
                    )
                )
            }
        }
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
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Record Salary Payment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildString {
                                append(staffName)
                                if (!staffCode.isNullOrBlank()) append(" • ").append(staffCode)
                                if (!monthLabel.isNullOrBlank()) append(" • ").append(monthLabel)
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

            // Scrollable Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selected Month Badge
                if (!monthLabel.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Selected Month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Itemized Salary Bill Table
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                            Text(
                                text = "Itemized Salary Bill (${monthLabel ?: "Monthly"})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "MONTHLY STATEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Table Header: Attendance Line Item | Count | Line Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Attendance Line Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.8f))
                            Text("Count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))
                            Text("Line Total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.1f))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Table Rows
                        if (billItems.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Present Base Salary", style = MaterialTheme.typography.bodySmall, fontSize = 11.5.sp, modifier = Modifier.weight(1.8f))
                                Text("1 month", style = MaterialTheme.typography.bodySmall, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))
                                Text(Money.format(earnedCents), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.1f))
                            }
                        } else {
                            billItems.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = line.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = if (line.isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1.8f)
                                    )
                                    Text(
                                        text = line.countLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    Text(
                                        text = if (line.isDeduction) "- ${Money.format(line.lineTotalCents)}" else Money.format(line.lineTotalCents),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (line.isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Sum of Totals -> Gross Total Salary, Less Paid, Net Unpaid Due
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gross Total Salary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(earnedCents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Less: Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(paidCents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Unpaid Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(dueCents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                            }
                        }
                    }
                }

                // Payment History for this month
                if (paidCents > 0 || paymentHistory.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "This Month's Payment History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (paymentHistory.isNotEmpty()) {
                            paymentHistory.forEach { record ->
                                Surface(
                                    color = SecondaryEmerald.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SecondaryEmerald.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SecondaryEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Paid ${Money.format(record.amountCents)} via ${record.paymentMethod}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (record.referenceNumber?.isNotBlank() == true) {
                                                    Text(
                                                        text = "Ref: ${record.referenceNumber}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        if (record.paidAt.isNotBlank()) {
                                            Text(
                                                text = Dates.formatDateShort(record.paidAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (paidCents > 0) {
                            Surface(
                                color = SecondaryEmerald.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Paid: ${Money.format(paidCents)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Entry Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Disburse Payment",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount to Pay (₹)") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Method Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentMethods.forEach { (code, label) ->
                                FilterChip(
                                    selected = selectedMethod == code,
                                    onClick = { selectedMethod = code },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Reference / Notes
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = { referenceInput = it },
                        label = { Text("Payment Reference / Notes (Optional)") },
                        placeholder = { Text("e.g. Bank UTR, UPI Txn ID, Cheque No, or Note") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "💡 You can make multiple partial payments for a month. Any remaining balance stays due for future payment.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (parsedCents > 0) {
                            val methodLabel = paymentMethods.firstOrNull { it.first == selectedMethod }?.second ?: selectedMethod
                            val finalRef = referenceInput.trim().ifEmpty { "Salary Payment via $methodLabel" }
                            onConfirmPay(parsedCents, selectedMethod, finalRef)
                        }
                    },
                    enabled = !isSubmitting && parsedCents > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isSubmitting) "Paying…" else "Confirm Payment",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
