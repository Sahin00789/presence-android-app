package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.AttendanceEventDto
import com.als.presence.data.model.AttendanceExceptionDto
import com.als.presence.data.model.AttendanceRecordDto
import com.als.presence.data.model.CorrectDailyAttendanceRequest
import com.als.presence.data.model.DailyAttendanceDto
import com.als.presence.data.model.ClockInRequest
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.CreateShiftTemplateRequest
import com.als.presence.data.model.UpdateShiftTemplateRequest
import com.als.presence.data.model.CreateHolidayRequest
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.LocationCheckResultDto
import com.als.presence.data.remote.ActiveLocationSessionDto
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.InviteStaffRequest
import com.als.presence.data.model.InvitationDto
import com.als.presence.data.model.CreateInvitationRequest
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.LocationTodayPresenceResponseDto
import com.als.presence.data.model.ManualClockInRequest
import com.als.presence.data.model.OrganizationDto
import com.als.presence.data.model.OvertimeDto
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.OrganizationSummaryDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PayslipDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ResolveExceptionRequest
import com.als.presence.data.model.SalaryStructureDto
import com.als.presence.data.model.ScheduleEntryDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UpsertSalaryRequest
import com.als.presence.data.model.WalletSummaryDto
import com.als.presence.data.model.WalletTransactionDto

/**
 * Single read/write gateway for every operational domain used by the role
 * dashboards. The UI never talks to Retrofit directly; it observes [Resource]
 * results produced here.
 */
interface OperationsRepository {

    // Organization ----------------------------------------------------------
    suspend fun getOrganizationSummary(): Resource<OrganizationSummaryDto>
    suspend fun updateOrganization(
        name: String? = null,
        timezone: String? = null,
        currency: String? = null,
        logoBase64: String? = null
    ): Resource<OrganizationDto>

    // Workforce -------------------------------------------------------------
    suspend fun getStaff(): Resource<List<StaffDto>>
    suspend fun getMembers(): Resource<List<OrganizationMemberDto>>
    suspend fun inviteStaff(request: InviteStaffRequest): Resource<StaffDto>
    suspend fun createInvitation(request: CreateInvitationRequest): Resource<InvitationDto>
    suspend fun getInvitations(): Resource<List<InvitationDto>>
    suspend fun getSalaryHistory(staffProfileId: String): Resource<List<SalaryStructureDto>>
    suspend fun setSalary(
        staffProfileId: String,
        request: UpsertSalaryRequest
    ): Resource<SalaryStructureDto>
    suspend fun deactivateMember(userId: String): Resource<Unit>

    // Premises --------------------------------------------------------------
    suspend fun getPremises(): Resource<List<PremisesDto>>
    suspend fun createPremises(request: CreatePremisesRequest): Resource<PremisesDto>
    suspend fun updatePremises(
        premisesId: String,
        request: UpdatePremisesRequest
    ): Resource<PremisesDto>

    // Attendance ------------------------------------------------------------
    suspend fun getAttendanceRecords(
        from: String? = null,
        to: String? = null,
        staffProfileId: String? = null
    ): Resource<List<AttendanceRecordDto>>

    suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>>
    suspend fun resolveException(
        exceptionId: String,
        request: ResolveExceptionRequest
    ): Resource<Unit>

    suspend fun clockIn(request: ClockInRequest): Resource<AttendanceEventDto>
    suspend fun clockOut(attendanceEventId: String?): Resource<AttendanceEventDto>
    suspend fun clockInManual(request: ManualClockInRequest): Resource<AttendanceEventDto>

    // Face ------------------------------------------------------------------
    /** Owner/Manager supervised enrolment; staff never enrol themselves. */
    suspend fun enrollFaceSupervised(
        staffProfileId: String,
        embeddings: Map<String, List<Double>>,
        modelVersion: String,
        qualityScore: Double? = null,
        premisesId: String? = null
    ): Resource<FaceStatusDto>

    suspend fun enrollFace(
        embeddingVector: List<Double>,
        qualityScore: Double? = null
    ): Resource<FaceStatusDto>

