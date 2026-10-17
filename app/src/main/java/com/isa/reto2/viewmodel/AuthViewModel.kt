package com.isa.reto2.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.RetoApp
import com.isa.reto2.data.ROLE_USER
import com.isa.reto2.data.Security
import com.isa.reto2.data.SessionManager
import com.isa.reto2.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val userDao = (app as RetoApp).database.userDao()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() { _state.value = _state.value.copy(error = null) }

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
            val user = userDao.getByEmail(email.trim().lowercase())
            if (user == null || user.password != Security.sha256(password)) {
                _state.value = AuthUiState(error = "Correo o contraseña incorrectos")
            } else {
                SessionManager.currentUser = user
                _state.value = AuthUiState()
                onSuccess()
            }
        }
    }

    fun register(email: String, password: String, confirm: String, onSuccess: () -> Unit) {
        validate(email, password, confirm)?.let { _state.value = AuthUiState(error = it); return }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                val user = UserEntity(
                    email = email.trim().lowercase(),
                    password = Security.sha256(password),
                    role = ROLE_USER
                )
                val id = userDao.insert(user)
                SessionManager.currentUser = user.copy(id = id)
                _state.value = AuthUiState()
                onSuccess()
            } catch (e: SQLiteConstraintException) {
                _state.value = AuthUiState(error = "Ese correo ya está registrado")
            }
        }
    }
}
