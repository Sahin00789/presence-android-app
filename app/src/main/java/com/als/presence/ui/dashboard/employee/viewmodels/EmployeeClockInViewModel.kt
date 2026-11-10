package com.als.presence.ui.dashboard.employee.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.ClaimUnhandledEventRequestDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.LocationStaffCardDto
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftDto
import com.als.presence.data.model.UnhandledEventItemDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.domain.usecase.location.GetGpsLocationUseCase
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.NextSessionUi
import com.als.presence.ui.dashboard.common.SessionKind
import com.als.presence.ui.dashboard.common.errorText
import com.als.presence.ui.dashboard.common.nextSessionFor
import com.als.presence.ui.dashboard.common.toNextSession
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

import com.als.presence.data.model.UnworkedPaymentRuleDto

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
        val unworkedRules: List<UnworkedPaymentRuleDto> = emptyList(),
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
        val lastGpsVerifiedTime: String? = null,
        val unhandledEvents: List<UnhandledEventItemDto> = emptyList(),
        val isClaimingEvent: Boolean = false
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
            val employeesRes = repository.getEmployees()
            val unhandledRes = repository.getUnhandledEvents()

            val records = (today as? Resource.Success)?.data ?: emptyList()
            val overtimeEntries = (overtime as? Resource.Success)?.data ?: emptyList()
            val assignments = (assignmentsRes as? Resource.Success)?.data ?: emptyList()
            val templates = (templatesRes as? Resource.Success)?.data ?: emptyList()
            val holidays = (holidaysRes as? Resource.Success)?.data ?: emptyList()
            val presenceData = (presence as? Resource.Success)?.data
            val employeesList = (employeesRes as? Resource.Success)?.data ?: emptyList()
            val unhandledEventsList = (unhandledRes as? Resource.Success)?.data?.events ?: emptyList()

            val userId = sessionManager.userId
            val myStaffDto = employeesList.firstOrNull {
                it.userId == userId || it.id == userId
            } ?: employeesList.firstOrNull()

            val myStaffCard = presenceData?.staffList?.firstOrNull {
                it.userId == userId ||
                it.staffProfileId == userId ||
                it.employeeId == userId ||
                (myStaffDto != null && (it.staffProfileId == myStaffDto.id || it.employeeId == myStaffDto.id || it.userId == myStaffDto.userId))
            } ?: if (presenceData?.staffList?.size == 1) presenceData.staffList.first() else null

            val myEmployeeIds = setOfNotNull(
                userId,
                myStaffDto?.id,
                myStaffDto?.employeeId,
                myStaffDto?.userId,
                myStaffCard?.staffProfileId,
                myStaffCard?.employeeId,
                myStaffCard?.userId
            ).filter { it.isNotBlank() }.toSet()

            val presenceNextSession = presenceData?.nextSessionFor(userId) ?: myStaffCard?.toNextSession(
                premisesLatitude = presenceData?.latitude,
                premisesLongitude = presenceData?.longitude,
                radiusMeters = presenceData?.radiusMeters ?: 50
            )

            val myOvertime = overtimeEntries
                .filter { entry ->
                    (entry.status in listOf("CREATED", "IN_PROGRESS", "APPROVED", "REQUESTED")) &&
                    (myEmployeeIds.isEmpty() || myEmployeeIds.contains(entry.staffProfileId))
                }
                .sortedBy { "${it.workDate}T${it.startTime}" }
                .ifEmpty {
                    overtimeEntries.filter { entry ->
                        entry.status in listOf("CREATED", "IN_PROGRESS", "APPROVED", "REQUESTED")
                    }.sortedBy { "${it.workDate}T${it.startTime}" }
                }

            val myAssignments = assignments
                .filter { a ->
                    myEmployeeIds.isEmpty() ||
                    myEmployeeIds.contains(a.employeeId) ||
                    myEmployeeIds.contains(a.staffId) ||
                    (myStaffDto != null && (a.employeeId == myStaffDto.id || a.staffId == myStaffDto.id))
                }
                .distinctBy { it.shiftId.ifBlank { it.id } }
                .ifEmpty { assignments.distinctBy { it.shiftId.ifBlank { it.id } } }

            val todayIso = Dates.todayIso()
            val todayHoliday = holidays.firstOrNull { it.holidayDate == todayIso || it.date == todayIso }
            val isTodayHoliday = todayHoliday != null

            val hasMatchingShiftToday = myAssignments.any { a ->
                val template = templates.firstOrNull { it.id == a.shiftId }
                template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
            } || (myAssignments.isEmpty() && templates.any { t ->
                Dates.isShiftMatchingDay(t.dayName, todayIso, t.daysOfWeek)
            }) || (presenceNextSession?.kind == SessionKind.SHIFT && (presenceNextSession.day.equals(Dates.getDayNameByDate(todayIso), ignoreCase = true) || presenceNextSession.day.equals("Scheduled Shift", ignoreCase = true)))
            val isTodayOffDay = !isTodayHoliday && !hasMatchingShiftToday
            val isNonWorkingDay = isTodayHoliday || isTodayOffDay
            val activeRecord = records.firstOrNull { !it.clockIn.isNullOrBlank() && it.clockOut == null }

            val finalNextSession = if (activeRecord != null) {
                if (activeRecord.overtimeId != null) {
                    val ot = myOvertime.firstOrNull { it.id == activeRecord.overtimeId }
                    val otTitle = ot?.reason?.takeIf { it.isNotBlank() } ?: "Overtime"
                    NextSessionUi(
                        kind = SessionKind.OVERTIME,
                        badge = "ON OVERTIME",
                        title = otTitle,
                        window = Dates.formatWindow(activeRecord.assignedStartTime ?: ot?.startTime, activeRecord.assignedEndTime ?: ot?.endTime),
                        day = Dates.getDayNameByDate(activeRecord.workDate),
                        isActive = true,
                        startTime = activeRecord.assignedStartTime ?: ot?.startTime,
                        endTime = activeRecord.assignedEndTime ?: ot?.endTime,
                        overtimeId = activeRecord.overtimeId,
                        premisesLatitude = presenceData?.latitude,
                        premisesLongitude = presenceData?.longitude,
                        radiusMeters = presenceData?.radiusMeters ?: 50,
                        workDate = activeRecord.workDate ?: todayIso
                    )
                } else {
                    NextSessionUi(
                        kind = SessionKind.SHIFT,
                        badge = "ON SHIFT",
                        title = activeRecord.shiftName ?: "Assigned Shift",
                        window = Dates.formatWindow(activeRecord.assignedStartTime, activeRecord.assignedEndTime),
                        day = Dates.getDayNameByDate(activeRecord.workDate),
                        isActive = true,
                        startTime = activeRecord.assignedStartTime,
                        endTime = activeRecord.assignedEndTime,
                        premisesLatitude = presenceData?.latitude,
                        premisesLongitude = presenceData?.longitude,
                        radiusMeters = presenceData?.radiusMeters ?: 50,
                        workDate = activeRecord.workDate ?: todayIso
                    )
                }
            } else if (presenceNextSession != null && (presenceNextSession.isActive || !Dates.isSessionOver(presenceNextSession.workDate ?: todayIso, presenceNextSession.endTime, presenceNextSession.startTime))) {
                presenceNextSession
            } else {
                buildFallbackNextSession(
                    records = records,
                    overtimeEntries = myOvertime,
                    assignments = myAssignments,
                    templates = templates,
                    holidays = holidays,
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

            val unworkedRulesRes = repository.getUnworkedRules()
            val unworkedRulesList = (unworkedRulesRes as? Resource.Success)?.data ?: emptyList()

            val verifiedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

            _state.update {
                it.copy(
                    isLoading = false,
                    todayRecords = records,
                    activeEvent = records.firstOrNull { record -> !record.clockIn.isNullOrBlank() && record.clockOut == null },
                    openOvertime = myOvertime.filter { Dates.isToday(it.workDate) },
                    upcomingOvertime = myOvertime.filter { it.workDate > todayIso },
                    nextSession = finalNextSession,
                    assignedShifts = myAssignments,
                    shiftTemplates = templates,
                    holidays = holidays,
                    unworkedRules = unworkedRulesList,
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
                    lastGpsVerifiedTime = verifiedTime,
                    unhandledEvents = unhandledEventsList
                )
            }
        }
    }

    fun claimUnhandledEvent(item: UnhandledEventItemDto) {
        viewModelScope.launch {
            _state.update { it.copy(isClaimingEvent = true) }
            val res = repository.claimUnhandledEvent(
                ClaimUnhandledEventRequestDto(
                    eventId = item.id,
                    type = item.type,
                    date = item.date,
                    metadata = item.metadata
                )
            )
            when (res) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isClaimingEvent = false,
                            message = res.data?.message ?: "Claim completed successfully",
                            unhandledEvents = it.unhandledEvents.filterNot { ev -> ev.id == item.id }
                        )
                    }
                    refresh(silent = true)
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isClaimingEvent = false,
                            error = res.message ?: "Failed to process claim"
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isClaimingEvent = false) }
                }
            }
        }
    }

    private fun buildFallbackNextSession(
        records: List<AttendanceRecordDto>,
        overtimeEntries: List<OvertimeDto>,
        assignments: List<ShiftAssignmentDto>,
        templates: List<ShiftDto>,
        holidays: List<HolidayDto>,
        presenceData: LocationTodayPresenceResponseDto?,
        userId: String?
    ): NextSessionUi? {
        val todayIso = Dates.todayIso()

        // 1. Check if there are any remaining eligible sessions for TODAY (not completed, time range not over)
        val validTodayOvertime = overtimeEntries.firstOrNull { ot ->
            Dates.isToday(ot.workDate) &&
                (ot.status in listOf("APPROVED", "IN_PROGRESS", "CREATED", "REQUESTED")) &&
                records.none { r -> r.overtimeId == ot.id && !r.clockOut.isNullOrBlank() } &&
                !Dates.isSessionOver(ot.workDate, ot.endTime, ot.startTime)
        }

        val shiftCompletedToday = records.any { it.overtimeId == null && !it.clockIn.isNullOrBlank() && !it.clockOut.isNullOrBlank() }
        val isTodayHoliday = holidays.any { it.holidayDate == todayIso || it.date == todayIso }

        val todayAssignment = if (!shiftCompletedToday && !isTodayHoliday) {
            assignments.firstOrNull { a ->
                val template = templates.firstOrNull { it.id == a.shiftId }
                template != null && Dates.isShiftMatchingDay(template.dayName, todayIso, template.daysOfWeek)
            } ?: if (assignments.isEmpty()) {
                templates.firstOrNull { Dates.isShiftMatchingDay(it.dayName, todayIso, it.daysOfWeek) }
            } else null
        } else null

        val todayTemplate = when (todayAssignment) {
            is ShiftAssignmentDto -> templates.firstOrNull { it.id == todayAssignment.shiftId }
            is ShiftDto -> todayAssignment
            else -> null
        }

        val validTodayShift = if (todayTemplate != null && !Dates.isSessionOver(todayIso, todayTemplate.endTime, todayTemplate.startTime)) {
            todayAssignment to todayTemplate
        } else null

        if (validTodayOvertime != null && validTodayShift != null) {
            val otStart = validTodayOvertime.startTime ?: "99:99"
            val shiftStart = validTodayShift.second.startTime.ifBlank { "09:00:00" }
            if (otStart < shiftStart) {
                return sessionFromOvertime(validTodayOvertime, presenceData)
            } else {
                return sessionFromShift(validTodayShift.first, validTodayShift.second, todayIso, presenceData)
            }
        } else if (validTodayOvertime != null) {
            return sessionFromOvertime(validTodayOvertime, presenceData)
        } else if (validTodayShift != null) {
            return sessionFromShift(validTodayShift.first, validTodayShift.second, todayIso, presenceData)
        }

        // 2. Both shift & overtime on today are over or completed. Look ahead to future days (up to 14 days)
        for (dayOffset in 1..14) {
            val futureIso = Dates.isoDaysAhead(dayOffset.toLong())

            val futureOvertime = overtimeEntries.firstOrNull { ot ->
                ot.workDate == futureIso &&
                    (ot.status in listOf("APPROVED", "IN_PROGRESS", "CREATED", "REQUESTED"))
            }

            val isFutureHoliday = holidays.any { it.holidayDate == futureIso || it.date == futureIso }
            val futureAssignment = if (!isFutureHoliday) {
                assignments.firstOrNull { a ->
                    val template = templates.firstOrNull { it.id == a.shiftId }
                    template != null && Dates.isShiftMatchingDay(template.dayName, futureIso, template.daysOfWeek)
                } ?: if (assignments.isEmpty()) {
                    templates.firstOrNull { Dates.isShiftMatchingDay(it.dayName, futureIso, it.daysOfWeek) }
                } else null
            } else null

            val futureTemplate = when (futureAssignment) {
                is ShiftAssignmentDto -> templates.firstOrNull { it.id == futureAssignment.shiftId }
                is ShiftDto -> futureAssignment
                else -> null
            }

            if (futureOvertime != null && futureTemplate != null) {
                val otStart = futureOvertime.startTime ?: "99:99"
                val shiftStart = futureTemplate.startTime.ifBlank { "09:00:00" }
                if (otStart < shiftStart) {
                    return sessionFromOvertime(futureOvertime, presenceData)
                } else {
                    return sessionFromShift(futureAssignment, futureTemplate, futureIso, presenceData)
                }
            } else if (futureOvertime != null) {
                return sessionFromOvertime(futureOvertime, presenceData)
            } else if (futureTemplate != null) {
                return sessionFromShift(futureAssignment, futureTemplate, futureIso, presenceData)
            }
        }

        // 3. Fallback if no matching assignment within 14 days
        val fallbackAssignment = assignments.firstOrNull()
        val fallbackTemplate = templates.firstOrNull { it.id == fallbackAssignment?.shiftId } ?: templates.firstOrNull()
        if (fallbackTemplate != null) {
            val tomorrowIso = Dates.isoDaysAhead(1)
            return sessionFromShift(fallbackAssignment, fallbackTemplate, tomorrowIso, presenceData)
        }

        return null
    }

    private fun sessionFromOvertime(
        ot: OvertimeDto,
        presenceData: LocationTodayPresenceResponseDto?
    ): NextSessionUi {
        val isSameDay = Dates.isToday(ot.workDate)
        val isWithinWindow = isSameDay && Dates.isWithinTimeWindow(ot.startTime, ot.endTime)
        val otTitle = ot.reason?.takeIf { it.isNotBlank() } ?: "Overtime"
        return NextSessionUi(
            kind = SessionKind.OVERTIME,
            badge = if (isWithinWindow) "RUNNING OVERTIME" else "UPCOMING OVERTIME",
            title = if (isSameDay) otTitle else "$otTitle (${Dates.formatDate(ot.workDate)})",
            window = Dates.formatWindow(ot.startTime, ot.endTime),
            day = Dates.getDayNameByDate(ot.workDate),
            isActive = isWithinWindow,
            startTime = ot.startTime,
            endTime = ot.endTime,
            overtimeId = ot.id,
            premisesLatitude = presenceData?.latitude,
            premisesLongitude = presenceData?.longitude,
            radiusMeters = presenceData?.radiusMeters ?: 50,
            workDate = ot.workDate
        )
    }

    private fun sessionFromShift(
        assignment: Any?,
        template: ShiftDto,
        workDateIso: String,
        presenceData: LocationTodayPresenceResponseDto?
    ): NextSessionUi {
        val shiftName = (assignment as? ShiftAssignmentDto)?.shiftName
            ?: template.shiftName.ifBlank { "Assigned Shift" }
        val startTime = template.startTime.ifBlank { "09:00:00" }
        val endTime = template.endTime.ifBlank { "18:00:00" }
        val dayName = template.dayName.ifBlank { Dates.getDayNameByDate(workDateIso) }

        val isSameDay = Dates.isToday(workDateIso)
        val isWithinWindow = isSameDay && Dates.isWithinTimeWindow(startTime, endTime)

        return NextSessionUi(
            kind = SessionKind.SHIFT,
            badge = if (isWithinWindow) "RUNNING SHIFT" else "UPCOMING SHIFT",
            title = if (isSameDay) shiftName else "$shiftName (${Dates.formatDate(workDateIso)})",
            window = Dates.formatWindow(startTime, endTime),
            day = dayName,
            isActive = isWithinWindow,
            startTime = startTime,
            endTime = endTime,
            halfDayThreshold = template.halfDayThreshold,
            premisesLatitude = presenceData?.latitude,
            premisesLongitude = presenceData?.longitude,
            radiusMeters = presenceData?.radiusMeters ?: 50,
            workDate = workDateIso
        )
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
