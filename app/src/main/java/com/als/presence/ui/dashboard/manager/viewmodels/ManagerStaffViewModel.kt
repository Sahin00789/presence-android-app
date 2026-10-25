package com.als.presence.ui.dashboard.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.InviteStaffRequest
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.StaffDto
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

import com.als.presence.data.remote.DomainWebSocketEvent
import com.als.presence.data.remote.EventsWebSocketService

/** Manager "Staff" tab: roster management plus supervised face enrolment. */
@HiltViewModel
class ManagerStaffViewModel @Inject constructor(
    private val repository: OperationsRepository,
    val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val webSocketService: EventsWebSocketService
) : ViewModel() {

    private val _staff = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _staff.asStateFlow()

    private val _faceStatuses = MutableStateFlow<Map<String, FaceStatusDto>>(emptyMap())
    val faceStatuses: StateFlow<Map<String, FaceStatusDto>> = _faceStatuses.asStateFlow()

    private val _faceSubmittingFor = MutableStateFlow<String?>(null)
    val faceSubmittingFor: StateFlow<String?> = _faceSubmittingFor.asStateFlow()

    private val _payrollPeriods = MutableStateFlow<UiState<List<PayrollPeriodDto>>>(UiState.Loading)
    val payrollPeriods: StateFlow<UiState<List<PayrollPeriodDto>>> = _payrollPeriods.asStateFlow()

    private val _payrollItemsMap = MutableStateFlow<Map<String, List<PayrollItemDto>>>(emptyMap())
    val payrollItemsMap: StateFlow<Map<String, List<PayrollItemDto>>> = _payrollItemsMap.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                if (event is DomainWebSocketEvent.StaffUpdate ||
                    event is DomainWebSocketEvent.WalletUpdate ||
                    event is DomainWebSocketEvent.PremisesUpdate
                ) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        loadStaff()
        loadPayrollData()
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

    /**
     * Owner/Manager captures the face with the employee present. Staff can never
     * trigger this from their own device.
     */
    fun enrollFace(staffProfileId: String, embeddings: Map<String, FaceEmbeddingEngine.Embedding>) {
        viewModelScope.launch {
            _faceSubmittingFor.value = staffProfileId
            val center = embeddings["CENTER"] ?: return@launch
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
                    _message.value = "Face enrolment revoked. The employee must be re-enrolled in person."
                }
                is Resource.Error -> _message.value = result.message ?: "Could not revoke face enrolment"
                Resource.Loading -> Unit
            }
            _faceSubmittingFor.value = null
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
                    _message.value = "${result.data.fullName} added to the roster."
                    loadStaff()
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
                    loadStaff()
                }
                is Resource.Error -> _message.value = result.message ?: "Could not deactivate account"
                Resource.Loading -> Unit
            }
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
                is Resource.Success -> {
                    _message.value = "Payment rules updated to ${Money.format(baseCents)}/shift."
                    loadStaff()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
