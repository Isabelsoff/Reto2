package com.isa.reto2.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isa.reto2.data.SessionManager
import com.isa.reto2.data.TYPE_BUY
import com.isa.reto2.data.TYPE_SELL
import com.isa.reto2.hardware.CameraHelper
import com.isa.reto2.ui.theme.CardGray
import com.isa.reto2.ui.theme.CardText
import com.isa.reto2.viewmodel.CURRENCIES
import com.isa.reto2.viewmodel.OperationsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(
    onProfile: () -> Unit,
    onAudit: () -> Unit,
    onHardware: () -> Unit,
    onLogout: () -> Unit,
    vm: OperationsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val balance by vm.balance.collectAsStateWithLifecycle(initialValue = 0.0)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vm.onPhotoCaptured(bitmap)
            Toast.makeText(context, "Foto de comprobante capturada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Operaciones") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onHardware) { Icon(Icons.Default.Build, "Hardware") }
                    if (SessionManager.isAdmin) {
                        IconButton(onClick = onAudit) { Icon(Icons.Default.AdminPanelSettings, "Auditoría") }
                    }
                    IconButton(onClick = onProfile) { Icon(Icons.Default.Person, "Perfil") }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Balance: ${money(balance)} USD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CURRENCIES.forEach { c ->
                        FilterChip(
                            selected = state.currency == c,
                            onClick = { vm.selectCurrency(c) },
                            label = { Text(c) }
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGray, contentColor = CardText)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tasa de referencia (1 USD)", style = MaterialTheme.typography.labelMedium)
                            when {
                                state.loadingRate -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(top = 8.dp))
                                state.rate != null -> Text(
                                    "${String.format(Locale.US, "%,.4f", state.rate)} ${state.currency}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                else -> Text("—", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        IconButton(onClick = vm::loadRate, enabled = !state.loadingRate) {
                            Icon(Icons.Default.Refresh, "Actualizar tasa")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = vm::onAmountChange,
                    label = { Text("Cantidad en USD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (CameraHelper.hasCameraPermission(context)) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.photoBitmap != null) "Cambiar Foto Comprobante" else "Tomar Foto Comprobante")
                    }

                    if (state.photoBitmap != null) {
                        IconButton(onClick = vm::removePhoto) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (state.photoBitmap != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = state.photoBitmap!!.asImageBitmap(),
                            contentDescription = "Comprobante de operación",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            item {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.execute(TYPE_BUY) }, modifier = Modifier.weight(1f)) { Text("Comprar USD") }
                    OutlinedButton(onClick = { vm.execute(TYPE_SELL) }, modifier = Modifier.weight(1f)) { Text("Vender USD") }
                }
            }
            item {
                Text("Mis operaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (history.isEmpty()) {
                item { Text("Aún no tienes operaciones", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(history, key = { it.id }) { tx -> TransactionCard(tx) }
        }
    }
}
