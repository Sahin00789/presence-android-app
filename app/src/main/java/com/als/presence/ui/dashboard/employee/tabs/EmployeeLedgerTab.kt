package com.als.presence.ui.dashboard.employee.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.als.presence.core.common.UiState
import com.als.presence.core.designsystem.LocalScreenSizeInfo
import com.als.presence.core.designsystem.PresenceDesign
import com.als.presence.data.model.MonthlyEarningLedgerResponseDto
import com.als.presence.data.model.MyOverallPayrollSummaryDto
import com.als.presence.data.model.StaffEarningLedgerDto
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.PresenceVariant
import com.als.presence.ui.dashboard.common.components.PresenceButton
import com.als.presence.ui.dashboard.common.components.PresenceEmptyState
import com.als.presence.ui.dashboard.common.components.PresenceMicroLabel
import com.als.presence.ui.dashboard.common.components.PresenceSectionHeader
import java.time.YearMonth

/**
 * Editorial, 2026 Fintech-Quality Redesign for Employee Salary & Earning Ledger.
 * Features:
 * - Selected month/year interactive timeline selector
 * - Atmospheric Gradient Financial Hero (Large financial typography for earned, paid & due)
 * - Lifetime settlement progress ratio bar
 * - Single-tap Payslip PDF download action
 * - Structured transaction rows (NOT individual cards!) with semantic icons, linked source attendance, and clear financial credits/debits
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
    val screenInfo = LocalScreenSizeInfo.current
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val monthlyLedger = (monthlyLedgerState as? UiState.Success)?.data
    val ledgerEntries = (ledgerState as? UiState.Success)?.data.orEmpty().ifEmpty {
        monthlyLedger?.entries.orEmpty()
    }

    val currentMonth = remember { YearMonth.now() }
    val monthOffsetList = remember { (-60..0).toList() }
    val monthListState = rememberLazyListState(initialFirstVisibleItemIndex = monthOffsetList.lastIndex)

    val monthName = selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthYearLabel = "$monthName ${selectedMonth.year}"

    val overall = (overallSummaryState as? UiState.Success)?.data
    val earnedInr = monthlyLedger?.earnedInr ?: ledgerEntries.sumOf { it.amountCents }
    val paidInr = monthlyLedger?.paidAmount?.let { Money.parseToInr(it) } ?: 0L
    val dueInr = (earnedInr - paidInr).coerceAtLeast(0L)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (screenInfo.isTabletOrFoldable) 700.dp else Dp.Unspecified)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = screenInfo.horizontalScreenPadding,
                    vertical = if (screenInfo.isVeryCompact) 12.dp else 18.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // TOP HEADER WITH EDITORIAL TITLE & REFRESH ACTION
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Salary & Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Verified earnings, payment settlement & payslips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 1. MONTH SELECTOR CHIP ROW
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                LazyRow(
                    state = monthListState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthOffsetList.size) { idx ->
                        val offset = monthOffsetList[idx]
                        val ym = currentMonth.plusMonths(offset.toLong())
                        val isSelected = ym == selectedMonth
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMonthSelected(ym) },
                            label = {
                                Text(
                                    text = "${ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${ym.year}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

            // -------------------------------------------------------------
            // 2. FINTECH ATMOSPHERIC GRADIENT HERO SURFACE (SALARY METRICS)
            // -------------------------------------------------------------
            val heroStart = if (isDark) Color(0xFF13222E) else Color(0xFFE6F4EA)
            val heroEnd = if (isDark) Color(0xFF1B2338) else Color(0xFFEDF2FD)
            val heroBorder = if (isDark) Color(0xFF243B3B) else Color(0xFFC7E2D3)
            val glowColor = if (isDark) Color(0xFF0D9488).copy(alpha = 0.16f) else Color(0xFF0D9488).copy(alpha = 0.10f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(heroStart, heroEnd)))
                    .border(BorderStroke(1.dp, heroBorder), RoundedCornerShape(22.dp))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.2f),
                                radius = size.width * 0.6f
                            ),
                            radius = size.width * 0.6f,
                            center = Offset(size.width * 0.85f, size.height * 0.2f)
                        )
                    }
                    .padding(if (screenInfo.isVeryCompact) 16.dp else 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            PresenceMicroLabel(text = "$monthYearLabel Total Earned")
                            Text(
                                text = Money.format(earnedInr),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Single-tap PDF Download Action
                        PresenceButton(
                            text = if (screenInfo.isVeryCompact) "Payslip" else "Download Payslip",
                            onClick = { onDownloadPayslip(selectedMonth) },
                            variant = PresenceVariant.Soft,
                            size = com.als.presence.ui.dashboard.common.PresenceSize.Sm,
                            icon = Icons.Default.PictureAsPdf
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month Paid & Outstanding Net Due Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.6f else 0.85f))
                            .border(BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            PresenceMicroLabel(text = "Amount Paid")
                            Text(
                                text = Money.format(paidInr),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PresenceDesign.Positive
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            PresenceMicroLabel(text = "Outstanding Due")
                            Text(
                                text = Money.format(dueInr),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (dueInr > 0L) PresenceDesign.DangerText else PresenceDesign.Positive
                            )
                        }
                    }

                    // Progress bar: Paid ratio
                    if (earnedInr > 0L) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val paidRatio = (paidInr.toFloat() / earnedInr.toFloat()).coerceIn(0f, 1f)
                        Column {
                            LinearProgressIndicator(
                                progress = { paidRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = PresenceDesign.Positive,
                                trackColor = PresenceDesign.DangerText.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Settled: ${(paidRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PresenceDesign.Positive,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (dueInr == 0L) "All Settled" else "Pending Disbursement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Lifetime Summary Row if available
                    if (overall != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lifetime Earned: ${Money.format(overall.totalEarnedInr)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Lifetime Paid: ${Money.format(overall.totalPaidInr)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PresenceDesign.Positive,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. DAILY EARNING LEDGER TRANSACTIONS (STRUCTURED ROWS, NOT CARDS!)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$monthYearLabel Ledger Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${ledgerEntries.size} item${if (ledgerEntries.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (ledgerEntries.isEmpty()) {
                PresenceEmptyState(
                    title = "No Earning Entries",
                    description = "No attendance or overtime earnings recorded for $monthYearLabel yet."
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 1.dp
                ) {
                    Column {
                        ledgerEntries.forEachIndexed { index, entry ->
                            LedgerTransactionRow(entry = entry)
                            if (index < ledgerEntries.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Fintech-Quality Transaction Row for Staff Earning Ledger.
 * Uses semantic icon badges, clear typography, and financial amounts.
 */
