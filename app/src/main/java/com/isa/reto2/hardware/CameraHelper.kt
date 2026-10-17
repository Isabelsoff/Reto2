package com.isa.reto2.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Helper para gestionar el acceso de hardware a la cámara del teléfono.
 */
object CameraHelper {

    /**
     * Verifica si se ha concedido el permiso de Cámara.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si el dispositivo físicamente posee hardware de cámara.
     */
    fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     * Crea un archivo temporal en la carpeta de imágenes de la app
     * y genera un Uri mediante FileProvider para capturar fotos en alta resolución.
     */
    fun createImageUri(context: Context): Uri {
        val imageFile = File(
            context.getExternalFilesDir("Pictures"),
            "camera_photo_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    /**
     * Guarda un Bitmap (por ejemplo, capturado con TakePicturePreview) en el almacenamiento interno de la app.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.filesDir, "captured_photo_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
