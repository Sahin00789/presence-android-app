package com.als.presence.ui.dashboard.employee.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.core.common.Resource
import com.als.presence.core.common.UiState
import com.als.presence.core.face.FaceEmbeddingEngine
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.FaceStatusDto
import com.als.presence.data.model.FaceVerifyResponseDto
import com.als.presence.domain.repository.OperationsRepository
import com.als.presence.ui.dashboard.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Employee Biometric Check screen.
 * Allows employees to check their facial biometric enrollment status and test live face matching against their enrolled template.
 */
@HiltViewModel
class EmployeeBiometricViewModel @Inject constructor(
    private val repository: OperationsRepository,
    val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _faceStatus = MutableStateFlow<UiState<FaceStatusDto>>(UiState.Loading)
    val faceStatus: StateFlow<UiState<FaceStatusDto>> = _faceStatus.asStateFlow()

    private val _verifyResult = MutableStateFlow<FaceVerifyResponseDto?>(null)
    val verifyResult: StateFlow<FaceVerifyResponseDto?> = _verifyResult.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val currentUserId: String? get() = sessionManager.userId

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            _faceStatus.value = UiState.Loading
            _faceStatus.value = repository.getFaceStatus().toUiState()
        }
    }

    fun testFaceVerification(embedding: FaceEmbeddingEngine.Embedding) {
        viewModelScope.launch {
            _isVerifying.value = true
            _errorMessage.value = null
            _verifyResult.value = null
            when (val result = repository.verifyFace(embedding)) {
                is Resource.Success -> {
                    _verifyResult.value = result.data
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                }
                Resource.Loading -> Unit
            }
            _isVerifying.value = false
        }
    }

    fun clearVerifyResult() {
        _verifyResult.value = null
        _errorMessage.value = null
    }
}
