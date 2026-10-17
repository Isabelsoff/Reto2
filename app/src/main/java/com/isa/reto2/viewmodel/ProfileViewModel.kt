package com.isa.reto2.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.RetoApp
import com.isa.reto2.data.SessionManager
import com.isa.reto2.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val userDao = (app as RetoApp).database.userDao()
    private val userId: Long? = SessionManager.currentUser?.id

    val user: Flow<UserEntity?> = userId?.let { userDao.observeById(it) } ?: emptyFlow()

    /** Copia la imagen al almacenamiento interno y guarda la ruta en Room. */
    fun changePhoto(uri: Uri) {
        val id = userId ?: return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val old = userDao.getById(id)?.photoPath
                val dest = File(ctx.filesDir, "profile_${id}_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                } ?: return@withContext null
                old?.let { File(it).delete() }
                dest.absolutePath
            }
            if (path != null) userDao.updatePhoto(id, path)
        }
    }

    /** Guarda la foto tomada directamente con la Cámara en la base de datos. */
    fun changePhotoBitmap(bitmap: android.graphics.Bitmap) {
        val id = userId ?: return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val ctx = getApplication<Application>()
                val old = userDao.getById(id)?.photoPath
                val dest = File(ctx.filesDir, "profile_${id}_${System.currentTimeMillis()}.jpg")
                dest.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                old?.let { File(it).delete() }
                dest.absolutePath
            }
            userDao.updatePhoto(id, path)
        }
    }
}
