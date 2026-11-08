package com.als.presence.ui.dashboard.employee.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.LocationStaffCardDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.domain.usecase.location.GetGpsLocationUseCase
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.NextSessionUi
import com.als.presence.ui.dashboard.common.SessionKind
import com.als.presence.ui.dashboard.common.errorText
import com.als.presence.ui.dashboard.common.nextSessionFor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Employee "Clock-In" tab state management.
 */
@HiltViewModel
class EmployeeClockInViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val getGpsLocation: GetGpsLocationUseCase,
    val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    data class ClockInUiState(
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val activeEvent: AttendanceRecordDto? = null,
        val todayRecords: List<AttendanceRecordDto> = emptyList(),
        val openOvertime: List<OvertimeDto> = emptyList(),
        val upcomingOvertime: List<OvertimeDto> = emptyList(),
        val nextSession: NextSessionUi? = null,
        val assignedShifts: List<ShiftAssignmentDto> = emptyList(),
        val shiftTemplates: List<ShiftDto> = emptyList(),
        val holidays: List<HolidayDto> = emptyList(),
        val activeStaffCard: LocationStaffCardDto? = null,
        val location: GpsLocation? = null,
        val error: String? = null,
        val message: String? = null,
        val faceEnrolled: Boolean = false,
        val faceModelVersion: String? = null,
        val pendingEmbedding: List<Double>? = null,
        val pendingEmbeddingModelVersion: String? = null,
        val pendingQuality: Double? = null,
        val premisesName: String? = null,
        val premisesAddress: String? = null,
        val premisesLatitude: Double? = null,
        val premisesLongitude: Double? = null,
        val premisesRadiusMeters: Int = 50,
        val distanceToPremisesMeters: Double? = null,
        val isWithinGeofence: Boolean? = null,
        val lastGpsVerifiedTime: String? = null
    )

    private val _state = MutableStateFlow(ClockInUiState())
    val state: StateFlow<ClockInUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun setFaceEmbedding(embedding: FaceEmbeddingEngine.Embedding) {
        _state.update {
            it.copy(
                pendingEmbedding = embedding.vector.toList(),
                pendingEmbeddingModelVersion = embedding.modelVersion,
                pendingQuality = embedding.qualityScore
            )
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.ATTENDANCE)
            _state.update { it.copy(isLoading = if (silent) it.isLoading else true, error = null) }

            val today = repository.getAttendanceRecords(
                from = Dates.todayIso(),
                to = Dates.todayIso()
            )
            val overtime = repository.getOvertime(
                from = Dates.isoDaysAgo(1),
                to = Dates.isoDaysAhead(14)
            )
            val presence = repository.getTodayLocationPresence()
            val assignmentsRes = repository.getShiftAssignments()
            val templatesRes = repository.getShifts()
            val holidaysRes = repository.getHolidays()

            val records = (today as? Resource.Success)?.data ?: emptyList()
            val overtimeEntries = (overtime as? Resource.Success)?.data ?: emptyList()
            val assignments = (assignmentsRes as? Resource.Success)?.data ?: emptyList()
            val templates = (templatesRes as? Resource.Success)?.data ?: emptyList()
            val holidays = (holidaysRes as? Resource.Success)?.data ?: emptyList()
            val presenceData = (presence as? Resource.Success)?.data

            val userId = sessionManager.userId
            val presenceNextSession = presenceData?.nextSessionFor(userId)
            val myStaffCard = presenceData?.staffList?.firstOrNull {
                it.userId == userId || it.staffProfileId == userId
            }

            val myEmployeeIds = setOfNotNull(
                userId,
                myStaffCard?.staffProfileId,
                myStaffCard?.employeeId,
                myStaffCard?.userId
            ).filter { it.isNotBlank() }

            val myOvertime = overtimeEntries
                .filter { entry ->
                    (entry.status == "APPROVED" || entry.status == "IN_PROGRESS") &&
                    (myEmployeeIds.isEmpty() || myEmployeeIds.contains(entry.staffProfileId))
                }
                .sortedBy { "${it.workDate}T${it.startTime}" }

            val myAssignments = assignments
                .filter { a -> myEmployeeIds.isEmpty() || myEmployeeIds.contains(a.employeeId) || myEmployeeIds.contains(a.staffId) }
                .distinctBy { it.shiftId.ifBlank { it.id } }

            val todayIso = Dates.todayIso()
            val todayHoliday = holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
            val isTodayHoliday = todayHoliday != null

            val hasMatchingShiftToday = myAssignments.any { a ->
                val template = templates.firstOrNull { it.id == a.shiftId }
                template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
            }
            val isTodayOffDay = !isTodayHoliday && !hasMatchingShiftToday
            val isNonWorkingDay = isTodayHoliday || isTodayOffDay
            val todayOvertime = myOvertime.firstOrNull { Dates.isToday(it.workDate) && (it.status == "APPROVED" || it.status == "IN_PROGRESS") }

            val finalNextSession = when {
                todayOvertime != null -> NextSessionUi(
                    kind = SessionKind.OVERTIME,
                    badge = if (Dates.isWithinTimeWindow(todayOvertime.startTime, todayOvertime.endTime)) "RUNNING OVERTIME" else "UPCOMING OVERTIME",
                    title = todayOvertime.reason?.takeIf { it.isNotBlank() } ?: "Overtime",
                    window = Dates.formatWindow(todayOvertime.startTime, todayOvertime.endTime),
                    day = Dates.formatDate(todayOvertime.workDate),
                    isActive = Dates.isWithinTimeWindow(todayOvertime.startTime, todayOvertime.endTime),
                    startTime = todayOvertime.startTime,
                    endTime = todayOvertime.endTime,
                    overtimeId = todayOvertime.id,
                    premisesLatitude = presenceData?.latitude,
                    premisesLongitude = presenceData?.longitude,
                    radiusMeters = presenceData?.radiusMeters ?: 50
                )
                isNonWorkingDay -> null
                else -> presenceNextSession ?: buildFallbackNextSession(
                    overtimeEntries = myOvertime,
                    assignments = myAssignments,
                    templates = templates,
                    presenceData = presenceData,
                    userId = userId
                )
            }

            val failure = when {
                today is Resource.Error -> today.errorText()
                overtime is Resource.Error -> overtime.errorText()
                else -> null
            }

            val faceStatus = repository.getFaceStatus()
            val faceEnrolled = (faceStatus as? Resource.Success)?.data?.enrolled == true
            val faceModelVersion = (faceStatus as? Resource.Success)?.data?.modelVersion

            val premLat = presenceData?.latitude
            val premLng = presenceData?.longitude
            val premRadius = presenceData?.radiusMeters ?: 50

            var currentLocation = _state.value.location
            var distance: Double? = _state.value.distanceToPremisesMeters
            var isWithin: Boolean? = _state.value.isWithinGeofence

            if (currentLocation == null) {
                when (val locRes = getGpsLocation()) {
                    is Resource.Success -> {
                        currentLocation = locRes.data
                    }
                    else -> {}
                }
            }

            if (currentLocation != null && premLat != null && premLng != null) {
                distance = calculateDistanceMeters(
                    currentLocation.latitude, currentLocation.longitude,
                    premLat, premLng
                )
                isWithin = distance <= premRadius
            } else {
                distance = null
                isWithin = null
            }

            val verifiedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

            _state.update {
                it.copy(
                    isLoading = false,
                    todayRecords = records,
                    activeEvent = records.firstOrNull { record -> !record.clockIn.isNullOrBlank() && record.clockOut == null },
                    openOvertime = myOvertime,
                    upcomingOvertime = myOvertime,
                    nextSession = finalNextSession,
                    assignedShifts = myAssignments,
                    shiftTemplates = templates,
                    holidays = holidays,
                    activeStaffCard = myStaffCard,
                    location = currentLocation,
                    error = failure,
                    faceEnrolled = faceEnrolled,
                    faceModelVersion = faceModelVersion,
                    premisesName = presenceData?.premisesName ?: "Main Office",
                    premisesAddress = presenceData?.address ?: "Your assigned work location",
                    premisesLatitude = premLat,
                    premisesLongitude = premLng,
                    premisesRadiusMeters = premRadius,
                    distanceToPremisesMeters = distance,
                    isWithinGeofence = isWithin,
                    lastGpsVerifiedTime = verifiedTime
                )
            }
        }
    }

    private fun buildFallbackNextSession(
        overtimeEntries: List<OvertimeDto>,
        assignments: List<ShiftAssignmentDto>,
        templates: List<ShiftDto>,
        presenceData: LocationTodayPresenceResponseDto?,
        userId: String?
    ): NextSessionUi? {
        val todayIso = Dates.todayIso()

        val todayOvertime = overtimeEntries.firstOrNull {
            Dates.isToday(it.workDate) && (it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (todayOvertime != null) {
            val isWithinWindow = Dates.isWithinTimeWindow(todayOvertime.startTime, todayOvertime.endTime)
            val otTitle = todayOvertime.reason?.takeIf { it.isNotBlank() } ?: "Overtime"
            return NextSessionUi(
                kind = SessionKind.OVERTIME,
                badge = if (isWithinWindow) "RUNNING OVERTIME" else "UPCOMING OVERTIME",
                title = otTitle,
                window = Dates.formatWindow(todayOvertime.startTime, todayOvertime.endTime),
                day = Dates.formatDate(todayOvertime.workDate),
                isActive = isWithinWindow,
                startTime = todayOvertime.startTime,
                endTime = todayOvertime.endTime,
                overtimeId = todayOvertime.id,
                premisesLatitude = presenceData?.latitude,
                premisesLongitude = presenceData?.longitude,
                radiusMeters = presenceData?.radiusMeters ?: 50
            )
        }

        val todayAssignment = assignments.firstOrNull { a ->
            val template = templates.firstOrNull { it.id == a.shiftId }
            template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
        }

        if (todayAssignment != null) {
            val template = templates.firstOrNull { it.id == todayAssignment.shiftId }
            val shiftName = todayAssignment.shiftName ?: template?.shiftName ?: "Assigned Shift"
            val startTime = template?.startTime?.ifBlank { null } ?: "09:00:00"
            val endTime = template?.endTime?.ifBlank { null } ?: "18:00:00"
            val dayName = template?.dayName ?: "Scheduled Shift"

            val isWithinWindow = Dates.isWithinTimeWindow(startTime, endTime)

            return NextSessionUi(
                kind = SessionKind.SHIFT,
                badge = if (isWithinWindow) "RUNNING SHIFT" else "UPCOMING SHIFT",
                title = shiftName,
                window = Dates.formatWindow(startTime, endTime),
                day = dayName,
                isActive = isWithinWindow,
                startTime = startTime,
                endTime = endTime,
                halfDayThreshold = template?.halfDayThreshold,
                premisesLatitude = presenceData?.latitude,
                premisesLongitude = presenceData?.longitude,
                radiusMeters = presenceData?.radiusMeters ?: 50
            )
        }

        val futureOvertime = overtimeEntries.firstOrNull { entry ->
            entry.workDate >= todayIso && (entry.status == "APPROVED" || entry.status == "IN_PROGRESS")
        }
        if (futureOvertime != null) {
            val isSameDay = Dates.isToday(futureOvertime.workDate)
            val isWithinWindow = Dates.isWithinTimeWindow(futureOvertime.startTime, futureOvertime.endTime)
            val isRunning = isSameDay && isWithinWindow
            val otTitle = futureOvertime.reason?.takeIf { it.isNotBlank() } ?: "Overtime"
            return NextSessionUi(
                kind = SessionKind.OVERTIME,
                badge = if (isRunning) "RUNNING OVERTIME" else "UPCOMING OVERTIME",
                title = "$otTitle (${futureOvertime.workDate})",
                window = Dates.formatWindow(futureOvertime.startTime, futureOvertime.endTime),
                day = Dates.formatDate(futureOvertime.workDate),
                isActive = isRunning,
                startTime = futureOvertime.startTime,
                endTime = futureOvertime.endTime,
                overtimeId = futureOvertime.id,
                premisesLatitude = presenceData?.latitude,
                premisesLongitude = presenceData?.longitude,
                radiusMeters = presenceData?.radiusMeters ?: 50
            )
        }

        return null
    }

    fun locate() {
        viewModelScope.launch {
            when (val result = getGpsLocation()) {
                is Resource.Success -> {
                    val loc = result.data
                    val premLat = _state.value.premisesLatitude
                    val premLng = _state.value.premisesLongitude
                    val premRadius = _state.value.premisesRadiusMeters
                    val verifiedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

                    if (premLat != null && premLng != null) {
                        val dist = calculateDistanceMeters(loc.latitude, loc.longitude, premLat, premLng)
                        _state.update {
                            it.copy(
                                location = loc,
                                distanceToPremisesMeters = dist,
                                isWithinGeofence = dist <= premRadius,
                                lastGpsVerifiedTime = verifiedTime,
                                error = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                location = loc,
                                distanceToPremisesMeters = null,
                                isWithinGeofence = null,
                                lastGpsVerifiedTime = verifiedTime,
                                error = null
                            )
                        }
                    }
                }
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
                                        "Clocked in for overtime successfully."
                                    } else {
                                        "Clocked in successfully."
                                    }
                                )
                            }
                            refresh(silent = true)
                        }
                        is Resource.Error -> {
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    error = result.message ?: "Clock-in failed"
                                )
                            }
                        }
                        Resource.Loading -> Unit
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun clockOut() {
        val eventId = _state.value.activeEvent?.id ?: ""
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, message = null) }
            when (val result = repository.clockOut(eventId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            message = "Clocked out successfully."
                        )
                    }
                    refresh(silent = true)
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = result.message ?: "Clock-out failed"
                        )
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
