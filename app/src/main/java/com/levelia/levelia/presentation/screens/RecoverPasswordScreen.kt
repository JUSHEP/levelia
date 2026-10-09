package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.presentation.screens.components.EnlaceInferior
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.LeveliaTextField
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TituloPixel

/** "Recupera tu contraseña": pide el correo y lo manda al ViewModel. */
@Composable
fun RecoverPasswordScreen(
    email: String,
    error: String?,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onEnviar: () -> Unit,
    onBack: () -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.PASTO_LEGAL,
        onBack = onBack.takeIf { !isLoading },
        espacioSuperior = 64.dp,
    ) {
        TituloPixel("RECUPERA TU\nCONTRASEÑA", tamano = 20.sp)
        Text(
            "Escribe tu correo y te enviaremos las instrucciones para crear una contraseña nueva.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(Modifier.height(28.dp))

        LeveliaTextField(
            label = "Correo",
            value = email,
            onValueChange = onEmailChanged,
            placeholder = "nombre@correo.com",
            error = error,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        Spacer(Modifier.height(24.dp))
        LeveliaButton(text = "Enviar", onClick = onEnviar, isLoading = isLoading)

        Spacer(Modifier.height(14.dp))
        EnlaceInferior(
            pregunta = "¿Ya la recordaste?",
            enlace = "Inicia sesión",
            onClick = onBack,
            enabled = !isLoading,
        )
    }
}
