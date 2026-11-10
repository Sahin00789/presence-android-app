package com.als.presence.ui.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.als.presence.core.common.UiState
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Full-screen Payroll Console.
 */
@Composable
fun PayrollScreen(
    onBack: () -> Unit,
    viewModel: PayrollViewModel = hiltViewModel()
) {
    val periodsState by viewModel.periods.collectAsStateWithLifecycle()
    val itemsState by viewModel.items.collectAsStateWithLifecycle()
    val staffState by viewModel.staff.collectAsStateWithLifecycle()
    val selectedPeriodId by viewModel.selectedPeriodId.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var activeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            activeMessage = message
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PresenceSecondaryScaffold(
            crumb = "PAYROLL",
            title = "Monthly Payroll",
            onBack = onBack,
            onRefresh = viewModel::refresh
        ) {
            PayrollScreenContent(
                periodsState = periodsState,
                itemsState = itemsState,
                staffState = staffState,
                selectedPeriodId = selectedPeriodId,
                isSubmitting = isSubmitting,
                onRefresh = viewModel::refresh,
                onSelectPeriod = viewModel::selectPeriod,
                onCalculate = { },
                onRecordPayment = viewModel::recordPayment,
                onCreatePeriod = { _, _ -> },
                onApprove = { }
            )
        }

        MessageSnackbar(message = activeMessage, onDismiss = { activeMessage = null })
    }
}

/**
 * Main Payroll Console Screen UI content.
 */
