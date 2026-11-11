package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.AttendanceDao
import com.als.presence.data.local.AttendanceEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.data.remote.FaceApiService
import com.als.presence.data.sync.SnapshotProjectionEngine
import com.als.presence.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceApiService: AttendanceApiService,
    private val faceApiService: FaceApiService,
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
            return result
        }
        val cached = attendanceDao.getAll()
        val filtered = if (!staffProfileId.isNullOrBlank()) {
            cached.filter { it.employeeId == staffProfileId }
        } else cached
        return if (filtered.isNotEmpty()) {
            Resource.Success(filtered.map { it.toDto() })
        } else if (result is Resource.Error) {
            result
        } else {
            Resource.Success(emptyList())
        }
    }

    override suspend fun getFaceStatus(): Resource<FaceStatusDto> {
        return safeApiCall("Error checking face status") {
            val response = faceApiService.getStatus()
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()!!
                retrofit2.Response.success(
                    FaceStatusDto(
                        enrolled = status.enrolled,
                        modelVersion = status.modelVersion,
                        employeeProfileId = status.employeeProfileId,
                        enrolledAt = status.enrolledAt,
                        embeddingDimensions = 128
                    )
                )
            } else {
                retrofit2.Response.success(FaceStatusDto(enrolled = false))
            }
        }
    }

    override suspend fun clockIn(request: ClockInRequest): Resource<AttendanceRecordDto> {
        val result = safeApiCall("Error clocking in") {
            if (request.overtimeId != null) {
                attendanceApiService.clockInOvertime(request)
            } else {
                attendanceApiService.clockInShift(request)
            }
        }
        return when (result) {
            is Resource.Success -> {
                val record = result.data
                if (record != null) {
                    attendanceDao.upsert(AttendanceEntity.fromDto(record))
                    snapshotProjectionEngine.rebuildOwnerPingsSnapshot("ALL")
                }
                result
            }
            is Resource.Error -> result
            Resource.Loading -> result
        }
    }

    override suspend fun clockOut(eventId: String): Resource<ClockOutResponseDto> {
        val result = safeApiCall("Error clocking out") {
            attendanceApiService.clockOut(
                ClockOutRequest(attendanceEventId = eventId.takeIf { it.isNotBlank() })
            )
        }
        return when (result) {
            is Resource.Success -> {
                snapshotProjectionEngine.rebuildOwnerPingsSnapshot("ALL")
                result
            }
            is Resource.Error -> result
            Resource.Loading -> result
        }
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

    override suspend fun manualMarkShift(request: ManualMarkShiftAttendanceRequest): Resource<Unit> {
        return safeApiCall("Error marking shift attendance") {
            attendanceApiService.manualMarkShift(request)
        }
    }

    override suspend fun manualMarkOvertime(request: ManualMarkOvertimeAttendanceRequest): Resource<Unit> {
        return safeApiCall("Error marking overtime attendance") {
            attendanceApiService.manualMarkOvertime(request)
        }
    }

    override suspend fun getTodayPresence(): Resource<LocationTodayPresenceResponseDto> {
        return safeApiCall("Error loading presence") {
            attendanceApiService.getTodayLocationPresence()
        }
    }

    override suspend fun getTodayLocationPresence(premisesId: String?): Resource<LocationTodayPresenceResponseDto> {
        return safeApiCall("Error loading presence") {
            attendanceApiService.getTodayLocationPresence(premisesId = premisesId)
        }
    }

    override suspend fun getActiveLocationSession(): Resource<LocationSessionDto> {
        return safeApiCall("Error loading active location session") {
            attendanceApiService.getActiveLocationSession()
        }
    }

    override suspend fun submitLocationCheck(
        attendanceEventId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        isMock: Boolean,
        capturedAt: Long
    ): Resource<LocationCheckResultDto> {
        return safeApiCall("Error submitting location check") {
            attendanceApiService.submitShiftLocationPing(
                LocationCheckRequestDto(
                    attendanceEventId = attendanceEventId,
                    shiftAttendanceId = attendanceEventId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    isMock = isMock
                )
            )
        }
    }

    override suspend fun assignStaffPremises(employeeId: String, premisesId: String?): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun getShiftLocationPings(shiftAttendanceId: String): Resource<List<GpsPingSummaryDto>> {
        return safeApiCall("Error loading shift location pings") {
            attendanceApiService.getShiftLocationPings(shiftAttendanceId)
        }
    }

    override suspend fun getOvertimeLocationPings(overtimeAttendanceId: String): Resource<List<GpsPingSummaryDto>> {
        return safeApiCall("Error loading overtime location pings") {
            attendanceApiService.getOvertimeLocationPings(overtimeAttendanceId)
        }
    }

    override suspend fun getUnhandledEvents(): Resource<UnhandledEventsResponseDto> {
        return safeApiCall("Error loading pending actions") {
            attendanceApiService.getUnhandledEvents()
        }
    }

    override suspend fun claimUnhandledEvent(request: ClaimUnhandledEventRequestDto): Resource<ClaimUnhandledEventResponseDto> {
        return safeApiCall("Error claiming action") {
            attendanceApiService.claimUnhandledEvent(request)
        }
    }
}

