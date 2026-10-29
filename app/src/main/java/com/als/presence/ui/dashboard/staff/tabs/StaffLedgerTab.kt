package com.als.presence.ui.dashboard.staff.tabs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.data.model.EmployeeDto
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.KeyValueRow
import com.als.presence.ui.dashboard.common.MetricTile
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.SectionEmpty
import com.als.presence.ui.dashboard.common.SectionError
import com.als.presence.ui.dashboard.common.SectionHeader
import com.als.presence.ui.dashboard.common.SectionLoading
import com.als.presence.ui.dashboard.common.StatusPill

/** Staff "Ledger" tab: balances derived from the append-only ledger. */
@Composable
fun staffLedgerTab(
    summaryState: UiState<EmployeeDto>,
    ledgerState: UiState<List<StaffEarningLedgerDto>>,
    payslipsState: UiState<List<PayslipDto>>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val summary = (summaryState as? UiState.Success)?.data
    val ledgerEntries = (ledgerState as? UiState.Success)?.data.orEmpty()
    val payslips = (payslipsState as? UiState.Success)?.data.orEmpty()

    val totalCents = (summary?.earnedBalanceCents ?: 0) + (summary?.approvedUnpaidBalanceCents ?: 0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Salary Ledger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Append-only earnings and balances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh ledger",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (summaryState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading your salary ledger…")
            is UiState.Error -> SectionError(summaryState.message)
            is UiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Money.format(totalCents),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile(
                                label = "Earned",
                                value = Money.format(summary?.earnedBalanceCents ?: 0),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Unpaid",
                                value = Money.format(summary?.approvedUnpaidBalanceCents ?: 0),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                            MetricTile(
                                label = "Paid",
                                value = Money.format(summary?.paidBalanceCents ?: 0),
                                modifier = Modifier.weight(1f),
                                accent = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Recent ledger entries")
        when (ledgerState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading entries…")
            is UiState.Error -> SectionError(ledgerState.message)
            is UiState.Success -> {
                if (ledgerEntries.isEmpty()) {
                    SectionEmpty("No ledger entries yet. Accruals appear after attendance is approved.")
                } else {
                    ledgerEntries.forEach { entry -> LedgerRow(entry) }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        SectionHeader(title = "Payslips")
        when (payslipsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading payslips…")
            is UiState.Error -> SectionError(payslipsState.message)
            is UiState.Success -> {
                if (payslips.isEmpty()) {
                    SectionEmpty("No payslips yet. They appear once payroll is calculated.")
                } else {
                    payslips.forEach { payslip -> PayslipRow(payslip) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LedgerRow(entry: StaffEarningLedgerDto) {
    val amountCents = entry.amount.takeIf { it > 0 } ?: entry.amountCents
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp)
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
                text = "+" + Money.format(amountCents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PayslipRow(payslip: PayslipDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${Dates.formatDateShort(payslip.periodStartDate)} – ${Dates.formatDateShort(payslip.periodEndDate)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(payslip.periodStatus)
            }
            KeyValueRow(label = "Gross", value = Money.format(payslip.grossSalaryCents))
            KeyValueRow(label = "Deductions", value = Money.format(payslip.deductionsCents))
            KeyValueRow(
                label = "Net paid",
                value = Money.format(payslip.netSalaryCents),
                valueColor = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Present ${payslip.attendanceSummary.presentDays} • Late ${payslip.attendanceSummary.lateDays} • Leave ${payslip.attendanceSummary.leaveDays}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
