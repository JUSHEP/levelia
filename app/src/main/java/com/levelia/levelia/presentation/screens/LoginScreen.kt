package com.levelia.levelia.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.AuthTextField
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.TextoSecundario
import com.levelia.levelia.ui.theme.TextoSobreClaro

@Composable
fun LoginScreen(
    email: String,
    password: String,
    passwordVisible: Boolean,
    rememberMe: Boolean,
    isLoading: Boolean,
    error: String?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val context = LocalContext.current

    AuthColumn(title = "Iniciar sesión") {
        AuthTextField(
            label = "Email",
            value = email,
            onValueChange = onEmailChanged,
            placeholder = "tu@email.com",
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AuthTextField(
            label = "Contraseña",
            value = password,
            onValueChange = onPasswordChanged,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Toda la fila (casilla + texto) alterna "Recuérdame"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .toggleable(
                        value = rememberMe,
                        enabled = !isLoading,
                        role = Role.Checkbox,
                        onValueChange = onRememberMeChanged,
                    )
                    .padding(vertical = 12.dp)
                    .padding(end = 8.dp),
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = null,
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MoradoAcento,
                        uncheckedColor = TextoSecundario,
                        checkmarkColor = TextoSobreClaro,
                    ),
                )
                Text("Recuérdame", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
            }
            TextButton(
                onClick = {
                    // TODO: recuperación de contraseña
                    Toast.makeText(context, "Disponible próximamente", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("¿Olvidaste?", color = MoradoAcento)
            }
        }

        ErrorMessage(error)
        LoadingButton(text = "Iniciar sesión", isLoading = isLoading, onClick = onLogin)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", color = TextoSecundario)
            TextButton(onClick = onRegisterClick, enabled = !isLoading) {
                Text("Regístrate", color = MoradoAcento)
            }
        }
    }
}
