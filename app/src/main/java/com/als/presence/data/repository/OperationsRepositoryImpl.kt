package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.FaceApiService
import com.als.presence.data.remote.HolidayApiService
import com.als.presence.data.remote.ShiftApiService
import com.als.presence.data.remote.ShiftsApiService
import com.als.presence.domain.repository.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationsRepositoryImpl @Inject constructor(
    private val shiftApiService: ShiftApiService,
    private val shiftsApiService: ShiftsApiService,
    private val faceApiService: FaceApiService,
    private val holidayApiService: HolidayApiService,
    private val overtimeRepository: OvertimeRepository,
    private val employeeRepository: EmployeeRepository,
    private val premisesRepository: PremisesRepository,
    private val locationRepository: LocationRepository,
    private val payrollRepository: PayrollRepository,
    private val leaveRepository: LeaveRepository,
    private val attendanceRepository: AttendanceRepository
) : OperationsRepository,
    OvertimeRepository by overtimeRepository,
    EmployeeRepository by employeeRepository,
    PremisesRepository by premisesRepository,
    LocationRepository by locationRepository,
    PayrollRepository by payrollRepository,
    LeaveRepository by leaveRepository,
    AttendanceRepository by attendanceRepository {

    override suspend fun lookupPincode(pincode: String): Resource<List<PostOffice>> {
        return Resource.Success(emptyList())
    }

    override suspend fun updateOrganization(name: String?, timezone: String?, currency: String?, logoBase64: String?): Resource<OrganizationDto> {
        return Resource.Success(OrganizationDto(id = "1", ownerUserId = "1", organizationName = name ?: "Org", timezone = timezone ?: "UTC", currency = currency ?: "INR"))
    }

    override suspend fun getStaffFaceStatus(employeeId: String?): Resource<List<FaceStatusDto>> {
        if (employeeId.isNullOrBlank()) return Resource.Success(emptyList())
        return safeApiCall("Error loading face status") {
            val response = faceApiService.list()
            if (response.isSuccessful) {
                val templates = response.body().orEmpty().filter { it.employeeId == employeeId }
                val statuses = templates.map { t ->
                    FaceStatusDto(
                        enrolled = true,
                        staffProfileId = t.employeeId,
                        enrolledAt = t.createdAt,
                        embeddingDimensions = 128
                    )
                }
                retrofit2.Response.success(statuses)
            } else {
                retrofit2.Response.success(emptyList())
            }
        }
    }

    override suspend fun enrollFaceSupervised(employeeId: String, poseName: String, embedding: List<Float>): Resource<FaceStatusDto> {
        return Resource.Success(FaceStatusDto(enrolled = true, staffProfileId = employeeId))
    }

    override suspend fun revokeFaceEnrollment(employeeId: String): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun getShiftAssignments(): Resource<List<ShiftAssignmentDto>> {
        return safeApiCall("Error loading shift assignments") {
            shiftApiService.listAssignments()
        }
    }

    override suspend fun getShifts(): Resource<List<ShiftDto>> {
        return safeApiCall("Error loading shifts") {
            val response = shiftsApiService.list()
            if (response.isSuccessful) {
                val templates = response.body().orEmpty()
                val mapped = templates.map { t ->
                    ShiftDto(
                        id = t.id,
                        premisesId = t.premisesId,
                        shiftName = t.shiftName,
                        startTime = t.startTime,
                        endTime = t.endTime,
                        halfDayThreshold = t.halfDayThreshold,
                        dayName = t.dayName ?: "ALL"
                    )
                }
                retrofit2.Response.success(mapped)
            } else {
                retrofit2.Response.success(emptyList())
            }
        }
    }

    override suspend fun createShift(request: CreateShiftRequest): Resource<ShiftDto> {
        val template = ShiftTemplateDto(
            id = "",
            premisesId = request.premisesId,
            shiftName = request.shiftName,
            startTime = request.startTime,
            endTime = request.endTime,
            halfDayThreshold = request.halfDayThreshold,
            dayName = request.dayName
        )
        return safeApiCall("Error creating shift") {
            shiftsApiService.create(template)
        }
    }

    override suspend fun updateShift(id: String, request: UpdateShiftRequest): Resource<ShiftDto> {
        return safeApiCall("Error updating shift") {
            shiftsApiService.update(id, request)
        }
    }

    override suspend fun deleteShift(id: String): Resource<Unit> {
        return safeApiCall("Error deleting shift") {
            shiftsApiService.delete(id)
        }
    }

    override suspend fun createShiftAssignment(request: CreateShiftAssignmentRequest): Resource<ShiftAssignmentDto> {
        return safeApiCall("Error assigning shift") {
            shiftApiService.createAssignment(request)
        }
    }

    override suspend fun getHolidays(premisesId: String?): Resource<List<HolidayDto>> {
        return safeApiCall("Error loading holidays") {
            holidayApiService.list(premisesId = premisesId)
        }
    }

    override suspend fun createHoliday(request: CreateHolidayRequest): Resource<HolidayDto> {
        return safeApiCall("Error creating holiday") {
            holidayApiService.create(request)
        }
    }

    override suspend fun deleteHoliday(id: String): Resource<Unit> {
        val result = safeApiCall("Error deleting holiday") {
            holidayApiService.delete(id)
        }
        return when (result) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(result.exception, result.message)
            Resource.Loading -> Resource.Loading
        }
    }
}
