package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.CrearMisionState
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CampoDescripcionMision
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.LeveliaTextField
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.SelectorDificultadMision
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.PlusJakartaSans
import java.time.DateTimeException
import java.time.LocalDate

/** Mismo límite que el servidor para la descripción (supabase/misiones.sql). */
private const val DESCRIPCION_MAX = 300

/**
 * "Crea tu propia misión": nombre, descripción, dificultad y, solo si la dificultad es "Única", la fecha
 * límite. Recibe estado + lambdas (los datos y las reglas viven en MisionesViewModel).
 */
@Composable
fun CrearMisionScreen(
    state: CrearMisionState,
    onBack: () -> Unit,
    onTitulo: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onDificultad: (Dificultad) -> Unit,
    onFechaLimite: (LocalDate?) -> Unit,
    onCrear: () -> Unit,
) {
    val habilitado = !state.guardando
    // Lo que se ve escrito en el campo ("DD/MM/AAAA"); al completarse se convierte en fecha para el ViewModel
    var textoFecha by rememberSaveable { mutableStateOf("") }

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack.takeIf { habilitado },
        espacioSuperior = 20.dp,
        decoracion = { BurbujasDecorativas() },
    ) {
        TituloPixel(
            "Crea tu propia misión",
            tamano = 20.sp,
            interlineado = 1.05f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            // Mismo corte de línea que el diseño
            "Personaliza tus propias misiones y enfócate\nen tus metas",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        LeveliaTextField(
            // Con error, LeveliaTextField le pone el "*" al principio
            label = if (state.errorTitulo != null) "nombre de misión" else "nombre de misión*",
            value = state.titulo,
            onValueChange = onTitulo,
            placeholder = "Introduce el Nombre de la misión",
            error = state.errorTitulo,
            enabled = habilitado,
        )
        Spacer(Modifier.height(28.dp))

        CampoDescripcionMision(
            value = state.descripcion,
            onValueChange = { onDescripcion(it.take(DESCRIPCION_MAX)) },
            placeholder = "Da más detalles y característica acerca de tu misión",
            enabled = habilitado,
        )
        Spacer(Modifier.height(28.dp))

        SelectorDificultadMision(
            seleccion = state.dificultad,
            onSeleccion = onDificultad,
            error = state.errorDificultad,
            enabled = habilitado,
        )

        // La fecha límite solo existe en las misiones "Única"
        if (state.dificultad == Dificultad.UNICA) {
            Spacer(Modifier.height(28.dp))
            LeveliaTextField(
                label = if (state.errorFecha != null) "fecha límite" else "fecha límite*",
                value = textoFecha,
                onValueChange = { entrada ->
                    val texto = formatearFecha(entrada)
                    textoFecha = texto
                    onFechaLimite(fechaDeTexto(texto))
                },
                placeholder = "DD/MM/AAAA",
                error = state.errorFecha,
                enabled = habilitado,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        Spacer(Modifier.height(32.dp))
        LeveliaButton(
            text = "CREAR",
            onClick = onCrear,
            isLoading = state.guardando,
            modifier = Modifier.width(186.dp).height(47.dp).align(Alignment.CenterHorizontally),
        )

        // Error del servidor o de red: desaparece al editar cualquier campo
        if (state.error != null) {
            Text(
                state.error,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

/** Deja solo los dígitos (máximo 8) y pone las "/" de DD/MM/AAAA mientras se escribe. */
private fun formatearFecha(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(8)
    return buildString {
        digitos.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('/')
            append(c)
        }
    }
}

/** La fecha del texto "DD/MM/AAAA"; null si está incompleto o no existe (por ejemplo 31/02). */
private fun fechaDeTexto(texto: String): LocalDate? {
    val partes = texto.split('/')
    if (partes.size != 3 || partes[2].length != 4) return null
    return try {
        LocalDate.of(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
    } catch (e: DateTimeException) {
        null
    }
}
