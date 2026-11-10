package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.LeaveRequestDao
import com.als.presence.data.local.LeaveRequestEntity
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.UpcomingShiftDto
import com.als.presence.data.remote.LeaveApiService
import com.als.presence.domain.repository.LeaveRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepositoryImpl @Inject constructor(
    private val leaveApiService: LeaveApiService,
    private val leaveRequestDao: LeaveRequestDao
) : LeaveRepository {

    override suspend fun getLeaves(): Resource<List<LeaveDto>> {
        val result = safeApiCall("Error loading leaves") {
            leaveApiService.list()
        }
        if (result is Resource.Success) {
            val entities = result.data.map { LeaveRequestEntity.fromDto(it) }
            leaveRequestDao.upsertAll(entities)
        }
        val cached = leaveRequestDao.getAll()
        return if (cached.isNotEmpty()) {
            Resource.Success(cached.map { it.toDto() })
        } else if (result is Resource.Error) {
            result
        } else {
            Resource.Success(emptyList())
        }
    }

    override suspend fun getUpcomingShifts(): Resource<List<UpcomingShiftDto>> {
        val result = safeApiCall("Error loading upcoming shifts") {
            leaveApiService.getUpcomingShifts()
        }
        return result
    }

    override suspend fun requestLeave(request: LeaveRequestCreate): Resource<List<LeaveDto>> {
        val result = safeApiCall("Error submitting leave request") {
            leaveApiService.create(request)
        }
        if (result is Resource.Success) {
            val entities = result.data.map { LeaveRequestEntity.fromDto(it) }
            leaveRequestDao.upsertAll(entities)
        }
        return result
    }

    override suspend fun reviewLeave(id: String, decision: String, note: String?): Resource<LeaveDto> {
        val result = safeApiCall("Error reviewing leave request") {
            leaveApiService.review(
                id = id,
                body = com.als.presence.data.model.ReviewLeaveRequest(
                    decision = decision,
                    reviewRemarks = note
                )
            )
        }
        if (result is Resource.Success) {
            leaveRequestDao.upsert(LeaveRequestEntity.fromDto(result.data))
        }
        return result
    }
}