@Composable
fun PayrollScreenContent(
    periodsState: UiState<List<PayrollPeriodDto>>,
    itemsState: UiState<List<PayrollItemDto>>,
    staffState: UiState<List<StaffDto>>,
    selectedPeriodId: String?,
    isSubmitting: Boolean,
    onRefresh: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onCalculate: (String) -> Unit,
    onRecordPayment: (itemId: String, amountPaise: Long, paymentMethod: String, reference: String) -> Unit,
    onCreatePeriod: (startDate: String, endDate: String) -> Unit,
    onApprove: (String) -> Unit,
    modifier: Modifier = Modifier,
    premisesState: UiState<List<PremisesDto>> = UiState.Success(emptyList())
) {
    val periods = (periodsState as? UiState.Success)?.data.orEmpty()
    val items = (itemsState as? UiState.Success)?.data.orEmpty()
    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val premises = (premisesState as? UiState.Success)?.data.orEmpty()

    var selectedPremisesId by remember(premises) { mutableStateOf(premises.firstOrNull()?.id) }
    var paying by remember { mutableStateOf<PayrollItemDto?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val currentMonth = YearMonth.now()
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = 5000 - 2)

    val periodByMonth = remember(periods) {
        periods.mapNotNull { period ->
            try {
                if (period.startDate.length >= 7) {
                    YearMonth.parse(period.startDate.take(7)) to period
                } else null
            } catch (_: Exception) {
                null
            }
        }.toMap()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Payroll Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            PresenceRefreshButton(
                onClick = onRefresh,
                isRefreshing = isSubmitting || periodsState is UiState.Loading || itemsState is UiState.Loading,
                contentDescription = "Refresh payroll"
            )
        }

        // Top Premises Selector
        if (premises.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Premises",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPremisesId == null,
                        onClick = { selectedPremisesId = null },
                        label = { Text("All Premises", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    premises.forEach { p ->
                        FilterChip(
                            selected = selectedPremisesId == p.id,
                            onClick = { selectedPremisesId = p.id },
                            label = { Text(Dates.cleanPremisesName(p.name), style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Month Selector Scroll Row (reports style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Payroll Period",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    selectedMonth = currentMonth
                    val targetPeriod = periodByMonth[currentMonth]
                    if (targetPeriod != null) {
                        onSelectPeriod(targetPeriod.id)
                    }
                    coroutineScope.launch { monthListState.animateScrollToItem(5000 - 2) }
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = "Current month",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyRow(
            state = monthListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(10000) { index ->
                val month = currentMonth.minusMonths((5000 - index).toLong())
                val matchingPeriod = periodByMonth[month]
                val isSelected = (matchingPeriod?.id == selectedPeriodId) || (matchingPeriod == null && month == selectedMonth)

                Surface(
                    modifier = Modifier
                        .width(76.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                           else if (matchingPeriod != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    onClick = {
                        selectedMonth = month
                        if (matchingPeriod != null) {
                            onSelectPeriod(matchingPeriod.id)
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White
                                   else if (matchingPeriod != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${month.year}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f)
                                   else if (matchingPeriod != null) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        when (periodsState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading payroll periods…")
            is UiState.Error -> SectionError(periodsState.message)
            is UiState.Success -> {
                val activeMonthPeriod = periodByMonth[selectedMonth]

                LaunchedEffect(selectedPeriodId, periods) {
                    val matching = periods.firstOrNull { it.id == selectedPeriodId }
                    if (matching != null) {
                        try {
                            selectedMonth = YearMonth.parse(matching.startDate.take(7))
                        } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(selectedMonth, activeMonthPeriod) {
                    if (activeMonthPeriod != null && activeMonthPeriod.id != selectedPeriodId) {
                        onSelectPeriod(activeMonthPeriod.id)
                    }
                }

                LaunchedEffect(selectedMonth, activeMonthPeriod, isSubmitting) {
                    if (activeMonthPeriod == null && !isSubmitting) {
                        val monthStart = selectedMonth.atDay(1).toString()
                        val monthEnd = selectedMonth.atEndOfMonth().toString()
                        onCreatePeriod(monthStart, monthEnd)
                    }
                }

                if (activeMonthPeriod != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            label = "Gross",
                            value = Money.format(activeMonthPeriod.grossSalaryPaise),
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Net payable",
                            value = Money.format(activeMonthPeriod.netSalaryPaise),
                            modifier = Modifier.weight(1f),
                            accent = MaterialTheme.colorScheme.secondary
                        )
                        MetricTile(
                            label = "Status",
                            value = activeMonthPeriod.status.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f),
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onCalculate(activeMonthPeriod.id) },
                            enabled = !isSubmitting &&
                                activeMonthPeriod.status != "LOCKED" &&
                                activeMonthPeriod.status != "APPROVED",
                            label = { Text("Recalculate") }
                        )
                        if (activeMonthPeriod.status == "PENDING_APPROVAL" ||
                            activeMonthPeriod.status == "READY_FOR_REVIEW"
                        ) {
                            AssistChip(
                                onClick = { onApprove(activeMonthPeriod.id) },
                                enabled = !isSubmitting,
                                label = { Text("Approve & lock") }
                            )
                        }
                        Text(
                            text = "${activeMonthPeriod.itemCount} employees • locked ${Dates.formatTimestamp(activeMonthPeriod.lockedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val monthStart = selectedMonth.atDay(1).toString()
                    val monthEnd = selectedMonth.atEndOfMonth().toString()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Auto-generating payroll for ${selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedMonth.year}…",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Period window: $monthStart to $monthEnd",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SectionLoading("Computing attendance and shift payment rules…")
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Salary items")
        when (itemsState) {
            is UiState.Idle -> SectionLoading("Select a payroll period.")
            is UiState.Loading -> SectionLoading("Loading salary items…")
            is UiState.Error -> SectionError(itemsState.message)
            is UiState.Success -> {
                val filteredItems = items.filter { item ->
                    selectedPremisesId == null || staff.firstOrNull { s -> s.id == item.employeeId }?.premisesId == selectedPremisesId
                }
                if (filteredItems.isEmpty()) {
                    SectionEmpty("This period has no calculated items yet for selected premises.")
                } else {
                    filteredItems.forEach { item ->
                        PayrollItemCard(item = item, onPay = { paying = item })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        SectionHeader(title = "Wallet liabilities")
        when (staffState) {
            is UiState.Loading, is UiState.Idle -> SectionLoading("Loading wallet balances…")
            is UiState.Error -> SectionError(staffState.message)
            is UiState.Success -> {
                val filteredStaff = staff.filter { member ->
                    selectedPremisesId == null || member.premisesId == null || member.premisesId == selectedPremisesId
                }
                if (filteredStaff.isEmpty()) {
                    SectionEmpty("No employee wallets for selected premises.")
                } else {
                    filteredStaff.forEach { member ->
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
                                InitialAvatar(name = member.fullName)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Earned ${Money.format(member.earnedBalancePaise)} • Paid ${Money.format(member.paidBalancePaise)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Money.format(
                                        member.earnedBalancePaise +
                                            member.approvedUnpaidBalancePaise +
                                            member.paidBalancePaise
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    paying?.let { item ->
        val paidAmt = item.payment?.amountPaise ?: 0L
        val grossAmt = item.resolvedGrossSalaryPaise
        PayStaffModal(
            staffName = item.resolvedStaffName,
            staffCode = item.resolvedStaffCode,
            earnedPaise = grossAmt,
            paidPaise = paidAmt,
            breakdown = item.breakdown,
            attendanceSummary = item.attendanceSummary,
            paymentHistory = if (item.payment != null) listOf(item.payment) else emptyList(),
            onDismiss = { paying = null },
            onConfirmPay = { amount, method, reference ->
                onRecordPayment(item.id, amount, method, reference)
                paying = null
            }
        )
    }
}

@Composable
private fun PayrollItemCard(item: PayrollItemDto, onPay: () -> Unit) {
    val grossAmt = item.resolvedGrossSalaryPaise
    val netAmt = item.resolvedNetSalaryPaise

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
                InitialAvatar(name = item.resolvedStaffName)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.resolvedStaffName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.resolvedStaffCode} • present ${item.attendanceSummary.presentDays}/${item.attendanceSummary.totalDays} shifts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(item.payment?.let { "PAID" } ?: "PENDING")
            }

            KeyValueRow(label = "Gross", value = Money.format(grossAmt))
            KeyValueRow(label = "Additions", value = Money.format(item.additionsPaise))
            KeyValueRow(
                label = "Net",
                value = Money.format(netAmt),
                valueColor = MaterialTheme.colorScheme.secondary
            )

            if (item.payment == null) {
                val dueAmt = (netAmt - (item.payment?.amountPaise ?: 0L)).coerceAtLeast(0L)
                FilledTonalButton(
                    onClick = onPay,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay Salary (${Money.format(dueAmt)})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Paid ${Money.format(item.payment.amountPaise)} • ${item.payment.referenceNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
