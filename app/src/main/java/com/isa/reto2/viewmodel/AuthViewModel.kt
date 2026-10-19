package com.isa.reto2.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val isCheckingSession: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val authRepo = AuthRepository()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun checkActiveSession(onSessionActive: () -> Unit) {
        if (!authRepo.hasFirebaseUser()) return
        viewModelScope.launch {
            _state.value = AuthUiState(isCheckingSession = true, loading = true)
            try {
                val user = authRepo.checkAndLoadActiveSession()
                if (user != null) {
                    _state.value = AuthUiState()
                    onSessionActive()
                } else {
                    _state.value = AuthUiState()
                }
            } catch (e: Exception) {
                _state.value = AuthUiState()
            }
        }
    }

    private fun validate(email: String, password: String, confirm: String? = null): String? = when {
        email.isBlank() || password.isBlank() || (confirm != null && confirm.isBlank()) ->
            "Completa todos los campos"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Correo no válido"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        confirm != null && confirm != password -> "Las contraseñas no coinciden"
        else -> null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        validate(email, password)?.let { _state.value = AuthUiState(error = it); return }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            authRepo.login(email, password)
                .onSuccess {
                    _state.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message ?: "Correo o contraseña incorrectos")
                }
        }
    }

    fun register(email: String, password: String, confirm: String, onSuccess: () -> Unit) {
        validate(email, password, confirm)?.let { _state.value = AuthUiState(error = it); return }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            authRepo.register(email, password)
                .onSuccess {
                    _state.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message ?: "Error al registrar")
                }
        }
    }
}
