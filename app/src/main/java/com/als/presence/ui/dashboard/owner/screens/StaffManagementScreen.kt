package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/**
 * "Staff Management" screen — delegates to unified [EmployeesManagementScreen].
 */
@Composable
fun StaffManagementScreen(
    onBack: () -> Unit,
    canManageSalaries: Boolean,
    canAddStaff: Boolean,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    EmployeesManagementScreen(
        onBack = onBack,
        canManage = canManageSalaries || canAddStaff,
        viewModel = viewModel
    )
}