@Composable
private fun LedgerTransactionRow(entry: StaffEarningLedgerDto) {
    val isShift = entry.type.equals("SHIFT", ignoreCase = true)
    val isOvertime = entry.type.equals("OVERTIME", ignoreCase = true)
    val isHoliday = entry.type.equals("HOLIDAY", ignoreCase = true)
    val isOffDay = entry.type.equals("OFF_DAY", ignoreCase = true) || entry.type.equals("WEEKLY_OFF", ignoreCase = true)

    val icon = when {
        isShift -> Icons.Default.Schedule
        isOvertime -> Icons.Default.MoreTime
        isHoliday -> Icons.Default.Celebration
        isOffDay -> Icons.Default.CheckCircle
        else -> Icons.Default.MonetizationOn
    }

    val iconColor = when {
        isShift -> MaterialTheme.colorScheme.primary
        isOvertime -> PresenceDesign.Caution
        isHoliday -> Color(0xFF7C3AED)
        isOffDay -> Color(0xFF0D9488)
        else -> PresenceDesign.Positive
    }

    val iconBg = iconColor.copy(alpha = 0.12f)

    val displayTitle = when {
        isShift -> "Shift Attendance Remuneration"
        isOvertime -> "Overtime Session Earning"
        isHoliday -> "Public Holiday Remuneration"
        isOffDay -> "Weekly Off Remuneration"
        else -> entry.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Dates.formatDate(entry.earningDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "+${Money.format(entry.amountCents)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = PresenceDesign.Positive
        )
    }
}
