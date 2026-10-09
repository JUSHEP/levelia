package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.presentation.screens.components.authTextFieldColors
import com.levelia.levelia.presentation.viewmodel.AuthViewModel.Companion.LONGITUD_CODIGO
import com.levelia.levelia.presentation.viewmodel.AuthViewModel.Companion.MAX_INTENTOS
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.TextoSecundario
import com.levelia.levelia.ui.theme.VerdeBoton

@Composable
fun RegisterStep2Screen(
    state: RegisterState,
    codigoDePrueba: String?,
    onCodigoDigitChanged: (posicion: Int, digito: String) -> Unit,
    onVerificar: () -> Unit,
    onReenviar: () -> Unit,
    onContinuar: () -> Unit,
    onBack: () -> Unit,
) {
    val habilitado = !state.isLoading

    AuthColumn(title = "Verifica tu email", paso = 2, onBack = onBack.takeIf { habilitado }) {
        if (state.emailVerificado) {
            // Se volvió desde el paso 3: el email ya está verificado, no hace falta otro código
            Text("✓ Email verificado", color = VerdeBoton, fontWeight = FontWeight.SemiBold)
            Text(state.email, color = TextoSecundario)
            LoadingButton(text = "Continuar", isLoading = false, onClick = onContinuar)
            return@AuthColumn
        }

        Text(
            "Ingresa el código de 6 dígitos que enviamos a ${state.email}",
            color = TextoSecundario,
            textAlign = TextAlign.Center,
        )

        CodigoOtp(
            codigo = state.codigoVerificacion,
            habilitado = habilitado && state.intentosFallidos < MAX_INTENTOS,
            onDigitChanged = onCodigoDigitChanged,
        )

        if (codigoDePrueba != null) {
            Text(
                "Código de prueba (solo debug): $codigoDePrueba",
                color = MoradoAcento,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        ErrorMessage(state.error)

        val codigoCompleto = state.codigoVerificacion.length == LONGITUD_CODIGO &&
            state.codigoVerificacion.all(Char::isDigit)
        LoadingButton(
            text = "Verificar código",
            isLoading = state.isLoading,
            onClick = onVerificar,
            enabled = codigoCompleto && state.intentosFallidos < MAX_INTENTOS,
        )

        TextButton(onClick = onReenviar, enabled = habilitado && state.tiempoReenvio == 0) {
            Text(
                if (state.tiempoReenvio > 0) {
                    "Reenviar código en ${formatearTiempo(state.tiempoReenvio)}"
                } else {
                    "Reenviar código"
                },
                color = if (habilitado && state.tiempoReenvio == 0) MoradoAcento else TextoSecundario,
            )
        }
    }
}

@Composable
private fun CodigoOtp(
    codigo: String,
    habilitado: Boolean,
    onDigitChanged: (Int, String) -> Unit,
) {
    val focusRequesters = remember { List(LONGITUD_CODIGO) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    // Al limpiarse el código (intento fallido o reenvío) el foco vuelve a la primera casilla
    LaunchedEffect(codigo.isEmpty(), habilitado) {
        if (codigo.isEmpty() && habilitado) focusRequesters.first().requestFocus()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(LONGITUD_CODIGO) { posicion ->
            val digito = codigo.getOrNull(posicion)?.takeIf(Char::isDigit)?.toString() ?: ""
            OutlinedTextField(
                value = digito,
                onValueChange = { nuevo ->
                    onDigitChanged(posicion, nuevo)
                    val digitos = nuevo.filter(Char::isDigit)
                    when {
                        digitos.length == LONGITUD_CODIGO -> focusManager.clearFocus() // código pegado
                        digitos.isNotEmpty() && posicion < LONGITUD_CODIGO - 1 ->
                            focusRequesters[posicion + 1].requestFocus()
                        digitos.isNotEmpty() -> focusManager.clearFocus() // última casilla
                        nuevo.isEmpty() && posicion > 0 -> focusRequesters[posicion - 1].requestFocus()
                    }
                },
                enabled = habilitado,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(10.dp),
                colors = authTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[posicion]),
            )
        }
    }
}

private fun formatearTiempo(segundos: Int) = "%02d:%02d".format(segundos / 60, segundos % 60)
