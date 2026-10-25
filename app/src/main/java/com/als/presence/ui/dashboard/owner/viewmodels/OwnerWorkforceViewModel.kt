package com.als.presence.ui.dashboard.owner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.CreateInvitationRequest
import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.DayShiftAssignmentRequest
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.InviteStaffRequest
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.data.model.UpsertSalaryRequest
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Dates
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Owner "Workforce" tab state management. */
@HiltViewModel
class OwnerWorkforceViewModel @Inject constructor(
    private val repository: OperationsRepository,
    val faceEmbeddingEngine: FaceEmbeddingEngine
) : ViewModel() {

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

    private val _faceStatuses = MutableStateFlow<Map<String, FaceStatusDto>>(emptyMap())
    val faceStatuses: StateFlow<Map<String, FaceStatusDto>> = _faceStatuses.asStateFlow()

    private val _faceSubmittingFor = MutableStateFlow<String?>(null)
    val faceSubmittingFor: StateFlow<String?> = _faceSubmittingFor.asStateFlow()

    private val _members = MutableStateFlow<UiState<List<OrganizationMemberDto>>>(UiState.Loading)
    val members: StateFlow<UiState<List<OrganizationMemberDto>>> = _members.asStateFlow()

    private val _premises = MutableStateFlow<UiState<List<PremisesDto>>>(UiState.Loading)
    val premises: StateFlow<UiState<List<PremisesDto>>> = _premises.asStateFlow()

    private val _payrollPeriods = MutableStateFlow<UiState<List<PayrollPeriodDto>>>(UiState.Loading)
    val payrollPeriods: StateFlow<UiState<List<PayrollPeriodDto>>> = _payrollPeriods.asStateFlow()

    private val _payrollItemsMap = MutableStateFlow<Map<String, List<PayrollItemDto>>>(emptyMap())
    val payrollItemsMap: StateFlow<Map<String, List<PayrollItemDto>>> = _payrollItemsMap.asStateFlow()

    private val _shiftAssignments = MutableStateFlow<UiState<List<ShiftAssignmentDto>>>(UiState.Loading)
    val shiftAssignments: StateFlow<UiState<List<ShiftAssignmentDto>>> = _shiftAssignments.asStateFlow()

    private val _shiftTemplates = MutableStateFlow<UiState<List<ShiftTemplateDto>>>(UiState.Loading)
    val shiftTemplates: StateFlow<UiState<List<ShiftTemplateDto>>> = _shiftTemplates.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadStaff()
        loadMembers()
        loadPremises()
        loadPayrollData()
        loadShiftsData()
    }

    fun loadStaff() {
        viewModelScope.launch {
            _staff.value = UiState.Loading
            val result = repository.getStaff()
            _staff.value = result.toUiState()
            if (result is Resource.Success) {
                val statuses = buildMap {
                    result.data.forEach { member ->
                        val status = repository.getStaffFaceStatus(member.id)
                        if (status is Resource.Success) put(member.id, status.data)
                    }
                }
                _faceStatuses.value = statuses
            }
        }
    }

    /** Supervised enrolment triggered by the Owner with the employee present. */
    fun enrollFace(staffProfileId: String, embeddings: Map<String, FaceEmbeddingEngine.Embedding>) {
        viewModelScope.launch {
            _faceSubmittingFor.value = staffProfileId
            val center = embeddings["CENTER"] ?: return@launch
            
            // Extract the Double vectors from the Engine's Embedding wrapper
            val vectorMap = embeddings.mapValues { it.value.vector }
            
            when (
                val result = repository.enrollFaceSupervised(
                    staffProfileId = staffProfileId,
                    embeddings = vectorMap,
                    modelVersion = center.modelVersion,
                    qualityScore = center.qualityScore
                )
            ) {
                is Resource.Success -> {
                    _faceStatuses.value = _faceStatuses.value + (staffProfileId to result.data)
                    _message.value = "Face enrolled under supervision."
                }
                is Resource.Error -> _message.value = result.message ?: "Face enrolment failed"
                Resource.Loading -> Unit
            }
            _faceSubmittingFor.value = null
        }
    }

    fun revokeFace(staffProfileId: String) {
        viewModelScope.launch {
            _faceSubmittingFor.value = staffProfileId
            when (val result = repository.revokeFaceEnrollment(staffProfileId)) {
                is Resource.Success -> {
                    val previous = _faceStatuses.value[staffProfileId]
                    _faceStatuses.value = _faceStatuses.value + (
                        staffProfileId to FaceStatusDto(
                            enrolled = false,
                            staffProfileId = staffProfileId,
                            enrolledAt = previous?.enrolledAt,
                            embeddingDimensions = previous?.embeddingDimensions,
                            revokedAt = Instant.now().toString()
                        )
                    )
                    _message.value = "Face enrolment revoked."
                }
                is Resource.Error -> _message.value = result.message ?: "Could not revoke face enrolment"
                Resource.Loading -> Unit
            }
            _faceSubmittingFor.value = null
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _members.value = UiState.Loading
            val result = repository.getMembers()
            _members.value = result.toUiState()
            if (result is Resource.Success) {
                val statuses = buildMap {
                    result.data.forEach { member ->
                        val targetProfileId = member.staffProfileId ?: member.userId
                        val status = repository.getStaffFaceStatus(targetProfileId)
                        if (status is Resource.Success) put(targetProfileId, status.data)
                    }
                }
                _faceStatuses.value = _faceStatuses.value + statuses
            }
        }
    }

    fun loadPremises() {
        viewModelScope.launch {
            _premises.value = UiState.Loading
            _premises.value = repository.getPremises().toUiState()
        }
    }

    fun loadPayrollData() {
        viewModelScope.launch {
            _payrollPeriods.value = UiState.Loading
            when (val result = repository.getPayrollPeriods()) {
                is Resource.Success -> {
                    _payrollPeriods.value = UiState.Success(result.data)
                    val itemsMap = mutableMapOf<String, List<PayrollItemDto>>()
                    result.data.forEach { period ->
                        val itemsRes = repository.getPayrollItems(period.id)
                        if (itemsRes is Resource.Success) {
                            itemsMap[period.id] = itemsRes.data
                        }
                    }
                    _payrollItemsMap.value = itemsMap
                }
                is Resource.Error -> {
                    _payrollPeriods.value = UiState.Error(result.message ?: "Could not load payroll records")
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun loadShiftsData() {
        viewModelScope.launch {
            _shiftAssignments.value = UiState.Loading
            _shiftAssignments.value = repository.getShiftAssignments().toUiState()

            _shiftTemplates.value = UiState.Loading
            _shiftTemplates.value = repository.getShiftTemplates().toUiState()
        }
    }

    fun recordPayment(itemId: String, amountCents: Long, paymentMethod: String = "BANK_TRANSFER", reference: String = "") {
        val method = paymentMethod.ifBlank { "BANK_TRANSFER" }
        val ref = reference.trim().ifEmpty { "Salary Payment via $method" }
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.recordPayment(
                    itemId = itemId,
                    amountCents = amountCents,
                    paymentMethod = method,
                    referenceNumber = ref
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Payment of ${Money.format(amountCents)} recorded."
                    refresh()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun addStaff(
        fullName: String,
        email: String,
        department: String,
        address: String? = null,
        premisesId: String? = null
    ) {
        val generatedCode = "EMP-${(1000..9999).random()}"
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.inviteStaff(
                    InviteStaffRequest(
                        email = email.trim(),
                        fullName = fullName.trim(),
                        staffCode = generatedCode,
                        department = department.trim().ifBlank { null },
                        role = "STAFF",
                        premisesId = premisesId
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "${result.data.fullName} onboarded."
                    refresh()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun inviteManager(
        fullName: String,
        email: String,
        department: String,
        address: String? = null,
        premisesId: String? = null
    ) {
        val generatedCode = "MGR-${(1000..9999).random()}"
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.inviteStaff(
                    InviteStaffRequest(
                        email = email.trim(),
                        fullName = fullName.trim(),
                        staffCode = generatedCode,
                        department = department.trim().ifBlank { null },
                        role = "MANAGER",
                        premisesId = premisesId
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "${result.data.fullName} added as Manager."
                    refresh()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun deactivateMember(userId: String, name: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.deactivateMember(userId)) {
                is Resource.Success -> {
                    _message.value = "$name account deactivated."
                    refresh()
                }
                is Resource.Error -> _message.value = result.message ?: "Could not deactivate account"
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun addPremises(name: String, latitude: String, longitude: String, radiusMeters: Int) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createPremises(
                    CreatePremisesRequest(
                        name = name.trim(),
                        latitude = latitude.trim(),
                        longitude = longitude.trim(),
                        radiusMeters = radiusMeters
                    )
                )
            ) {
                is Resource.Success -> _message.value = "Premises “${result.data.name}” saved."
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            loadPremises()
            _isSubmitting.value = false
        }
    }

    fun updatePremises(
        premisesId: String,
        name: String,
        latitude: String,
        longitude: String,
        radiusMeters: Int,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updatePremises(
                    premisesId,
                    UpdatePremisesRequest(
                        name = name.trim(),
                        latitude = latitude.trim(),
                        longitude = longitude.trim(),
                        radiusMeters = radiusMeters,
                        isActive = isActive
                    )
                )
            ) {
                is Resource.Success -> _message.value = "Premises “${result.data.name}” updated."
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            loadPremises()
            _isSubmitting.value = false
        }
    }

    fun setSalary(
        staffProfileId: String,
        shiftRate: String
    ) {
        val baseCents = Money.parseToCents(shiftRate) ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.setSalary(
                    staffProfileId,
                    UpsertSalaryRequest(
                        compensationType = "SHIFT",
                        baseRateCents = baseCents,
                        overtimeRateCents = 0,
                        effectiveFrom = Dates.todayIso()
                    )
                )
            ) {
                is Resource.Success -> _message.value = "Payment rules updated to ${Money.format(baseCents)}/shift."
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            loadStaff()
            _isSubmitting.value = false
        }
    }

    fun assignShift(
        staffProfileId: String,
        shiftTemplateId: String,
        startDate: String,
        premisesId: String? = null,
        daysOfWeek: List<Int>? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShiftAssignment(
                    CreateShiftAssignmentRequest(
                        staffProfileId = staffProfileId,
                        shiftTemplateId = shiftTemplateId,
                        premisesId = premisesId,
                        startDate = startDate,
                        daysOfWeek = daysOfWeek
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift assigned successfully."
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun assignShifts(
        staffProfileId: String,
        assignmentsList: List<DayShiftAssignmentRequest>,
        startDate: String,
        premisesId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            var successCount = 0
            var lastError: String? = null

            for (item in assignmentsList) {
                val paymentCents = item.presentPay?.let { Money.parseToCents(it) }
                val result = repository.createShiftAssignment(
                    CreateShiftAssignmentRequest(
                        staffProfileId = staffProfileId,
                        shiftTemplateId = item.shiftTemplateId,
                        premisesId = premisesId,
                        startDate = startDate,
                        paymentCents = paymentCents,
                        daysOfWeek = item.daysOfWeek
                    )
                )
                when (result) {
                    is Resource.Success -> successCount++
                    is Resource.Error -> lastError = result.message
                    Resource.Loading -> Unit
                }
            }

            if (successCount > 0) {
                _message.value = "Assigned $successCount shift schedule(s) successfully."
            } else if (lastError != null) {
                _message.value = lastError
            }
            _isSubmitting.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
