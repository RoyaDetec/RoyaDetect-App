package com.example.royadetect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.royadetect.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CropDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropDetailsUiState())
    val uiState: StateFlow<CropDetailsUiState> = _uiState.asStateFlow()

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun saveCropDetails() {
        // Solo redirige al login sin hacer ninguna operación
        _uiState.value = _uiState.value.copy(
            shouldNavigateToLogin = true
        )
    }

    fun skipCropDetails() {
        _uiState.value = _uiState.value.copy(
            shouldNavigateToLogin = true
        )
    }

    private fun validateInput(cropName: String, cropArea: Double): Boolean {
        val cropNameError = when {
            cropName.isBlank() -> "El nombre del cultivo es requerido"
            cropName.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            else -> null
        }

        val cropAreaError = when {
            cropArea <= 0 -> "El área debe ser mayor a 0"
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            cropNameError = cropNameError,
            cropAreaError = cropAreaError
        )

        return cropNameError == null && cropAreaError == null
    }
}

data class CropDetailsUiState(
    val isLoading: Boolean = false,
    val shouldNavigateToLogin: Boolean = false,
    val errorMessage: String? = null,
    val cropNameError: String? = null,
    val cropAreaError: String? = null
)