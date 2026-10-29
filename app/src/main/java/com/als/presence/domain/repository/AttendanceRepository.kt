package com.als.presence.domain.repository

import com.als.presence.core.common.Resource
import com.als.presence.data.model.*

interface AttendanceRepository {
    suspend fun getAttendanceRecords(from: String? = null, to: String? = null): Resource<List<AttendanceRecordDto>>
    suspend fun getFaceStatus(): Resource<FaceStatusDto>
    suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto>
    suspend fun clockOut(eventId: String): Resource<ClockOutResponseDto>
    suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>>
    suspend fun resolveException(exceptionId: String, correctedStatus: String, reason: String, note: String? = null): Resource<Unit>
}
