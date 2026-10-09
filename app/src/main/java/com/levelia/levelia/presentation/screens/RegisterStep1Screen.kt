package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.AuthTextField
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton

@Composable
fun RegisterStep1Screen(
    state: RegisterState,
    onNombreChanged: (String) -> Unit,
    onEdadChanged: (Int) -> Unit,
    onEmailChanged: (String) -> Unit,
    onContraseñaChanged: (String) -> Unit,
    onCrearCuenta: () -> Unit,
    onBack: () -> Unit,
) {
    val habilitado = !state.isLoading

    AuthColumn(title = "Crea tu cuenta", paso = 1, onBack = onBack.takeIf { habilitado }) {
        AuthTextField(
            label = "Nombre",
            value = state.nombre,
            onValueChange = onNombreChanged,
            placeholder = "Tu nombre",
            enabled = habilitado,
        )
        AuthTextField(
            label = "Edad",
            value = if (state.edad > 0) state.edad.toString() else "",
            onValueChange = { texto -> onEdadChanged(texto.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) },
            placeholder = "Ej: 18",
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        AuthTextField(
            label = "Email",
            value = state.email,
            onValueChange = onEmailChanged,
            placeholder = "tu@email.com",
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AuthTextField(
            label = "Contraseña",
            value = state.contraseña,
            onValueChange = onContraseñaChanged,
            supportingText = "Mínimo 8 caracteres",
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )
        ErrorMessage(state.error)
        LoadingButton(text = "Crear cuenta", isLoading = state.isLoading, onClick = onCrearCuenta)
    }
}
