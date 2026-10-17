package com.isa.reto2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundGray = Color(0xFF2B2E33)
val AccentBlue = Color(0xFF8AB4F8)
val CardGray = Color(0xFFE8EAED)
val CardText = Color(0xFF202124)
val AuditRed = Color(0xFFD93025)
val AuditRedBg = Color(0xFFFCE4E4)
val SuccessGreen = Color(0xFF188038)

private val Scheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF1B1D21),
    background = BackgroundGray,
    onBackground = Color.White,
    surface = BackgroundGray,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3E45),
    onSurfaceVariant = Color(0xFFD0D3D8),
    error = Color(0xFFFF8A80),
    outline = Color(0xFF8E939B)
)

@Composable
fun Reto2Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
