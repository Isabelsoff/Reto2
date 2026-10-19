package com.isa.reto2.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.data.AppUser
import com.isa.reto2.data.FirestoreTransactionRepository
import com.isa.reto2.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firestoreRepo = FirestoreTransactionRepository()
    private val uid: String? get() = SessionManager.currentUser?.uid

    val user: Flow<AppUser?> = uid?.let { firestoreRepo.observeUser(it) } ?: emptyFlow()

    fun changePhoto(uri: Uri) {
        val userId = uid ?: return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val dest = File(ctx.filesDir, "profile_${userId}_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                } ?: return@withContext null
                dest.absolutePath
            }
            if (path != null) {
                firestoreRepo.updateProfilePhoto(userId, path)
            }
        }
    }

    fun changePhotoBitmap(bitmap: Bitmap) {
        val userId = uid ?: return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val dest = File(ctx.filesDir, "profile_${userId}_${System.currentTimeMillis()}.jpg")
                dest.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                dest.absolutePath
            }
            firestoreRepo.updateProfilePhoto(userId, path)
        }
    }
}
