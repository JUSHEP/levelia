package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.presentation.screens.components.EnlaceInferior
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.LeveliaTextField
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TituloPixel

/**
 * "Recupera tu contraseña" (diseño de Figma): pide el correo y el ViewModel pide a Supabase el código.
 * El diseño decía "enlace"; se cambió a "código" porque el correo trae un código de 6 dígitos.
 */
@Composable
fun RecoverPasswordScreen(
    email: String,
    error: String?,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onEnviar: () -> Unit,
    onRegistrate: () -> Unit,
    onBack: () -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.BANNER,
        onBack = onBack.takeIf { !isLoading },
        espacioSuperior = 166.dp,
    ) {
        TituloPixel("Recupera tu\ncontraseña", tamano = 28.sp)

        Spacer(Modifier.height(36.dp))

        LeveliaTextField(
            label = "correo",
            value = email,
            onValueChange = onEmailChanged,
            placeholder = "nombre@correo.com",
            error = error,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        Text(
            "Ingresa el correo asociado a tu cuenta y te enviaremos un código para restablecerla.",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
        )

        Spacer(Modifier.height(20.dp))
        LeveliaButton(text = "Enviar código", onClick = onEnviar, isLoading = isLoading)

        Spacer(Modifier.height(14.dp))
        EnlaceInferior(
            pregunta = "¿No tienes cuenta?",
            enlace = "Regístrate",
            onClick = onRegistrate,
            enabled = !isLoading,
        )
    }
}
