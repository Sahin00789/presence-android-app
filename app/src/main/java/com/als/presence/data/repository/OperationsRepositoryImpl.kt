package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.ApiErrorDto
import com.als.presence.data.model.AttendanceEventDto
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.CorrectDailyAttendanceRequest
import com.als.presence.data.model.DailyAttendanceDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.ClockOutRequest
import com.als.presence.data.model.CreateHolidayRequest
import com.als.presence.data.model.CreateInvitationRequest
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.CreatePayrollPeriodRequest
import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.CreateShiftTemplateRequest
import com.als.presence.data.model.UpdateShiftTemplateRequest
import com.als.presence.data.model.EnrollFaceRequest
import com.als.presence.data.model.EnrollMultiPoseRequest
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.InvitationDto
import com.als.presence.data.model.InviteStaffRequest
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.LeaveReviewRequest
import com.als.presence.data.model.LocationCheckRequest
import com.als.presence.data.model.LocationCheckResultDto
import com.als.presence.data.remote.ActiveLocationSessionDto
import com.als.presence.data.model.ManualClockInRequest
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.RecordPaymentRequest
import com.als.presence.data.model.ResolveExceptionRequest
import com.als.presence.data.model.ResolveExceptionResponse
import com.als.presence.data.model.SalaryStructureDto
import com.als.presence.data.model.ScheduleEntryDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UpsertSalaryRequest
import com.als.presence.data.model.WalletSummaryDto
import com.als.presence.data.model.WalletTransactionDto
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.data.remote.FaceApiService
import com.als.presence.data.remote.InvitationApiService
import com.als.presence.data.remote.LeaveApiService
import com.als.presence.data.remote.LocationApiService
import com.als.presence.data.remote.OvertimeApiService
import com.als.presence.data.remote.OrganizationApiService
import com.als.presence.data.remote.PayrollApiService
import com.als.presence.data.remote.PremisesApiService
import com.als.presence.data.remote.ShiftApiService
import com.als.presence.data.remote.StaffApiService
import com.als.presence.data.remote.WalletApiService
import com.als.presence.domain.repository.OperationsRepository
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit-backed implementation of [OperationsRepository].
 *
 * Every call is funnelled through [apiCall] so the UI only ever receives a
 * [Resource] and never an HTTP exception. Error bodies are decoded into the
 * backend's `ApiErrorDto` so the user sees the real server message.
 */
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.AssignStaffPremisesRequest
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.OrganizationDto
import com.als.presence.data.model.OrganizationSummaryDto
import com.als.presence.data.model.UpdateOrganizationRequest
import com.als.presence.data.model.UserDto
import kotlinx.coroutines.flow.firstOrNull

import com.als.presence.data.local.attendance.AttendanceDao
import com.als.presence.data.local.attendance.AttendanceEntity
import com.als.presence.data.local.premises.PremisesDao
import com.als.presence.data.local.premises.PremisesEntity
import com.als.presence.data.local.shift.ShiftDao
import com.als.presence.data.local.shift.ShiftTemplateEntity
import com.als.presence.data.local.staff.StaffDao
import com.als.presence.data.local.staff.StaffEntity
import com.als.presence.data.local.wallet.WalletDao
import com.als.presence.data.local.wallet.WalletEntity

