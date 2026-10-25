package com.als.presence.ui.dashboard.owner.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.als.presence.ui.dashboard.owner.viewmodels.OwnerWorkforceViewModel

/**
 * "Manager Management" screen — delegates to unified [EmployeesManagementScreen].
 */
@Composable
fun ManagerManagementScreen(
    onBack: () -> Unit,
    canInvite: Boolean,
    viewModel: OwnerWorkforceViewModel = hiltViewModel()
) {
    EmployeesManagementScreen(
        onBack = onBack,
        canManage = canInvite,
        viewModel = viewModel
    )
}
