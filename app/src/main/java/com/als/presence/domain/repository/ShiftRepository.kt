package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface ShiftRepository {
    suspend fun getShiftAssignments(): Resource<List<ShiftAssignmentDto>>
    suspend fun getShifts(): Resource<List<ShiftDto>>
    suspend fun createShift(request: CreateShiftRequest): Resource<ShiftDto>
    suspend fun updateShift(id: String, request: UpdateShiftRequest): Resource<ShiftDto>
    suspend fun deleteShift(id: String): Resource<Unit>
    suspend fun createShiftAssignment(request: CreateShiftAssignmentRequest): Resource<ShiftAssignmentDto>
}
