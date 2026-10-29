package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.LeaveDto
import com.als.presence.data.model.LeaveRequestCreate

interface LeaveRepository {
    suspend fun getLeaves(): Resource<List<LeaveDto>>
    suspend fun requestLeave(request: LeaveRequestCreate): Resource<LeaveDto>
    suspend fun reviewLeave(id: String, decision: String, note: String? = null): Resource<LeaveDto>
}
