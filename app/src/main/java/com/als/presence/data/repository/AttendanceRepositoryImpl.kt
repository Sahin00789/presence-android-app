package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.AttendanceDao
import com.als.presence.data.local.AttendanceEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.data.sync.SnapshotProjectionEngine
import com.als.presence.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceApiService: AttendanceApiService,
    private val attendanceDao: AttendanceDao,
    private val snapshotProjectionEngine: SnapshotProjectionEngine
) : AttendanceRepository {

    fun observeAttendance(): Flow<List<AttendanceRecordDto>> {
        return attendanceDao.observeAll().map { list -> list.map { it.toDto() } }
    }

    override suspend fun getAttendanceRecords(from: String?, to: String?, staffProfileId: String?): Resource<List<AttendanceRecordDto>> {
        val result = safeApiCall("Error loading attendance") {
            attendanceApiService.list(from = from, to = to, staffProfileId = staffProfileId)
        }
        if (result is Resource.Success && result.data != null) {
            val entities = result.data.map { AttendanceEntity.fromDto(it) }
            attendanceDao.upsertAll(entities)
        }
        return result
    }

    override suspend fun getFaceStatus(): Resource<FaceStatusDto> {
        return Resource.Success(FaceStatusDto(enrolled = true, modelVersion = "1.0"))
    }

    override suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto> {
        val record = AttendanceRecordDto(id = "att-${System.currentTimeMillis()}", status = "PRESENT", clockIn = "09:00:00")
        attendanceDao.upsert(AttendanceEntity.fromDto(record))
        snapshotProjectionEngine.rebuildOwnerPingsSnapshot("ALL")
        return Resource.Success(record)
    }

    override suspend fun clockOut(eventId: String): Resource<ClockOutResponseDto> {
        val clockOutDto = ClockOutResponseDto(id = eventId, clockOut = "17:00:00")
        snapshotProjectionEngine.rebuildOwnerPingsSnapshot("ALL")
        return Resource.Success(clockOutDto)
    }

    override suspend fun getExceptions(): Resource<List<AttendanceExceptionDto>> = Resource.Success(emptyList())

    override suspend fun resolveException(exceptionId: String, correctedStatus: String, reason: String, note: String?): Resource<Unit> {
        return safeApiCall("Error resolving exception") {
            attendanceApiService.manualMark(
                ManualMarkAttendanceRequest(
                    attendanceEventId = exceptionId,
                    action = correctedStatus,
                    reason = reason
                )
            )
        }
    }
}
