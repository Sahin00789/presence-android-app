package com.als.presence.ui.dashboard.staff.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.location.GpsLocation
import com.als.presence.core.location.LocationMonitorScheduler
import com.als.presence.core.location.ShiftLocationService
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.OvertimeDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.domain.usecase.location.GetGpsLocationUseCase
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.errorText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Staff "Clock-In" tab state management.
 */
@HiltViewModel
class StaffClockInViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val getGpsLocation: GetGpsLocationUseCase,
    val faceEmbeddingEngine: FaceEmbeddingEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class ClockInUiState(
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val activeEvent: AttendanceRecordDto? = null,
        val todayRecords: List<AttendanceRecordDto> = emptyList(),
        val openOvertime: List<OvertimeDto> = emptyList(),
        val upcomingOvertime: List<OvertimeDto> = emptyList(),
        val location: GpsLocation? = null,
        val error: String? = null,
        val message: String? = null,
        val faceEnrolled: Boolean = false,
        val faceModelVersion: String? = null,
        val pendingEmbedding: List<Double>? = null,
        val pendingEmbeddingModelVersion: String? = null,
        val pendingQuality: Double? = null
    )

    private val _state = MutableStateFlow(ClockInUiState())
    val state: StateFlow<ClockInUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val today = repository.getAttendanceRecords(
                from = Dates.todayIso(),
                to = Dates.todayIso()
            )
            val overtime = repository.getOvertime(
                from = Dates.isoDaysAgo(7),
                to = Dates.todayIso()
            )

            val records = (today as? Resource.Success)?.data ?: emptyList()
            val overtimeEntries = (overtime as? Resource.Success)?.data ?: emptyList()

            val failure = when {
                today is Resource.Error -> today.errorText()
                overtime is Resource.Error -> overtime.errorText()
                else -> null
            }

            val faceStatus = repository.getFaceStatus()
            val faceEnrolled = (faceStatus as? Resource.Success)?.data?.enrolled == true
            val faceModelVersion = (faceStatus as? Resource.Success)?.data?.modelVersion

            _state.update {
                it.copy(
                    isLoading = false,
                    todayRecords = records,
                    activeEvent = records.firstOrNull { record -> record.clockOut == null },
                    openOvertime = overtimeEntries.filter { entry -> entry.status == "APPROVED" },
                    upcomingOvertime = overtimeEntries.filter { entry ->
                        entry.status == "APPROVED" || entry.status == "IN_PROGRESS"
                    },
                    error = failure,
                    faceEnrolled = faceEnrolled,
                    faceModelVersion = faceModelVersion
                )
            }
        }
    }

    fun locate() {
        viewModelScope.launch {
            when (val result = getGpsLocation()) {
                is Resource.Success -> _state.update { it.copy(location = result.data, error = null) }
                is Resource.Error -> _state.update { it.copy(error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun clockIn(overtimeId: String? = null) {
        val pending = _state.value.pendingEmbedding
        if (pending == null) {
            _state.update {
                it.copy(
                    error = "Capture your face to verify before clocking in."
                )
            }
            return
        }
        if (!_state.value.faceEnrolled) {
            _state.update {
                it.copy(
                    error = "Face not enrolled. Please contact your Manager/Owner to get enrolled at the premises."
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, message = null) }

            when (val location = getGpsLocation()) {
                is Resource.Error -> {
                    _state.update { it.copy(isSubmitting = false, error = location.message) }
                    return@launch
                }
                is Resource.Success -> {
                    val fix = location.data
                    _state.update { it.copy(location = fix) }

                    when (
                        val result = repository.clockIn(
                            ClockInRequest(
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyMeters = fix.accuracyMeters.toDouble(),
                                isMocked = fix.isMocked,
                                faceVerified = true,
                                overtimeId = overtimeId,
                                embeddingVector = pending
                            )
                        )
                    ) {
                        is Resource.Success -> {
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    message = if (overtimeId != null) {
                                        "Overtime check-in recorded at ${Dates.formatTime(result.data.clockIn)}."
                                    } else {
                                        "Clocked in at ${Dates.formatTime(result.data.clockIn)}."
                                    },
                                    pendingEmbedding = null,
                                    pendingEmbeddingModelVersion = null,
                                    pendingQuality = null
                                )
                            }
                            // Start ~10-minute active-shift GPS monitoring.
                            ShiftLocationService.start(context)
                            LocationMonitorScheduler.schedule(context)
                            refresh()
                        }
                        is Resource.Error -> _state.update {
                            it.copy(isSubmitting = false, error = result.message)
                        }
                        Resource.Loading -> Unit
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun clockOut() {
        val active = _state.value.activeEvent ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, message = null) }
            when (val result = repository.clockOut(active.id)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            message = "Clocked out at ${Dates.formatTime(result.data.clockOut)}."
                        )
                    }
                    ShiftLocationService.stop(context)
                    LocationMonitorScheduler.cancel(context)
                    refresh()
                }
                is Resource.Error -> _state.update {
                    it.copy(isSubmitting = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun setFaceEmbedding(embedding: FaceEmbeddingEngine.Embedding) {
        _state.update {
            it.copy(
                pendingEmbedding = embedding.vector,
                pendingEmbeddingModelVersion = embedding.modelVersion,
                pendingQuality = embedding.qualityScore,
                error = null
            )
        }
    }

    fun clearPendingFace() {
        _state.update {
            it.copy(
                pendingEmbedding = null,
                pendingEmbeddingModelVersion = null,
                pendingQuality = null
            )
        }
    }
}
