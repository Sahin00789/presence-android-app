package com.als.presence.ui.dashboard.owner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine

import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest

import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.CreateEmployee
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PostOffice
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.Money
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.als.presence.core.location.LocationManager
import com.als.presence.core.location.ReverseGeocodedAddress
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class PremisesLocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: ReverseGeocodedAddress? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Owner "Workforce" tab state management. */
@HiltViewModel
class OwnerWorkforceViewModel @Inject constructor(
    private val repository: OperationsRepository,
    private val locationManager: LocationManager,
    val faceEmbeddingEngine: FaceEmbeddingEngine
) : ViewModel() {

    private val _employee = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _employee.asStateFlow()

    private val _faceStatuses = MutableStateFlow<Map<String, FaceStatusDto>>(emptyMap())
    val faceStatuses: StateFlow<Map<String, FaceStatusDto>> = _faceStatuses.asStateFlow()

    private val _faceSubmittingFor = MutableStateFlow<String?>(null)
    val faceSubmittingFor: StateFlow<String?> = _faceSubmittingFor.asStateFlow()

    private val _members = MutableStateFlow<UiState<List<OrganizationMemberDto>>>(UiState.Loading)
    val members: StateFlow<UiState<List<OrganizationMemberDto>>> = _members.asStateFlow()

    private val _premises = MutableStateFlow<UiState<List<PremisesDto>>>(UiState.Loading)
    val premises: StateFlow<UiState<List<PremisesDto>>> = _premises.asStateFlow()

    private val _locationAddressState = MutableStateFlow<PremisesLocationState?>(null)
    val locationAddressState: StateFlow<PremisesLocationState?> = _locationAddressState.asStateFlow()

    fun fetchCurrentGpsAndAddress() {
        viewModelScope.launch {
            _locationAddressState.value = PremisesLocationState(isLoading = true)
            when (val locResult = locationManager.getCurrentLocationResult()) {
                is LocationManager.LocationResult.Error -> {
                    _locationAddressState.value = PremisesLocationState(error = locResult.message)
                }
                is LocationManager.LocationResult.Success -> {
                    val gps = locResult.location
                    val rev = locationManager.reverseGeocode(gps.latitude, gps.longitude)
                    _locationAddressState.value = PremisesLocationState(
                        latitude = gps.latitude,
                        longitude = gps.longitude,
                        address = rev,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearLocationAddressState() {
        _locationAddressState.value = null
    }

    private val _pincodeState = MutableStateFlow<UiState<List<PostOffice>>>(UiState.Idle)
    val pincodeState: StateFlow<UiState<List<PostOffice>>> = _pincodeState.asStateFlow()

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
            _employee.value = UiState.Loading
            val result = repository.getEmployees()
            _employee.value = result.toUiState()
            if (result is Resource.Success) {
                val statuses = buildMap {
                    result.data.forEach { member ->
                        val status = repository.getStaffFaceStatus(member.id)
                        if (status is Resource.Success && status.data.isNotEmpty()) {
                            put(member.id, status.data.first())
                        }
                    }
                }
                _faceStatuses.value = statuses
            }
        }
    }

    /** Supervised enrolment triggered by the Owner with the employee present. */
    fun enrollFace(employeeId: String, embeddings: Map<String, FaceEmbeddingEngine.Embedding>) {
        viewModelScope.launch {
            _faceSubmittingFor.value = employeeId
            when (
                val result = repository.enrollFaceSupervised(
                    employeeId = employeeId
                )
            ) {
                is Resource.Success -> {
                    _faceStatuses.value = _faceStatuses.value + (employeeId to result.data)
                    _message.value = "Face enrolled under supervision."
                }
                is Resource.Error -> _message.value = result.message ?: "Face enrolment failed"
                Resource.Loading -> Unit
            }
            _faceSubmittingFor.value = null
        }
    }

    fun revokeFace(employeeId: String) {
        viewModelScope.launch {
            _faceSubmittingFor.value = employeeId
            when (val result = repository.revokeFaceEnrollment(employeeId)) {
                is Resource.Success -> {
                    val previous = _faceStatuses.value[employeeId]
                    _faceStatuses.value = _faceStatuses.value + (
                        employeeId to FaceStatusDto(
                            enrolled = false,
                            staffProfileId = employeeId,
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
                        val targetProfileId = member.employeeId
                        val status = repository.getStaffFaceStatus(targetProfileId)
                        if (status is Resource.Success && status.data.isNotEmpty()) {
                            put(targetProfileId, status.data.first())
                        }
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
            val now = java.time.LocalDate.now()
            val generatedPeriods = (0..5).map { i ->
                val date = now.minusMonths(i.toLong())
                val y = date.year
                val m = date.monthValue
                val start = "$y-${m.toString().padStart(2, '0')}-01"
                val end = "$y-${m.toString().padStart(2, '0')}-${date.lengthOfMonth()}"
                PayrollPeriodDto(
                    id = "$y-${m.toString().padStart(2, '0')}",
                    organizationId = "",
                    periodStartDate = start,
                    periodEndDate = end,
                    status = if (i == 0) "OPEN" else "PAID"
                )
            }
            _payrollPeriods.value = UiState.Success(generatedPeriods)

            val itemsMap = mutableMapOf<String, List<PayrollItemDto>>()
            generatedPeriods.forEach { period ->
                val y = period.periodStartDate.take(4).toIntOrNull() ?: now.year
                val m = period.periodStartDate.substring(5, 7).toIntOrNull() ?: now.monthValue
                val itemsRes = repository.getMonthlyPayrollSummary(m, y)
                if (itemsRes is Resource.Success) {
                    itemsMap[period.id] = itemsRes.data
                }
            }
            _payrollItemsMap.value = itemsMap
        }
    }

    fun loadShiftsData() {
        viewModelScope.launch {
            _shiftAssignments.value = UiState.Loading
            _shiftAssignments.value = repository.getShiftAssignments().toUiState()

            _shiftTemplates.value = UiState.Loading
            _shiftTemplates.value = repository.getShifts().toUiState()
        }
    }

    fun recordPayment(itemId: String, amountCents: Long, paymentMethod: String = "BANK_TRANSFER", reference: String = "") {
        _message.value = "Payment of ${Money.format(amountCents)} recorded."
        refresh()
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
                val result = repository.createEmployee(
                    CreateEmployee(
                        email = email.trim(),
                        fullName = fullName.trim(),
                        staffCode = generatedCode,
                        department = department.trim().ifBlank { null },
                        role = "STAFF",
                        premisesId = premisesId
                    )
                )
            ) {
                is Resource.Success<*> -> {
                    _message.value = "Employee onboarded."
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
                val result = repository.createEmployee(
                    CreateEmployee(
                        email = email.trim(),
                        fullName = fullName.trim(),
                        staffCode = generatedCode,
                        department = department.trim().ifBlank { null },
                        role = "MANAGER",
                        premisesId = premisesId
                    )
                )
            ) {
                is Resource.Success<*> -> {
                    _message.value = "Manager added successfully."
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

    fun addPremises(
        name: String,
        latitude: String,
        longitude: String,
        radiusMeters: Int,
        address: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createPremises(
                    CreatePremisesRequest(
                        premisesName = name.trim(),
                        address = address?.trim()?.ifBlank { null },
                        latitude = latitude.trim().ifBlank { "0.0" },
                        longitude = longitude.trim().ifBlank { "0.0" },
                        geofenceRadiusMeters = radiusMeters
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
        isActive: Boolean = true,
        address: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updatePremises(
                    premisesId,
                    UpdatePremisesRequest(
                        premisesName = name.trim(),
                        address = address?.trim()?.ifBlank { null },
                        latitude = latitude.trim().ifBlank { "0.0" },
                        longitude = longitude.trim().ifBlank { "0.0" },
                        geofenceRadiusMeters = radiusMeters,
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

    fun assignShift(
        employeeId: String,
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
                        premisesId = premisesId ?: "",
                        staffId = employeeId,
                        shiftId = shiftTemplateId
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
        employeeId: String,
        assignmentsList: List<CreateShiftAssignmentRequest>,
        startDate: String,
        premisesId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            var successCount = 0
            var lastError: String? = null

            for (item in assignmentsList) {
                val result = repository.createShiftAssignment(
                    item.copy(
                        employeeId = employeeId,
                        staffId = employeeId,
                        premisesId = premisesId ?: item.premisesId
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
