package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface EmployeeRepository {
    suspend fun getEmployees(): Resource<List<StaffDto>>
    suspend fun createEmployee(request: CreateEmployeeRequest): Resource<EmployeeDto>
    suspend fun updateEmployee(employeeId: String, request: UpdateEmployeeRequest): Resource<EmployeeDto>
    suspend fun deactivateMember(employeeId: String): Resource<Unit>
    suspend fun getMembers(): Resource<List<OrganizationMemberDto>>
}
