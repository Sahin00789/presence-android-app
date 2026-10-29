package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*
import com.als.presence.data.remote.PostalApiService
import com.als.presence.domain.repository.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationsRepositoryImpl @Inject constructor(
    private val postalApiService: PostalApiService,
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
        return try {
            val response = postalApiService.lookup(pincode)
            if (response.isSuccessful) {
                val list = response.body()
                val first = list?.firstOrNull()
                if (first != null && first.status.equals("Success", ignoreCase = true)) {
                    val offices = first.postOffice.orEmpty()
                    Resource.Success(offices)
                } else {
                    val msg = first?.message ?: "No post office details found for pincode $pincode"
                    Resource.Error(Exception(msg), message = msg)
                }
            } else {
                val msg = "Postal API status ${response.code()}"
                Resource.Error(Exception(msg), message = msg)
            }
        } catch (e: Exception) {
            Resource.Error(e, message = e.localizedMessage ?: "Failed to lookup pincode")
        }
    }

    override suspend fun updateOrganization(name: String?, timezone: String?, currency: String?, logoBase64: String?): Resource<OrganizationDto> {
        return Resource.Success(OrganizationDto(id = "1", ownerUserId = "1", organizationName = name ?: "Org", timezone = timezone ?: "UTC", currency = currency ?: "INR"))
    }

    override suspend fun getStaffFaceStatus(employeeId: String?): Resource<List<FaceStatusDto>> = Resource.Success(emptyList())

    override suspend fun enrollFaceSupervised(employeeId: String, poseName: String, embedding: List<Float>): Resource<FaceStatusDto> = Resource.Success(FaceStatusDto(enrolled = true, staffProfileId = employeeId))

    override suspend fun revokeFaceEnrollment(employeeId: String): Resource<Unit> = Resource.Success(Unit)

    override suspend fun getShiftAssignments(): Resource<List<ShiftAssignmentDto>> = Resource.Success(emptyList())

    override suspend fun getShifts(): Resource<List<ShiftDto>> = Resource.Success(emptyList())

    override suspend fun createShift(request: CreateShiftRequest): Resource<ShiftDto> = Resource.Success(ShiftDto(id = "s-1", premisesId = request.premisesId, shiftName = request.shiftName, startTime = request.startTime, endTime = request.endTime))

    override suspend fun updateShift(id: String, request: UpdateShiftRequest): Resource<ShiftDto> = Resource.Success(ShiftDto(id = id, shiftName = request.shiftName ?: "Shift"))

    override suspend fun deleteShift(id: String): Resource<Unit> = Resource.Success(Unit)

    override suspend fun createShiftAssignment(request: CreateShiftAssignmentRequest): Resource<ShiftAssignmentDto> = Resource.Success(ShiftAssignmentDto(id = "1", premisesId = request.premisesId, staffId = request.staffId, shiftId = request.shiftId))

    override suspend fun getHolidays(): Resource<List<HolidayDto>> = Resource.Success(emptyList())

    override suspend fun createHoliday(request: CreateHolidayRequest): Resource<HolidayDto> = Resource.Success(HolidayDto(id = "h-1", name = request.name, holidayDate = request.holidayDate, isPaid = request.isPaid))

    override suspend fun deleteHoliday(id: String): Resource<Unit> = Resource.Success(Unit)
}
