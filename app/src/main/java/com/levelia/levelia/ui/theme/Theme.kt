package com.levelia.levelia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Tema único oscuro con la paleta de marca: sin modo claro ni colores dinámicos del sistema
private val LeveliaColorScheme = darkColorScheme(
    primary = VerdeBoton,
    onPrimary = TextoSobreClaro,
    secondary = MoradoAcento,
    onSecondary = TextoSobreClaro,
    tertiary = AvatarCian,
    background = FondoPrincipal,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = Superficie,
    onSurfaceVariant = TextoSecundario,
    outline = TextoSecundario,
    error = ErrorClaro,
    onError = TextoSobreClaro,
)

@Composable
fun LeveliaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeveliaColorScheme,
        typography = Typography,
        content = content,
    )
}
