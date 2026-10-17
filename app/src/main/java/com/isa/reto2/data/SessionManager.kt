package com.isa.reto2.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Sesión en memoria. Si el proceso muere, currentUser vuelve a null y la app redirige al login. */
object SessionManager {
    var currentUser: UserEntity? by mutableStateOf(null)

    val isAdmin: Boolean get() = currentUser?.role == ROLE_ADMIN

    fun logout() {
        currentUser = null
    }
}
