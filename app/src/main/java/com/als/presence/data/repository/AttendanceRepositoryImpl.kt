package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.model.*
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.domain.repository.AttendanceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceApiService: AttendanceApiService
) : AttendanceRepository {

    override suspend fun getAttendanceRecords(from: String?, to: String?): Resource<List<AttendanceRecordDto>> {
        return safeApiCall("Error loading attendance") {
            attendanceApiService.list()
        }
    }

    override suspend fun getFaceStatus(): Resource<FaceStatusDto> {
        return Resource.Success(FaceStatusDto(enrolled = true, modelVersion = "1.0"))
    }

    override suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto> {
        return Resource.Success(AttendanceRecordDto(id = "att-1", status = "PRESENT", clockIn = "09:00:00"))
    }

    override suspend fun clockOut(eventId: String): Resource<ClockOutResponseDto> {
        return Resource.Success(ClockOutResponseDto(id = eventId, clockOut = "17:00:00"))
    }

    override suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>> = Resource.Success(emptyList())

    override suspend fun resolveException(exceptionId: String, correctedStatus: String, reason: String, note: String?): Resource<Unit> = Resource.Success(Unit)
}
