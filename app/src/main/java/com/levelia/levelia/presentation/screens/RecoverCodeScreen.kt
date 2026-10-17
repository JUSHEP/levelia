package com.levelia.levelia.presentation.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.levelia.levelia.domain.models.RecuperarState
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.presentation.viewmodel.AuthViewModel.Companion.LONGITUD_CODIGO
import com.levelia.levelia.presentation.viewmodel.AuthViewModel.Companion.MAX_INTENTOS
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.TextoSecundario

/** Código de recuperación: misma pantalla y textos que el paso 2 del registro (pedido así por el equipo). */
@Composable
fun RecoverCodeScreen(
    email: String,
    state: RecuperarState,
    onCodigoDigitChanged: (posicion: Int, digito: String) -> Unit,
    onVerificar: () -> Unit,
    onReenviar: () -> Unit,
    onBack: () -> Unit,
) {
    val habilitado = !state.cargando

    AuthColumn(title = "Verifica tu email", onBack = onBack.takeIf { habilitado }) {
        Text(
            "Ingresa el código de 6 dígitos que enviamos a $email",
            color = TextoSecundario,
            textAlign = TextAlign.Center,
        )

        CodigoOtp(
            codigo = state.codigo,
            habilitado = habilitado && state.intentosFallidos < MAX_INTENTOS,
            onDigitChanged = onCodigoDigitChanged,
        )

        ErrorMessage(state.error)

        val codigoCompleto = state.codigo.length == LONGITUD_CODIGO && state.codigo.all(Char::isDigit)
        LoadingButton(
            text = "Verificar código",
            isLoading = state.cargando,
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
