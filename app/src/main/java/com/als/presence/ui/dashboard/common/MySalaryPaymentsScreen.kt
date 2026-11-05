package com.als.presence.ui.dashboard.common

import android.widget.Toast
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.employee.viewmodels.EmployeeLedgerViewModel
import java.time.YearMonth

/**
 * Screen: "My Salary Payments" for Staff & Managers.
 * Features an Overall Payroll Summary card, a Month-Year Scrollable Chip Selector,
 * Selected Month Payment Details Card, PDF Payslip Download, and Earning Breakdown Entries.
 */
@Composable
fun MySalaryPaymentsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeLedgerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val overallSummaryState by viewModel.overallSummary.collectAsStateWithLifecycle()
    val monthlyLedgerState by viewModel.monthlyLedger.collectAsStateWithLifecycle()
    val ledgerEntriesState by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val currentMonth = remember { YearMonth.now() }
    val monthOffsetList = remember { (-60..0).toList() }
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = monthOffsetList.lastIndex)

    val monthName = selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthYearLabel = "$monthName ${selectedMonth.year}"

    val monthlyLedger = (monthlyLedgerState as? UiState.Success)?.data
    val ledgerEntries = (ledgerEntriesState as? UiState.Success)?.data.orEmpty().ifEmpty {
        monthlyLedger?.entries.orEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "PERSONAL FINANCE",
            title = "My Salary",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Overall Payroll Summary Card (Top)
                when (overallSummaryState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading overall salary summary…")
                    is UiState.Error -> SectionError((overallSummaryState as UiState.Error).message)
                    is UiState.Success -> {
                        val overall = (overallSummaryState as UiState.Success).data
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "NET OUTSTANDING BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Money.format(overall.totalDuePaise),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MetricTile(
                                        label = "Total Earned",
                                        value = Money.format(overall.totalEarnedPaise),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Total Paid",
                                        value = Money.format(overall.totalPaidPaise),
                                        modifier = Modifier.weight(1f),
                                        accent = MaterialTheme.colorScheme.secondary
                                    )
                                    MetricTile(
                                        label = "Total Due",
                                        value = Money.format(overall.totalDuePaise),
                                        modifier = Modifier.weight(1f),
                                        accent = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Month-Year Scrollable Chip Selector
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
                        items(monthOffsetList.size, key = { monthOffsetList[it] }) { index ->
                            val offset = monthOffsetList[index]
                            val month = currentMonth.plusMonths(offset.toLong())
                            val isSelected = month == selectedMonth
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectMonth(month) },
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

                // 3. Month Payslip Download Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$monthYearLabel Payslip",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Official PDF salary statement",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Downloading PDF payslip for $monthYearLabel…",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Download PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // 4. Selected Month Salary & Payment Details Card
                when (monthlyLedgerState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading $monthYearLabel salary statement…")
                    is UiState.Error -> SectionError((monthlyLedgerState as UiState.Error).message)
                    is UiState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$monthYearLabel STATEMENT".uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.0.sp,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    StatusPill(monthlyLedger?.status ?: "OPEN")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MetricTile(
                                        label = "Gross Earned",
                                        value = formatInrString(monthlyLedger?.earnedAmount),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Total Paid",
                                        value = formatInrString(monthlyLedger?.paidAmount),
                                        modifier = Modifier.weight(1f),
                                        accent = MaterialTheme.colorScheme.secondary
                                    )
                                    MetricTile(
                                        label = "Net Payable / Due",
                                        value = formatInrString(monthlyLedger?.dueAmount),
                                        modifier = Modifier.weight(1f),
                                        accent = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Monthly Earning Breakdown Entries
                SectionHeader(title = "$monthYearLabel Earning Breakdown")
                when (ledgerEntriesState) {
                    is UiState.Loading, is UiState.Idle -> SectionLoading("Loading breakdown entries…")
                    is UiState.Error -> SectionError((ledgerEntriesState as UiState.Error).message)
                    is UiState.Success -> {
                        if (ledgerEntries.isEmpty()) {
                            SectionEmpty("No ledger entries recorded for $monthYearLabel.")
                        } else {
                            ledgerEntries.forEach { entry -> PaymentLedgerRow(entry) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

private fun formatInrString(amountStr: String?): String {
    if (amountStr.isNullOrBlank()) return "₹0"
    val cleaned = amountStr.filter { ch -> ch.isDigit() }
    val num = cleaned.toLongOrNull() ?: 0L
    return Money.format(num)
}

@Composable
private fun PaymentLedgerRow(entry: StaffEarningLedgerDto) {
    val amountPaise = entry.amountCents
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Dates.formatDate(entry.earningDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+" + Money.format(amountPaise),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
