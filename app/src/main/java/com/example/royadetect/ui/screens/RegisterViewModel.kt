package com.example.royadetect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.royadetect.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        firstName: String,
        lastName: String,
        age: Int,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        Log.d("RegisterViewModel", "register() called with: firstName=$firstName, lastName=$lastName, age=$age, phone=$phone, email=$email")

        if (!validateInput(firstName, lastName, age, phone, email, password, confirmPassword)) {
            Log.d("RegisterViewModel", "Validation failed")
            return
        }

        Log.d("RegisterViewModel", "Validation passed, starting registration")

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                Log.d("RegisterViewModel", "Calling authRepository.register")
                authRepository.register(
                    firstName = firstName,
                    lastName = lastName,
                    age = age,
                    phone = phone,
                    email = email,
                    password = password
                ).collect { result ->
                    Log.d("RegisterViewModel", "Received result: $result")
                    result.fold(
                        onSuccess = { response ->
                            Log.d("RegisterViewModel", "Registration successful: $response")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isRegistrationSuccess = true,
                                errorMessage = null
                            )
                        },
                        onFailure = { error ->
                            Log.e("RegisterViewModel", "Registration failed", error)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error en el registro"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Exception in registration", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error inesperado: ${e.message}"
                )
            }
        }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        age: Int,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        val firstNameError = when {
            firstName.isBlank() -> "El nombre es requerido"
            firstName.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            else -> null
        }

        val lastNameError = when {
            lastName.isBlank() -> "El apellido es requerido"
            lastName.length < 2 -> "El apellido debe tener al menos 2 caracteres"
            else -> null
        }

        val ageError = when {
            age <= 0 -> "La edad es requerida"
            age < 18 -> "Debes ser mayor de edad"
            age > 120 -> "Edad no válida"
            else -> null
        }

        val phoneError = when {
            phone.isBlank() -> "El teléfono es requerido"
            phone.length < 9 -> "El teléfono debe tener al menos 9 dígitos"
            else -> null
        }

        val emailError = when {
            email.isBlank() -> "El email es requerido"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email no válido"
            else -> null
        }

        val passwordError = when {
            password.isBlank() -> "La contraseña es requerida"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }

        val confirmPasswordError = when {
            confirmPassword.isBlank() -> "Confirma tu contraseña"
            confirmPassword != password -> "Las contraseñas no coinciden"
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            firstNameError = firstNameError,
            lastNameError = lastNameError,
            ageError = ageError,
            phoneError = phoneError,
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )

        val isValid = listOf(
            firstNameError, lastNameError, ageError, phoneError,
            emailError, passwordError, confirmPasswordError
        ).all { it == null }

        Log.d("RegisterViewModel", "Validation result: $isValid")
        Log.d("RegisterViewModel", "Errors: firstName=$firstNameError, lastName=$lastNameError, age=$ageError, phone=$phoneError, email=$emailError, password=$passwordError, confirmPassword=$confirmPasswordError")

        return isValid
    }

    // Función para limpiar el estado de éxito
    fun clearRegistrationSuccess() {
        _uiState.value = _uiState.value.copy(isRegistrationSuccess = false)
    }
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isRegistrationSuccess: Boolean = false,
    val errorMessage: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val ageError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)