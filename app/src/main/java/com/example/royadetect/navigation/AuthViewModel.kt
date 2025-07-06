package com.example.royadetect.navigation

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
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Expone el usuario actual del repository
    val currentUser = authRepository.getCurrentUser()

    init {
        // Observar el estado de login del repository
        viewModelScope.launch {
            authRepository.isLoggedIn().collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }

        // Validar sesión al iniciar
        validateSession()
    }

    private fun validateSession() {
        viewModelScope.launch {
            authRepository.validateSession().collect { result ->
                result.fold(
                    onSuccess = { isValid ->
                        // La validación se maneja automáticamente en el repository
                        // Si no es válida, clearSession() ya se llamó
                    },
                    onFailure = {
                        // Si hay error, limpiar sesión
                        authRepository.logout()
                    }
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}