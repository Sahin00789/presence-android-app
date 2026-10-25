package com.als.presence.data.remote

import com.als.presence.data.model.AssignStaffPremisesRequest
import com.als.presence.data.model.CreateInvitationRequest
import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.InvitationDto
import com.als.presence.data.model.InviteStaffRequest
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.ScheduleEntryDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.CreateShiftTemplateRequest
import com.als.presence.data.model.UpdateShiftTemplateRequest
import com.als.presence.data.model.CreateHolidayRequest
import com.als.presence.data.model.HolidayDto
import com.als.presence.data.model.DeletedResponseDto
import com.als.presence.data.model.OrganizationDto
import com.als.presence.data.model.OrganizationSummaryDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.SalaryStructureDto
import com.als.presence.data.model.UpdateOrganizationRequest
import com.als.presence.data.model.UpsertSalaryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StaffApiService {

    @GET("api/v1/staff")
    suspend fun list(): Response<List<StaffDto>>

    @POST("api/v1/staff/invite")
    suspend fun invite(@Body request: InviteStaffRequest): Response<StaffDto>

    @GET("api/v1/staff/{staffProfileId}/salary")
    suspend fun salaryHistory(
        @Path("staffProfileId") staffProfileId: String
    ): Response<List<SalaryStructureDto>>

    @PUT("api/v1/staff/{staffProfileId}/salary")
    suspend fun setSalary(
        @Path("staffProfileId") staffProfileId: String,
        @Body request: UpsertSalaryRequest
    ): Response<SalaryStructureDto>

    @PATCH("api/v1/staff/{staffProfileId}/premises")
    suspend fun assignPremises(
        @Path("staffProfileId") staffProfileId: String,
        @Body request: AssignStaffPremisesRequest
    ): Response<Unit>
}

interface InvitationApiService {

    @GET("api/v1/invitations")
    suspend fun list(): Response<List<InvitationDto>>

    @POST("api/v1/invitations")
    suspend fun create(@Body request: CreateInvitationRequest): Response<InvitationDto>

    @DELETE("api/v1/invitations/{invitationId}")
    suspend fun revoke(@Path("invitationId") invitationId: String): Response<Unit>
}

interface PremisesApiService {

    @GET("api/v1/premises")
    suspend fun list(): Response<List<PremisesDto>>

    @POST("api/v1/premises")
    suspend fun create(@Body request: CreatePremisesRequest): Response<PremisesDto>

    @PUT("api/v1/premises/{id}")
    suspend fun update(
        @Path("id") premisesId: String,
        @Body request: UpdatePremisesRequest
    ): Response<PremisesDto>
}

interface OrganizationApiService {

    @GET("api/v1/organizations/current")
    suspend fun getOrganizationSummary(): Response<OrganizationSummaryDto>

    @PATCH("api/v1/organizations/current")
    suspend fun updateOrganization(
        @Body request: UpdateOrganizationRequest
    ): Response<OrganizationDto>

    @POST("api/v1/organizations/current/logo")
    suspend fun uploadLogo(
        @Body request: Map<String, String>
    ): Response<OrganizationDto>

    @GET("api/v1/organizations/members")
    suspend fun members(): Response<List<OrganizationMemberDto>>

    @POST("api/v1/organizations/members/{userId}/deactivate")
    suspend fun deactivateMember(@Path("userId") userId: String): Response<Unit>
}

interface ShiftApiService {

    @GET("api/v1/shifts/templates")
    suspend fun templates(): Response<List<ShiftTemplateDto>>

    @POST("api/v1/shifts/templates")
    suspend fun createTemplate(
        @Body request: CreateShiftTemplateRequest
    ): Response<ShiftTemplateDto>

    @PATCH("api/v1/shifts/templates/{templateId}")
    suspend fun updateTemplate(
        @Path("templateId") templateId: String,
        @Body request: UpdateShiftTemplateRequest
    ): Response<ShiftTemplateDto>

    @DELETE("api/v1/shifts/templates/{templateId}")
    suspend fun deleteTemplate(
        @Path("templateId") templateId: String
    ): Response<DeletedResponseDto>

    @GET("api/v1/shifts/holidays")
    suspend fun holidays(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<HolidayDto>>

    @POST("api/v1/shifts/holidays")
    suspend fun createHoliday(
        @Body request: CreateHolidayRequest
    ): Response<HolidayDto>

    @DELETE("api/v1/shifts/holidays/{holidayId}")
    suspend fun deleteHoliday(
        @Path("holidayId") holidayId: String
    ): Response<DeletedResponseDto>

    @GET("api/v1/shifts/assignments")
    suspend fun assignments(
        @Query("staffProfileId") staffProfileId: String? = null
    ): Response<List<ShiftAssignmentDto>>

    @POST("api/v1/shifts/assignments")
    suspend fun createAssignment(
        @Body request: CreateShiftAssignmentRequest
    ): Response<ShiftAssignmentDto>

    @DELETE("api/v1/shifts/assignments/{assignmentId}")
    suspend fun deleteAssignment(
        @Path("assignmentId") assignmentId: String
    ): Response<Unit>

    @GET("api/v1/shifts/my-schedule")
    suspend fun mySchedule(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<ScheduleEntryDto>>

    @GET("api/v1/shifts/schedule/{staffProfileId}")
    suspend fun staffSchedule(
        @Path("staffProfileId") staffProfileId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<ScheduleEntryDto>>
}
