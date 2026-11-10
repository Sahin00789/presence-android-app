package com.als.presence.ui.dashboard.owner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine

import com.als.presence.data.model.CreatePremisesRequest
import com.als.presence.data.model.CreateShiftAssignmentRequest
import com.als.presence.data.model.CreateShiftRequest
import com.als.presence.data.model.UpdateShiftRequest
import com.als.presence.data.model.CreateOvertimeRequest
import com.als.presence.data.model.OvertimeDto

import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.CreateEmployee
import com.als.presence.data.model.UpdateEmployeeRequest
import com.als.presence.data.model.OrganizationMemberDto
import com.als.presence.data.model.PayrollItemDto
import com.als.presence.data.model.PayrollPeriodDto
import com.als.presence.data.model.PostOffice
import com.als.presence.data.model.PremisesDto
import com.als.presence.data.model.ShiftAssignmentDto
import com.als.presence.data.model.ShiftTemplateDto
import com.als.presence.data.model.StaffDto
import com.als.presence.data.model.UnworkedPaymentRuleDto
import com.als.presence.data.model.CreateUnworkedPaymentRuleRequest
import com.als.presence.data.model.UpdatePremisesRequest
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import com.als.presence.core.location.LocationManager
import com.als.presence.core.location.ReverseGeocodedAddress
import com.als.presence.core.network.NetworkMonitor
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
    private val networkMonitor: NetworkMonitor,
    val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val syncCoordinator: com.als.presence.data.sync.SyncCoordinator
) : ViewModel() {

    private val _employee = MutableStateFlow<UiState<List<StaffDto>>>(UiState.Loading)
    val staff: StateFlow<UiState<List<StaffDto>>> = _employee.asStateFlow()
    val staffState: StateFlow<UiState<List<StaffDto>>> get() = staff

    private val _faceStatuses = MutableStateFlow<Map<String, FaceStatusDto>>(emptyMap())
    val faceStatuses: StateFlow<Map<String, FaceStatusDto>> = _faceStatuses.asStateFlow()

    private val _faceSubmittingFor = MutableStateFlow<String?>(null)
    val faceSubmittingFor: StateFlow<String?> = _faceSubmittingFor.asStateFlow()

    private val _members = MutableStateFlow<UiState<List<OrganizationMemberDto>>>(UiState.Loading)
    val members: StateFlow<UiState<List<OrganizationMemberDto>>> = _members.asStateFlow()

    private val _premises = MutableStateFlow<UiState<List<PremisesDto>>>(UiState.Loading)
    val premises: StateFlow<UiState<List<PremisesDto>>> = _premises.asStateFlow()
    val premisesState: StateFlow<UiState<List<PremisesDto>>> get() = premises

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
    val shiftAssignmentsState: StateFlow<UiState<List<ShiftAssignmentDto>>> get() = shiftAssignments

    private val _shiftTemplates = MutableStateFlow<UiState<List<ShiftTemplateDto>>>(UiState.Loading)
    val shiftTemplates: StateFlow<UiState<List<ShiftTemplateDto>>> = _shiftTemplates.asStateFlow()
    val shiftTemplatesState: StateFlow<UiState<List<ShiftTemplateDto>>> get() = shiftTemplates

    private val _overtime = MutableStateFlow<UiState<List<OvertimeDto>>>(UiState.Loading)
    val overtime: StateFlow<UiState<List<OvertimeDto>>> = _overtime.asStateFlow()
    val overtimeState: StateFlow<UiState<List<OvertimeDto>>> get() = overtime

    private val _unworkedRules = MutableStateFlow<UiState<List<UnworkedPaymentRuleDto>>>(UiState.Idle)
    val unworkedRules: StateFlow<UiState<List<UnworkedPaymentRuleDto>>> = _unworkedRules.asStateFlow()
    val unworkedRulesState: StateFlow<UiState<List<UnworkedPaymentRuleDto>>> get() = unworkedRules

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    val actionMessage: StateFlow<String?> get() = message

    fun clearActionMessage() {
        _message.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }

    init {
        refresh()
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isOnline.drop(1).collect { online ->
                if (online) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            syncCoordinator.performSelectiveSync(com.als.presence.data.sync.SyncCoordinator.SyncDomain.WORKFORCE)
            loadStaff()
            loadMembers()
            loadPremises()
            loadPayrollData()
            loadShiftsData()
            loadOvertime()
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            if (_employee.value !is UiState.Success) {
                _employee.value = UiState.Loading
            }
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

    fun enrollFaceTemplates(employeeId: String, embeddings: Map<String, FaceEmbeddingEngine.Embedding>) {
        enrollFace(employeeId, embeddings)
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
            if (_members.value !is UiState.Success) {
                _members.value = UiState.Loading
            }
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
            if (_premises.value !is UiState.Success) {
                _premises.value = UiState.Loading
            }
            _premises.value = repository.getPremises().toUiState()
        }
    }

    fun loadPayrollData() {
        viewModelScope.launch {
            if (_payrollPeriods.value !is UiState.Success) {
                _payrollPeriods.value = UiState.Loading
            }
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
            if (_shiftAssignments.value !is UiState.Success) {
                _shiftAssignments.value = UiState.Loading
            }
            if (_shiftTemplates.value !is UiState.Success) {
                _shiftTemplates.value = UiState.Loading
            }
            _shiftAssignments.value = repository.getShiftAssignments().toUiState()

            _shiftTemplates.value = repository.getShifts().toUiState()
        }
    }

    fun recordPayment(itemId: String, amountPaise: Long, paymentMethod: String = "BANK_TRANSFER", reference: String = "") {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.recordPayrollTransaction(
                com.als.presence.data.model.RecordPayrollTransactionRequest(
                    monthlyPayrollId = itemId,
                    amount = amountPaise.toDouble(),
                    paymentDate = java.time.LocalDate.now().toString(),
                    paymentMethod = paymentMethod,
                    reference = reference.ifBlank { null }
                )
            )
            _isSubmitting.value = false
            when (result) {
                is Resource.Success -> {
                    _message.value = "Payment recorded successfully!"
                    refresh()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to record payment"
                }
                else -> {}
            }
        }
    }

    fun addStaff(
        fullName: String,
        email: String,
        department: String?,
        designation: String? = null,
        address: String? = null,
        phone: String? = null,
        avatarBase64: String? = null,
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
                        department = department?.trim()?.ifBlank { null },
                        designation = designation?.trim()?.ifBlank { null },
                        address = address?.trim()?.ifBlank { null },
                        phone = phone?.trim()?.ifBlank { null },
                        avatarBase64 = avatarBase64,
                        role = "EMPLOYEE",
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

    fun updateStaff(
        employeeId: String,
        fullName: String,
        email: String? = null,
        department: String?,
        designation: String? = null,
        address: String? = null,
        phone: String? = null,
        avatarBase64: String? = null,
        premisesId: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updateEmployee(
                    employeeId = employeeId,
                    request = UpdateEmployeeRequest(
                        email = email?.trim()?.ifBlank { null },
                        fullName = fullName.trim(),
                        department = department?.trim()?.ifBlank { null },
                        designation = designation?.trim()?.ifBlank { null },
                        address = address?.trim()?.ifBlank { null },
                        phone = phone?.trim()?.ifBlank { null },
                        avatarBase64 = avatarBase64,
                        premisesId = premisesId
                    )
                )
            ) {
                is Resource.Success<*> -> {
                    _message.value = "Employee profile updated."
                    refresh()
                }
                is Resource.Error -> _message.value = result.message ?: "Failed to update employee profile"
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun revokeStaff(employeeId: String) {
        deactivateMember(employeeId, "Employee")
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

    fun assignShiftWithRequest(request: CreateShiftAssignmentRequest) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.createShiftAssignment(request)) {
                is Resource.Success -> {
                    _message.value = "Shift assigned successfully."
                    loadShiftsData()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun deleteShiftAssignment(assignmentId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.deleteShiftAssignment(assignmentId)) {
                is Resource.Success -> {
                    _message.value = "Shift assignment updated."
                    loadShiftsData()
                }
                is Resource.Error -> _message.value = result.message
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun loadUnworkedRules(employeeId: String? = null, premisesId: String? = null) {
        val cleanEmpId = employeeId?.takeIf { it.isNotBlank() }
        val cleanPremId = premisesId?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            _unworkedRules.value = UiState.Loading
            _unworkedRules.value = repository.getUnworkedRules(cleanEmpId, cleanPremId).toUiState()
        }
    }

    fun saveUnworkedRules(
        employeeId: String?,
        premisesId: String?,
        holidayPay: Long,
        offDayPay: Long
    ) {
        val cleanEmpId = employeeId?.takeIf { it.isNotBlank() }
        val cleanPremId = premisesId?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            _isSubmitting.value = true
            val r1 = repository.saveUnworkedRule(
                CreateUnworkedPaymentRuleRequest(
                    employeeId = cleanEmpId,
                    premisesId = cleanPremId,
                    dayType = "HOLIDAY",
                    paymentInr = holidayPay
                )
            )
            val r2 = repository.saveUnworkedRule(
                CreateUnworkedPaymentRuleRequest(
                    employeeId = cleanEmpId,
                    premisesId = cleanPremId,
                    dayType = "WEEKLY_OFF",
                    paymentInr = offDayPay
                )
            )
            if (r1 is Resource.Success && r2 is Resource.Success) {
                _message.value = "Unwork payment rules saved."
                loadUnworkedRules(cleanEmpId, cleanPremId)
            } else {
                val err = (r1 as? Resource.Error)?.message ?: (r2 as? Resource.Error)?.message ?: "Failed to save unwork payment rules"
                _message.value = err
            }
            _isSubmitting.value = false
        }
    }

    fun assignShiftWithRules(
        staffProfileId: String,
        shiftId: String,
        startDate: String,
        endDate: String,
        paymentRules: Map<String, Long>,
        daysOfWeek: List<Int>
    ) {
        assignShift(staffProfileId, shiftId, startDate, null, daysOfWeek)
    }

    fun loadOvertime() {
        viewModelScope.launch {
            if (_overtime.value !is UiState.Success) {
                _overtime.value = UiState.Loading
            }
            _overtime.value = repository.getOvertime().toUiState()
        }
    }

    fun createOvertime(
        employeeId: String,
        workDate: String,
        startTime: String,
        endTime: String,
        paymentInr: Long,
        reason: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createOvertime(
                    CreateOvertimeRequest(
                        staffProfileId = employeeId,
                        workDate = workDate,
                        startTime = startTime,
                        endTime = endTime,
                        paymentInr = paymentInr,
                        reason = reason
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Overtime assigned to employee successfully."
                    loadOvertime()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to book overtime"
                }
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun cancelOvertime(overtimeId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.cancelOvertime(overtimeId)) {
                is Resource.Success -> {
                    _message.value = "Overtime entry cancelled."
                    loadOvertime()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to cancel overtime"
                }
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun createShift(
        premisesId: String,
        shiftName: String,
        startTime: String,
        endTime: String,
        dayName: String,
        halfDayThreshold: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.createShift(
                    CreateShiftRequest(
                        premisesId = premisesId,
                        shiftName = shiftName,
                        startTime = startTime,
                        endTime = endTime,
                        dayName = dayName,
                        halfDayThreshold = halfDayThreshold
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift created successfully."
                    loadShiftsData()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to create shift"
                }
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun updateShift(
        id: String,
        shiftName: String,
        startTime: String,
        endTime: String,
        dayName: String,
        halfDayThreshold: String,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val result = repository.updateShift(
                    id,
                    UpdateShiftRequest(
                        shiftName = shiftName,
                        startTime = startTime,
                        endTime = endTime,
                        dayName = dayName,
                        halfDayThreshold = halfDayThreshold,
                        isActive = isActive
                    )
                )
            ) {
                is Resource.Success -> {
                    _message.value = "Shift updated successfully."
                    loadShiftsData()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to update shift"
                }
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }

    fun deleteShift(id: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.deleteShift(id)) {
                is Resource.Success -> {
                    _message.value = "Shift deleted successfully."
                    loadShiftsData()
                }
                is Resource.Error -> {
                    _message.value = result.message ?: "Failed to delete shift"
                }
                Resource.Loading -> Unit
            }
            _isSubmitting.value = false
        }
    }
}