@Singleton
class OperationsRepositoryImpl @Inject constructor(
    private val staffApi: StaffApiService,
    private val invitationApi: InvitationApiService,
    private val premisesApi: PremisesApiService,
    private val organizationApi: OrganizationApiService,
    private val attendanceApi: AttendanceApiService,
    private val overtimeApi: OvertimeApiService,
    private val walletApi: WalletApiService,
    private val payrollApi: PayrollApiService,
    private val shiftApi: ShiftApiService,
    private val faceApi: FaceApiService,
    private val leaveApi: LeaveApiService,
    private val locationApi: LocationApiService,
    private val json: Json,
    private val sessionManager: SessionManager,
    private val walletDao: WalletDao,
    private val attendanceDao: AttendanceDao,
    private val shiftDao: ShiftDao,
    private val premisesDao: PremisesDao,
    private val staffDao: StaffDao
) : OperationsRepository {

    // Organization ----------------------------------------------------------

    override suspend fun getOrganizationSummary(): Resource<OrganizationSummaryDto> =
        apiCall { organizationApi.getOrganizationSummary() }

    override suspend fun updateOrganization(
        name: String?,
        timezone: String?,
        currency: String?,
        logoBase64: String?
    ): Resource<OrganizationDto> {
        val result = apiCall {
            organizationApi.updateOrganization(
                UpdateOrganizationRequest(
                    name = name,
                    timezone = timezone,
                    currency = currency,
                    logo = logoBase64
                )
            )
        }
        if (result is Resource.Success) {
            val currentUser = sessionManager.userFlow.firstOrNull()
            if (currentUser != null) {
                sessionManager.updateUser(
                    UserDto(
                        id = currentUser.id,
                        email = currentUser.email,
                        fullName = currentUser.fullName,
                        role = currentUser.role.name,
                        organizationId = currentUser.organizationId,
                        organizationName = result.data.name,
                        avatarUrl = currentUser.avatarUrl,
                        organizationLogoUrl = result.data.logoUrl ?: currentUser.organizationLogoUrl
                    )
                )
            }
        }
        return result
    }

    // Workforce -------------------------------------------------------------

    override suspend fun getStaff(): Resource<List<StaffDto>> {
        val result = apiCall { staffApi.list() }
        if (result is Resource.Success) {
            staffDao.insertAll(result.data.map { dto ->
                StaffEntity(
                    id = dto.id,
                    userId = dto.userId,
                    email = dto.email,
                    fullName = dto.fullName,
                    staffCode = dto.staffCode,
                    department = dto.department,
                    designation = dto.designation,
                    premisesId = dto.premisesId,
                    premisesName = dto.premisesName,
                    hireDate = dto.hireDate,
                    isActive = dto.isActive,
                    compensationType = dto.compensationType,
                    baseRateCents = dto.baseRateCents,
                    overtimeRateCents = dto.overtimeRateCents,
                    earnedBalanceCents = dto.earnedBalanceCents,
                    approvedUnpaidBalanceCents = dto.approvedUnpaidBalanceCents,
                    paidBalanceCents = dto.paidBalanceCents,
                    totalBalanceCents = dto.totalBalanceCents
                )
            })
            return result
        }
        val cached = staffDao.observeAll().firstOrNull()
        return if (!cached.isNullOrEmpty()) {
            Resource.Success(cached.map { entity ->
                StaffDto(
                    id = entity.id,
                    userId = entity.userId,
                    email = entity.email,
                    fullName = entity.fullName,
                    staffCode = entity.staffCode,
                    department = entity.department,
                    designation = entity.designation,
                    premisesId = entity.premisesId,
                    premisesName = entity.premisesName,
                    hireDate = entity.hireDate,
                    isActive = entity.isActive,
                    compensationType = entity.compensationType,
                    baseRateCents = entity.baseRateCents,
                    overtimeRateCents = entity.overtimeRateCents,
                    earnedBalanceCents = entity.earnedBalanceCents,
                    approvedUnpaidBalanceCents = entity.approvedUnpaidBalanceCents,
                    paidBalanceCents = entity.paidBalanceCents,
                    totalBalanceCents = entity.totalBalanceCents
                )
            })
        } else {
            result
        }
    }

    override suspend fun getMembers(): Resource<List<OrganizationMemberDto>> =
        apiCall { organizationApi.members() }

    override suspend fun deactivateMember(userId: String): Resource<Unit> =
        apiCall { organizationApi.deactivateMember(userId) }

    override suspend fun inviteStaff(request: InviteStaffRequest): Resource<StaffDto> =
        apiCall { staffApi.invite(request) }

    override suspend fun createInvitation(
        request: CreateInvitationRequest
    ): Resource<InvitationDto> = apiCall { invitationApi.create(request) }

    override suspend fun getInvitations(): Resource<List<InvitationDto>> =
        apiCall { invitationApi.list() }

    override suspend fun getSalaryHistory(staffProfileId: String): Resource<List<SalaryStructureDto>> =
        apiCall { staffApi.salaryHistory(staffProfileId) }

    override suspend fun setSalary(
        staffProfileId: String,
        request: UpsertSalaryRequest
    ): Resource<SalaryStructureDto> = apiCall { staffApi.setSalary(staffProfileId, request) }

    // Premises --------------------------------------------------------------

    override suspend fun getPremises(): Resource<List<PremisesDto>> {
        val result = apiCall { premisesApi.list() }
        if (result is Resource.Success) {
            premisesDao.insertAll(result.data.map { dto ->
                PremisesEntity(
                    id = dto.id,
                    organizationId = dto.organizationId,
                    name = dto.name,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    radiusMeters = dto.radiusMeters,
                    isActive = dto.isActive
                )
            })
            return result
        }
        val cached = premisesDao.getAll()
        return if (cached.isNotEmpty()) {
            Resource.Success(cached.map { entity ->
                PremisesDto(
                    id = entity.id,
                    organizationId = entity.organizationId,
                    name = entity.name,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    radiusMeters = entity.radiusMeters,
                    isActive = entity.isActive
                )
            })
        } else {
            result
        }
    }

    override suspend fun createPremises(
        request: CreatePremisesRequest
    ): Resource<PremisesDto> = apiCall { premisesApi.create(request) }

    override suspend fun updatePremises(
        premisesId: String,
        request: UpdatePremisesRequest
    ): Resource<PremisesDto> = apiCall { premisesApi.update(premisesId, request) }

    // Attendance ------------------------------------------------------------

    override suspend fun getAttendanceRecords(
        from: String?,
        to: String?,
        staffProfileId: String?
    ): Resource<List<AttendanceRecordDto>> {
        val result = apiCall { attendanceApi.getRecords(from = from, to = to, staffProfileId = staffProfileId) }
        if (result is Resource.Success) {
            attendanceDao.insertAll(result.data.map { dto ->
                AttendanceEntity(
                    id = dto.id,
                    staffProfileId = dto.staffProfileId,
                    staffName = dto.staffName,
                    staffEmail = dto.staffEmail,
                    staffCode = dto.staffCode,
                    workDate = dto.workDate,
                    clockIn = dto.clockIn,
                    clockOut = dto.clockOut,
                    status = dto.status,
                    faceVerified = dto.faceVerified,
                    geofenceVerified = dto.geofenceVerified,
                    workedMinutes = dto.workedMinutes,
                    overtimeMinutes = dto.overtimeMinutes,
                    scheduledMinutes = dto.scheduledMinutes,
                    premisesId = dto.premisesId,
                    overtimeId = dto.overtimeId
                )
            })
            return result
        }
        val cachedFlow = if (staffProfileId != null) attendanceDao.observeForStaff(staffProfileId) else attendanceDao.observeAll()
        val cached = cachedFlow.firstOrNull()
        return if (!cached.isNullOrEmpty()) {
            Resource.Success(cached.map { entity ->
                AttendanceRecordDto(
                    id = entity.id,
                    staffProfileId = entity.staffProfileId,
                    staffName = entity.staffName,
                    staffEmail = entity.staffEmail,
                    staffCode = entity.staffCode,
                    workDate = entity.workDate,
                    clockIn = entity.clockIn,
                    clockOut = entity.clockOut,
                    status = entity.status,
                    faceVerified = entity.faceVerified,
                    geofenceVerified = entity.geofenceVerified,
                    workedMinutes = entity.workedMinutes,
                    overtimeMinutes = entity.overtimeMinutes,
                    scheduledMinutes = entity.scheduledMinutes,
                    premisesId = entity.premisesId,
                    overtimeId = entity.overtimeId
                )
            })
        } else {
            result
        }
    }

    override suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>> =
        apiCall { attendanceApi.getExceptions() }

    override suspend fun resolveException(
        exceptionId: String,
        request: ResolveExceptionRequest
    ): Resource<Unit> = apiCall { attendanceApi.resolveException(exceptionId, request) }.mapUnit()

    override suspend fun clockIn(request: ClockInRequest): Resource<AttendanceEventDto> =
        apiCall { attendanceApi.clockIn(request) }

    override suspend fun clockOut(attendanceEventId: String?): Resource<AttendanceEventDto> =
        apiCall { attendanceApi.clockOut(ClockOutRequest(attendanceEventId = attendanceEventId)) }

    override suspend fun clockInManual(request: ManualClockInRequest): Resource<AttendanceEventDto> =
        apiCall { attendanceApi.clockInManual(request) }.map { it.event }


    // Overtime --------------------------------------------------------------

    override suspend fun getOvertime(
        from: String?,
        to: String?,
        staffProfileId: String?
    ): Resource<List<OvertimeDto>> =
        apiCall { overtimeApi.list(from = from, to = to, staffProfileId = staffProfileId) }

    override suspend fun createOvertime(request: CreateOvertimeRequest): Resource<OvertimeDto> =
        apiCall { overtimeApi.create(request) }

    override suspend fun cancelOvertime(overtimeId: String): Resource<Unit> =
        apiCall { overtimeApi.cancel(overtimeId) }.mapUnit()

    // Wallet ----------------------------------------------------------------

    override suspend fun getWalletSummary(): Resource<WalletSummaryDto> {
        val result = apiCall { walletApi.summary() }
        if (result is Resource.Success) {
            walletDao.upsert(
                WalletEntity(
                    staffProfileId = result.data.staffProfileId,
                    earnedBalanceCents = result.data.earnedBalanceCents,
                    approvedUnpaidBalanceCents = result.data.approvedUnpaidBalanceCents,
                    paidBalanceCents = result.data.paidBalanceCents,
                    totalBalanceCents = result.data.totalBalanceCents
                )
            )
            return result
        }
        val currentUser = sessionManager.userFlow.firstOrNull()
        if (currentUser != null) {
            val cached = walletDao.get(currentUser.id)
            if (cached != null) {
                return Resource.Success(
                    WalletSummaryDto(
                        staffProfileId = cached.staffProfileId,
                        earnedBalanceCents = cached.earnedBalanceCents,
                        approvedUnpaidBalanceCents = cached.approvedUnpaidBalanceCents,
                        paidBalanceCents = cached.paidBalanceCents,
                        totalBalanceCents = cached.totalBalanceCents
                    )
                )
            }
        }
        return result
    }

    override suspend fun getWalletTransactions(): Resource<List<WalletTransactionDto>> =
        apiCall { walletApi.transactions() }

    // Payroll ---------------------------------------------------------------

    override suspend fun getPayrollPeriods(): Resource<List<PayrollPeriodDto>> =
        apiCall { payrollApi.periods() }

    override suspend fun getPayrollItems(periodId: String): Resource<List<PayrollItemDto>> =
        apiCall { payrollApi.items(periodId) }

    override suspend fun getMyPayslips(): Resource<List<PayslipDto>> =
        apiCall { payrollApi.myPayslips() }

    override suspend fun createPayrollPeriod(
        startDate: String,
        endDate: String
    ): Resource<PayrollPeriodDto> = apiCall {
        payrollApi.createPeriod(CreatePayrollPeriodRequest(startDate = startDate, endDate = endDate))
    }

    override suspend fun calculatePayroll(periodId: String): Resource<Unit> =
        apiCall { payrollApi.calculate(periodId) }.mapUnit()

    override suspend fun approvePayroll(periodId: String): Resource<Unit> =
        apiCall { payrollApi.approve(periodId) }.mapUnit()

    override suspend fun submitPayrollPeriod(periodId: String): Resource<Unit> =
        apiCall { payrollApi.submitPayrollPeriod(periodId) }.mapUnit()

    override suspend fun recordPayment(
        itemId: String,
        amountCents: Long,
        paymentMethod: String,
        referenceNumber: String
    ): Resource<Unit> = apiCall {
        payrollApi.recordPayment(
            itemId,
            RecordPaymentRequest(
                amountCents = amountCents,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber
            )
        )
    }.mapUnit()

    // Shifts ----------------------------------------------------------------

    override suspend fun getShiftTemplates(): Resource<List<ShiftTemplateDto>> {
        val result = apiCall { shiftApi.templates() }
        if (result is Resource.Success) {
            shiftDao.insertTemplates(result.data.map { dto ->
                ShiftTemplateEntity(
                    id = dto.id,
                    organizationId = dto.organizationId,
                    premisesId = dto.premisesId,
                    name = dto.name,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    breakMinutes = dto.breakMinutes,
                    lateGraceMinutes = dto.lateGraceMinutes,
                    halfDayThresholdMinutes = dto.halfDayThresholdMinutes,
                    daysOfWeekJson = dto.daysOfWeek?.joinToString(","),
                    lateClockInAllowed = dto.lateClockInAllowed,
                    overnight = dto.overnight,
                    isActive = dto.isActive
                )
            })
            return result
        }
        val cached = shiftDao.observeTemplates().firstOrNull()
        return if (!cached.isNullOrEmpty()) {
            Resource.Success(cached.map { entity ->
                ShiftTemplateDto(
                    id = entity.id,
                    organizationId = entity.organizationId,
                    premisesId = entity.premisesId,
                    name = entity.name,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    breakMinutes = entity.breakMinutes,
                    lateGraceMinutes = entity.lateGraceMinutes,
                    halfDayThresholdMinutes = entity.halfDayThresholdMinutes,
                    daysOfWeek = entity.daysOfWeekJson?.split(",")?.mapNotNull { it.toIntOrNull() },
                    lateClockInAllowed = entity.lateClockInAllowed,
                    overnight = entity.overnight,
                    isActive = entity.isActive
                )
            })
        } else {
            result
        }
    }

    // Face ------------------------------------------------------------------

    override suspend fun enrollFaceSupervised(
        staffProfileId: String,
        embeddings: Map<String, List<Double>>,
        modelVersion: String,
        qualityScore: Double?,
        premisesId: String?
    ): Resource<FaceStatusDto> = apiCall {
        faceApi.enrollSupervised(
            EnrollMultiPoseRequest(
                staffProfileId = staffProfileId,
                centerVector = embeddings["center"] ?: emptyList(),
                leftVector = embeddings["left"] ?: emptyList(),
                rightVector = embeddings["right"] ?: emptyList(),
                upVector = embeddings["up"] ?: emptyList(),
                downVector = embeddings["down"] ?: emptyList(),
                qualityScore = qualityScore,
                modelVersion = modelVersion,
                premisesId = premisesId
            )
        )
    }

    override suspend fun enrollFace(
        embeddingVector: List<Double>,
        qualityScore: Double?
    ): Resource<FaceStatusDto> = apiCall {
        faceApi.enrollSupervised(
            EnrollMultiPoseRequest(
                staffProfileId = "",
                centerVector = embeddingVector,
                leftVector = emptyList(),
                rightVector = emptyList(),
                upVector = emptyList(),
                downVector = emptyList(),
                qualityScore = qualityScore
            )
        )
    }

    override suspend fun getFaceStatus(): Resource<FaceStatusDto> =
        apiCall { faceApi.status() }

    override suspend fun getStaffFaceStatus(staffProfileId: String): Resource<FaceStatusDto> =
        apiCall { faceApi.staffStatus(staffProfileId) }

    override suspend fun revokeFaceEnrollment(staffProfileId: String): Resource<Unit> =
        apiCall { faceApi.revoke(staffProfileId) }.mapUnit()

    // Location monitoring ---------------------------------------------------

    override suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long?
    ): Resource<LocationCheckResultDto> = apiCall {
        locationApi.submitCheck(
            LocationCheckRequest(
                attendanceEventId = attendanceEventId,
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                isMock = isMock,
                timestamp = capturedAt?.toString()
            )
        )
    }

    override suspend fun getActiveLocationSession(): Resource<ActiveLocationSessionDto> =
        apiCall { locationApi.activeSession() }

    override suspend fun getTodayLocationPresence(
        premisesId: String?,
        date: String?
    ): Resource<LocationTodayPresenceResponseDto> =
        apiCall { locationApi.getTodayPresence(premisesId, date) }

    override suspend fun assignStaffPremises(
        staffProfileId: String,
        premisesId: String?
    ): Resource<Unit> =
        apiCall { staffApi.assignPremises(staffProfileId, AssignStaffPremisesRequest(premisesId)) }.mapUnit()

    // Leave -----------------------------------------------------------------

    override suspend fun getLeaves(): Resource<List<LeaveDto>> =
        apiCall { leaveApi.list() }

    override suspend fun requestLeave(request: LeaveRequestCreate): Resource<LeaveDto> =
        apiCall { leaveApi.request(request) }

    override suspend fun reviewLeave(id: String, decision: String, note: String?): Resource<LeaveDto> =
        apiCall { leaveApi.review(id, LeaveReviewRequest(decision = decision, note = note)) }

    override suspend fun createShiftTemplate(
        request: CreateShiftTemplateRequest
    ): Resource<ShiftTemplateDto> = apiCall { shiftApi.createTemplate(request) }

    override suspend fun updateShiftTemplate(
        templateId: String,
        request: UpdateShiftTemplateRequest
    ): Resource<ShiftTemplateDto> = apiCall { shiftApi.updateTemplate(templateId, request) }

    override suspend fun deleteShiftTemplate(
        templateId: String
    ): Resource<Unit> = apiCall { shiftApi.deleteTemplate(templateId) }.mapUnit()

    override suspend fun getHolidays(
        from: String?,
        to: String?
    ): Resource<List<HolidayDto>> = apiCall { shiftApi.holidays(from, to) }

    override suspend fun createHoliday(
        request: CreateHolidayRequest
    ): Resource<HolidayDto> = apiCall { shiftApi.createHoliday(request) }

    override suspend fun deleteHoliday(holidayId: String): Resource<Unit> =
        apiCall { shiftApi.deleteHoliday(holidayId) }.mapUnit()

    override suspend fun getShiftAssignments(
        staffProfileId: String?
    ): Resource<List<ShiftAssignmentDto>> =
        apiCall { shiftApi.assignments(staffProfileId) }

    override suspend fun createShiftAssignment(
        request: CreateShiftAssignmentRequest
    ): Resource<ShiftAssignmentDto> = apiCall { shiftApi.createAssignment(request) }

    override suspend fun getMySchedule(
        from: String?,
        to: String?
    ): Resource<List<ScheduleEntryDto>> = apiCall { shiftApi.mySchedule(from, to) }

    override suspend fun getStaffSchedule(
        staffProfileId: String,
        from: String?,
        to: String?
    ): Resource<List<ScheduleEntryDto>> =
        apiCall { shiftApi.staffSchedule(staffProfileId, from, to) }

    // -----------------------------------------------------------------------

    private suspend fun <T> apiCall(block: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Resource.Success(body)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    Resource.Success(Unit as T)
                }
            } else {
                Resource.Error(
                    ApiCallException(response.code(), decodeError(response)),
                    decodeError(response)
                )
            }
        } catch (error: Exception) {
            Resource.Error(error, sanitizeErrorMessage(error))
        }
    }

    private fun sanitizeErrorMessage(e: Throwable): String {
        val message = e.localizedMessage ?: e.message ?: ""
        return when {
            e is ConnectException || message.contains("Failed to connect", ignoreCase = true) ->
                "Unable to connect to server. Please check your connection."
            e is UnknownHostException || message.contains("Unable to resolve host", ignoreCase = true) ->
                "Unable to reach server. Please check your network."
            e is SocketTimeoutException || message.contains("timeout", ignoreCase = true) ->
                "Server connection timed out. Retrying…"
            e is IOException || message.contains("IOException", ignoreCase = true) ->
                "Network connection error. Please try again."
            message.isNotBlank() && !message.contains("10.0.2.2") && !message.contains("http://") && !message.contains("https://") ->
                message
            else -> "Unable to connect to server"
        }
    }

    override suspend fun getDailyAttendance(
        workDate: String,
        premisesId: String?
    ): Resource<List<DailyAttendanceDto>> = apiCall {
        attendanceApi.getDailyAttendance(workDate, premisesId)
    }

    override suspend fun correctDailyAttendance(
        id: String,
        request: CorrectDailyAttendanceRequest
    ): Resource<DailyAttendanceDto> = apiCall {
        attendanceApi.correctDailyAttendance(id, request)
    }

    override suspend fun getMyEarningsHistory(
        from: String?,
        to: String?
    ): Resource<List<DailyAttendanceDto>> = apiCall {
        attendanceApi.getMyEarningsHistory(from, to)
    }

    private fun decodeError(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        if (raw.isNullOrBlank()) {
            return "Request failed with status ${response.code()}"
        }
        val parsed = runCatching { json.decodeFromString(ApiErrorDto.serializer(), raw) }.getOrNull()
        return parsed?.message
            ?: parsed?.error
            ?: "Request failed with status ${response.code()}"
    }

    private fun <T> Resource<T>.mapUnit(): Resource<Unit> = when (this) {
        is Resource.Success -> Resource.Success(Unit)
        is Resource.Error -> this
        Resource.Loading -> Resource.Loading
    }

    private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
        is Resource.Success -> Resource.Success(transform(data))
        is Resource.Error -> this
        Resource.Loading -> Resource.Loading
    }
}

/** Raised when the backend answers with a non-2xx status. */
class ApiCallException(val statusCode: Int, override val message: String) : Exception(message)
