package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.remote.LeaveApiService
import com.als.presence.domain.repository.LeaveRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepositoryImpl @Inject constructor(
    private val leaveApiService: LeaveApiService
) : LeaveRepository {

    override suspend fun getLeaves(): Resource<List<LeaveDto>> {
        return safeApiCall("Error loading leaves") {
            leaveApiService.list()
        }
    }

    override suspend fun requestLeave(request: LeaveRequestCreate): Resource<LeaveDto> {
        return safeApiCall("Error submitting leave request") {
            leaveApiService.create(request)
        }
    }

    override suspend fun reviewLeave(id: String, decision: String, note: String?): Resource<LeaveDto> {
        return safeApiCall("Error reviewing leave request") {
            leaveApiService.review(
                id = id,
                body = com.als.presence.data.model.ReviewLeaveRequest(
                    decision = decision,
                    reviewRemarks = note
                )
            )
        }
    }
}
