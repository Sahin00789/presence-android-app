package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.EmployeeApiService
import com.als.presence.domain.repository.EmployeeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeRepositoryImpl @Inject constructor(
    private val employeeApiService: EmployeeApiService
) : EmployeeRepository {

    override suspend fun getEmployees(): Resource<List<StaffDto>> {
        return safeApiCall("Error loading employees") {
            employeeApiService.list()
        }
    }

    override suspend fun createEmployee(request: CreateEmployeeRequest): Resource<EmployeeDto> = Resource.Success(EmployeeDto(id = "1", fullName = request.fullName ?: "Staff"))

    override suspend fun deactivateMember(employeeId: String): Resource<Unit> = Resource.Success(Unit)

    override suspend fun getMembers(): Resource<List<OrganizationMemberDto>> = Resource.Success(emptyList())
}
