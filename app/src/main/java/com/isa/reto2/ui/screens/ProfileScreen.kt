package com.isa.reto2.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isa.reto2.hardware.CameraHelper
import com.isa.reto2.hardware.VibrationHelper
import com.isa.reto2.ui.theme.CardGray
import com.isa.reto2.ui.theme.CardText
import com.isa.reto2.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val vibrationHelper = remember { VibrationHelper(context) }
    val user by vm.user.collectAsStateWithLifecycle(initialValue = null)

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vibrationHelper.vibrateSuccess()
            vm.changePhoto(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vibrationHelper.vibrateSuccess()
            vm.changePhotoBitmap(bitmap)
            Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vibrationHelper.vibrateClick()
            cameraLauncher.launch(null)
        } else {
            vibrationHelper.vibrateError()
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val photo by produceState<ImageBitmap?>(initialValue = null, user?.photoPath) {
        val path = user?.photoPath
        value = if (path == null) null else withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(CardGray),
            contentAlignment = Alignment.Center
        ) {
            val img = photo
            if (img != null) {
                Image(img, "Foto de perfil", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, Modifier.size(64.dp), tint = CardText)
            }
        }
        Text(user?.email ?: "", style = MaterialTheme.typography.titleMedium)
        Text(
            "${money(user?.balanceUsd ?: 0.0)} USD",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Opciones de foto: Tomar con Cámara o Elegir de Galería
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (CameraHelper.hasCameraPermission(context)) {
                        vibrationHelper.vibrateClick()
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Cámara", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    vibrationHelper.vibrateClick()
                    galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Galería")
            }
        }

        OutlinedButton(
            onClick = {
                vibrationHelper.vibrateClick()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver a operaciones")
        }
    }
}
