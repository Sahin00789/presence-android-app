package com.als.presence.ui.dashboard.owner.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.als.presence.core.common.UiState
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
import com.als.presence.ui.dashboard.common.PayrollScreenContent

/**
 * Owner "Salary" tab. Delegates to the shared [PayrollScreenContent], which renders monthly
 * payroll periods, per-staff salary items and wallet liabilities.
 */
@Composable
fun OwnerSalaryTab(
    periodsState: UiState<List<PayrollPeriodDto>>,
    itemsState: UiState<List<PayrollItemDto>>,
    staffState: UiState<List<StaffDto>>,
    selectedPeriodId: String?,
    isSubmitting: Boolean,
    onRefresh: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onCalculate: (String) -> Unit,
    onRecordPayment: (itemId: String, amountCents: Long, paymentMethod: String, reference: String) -> Unit,
    onCreatePeriod: (startDate: String, endDate: String) -> Unit,
    onApprove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PayrollScreenContent(
        periodsState = periodsState,
        itemsState = itemsState,
        staffState = staffState,
        selectedPeriodId = selectedPeriodId,
        isSubmitting = isSubmitting,
        onRefresh = onRefresh,
        onSelectPeriod = onSelectPeriod,
        onCalculate = onCalculate,
        onRecordPayment = onRecordPayment,
        onCreatePeriod = onCreatePeriod,
        onApprove = onApprove,
        modifier = modifier
    )
}
