package com.als.presence.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.location.GpsLocation
import com.als.presence.data.model.OtpResponse
import com.als.presence.data.model.PostOffice
import com.als.presence.domain.model.LoggedDevice
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import com.als.presence.domain.usecase.auth.GoogleExchangeUseCase
import com.als.presence.domain.usecase.auth.LogoutUseCase
import com.als.presence.data.local.SessionManager
import com.als.presence.domain.usecase.auth.ObserveCurrentUserUseCase
import com.als.presence.domain.usecase.auth.RequestOtpUseCase
import com.als.presence.domain.usecase.auth.VerifyOtpUseCase
import com.als.presence.domain.usecase.device.GetLoggedDevicesUseCase
import com.als.presence.domain.usecase.device.RevokeDeviceUseCase
import com.als.presence.domain.usecase.location.GetGpsLocationUseCase
import com.als.presence.domain.usecase.location.LookupPincodeUseCase
import com.als.presence.domain.usecase.owner.RegisterOwnerWithGoogleUseCase
import com.als.presence.domain.repository.AuthRepository
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.core.network.NetworkMonitor
import com.als.presence.data.model.OrganizationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val operationsRepository: OperationsRepository,
    private val sessionManager: SessionManager,
    private val requestOtpUseCase: RequestOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val googleExchangeUseCase: GoogleExchangeUseCase,
    private val registerOwnerWithGoogleUseCase: RegisterOwnerWithGoogleUseCase,
    private val getLoggedDevicesUseCase: GetLoggedDevicesUseCase,
    private val revokeDeviceUseCase: RevokeDeviceUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val lookupPincodeUseCase: LookupPincodeUseCase,
    private val getGpsLocationUseCase: GetGpsLocationUseCase,
    observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val currentUser: StateFlow<User?> = observeCurrentUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    private val _updateProfileState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val updateProfileState: StateFlow<UiState<User>> = _updateProfileState.asStateFlow()

    private val _updateOrgState = MutableStateFlow<UiState<OrganizationDto>>(UiState.Idle)
    val updateOrgState: StateFlow<UiState<OrganizationDto>> = _updateOrgState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpResponse?>(null)
    val otpState: StateFlow<OtpResponse?> = _otpState.asStateFlow()

    private val _loggedDevicesState = MutableStateFlow<UiState<List<LoggedDevice>>>(UiState.Idle)
    val loggedDevicesState: StateFlow<UiState<List<LoggedDevice>>> = _loggedDevicesState.asStateFlow()

    private val _pincodeState = MutableStateFlow<UiState<List<PostOffice>>>(UiState.Idle)
    val pincodeState: StateFlow<UiState<List<PostOffice>>> = _pincodeState.asStateFlow()

    private val _gpsLocationState = MutableStateFlow<UiState<GpsLocation>>(UiState.Idle)
    val gpsLocationState: StateFlow<UiState<GpsLocation>> = _gpsLocationState.asStateFlow()

    fun requestOtp(email: String, role: Role = Role.STAFF) {
        viewModelScope.launch {
            if (!isOnline.value) {
                _uiState.value = UiState.Error(
                    message = "Server is not connected. Device is currently offline."
                )
                return@launch
            }
            _uiState.value = UiState.Loading
            when (val result = requestOtpUseCase(email, role.name)) {
                is Resource.Success -> {
                    _otpState.value = result.data
                    _uiState.value = UiState.Idle
                }
                is Resource.Error -> {
                    val errorMsg = result.message?.takeIf { it.isNotBlank() }
                        ?: "Failed to send verification code. Server is not connected."
                    _uiState.value = UiState.Error(message = errorMsg)
                }
                is Resource.Loading -> {
                    _uiState.value = UiState.Loading
                }
            }
        }
    }

    fun verifyOtp(
        email: String,
        code: String,
        role: Role = Role.STAFF,
        fullName: String? = null,
        organizationName: String? = null,
        timezone: String? = null,
        currency: String? = null,
        premisesName: String? = null,
        premisesAddress: String? = null,
        latitude: String? = null,
        longitude: String? = null,
        radiusMeters: Int? = null
    ) {
        viewModelScope.launch {
            if (!isOnline.value) {
                _uiState.value = UiState.Error(
                    message = "Server is not connected. Device is currently offline."
                )
                return@launch
            }
            _uiState.value = UiState.Loading
            when (val result = verifyOtpUseCase(
                email = email,
                otp = code,
                role = role.name,
                fullName = fullName,
                organizationName = organizationName,
                timezone = timezone,
                currency = currency,
                premisesName = premisesName,
                premisesAddress = premisesAddress,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters
            )) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Resource.Error -> {
                    val errorMsg = result.message?.takeIf { it.isNotBlank() }
                        ?: "OTP Verification failed. Server is not connected."
                    _uiState.value = UiState.Error(message = errorMsg)
                }
                is Resource.Loading -> {
                    _uiState.value = UiState.Loading
                }
            }
        }
    }

    fun loginWithGoogle(code: String, redirectUri: String? = null) {
        viewModelScope.launch {
            if (!isOnline.value) {
                _uiState.value = UiState.Error(
                    message = "Server is not connected. Device is currently offline."
                )
                return@launch
            }
            _uiState.value = UiState.Loading
            when (val result = googleExchangeUseCase(code = code, redirectUri = redirectUri ?: "")) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Resource.Error -> {
                    val errorMsg = result.message?.takeIf { it.isNotBlank() }
                        ?: "Google login failed. Server is not connected."
                    _uiState.value = UiState.Error(message = errorMsg)
                }
                is Resource.Loading -> {
                    _uiState.value = UiState.Loading
                }
            }
        }
    }

    fun registerOwnerWithGoogle(
        idToken: String,
        organizationName: String,
        timezone: String,
        currency: String,
        premisesName: String,
        radiusMeters: Int,
        premisesAddress: String? = null,
        latitude: String? = null,
        longitude: String? = null
    ) {
        viewModelScope.launch {
            if (!isOnline.value) {
                _uiState.value = UiState.Error(
                    message = "Server is not connected. Device is currently offline."
                )
                return@launch
            }
            _uiState.value = UiState.Loading
            when (val result = registerOwnerWithGoogleUseCase(
                idToken = idToken,
                orgName = organizationName,
                timezone = timezone,
                currency = currency,
                premisesName = premisesName,
                radius = radiusMeters,
                premisesAddress = premisesAddress,
                latitude = latitude,
                longitude = longitude
            )) {
                is Resource.Success -> _uiState.value = UiState.Success(result.data)
                is Resource.Error -> {
                    val errorMsg = result.message?.takeIf { it.isNotBlank() }
                        ?: "Owner registration failed. Server is not connected."
                    _uiState.value = UiState.Error(message = errorMsg)
                }
                is Resource.Loading -> _uiState.value = UiState.Loading
            }
        }
    }

    fun lookupPincode(pincode: String) {
        viewModelScope.launch {
            _pincodeState.value = UiState.Loading
            when (val result = lookupPincodeUseCase(pincode)) {
                is Resource.Success -> _pincodeState.value = UiState.Success(result.data)
                is Resource.Error -> _pincodeState.value = UiState.Error(result.message ?: "Failed to lookup pincode")
                is Resource.Loading -> _pincodeState.value = UiState.Loading
            }
        }
    }

    fun tryAutoLogin() {
        val currentRefreshToken = sessionManager.refreshToken
        if (currentRefreshToken.isNullOrBlank()) return
        viewModelScope.launch {
            if (!isOnline.value) return@launch
            _uiState.value = UiState.Loading
            when (val result = authRepository.refreshToken()) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Idle
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun fetchGpsLocation() {
        viewModelScope.launch {
            _gpsLocationState.value = UiState.Loading
            when (val result = getGpsLocationUseCase()) {
                is Resource.Success -> _gpsLocationState.value = UiState.Success(result.data)
                is Resource.Error -> _gpsLocationState.value = UiState.Error(result.message ?: "Failed to fetch GPS coordinates")
                is Resource.Loading -> _gpsLocationState.value = UiState.Loading
            }
        }
    }

    fun updateProfile(fullName: String? = null, phone: String? = null, avatarBase64: String? = null) {
        viewModelScope.launch {
            _updateProfileState.value = UiState.Loading
            when (val res = authRepository.updateProfile(fullName, phone, avatarBase64)) {
                is Resource.Success -> {
                    _updateProfileState.value = UiState.Success(res.data)
                }
                is Resource.Error -> {
                    _updateProfileState.value = UiState.Error(res.message ?: "Failed to update profile")
                }
                is Resource.Loading -> _updateProfileState.value = UiState.Loading
            }
        }
    }

    fun updateOrganization(name: String? = null, timezone: String? = null, currency: String? = null, logoBase64: String? = null) {
        viewModelScope.launch {
            _updateOrgState.value = UiState.Loading
            when (val res = operationsRepository.updateOrganization(name, timezone, currency, logoBase64)) {
                is Resource.Success -> {
                    _updateOrgState.value = UiState.Success(res.data)
                }
                is Resource.Error -> {
                    _updateOrgState.value = UiState.Error(res.message ?: "Failed to update organization")
                }
                is Resource.Loading -> _updateOrgState.value = UiState.Loading
            }
        }
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = UiState.Idle
    }

    fun resetUpdateOrgState() {
        _updateOrgState.value = UiState.Idle
    }

    fun fetchLoggedDevices() {
        viewModelScope.launch {
            _loggedDevicesState.value = UiState.Loading
            when (val result = getLoggedDevicesUseCase()) {
                is Resource.Success -> {
                    _loggedDevicesState.value = UiState.Success(result.data)
                }
                is Resource.Error -> {
                    _loggedDevicesState.value = UiState.Error(
                        message = result.message ?: "Failed to fetch logged devices"
                    )
                }
                is Resource.Loading -> {
                    _loggedDevicesState.value = UiState.Loading
                }
            }
        }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            when (val result = revokeDeviceUseCase(deviceId)) {
                is Resource.Success -> {
                    fetchLoggedDevices()
                }
                is Resource.Error -> {
                    _loggedDevicesState.value = UiState.Error(
                        message = result.message ?: "Failed to revoke device session"
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = UiState.Idle
            _otpState.value = null
            _loggedDevicesState.value = UiState.Idle
            _pincodeState.value = UiState.Idle
            _gpsLocationState.value = UiState.Idle
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }

    fun showError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    fun resetOtp() {
        _otpState.value = null
    }
}
