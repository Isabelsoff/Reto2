package com.isa.reto2.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth

/** Sesión en memoria respaldada por Firebase. */
object SessionManager {
    var currentUser: AppUser? by mutableStateOf(null)

    val isAdmin: Boolean get() = currentUser?.role == ROLE_ADMIN

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        currentUser = null
    }
}
