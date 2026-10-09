package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.DialogoAuth
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.presentation.screens.components.EnlaceInferior
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaDialog
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.LeveliaTextField
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.SubtituloAventura
import com.levelia.levelia.presentation.screens.components.TituloPixel

@Composable
fun RegisterStep1Screen(
    state: RegisterState,
    onNombreChanged: (String) -> Unit,
    onEdadChanged: (Int) -> Unit,
    onEmailChanged: (String) -> Unit,
    onContraseñaChanged: (String) -> Unit,
    onCrearCuenta: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onBack: () -> Unit,
) {
    val habilitado = !state.isLoading

    LeveliaScreen(
        pie = PieDePantalla.PASTO_LEGAL,
        onBack = onBack.takeIf { habilitado },
        indicadorPaso = "1 de 2",
        espacioSuperior = 64.dp,
        overlay = {
            state.dialogo?.let { dialogo ->
                val (titulo, mensaje, icono) = textosDe(dialogo)
                LeveliaDialog(
                    titulo = titulo,
                    mensaje = mensaje,
                    icono = icono,
                    onEntendido = onCerrarDialogo,
                )
            }
        },
    ) {
        TituloPixel("REGISTRATE")
        SubtituloAventura(prefijo = "Comienza tu ", destacado = "aventura")

        Spacer(Modifier.height(24.dp))

        LeveliaTextField(
            label = "Nombre",
            value = state.nombre,
            onValueChange = onNombreChanged,
            placeholder = "¿Como quieres que te llamemos?",
            error = state.errores.nombre,
            enabled = habilitado,
        )
        Spacer(Modifier.height(20.dp))
        LeveliaTextField(
            label = "Edad",
            value = if (state.edad > 0) state.edad.toString() else "",
            onValueChange = { texto -> onEdadChanged(texto.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) },
            error = state.errores.edad,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(20.dp))
        LeveliaTextField(
            label = "Correo",
            value = state.email,
            onValueChange = onEmailChanged,
            placeholder = "nombre@correo.com",
            error = state.errores.email,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(20.dp))
        LeveliaTextField(
            label = "Contraseña",
            value = state.contraseña,
            onValueChange = onContraseñaChanged,
            placeholder = "minimo 8 caracteres",
            error = state.errores.contraseña,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(24.dp))
        LeveliaButton(text = "Crear cuenta", onClick = onCrearCuenta, isLoading = state.isLoading)

        Spacer(Modifier.height(14.dp))
        EnlaceInferior(
            pregunta = "¿Ya tienes cuenta?",
            enlace = "Inicia sesión",
            onClick = onBack,
            enabled = habilitado,
        )
    }
}

private data class TextosDialogo(val titulo: String, val mensaje: String, val icono: Int)

private fun textosDe(dialogo: DialogoAuth): TextosDialogo = when (dialogo) {
    DialogoAuth.VALIDACION -> TextosDialogo(
        "Error de validación",
        "Por favor, completa todos los campos obligatorios.",
        R.drawable.ic_dialog_validacion,
    )
    DialogoAuth.CORREO_INVALIDO -> TextosDialogo(
        "Correo inválido",
        "Por favor, ingresa un correo electrónico válido.",
        R.drawable.ic_dialog_correo_invalido,
    )
    DialogoAuth.CONTRASENA_DEBIL -> TextosDialogo(
        "Contraseña débil",
        "La contraseña debe tener al menos 8 caracteres.",
        R.drawable.ic_dialog_contrasena_debil,
    )
    DialogoAuth.ERROR_SERVIDOR -> TextosDialogo(
        "Error en el servidor",
        "Algo salió mal con nuestros servidores. Inténtalo más tarde.",
        R.drawable.ic_dialog_error_servidor,
    )
}
