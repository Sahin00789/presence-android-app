package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface OperationsRepository :
    OvertimeRepository,
    EmployeeRepository,
    PremisesRepository,
    PayrollRepository,
    LeaveRepository,
    AttendanceRepository,
    ShiftRepository,
    BiometricsRepository,
    OrganizationRepository {

    suspend fun lookupPincode(pincode: String): Resource<List<PostOffice>>
    suspend fun getHolidays(premisesId: String? = null): Resource<List<HolidayDto>>
    suspend fun createHoliday(request: CreateHolidayRequest): Resource<HolidayDto>
    suspend fun deleteHoliday(id: String): Resource<Unit>
}
