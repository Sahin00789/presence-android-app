package com.als.presence.ui.dashboard.common

import androidx.compose.runtime.Composable
import com.als.presence.core.common.UiState
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.StaffDto
import com.als.presence.domain.model.User

@Deprecated("Renamed to LiveMonitorTab", ReplaceWith("LiveMonitorTab"))
@Composable
fun LocationsTab(
    user: User,
    premisesState: UiState<List<PremisesDto>>,
    presenceState: UiState<LocationTodayPresenceResponseDto>,
    staffState: UiState<List<StaffDto>>,
    selectedPremisesId: String?,
    isSubmitting: Boolean,
    onSelectPremises: (String) -> Unit,
    onRefresh: () -> Unit,
    onResolveGpsException: (exceptionId: String, status: String, reason: String) -> Unit = { _, _, _ -> },
    onCancelOvertime: (String) -> Unit = {}
) {
    LiveMonitorTab(
        user = user,
        premisesState = premisesState,
        presenceState = presenceState,
        staffState = staffState,
        selectedPremisesId = selectedPremisesId,
        isSubmitting = isSubmitting,
        onSelectPremises = onSelectPremises,
        onRefresh = onRefresh,
        onResolveGpsException = onResolveGpsException,
        onCancelOvertime = onCancelOvertime
    )
}
