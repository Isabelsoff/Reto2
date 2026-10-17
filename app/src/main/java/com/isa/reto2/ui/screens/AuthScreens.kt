package com.isa.reto2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isa.reto2.viewmodel.AuthViewModel

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoRegister: () -> Unit, vm: AuthViewModel = viewModel()) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val state by vm.state.collectAsStateWithLifecycle()

    AuthLayout(title = "Iniciar sesión") {
        OutlinedTextField(
            value = email, onValueChange = { email = it; vm.clearError() },
            label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it; vm.clearError() },
            label = { Text("Contraseña") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { vm.login(email, password, onLoggedIn) },
            enabled = !state.loading, modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            else Text("Entrar")
        }
        TextButton(onClick = onGoRegister) { Text("¿No tienes cuenta? Regístrate") }
        Text(
            "Demo: admin@reto.com / admin123 · user@reto.com / user123",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onBack: () -> Unit, vm: AuthViewModel = viewModel()) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val state by vm.state.collectAsStateWithLifecycle()

    AuthLayout(title = "Crear cuenta") {
        OutlinedTextField(
            value = email, onValueChange = { email = it; vm.clearError() },
            label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it; vm.clearError() },
            label = { Text("Contraseña (mín. 6)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it; vm.clearError() },
            label = { Text("Confirmar contraseña") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { vm.register(email, password, confirm, onRegistered) },
            enabled = !state.loading, modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            else Text("Registrarme")
        }
        TextButton(onClick = { vm.clearError(); onBack() }) { Text("Volver al login") }
    }
}

@Composable
private fun AuthLayout(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
