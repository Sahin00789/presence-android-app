package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.EmployeeDao
import com.als.presence.data.local.EmployeeEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.EmployeeApiService
import com.als.presence.data.sync.SnapshotProjectionEngine
import com.als.presence.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeRepositoryImpl @Inject constructor(
    private val employeeApiService: EmployeeApiService,
    private val employeeDao: EmployeeDao,
    private val snapshotProjectionEngine: SnapshotProjectionEngine
) : EmployeeRepository {

    fun observeEmployees(): Flow<List<StaffDto>> {
        return employeeDao.observeAll().map { list -> list.map { it.toDto() } }
    }

    override suspend fun getEmployees(): Resource<List<StaffDto>> {
        val result = safeApiCall("Error loading employees") {
            employeeApiService.list()
        }
        return when (result) {
            is Resource.Success -> {
                if (result.data != null) {
                    val entities = result.data.map { EmployeeEntity.fromDto(it) }
                    employeeDao.upsertAll(entities)
                    snapshotProjectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
                }
                result
            }
            is Resource.Error -> {
                val cached = employeeDao.getAll()
                if (cached.isNotEmpty()) {
                    Resource.Success(cached.map { it.toDto() })
                } else {
                    result
                }
            }
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun createEmployee(request: CreateEmployeeRequest): Resource<EmployeeDto> {
        val result = safeApiCall("Error onboarding employee") {
            employeeApiService.create(request)
        }
        if (result is Resource.Success && result.data != null) {
            employeeDao.upsert(EmployeeEntity.fromDto(result.data))
            snapshotProjectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
        }
        return result
    }

    override suspend fun updateEmployee(employeeId: String, request: UpdateEmployeeRequest): Resource<EmployeeDto> {
        val result = safeApiCall("Error updating employee profile") {
            employeeApiService.update(employeeId, request)
        }
        if (result is Resource.Success && result.data != null) {
            employeeDao.upsert(EmployeeEntity.fromDto(result.data))
            snapshotProjectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
        }
        return result
    }

    override suspend fun deactivateMember(employeeId: String): Resource<Unit> {
        val result = safeApiCall("Error deactivating employee") {
            employeeApiService.deactivate(employeeId)
        }
        if (result is Resource.Success) {
            employeeDao.markDeleted(employeeId)
            snapshotProjectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
        }
        return result
    }

    override suspend fun getMembers(): Resource<List<OrganizationMemberDto>> {
        return safeApiCall("Error loading organization members") {
            employeeApiService.getMembers()
        }
    }
}