    suspend fun getFaceStatus(): Resource<FaceStatusDto>
    suspend fun getStaffFaceStatus(staffProfileId: String): Resource<FaceStatusDto>
    suspend fun revokeFaceEnrollment(staffProfileId: String): Resource<Unit>

    // Location monitoring ---------------------------------------------------
    suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean = false,
        capturedAt: Long? = null
    ): Resource<LocationCheckResultDto>

    suspend fun getActiveLocationSession(): Resource<ActiveLocationSessionDto>

    suspend fun getTodayLocationPresence(
        premisesId: String? = null,
        date: String? = null
    ): Resource<LocationTodayPresenceResponseDto>

    suspend fun assignStaffPremises(
        staffProfileId: String,
        premisesId: String?
    ): Resource<Unit>

    // Leave -----------------------------------------------------------------
    suspend fun getLeaves(): Resource<List<LeaveDto>>
    suspend fun requestLeave(request: LeaveRequestCreate): Resource<LeaveDto>
    suspend fun reviewLeave(id: String, decision: String, note: String?): Resource<LeaveDto>

    // Overtime --------------------------------------------------------------
    suspend fun getOvertime(
        from: String? = null,
        to: String? = null,
        staffProfileId: String? = null
    ): Resource<List<OvertimeDto>>

    suspend fun createOvertime(request: CreateOvertimeRequest): Resource<OvertimeDto>
    suspend fun cancelOvertime(overtimeId: String): Resource<Unit>

    // Wallet ----------------------------------------------------------------
    suspend fun getWalletSummary(): Resource<WalletSummaryDto>
    suspend fun getWalletTransactions(): Resource<List<WalletTransactionDto>>

    // Payroll ---------------------------------------------------------------
    suspend fun getPayrollPeriods(): Resource<List<PayrollPeriodDto>>
    suspend fun getPayrollItems(periodId: String): Resource<List<PayrollItemDto>>
    suspend fun getMyPayslips(): Resource<List<PayslipDto>>
    suspend fun createPayrollPeriod(startDate: String, endDate: String): Resource<PayrollPeriodDto>
    suspend fun calculatePayroll(periodId: String): Resource<Unit>
    suspend fun approvePayroll(periodId: String): Resource<Unit>
    suspend fun submitPayrollPeriod(periodId: String): Resource<Unit>
    suspend fun recordPayment(
        itemId: String,
        amountCents: Long,
        paymentMethod: String,
        referenceNumber: String
    ): Resource<Unit>

    // Shifts ----------------------------------------------------------------
    suspend fun getShiftTemplates(): Resource<List<ShiftTemplateDto>>
    suspend fun createShiftTemplate(request: CreateShiftTemplateRequest): Resource<ShiftTemplateDto>
    suspend fun updateShiftTemplate(
        templateId: String,
        request: UpdateShiftTemplateRequest
    ): Resource<ShiftTemplateDto>
    suspend fun deleteShiftTemplate(templateId: String): Resource<Unit>
    suspend fun getHolidays(from: String? = null, to: String? = null): Resource<List<HolidayDto>>
    suspend fun createHoliday(request: CreateHolidayRequest): Resource<HolidayDto>
    suspend fun deleteHoliday(holidayId: String): Resource<Unit>
    suspend fun getShiftAssignments(staffProfileId: String? = null): Resource<List<ShiftAssignmentDto>>
    suspend fun createShiftAssignment(
        request: CreateShiftAssignmentRequest
    ): Resource<ShiftAssignmentDto>

    suspend fun getMySchedule(from: String? = null, to: String? = null): Resource<List<ScheduleEntryDto>>
    suspend fun getStaffSchedule(
        staffProfileId: String,
        from: String? = null,
        to: String? = null
    ): Resource<List<ScheduleEntryDto>>

    // Daily Attendance ------------------------------------------------------
    suspend fun getDailyAttendance(workDate: String, premisesId: String? = null): Resource<List<DailyAttendanceDto>>
    suspend fun correctDailyAttendance(id: String, request: CorrectDailyAttendanceRequest): Resource<DailyAttendanceDto>
    suspend fun getMyEarningsHistory(from: String? = null, to: String? = null): Resource<List<DailyAttendanceDto>>
}
