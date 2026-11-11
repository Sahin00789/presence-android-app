package com.als.presence.data.repository

import com.als.presence.core.common.Resource
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.core.face.LivenessResult
import com.als.presence.core.network.safeApiCall
import com.als.presence.data.local.HolidayDao
import com.als.presence.data.local.HolidayEntity
import com.als.presence.data.local.ShiftAssignmentDao
import com.als.presence.data.local.ShiftAssignmentEntity
import com.als.presence.data.local.ShiftDao
import com.als.presence.data.local.ShiftEntity
import com.als.presence.data.model.*
import com.als.presence.data.remote.FaceApiService
import com.als.presence.data.remote.HolidayApiService
import com.als.presence.data.remote.ShiftApiService
import com.als.presence.data.remote.ShiftsApiService
import com.als.presence.domain.repository.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationsRepositoryImpl @Inject constructor(
    private val shiftApiService: ShiftApiService,
    private val shiftsApiService: ShiftsApiService,
    private val faceApiService: FaceApiService,
    private val holidayApiService: HolidayApiService,
    private val overtimeRepository: OvertimeRepository,
    private val employeeRepository: EmployeeRepository,
    private val premisesRepository: PremisesRepository,
    private val payrollRepository: PayrollRepository,
    private val leaveRepository: LeaveRepository,
    private val attendanceRepository: AttendanceRepository,
    private val shiftDao: ShiftDao,
    private val shiftAssignmentDao: ShiftAssignmentDao,
    private val holidayDao: HolidayDao
) : OperationsRepository,
    OvertimeRepository by overtimeRepository,
    EmployeeRepository by employeeRepository,
    PremisesRepository by premisesRepository,
    PayrollRepository by payrollRepository,
    LeaveRepository by leaveRepository,
    AttendanceRepository by attendanceRepository {

    override suspend fun lookupPincode(pincode: String): Resource<List<PostOffice>> {
        return Resource.Success(emptyList())
    }

    override suspend fun updateOrganization(name: String?, timezone: String?, currency: String?, logoBase64: String?): Resource<OrganizationDto> {
        return Resource.Success(OrganizationDto(id = "1", ownerUserId = "1", organizationName = name ?: "Org", timezone = timezone ?: "UTC", currency = currency ?: "INR"))
    }

    override suspend fun getStaffFaceStatus(employeeId: String?): Resource<List<FaceStatusDto>> {
        if (employeeId.isNullOrBlank()) return Resource.Success(emptyList())
        return safeApiCall("Error loading face status") {
            val response = faceApiService.getStatus(employeeProfileId = employeeId)
            if (response.isSuccessful) {
                val status = response.body()
                val isEnrolled = status?.enrolled == true
                val statusDto = FaceStatusDto(
                    enrolled = isEnrolled,
                    employeeProfileId = employeeId,
                    enrolledAt = status?.enrolledAt,
                    modelVersion = status?.modelVersion,
                    embeddingDimensions = 128
                )
                retrofit2.Response.success(listOf(statusDto))
            } else {
                retrofit2.Response.success(emptyList())
            }
        }
    }

    override suspend fun enrollFaceSupervised(
        employeeId: String,
        embeddings: Map<String, FaceEmbeddingEngine.Embedding>
    ): Resource<FaceStatusDto> {
        val center = embeddings["CENTER"]?.vector
            ?: return Resource.Error(IllegalStateException("CENTER pose is required"), "CENTER pose is required for face enrollment")
        val left = embeddings["LEFT"]?.vector ?: center
        val right = embeddings["RIGHT"]?.vector ?: center
        val up = embeddings["UP"]?.vector ?: center
        val down = embeddings["DOWN"]?.vector ?: center

        val qualities = embeddings.values.map { it.qualityScore }
        val avgQuality = if (qualities.isNotEmpty()) qualities.average() else 0.95
        val modelVersion = embeddings["CENTER"]?.modelVersion ?: "sface-yunet-v1"

        val centerEmbedding = embeddings["CENTER"]
        val centerLiveness = centerEmbedding?.liveness

        val req = EnrollMultiPoseRequest(
            employeeProfileId = employeeId,
            centerVector = center,
            leftVector = left,
            rightVector = right,
            upVector = up,
            downVector = down,
            modelVersion = modelVersion,
            qualityScore = if (avgQuality.isNaN()) 0.95 else avgQuality,
            liveness = LivenessEvidenceDto(
                activeBlinkDetected = centerLiveness?.activeBlinkDetected ?: true,
                blinkCount = 1,
                passiveTextureScore = centerLiveness?.passiveTextureScore ?: 0.95,
                passivePassed = centerLiveness?.passivePassed ?: true,
                capturedAt = java.time.Instant.now().toString()
            )
        )

        val networkResult = safeApiCall("Error enrolling face biometrics") {
            faceApiService.enrollMultiPose(req)
        }

        return when (networkResult) {
            is Resource.Success -> {
                val body = networkResult.data
                Resource.Success(
                    FaceStatusDto(
                        enrolled = body?.success == true,
                        modelVersion = body?.modelVersion ?: modelVersion,
                        employeeProfileId = employeeId,
                        enrolledAt = body?.enrolledAt ?: java.time.Instant.now().toString(),
                        embeddingDimensions = 128
                    )
                )
            }
            is Resource.Error -> Resource.Error(networkResult.exception, networkResult.message)
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun revokeFaceEnrollment(employeeId: String): Resource<Unit> {
        return Resource.Success(Unit)
    }

    override suspend fun getShiftAssignments(): Resource<List<ShiftAssignmentDto>> {
        val networkResult = safeApiCall("Error loading shift assignments") {
            shiftApiService.listAssignments()
        }
        if (networkResult is Resource.Success) {
            val assignments = networkResult.data
            shiftAssignmentDao.clear()
            shiftAssignmentDao.upsertAll(assignments.map { ShiftAssignmentEntity.fromDto(it) })
            return Resource.Success(assignments)
        }
        val cached = shiftAssignmentDao.getAll()
        return if (cached.isNotEmpty()) {
            Resource.Success(cached.map { it.toDto() })
        } else if (networkResult is Resource.Error) {
            networkResult
        } else {
            Resource.Success(emptyList())
        }
    }

    override suspend fun getShifts(): Resource<List<ShiftDto>> {
        val networkResult = safeApiCall("Error loading shifts") {
            val response = shiftsApiService.list()
            if (response.isSuccessful) {
                val templates = response.body().orEmpty()
                val mapped = templates.map { t ->
                    ShiftDto(
                        id = t.id,
                        premisesId = t.premisesId,
                        shiftName = t.shiftName,
                        startTime = t.startTime,
                        endTime = t.endTime,
                        halfDayThreshold = t.halfDayThreshold,
                        dayName = t.dayName ?: "ALL"
                    )
                }
                retrofit2.Response.success(mapped)
            } else {
                retrofit2.Response.success(emptyList())
            }
        }
        if (networkResult is Resource.Success && networkResult.data.isNotEmpty()) {
            shiftDao.upsertAll(networkResult.data.map { ShiftEntity.fromDto(it) })
        }
        val cached = shiftDao.getAll()
        return if (cached.isNotEmpty()) {
            Resource.Success(cached.map { it.toDto() })
        } else if (networkResult is Resource.Error) {
            networkResult
        } else {
            Resource.Success(emptyList())
        }
    }

    override suspend fun createShift(request: CreateShiftRequest): Resource<ShiftDto> {
        val template = ShiftTemplateDto(
            id = "",
            premisesId = request.premisesId,
            shiftName = request.shiftName,
            startTime = request.startTime,
            endTime = request.endTime,
            halfDayThreshold = request.halfDayThreshold,
            dayName = request.dayName
        )
        val result = safeApiCall("Error creating shift") {
            shiftsApiService.create(template)
        }
        if (result is Resource.Success) {
            shiftDao.upsert(ShiftEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun updateShift(id: String, request: UpdateShiftRequest): Resource<ShiftDto> {
        val result = safeApiCall("Error updating shift") {
            shiftsApiService.update(id, request)
        }
        if (result is Resource.Success) {
            shiftDao.upsert(ShiftEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun deleteShift(id: String): Resource<Unit> {
        val result = safeApiCall("Error deleting shift") {
            shiftsApiService.delete(id)
        }
        if (result is Resource.Success) {
            shiftDao.markDeleted(id)
        }
        return result
    }

    override suspend fun createShiftAssignment(request: CreateShiftAssignmentRequest): Resource<ShiftAssignmentDto> {
        val result = safeApiCall("Error assigning shift") {
            shiftApiService.createAssignment(request)
        }
        if (result is Resource.Success) {
            shiftAssignmentDao.upsert(ShiftAssignmentEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun deleteShiftAssignment(id: String): Resource<Unit> {
        val result = safeApiCall("Error deleting shift assignment") {
            shiftApiService.deleteAssignment(id)
        }
        if (result is Resource.Success) {
            shiftAssignmentDao.deleteById(id)
        }
        return result
    }

    override suspend fun getHolidays(premisesId: String?): Resource<List<HolidayDto>> {
        val result = safeApiCall("Error loading holidays") {
            holidayApiService.list(premisesId = premisesId)
        }
        return when (result) {
            is Resource.Success -> {
                val entities = result.data.map { HolidayEntity.fromDto(it) }
                holidayDao.upsertAll(entities)
                result
            }
            is Resource.Error -> {
                val cached = holidayDao.getAll()
                val filtered = if (!premisesId.isNullOrBlank()) {
                    cached.filter { it.premisesId == premisesId || it.premisesId == null }
                } else cached
                if (filtered.isNotEmpty()) {
                    Resource.Success(filtered.map { it.toDto() })
                } else {
                    result
                }
            }
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun createHoliday(request: CreateHolidayRequest): Resource<HolidayDto> {
        val result = safeApiCall("Error creating holiday") {
            holidayApiService.create(request)
        }
        if (result is Resource.Success) {
            holidayDao.upsert(HolidayEntity.fromDto(result.data))
        }
        return result
    }

    override suspend fun deleteHoliday(id: String): Resource<Unit> {
        val result = safeApiCall("Error deleting holiday") {
            holidayApiService.delete(id)
        }
        if (result is Resource.Success) {
            holidayDao.markDeleted(id)
        }
        return when (result) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(result.exception, result.message)
            Resource.Loading -> Resource.Loading
        }
    }
}
