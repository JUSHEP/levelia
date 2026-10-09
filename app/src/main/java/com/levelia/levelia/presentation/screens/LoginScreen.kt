package com.levelia.levelia.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.ErroresLogin
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CasillaRecordarme
import com.levelia.levelia.presentation.screens.components.EnlaceInferior
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.LeveliaTextField
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.SubtituloAventura
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.EnlaceMenta

@Composable
fun LoginScreen(
    email: String,
    password: String,
    rememberMe: Boolean,
    isLoading: Boolean,
    errores: ErroresLogin,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val context = LocalContext.current

    LeveliaScreen(
        pie = PieDePantalla.BANNER,
        espacioSuperior = 110.dp,
        decoracion = { BurbujasDecorativas() },
    ) {
        TituloPixel("¡Bienvenido!", tamano = 26.sp)
        SubtituloAventura(prefijo = "Continúa tu ", destacado = "aventura")

        Spacer(Modifier.height(22.dp))

        LeveliaTextField(
            label = "correo",
            value = email,
            onValueChange = onEmailChanged,
            placeholder = "nombre@correo.com",
            error = errores.email,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(20.dp))
        LeveliaTextField(
            label = "contraseña",
            value = password,
            onValueChange = onPasswordChanged,
            placeholder = "Mínimo 8 caracteres",
            error = errores.contraseña,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CasillaRecordarme(
                marcada = rememberMe,
                onChange = onRememberMeChanged,
                texto = "Recordarme",
                enabled = !isLoading,
            )
            Text(
                "¿Olvidaste tu contraseña?",
                color = EnlaceMenta,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        // TODO: pantalla "Recupera tu contraseña" (ya está en el diseño)
                        Toast.makeText(context, "Disponible próximamente", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 10.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        LeveliaButton(text = "Iniciar sesión", onClick = onLogin, isLoading = isLoading)

        Spacer(Modifier.height(14.dp))
        EnlaceInferior(
            pregunta = "¿No tienes cuenta?",
            enlace = "Regístrate",
            onClick = onRegisterClick,
            enabled = !isLoading,
        )
    }
}
