package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate
import com.als.presence.data.model.UpcomingShiftDto

interface LeaveRepository {
    suspend fun getLeaves(): Resource<List<LeaveDto>>
    suspend fun getUpcomingShifts(): Resource<List<UpcomingShiftDto>>
    suspend fun requestLeave(request: LeaveRequestCreate): Resource<List<LeaveDto>>
    suspend fun reviewLeave(id: String, decision: String, note: String? = null): Resource<LeaveDto>
}
