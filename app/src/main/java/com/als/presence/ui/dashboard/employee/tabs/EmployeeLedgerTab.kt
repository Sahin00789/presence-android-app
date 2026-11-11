package com.als.presence.ui.dashboard.employee.tabs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.MonthlyEarningLedgerResponseDto
import com.als.presence.data.model.MyOverallPayrollSummaryDto
import com.als.presence.data.model.PaymentRecordDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceRefreshButton
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill
import java.time.YearMonth

/**
 * Employee "Ledger" tab.
 *
 * Order (top to bottom):
 *  1. Overall earned / paid / due card.
 *  2. Scrollable month selector cards (e.g. MAR 2026).
 *  3. Selected month earned / paid / due summary with PDF Payslip download.
 *  4. Selected month salary payments.
 *  5. Selected month earning ledger.
 */
@Composable
fun EmployeeLedgerTab(
    overallSummaryState: UiState<MyOverallPayrollSummaryDto>,
    monthlyLedgerState: UiState<MonthlyEarningLedgerResponseDto>,
    ledgerState: UiState<List<StaffEarningLedgerDto>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    selectedMonth: YearMonth = YearMonth.now(),
    onMonthSelected: (YearMonth) -> Unit = {},
    onDownloadPayslip: (YearMonth) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val monthlyLedger = (monthlyLedgerState as? UiState.Success)?.data
    val ledgerEntries = (ledgerState as? UiState.Success)?.data.orEmpty().ifEmpty {
        monthlyLedger?.entries.orEmpty()
    }
    val payments = monthlyLedger?.payments.orEmpty()

    val currentMonth = remember { YearMonth.now() }
    val monthOffsetList = remember { (-60..0).toList() }
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = monthOffsetList.lastIndex)

    val monthName = selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthYearLabel = "$monthName ${selectedMonth.year}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp)
            .padding(top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salary Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PresenceDesign.Ink
                )
                Text(
                    text = "Monthly earnings, payments & ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = PresenceDesign.Muted
                )
            }
            PresenceRefreshButton(
                onClick = onRefresh,
                isRefreshing = ledgerState is UiState.Loading || monthlyLedgerState is UiState.Loading,
                contentDescription = "Refresh ledger"
            )
        }

        // 1. Overall Summary Card (Top)
        when (overallSummaryState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading overall summary…")
            is UiState.Error -> SectionError(overallSummaryState.message)
            is UiState.Success -> {
                val overall = overallSummaryState.data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "OVERALL PAYROLL SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetricTile(
                                label = "Total Earned",
                                value = Money.format(overall.totalEarnedInr),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Total Paid",
                                value = Money.format(overall.totalPaidInr),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.secondary
                            )
                            MetricTile(
                                label = "Total Due",
                                value = Money.format(overall.totalDueInr),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        // 2. Month Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            LazyRow(
                state = monthListState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(monthOffsetList.size) { index ->
                    val offset = monthOffsetList[index]
                    val month = currentMonth.plusMonths(offset.toLong())
                    val isSelected = month == selectedMonth
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { onMonthSelected(month) },
                        label = {
                            Text(
                                text = "${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                                fontSize = 11.5.sp
                            )
                        }
                    )
                }
            }
        }

        // 3. Selected Month Summary Card (Earned, Paid, Due + PDF Download)
        when (monthlyLedgerState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading $monthYearLabel summary…")
            is UiState.Error -> SectionError(monthlyLedgerState.message)
            is UiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$monthYearLabel Summary".uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(monthlyLedger?.status ?: "OPEN")
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { onDownloadPayslip(selectedMonth) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "PDF Payslip",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricTile(
                                label = "Earned",
                                value = formatInrString(monthlyLedger?.earnedAmount),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Paid",
                                value = formatInrString(monthlyLedger?.paidAmount),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.secondary
                            )
                            MetricTile(
                                label = "Due",
                                value = formatInrString(monthlyLedger?.dueAmount),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        // 4. Selected Month Salary Payments
        SectionHeader(title = "$monthYearLabel Salary Payments")
        when (monthlyLedgerState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading payments…")
            is UiState.Error -> SectionError(monthlyLedgerState.message)
            is UiState.Success -> {
                if (payments.isEmpty()) {
                    SectionEmpty("No salary payments recorded for $monthYearLabel.")
                } else {
                    payments.forEach { payment -> PaymentRow(payment) }
                }
            }
        }

        // 5. Selected Month Earning Ledger
        SectionHeader(title = "$monthYearLabel Earning Ledger")
        when (ledgerState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading entries…")
            is UiState.Error -> SectionError(ledgerState.message)
            is UiState.Success -> {
                if (ledgerEntries.isEmpty()) {
                    SectionEmpty("No ledger entries recorded for $monthYearLabel.")
                } else {
                    ledgerEntries.forEach { entry -> LedgerRow(entry) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PaymentRow(payment: PaymentRecordDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salary Payment",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(payment.paymentMethod.lowercase().replace('_', ' ')
                            .replaceFirstChar { it.uppercase() })
                        if (payment.paymentDate.isNotBlank()) {
                            append(" • ${Dates.formatDate(payment.paymentDate)}")
                        }
                        if (!payment.referenceNumber.isNullOrBlank()) {
                            append(" • ${payment.referenceNumber}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "-" + Money.format(payment.amountInr),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

private fun formatInrString(amountStr: String?): String {
    if (amountStr.isNullOrBlank()) return "₹0"
    val intPart = amountStr.replace("₹", "").replace(",", "").trim().substringBefore('.')
    val num = intPart.toLongOrNull() ?: 0L
    return Money.format(num)
}

@Composable
private fun LedgerRow(entry: StaffEarningLedgerDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Dates.formatDate(entry.earningDate),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+" + Money.format(entry.amountInr),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
