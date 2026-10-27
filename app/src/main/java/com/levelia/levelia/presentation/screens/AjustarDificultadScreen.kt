package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_MISIONES
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaDificultadMision
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.BotonGuardar

/**
 * "Ajustes de misión": elegir Fácil, Media o Difícil (la dificultad "Única" no se puede elegir aquí).
 * [dificultadInicial] es la que sale marcada al abrir. "Guardar cambios" entrega la elegida con
 * [onGuardar]; la flecha atrás vuelve con [onBack] sin cambiar nada.
 */
@Composable
fun AjustarDificultadScreen(
    dificultadInicial: Dificultad,
    onGuardar: (Dificultad) -> Unit,
    onBack: () -> Unit,
    onTab: (TabInferior) -> Unit,
    /** Guardando en la base: el botón muestra la carga. */
    guardando: Boolean = false,
    error: String? = null,
    onCerrarAviso: () -> Unit = {},
) {
    var seleccion by rememberSaveable { mutableStateOf(dificultadInicial) }

    val barra: @Composable BoxScope.() -> Unit = {
        BarraInferior(
            seleccion = TabInferior.MISIONES,
            onTab = onTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 16.dp,
        margenHorizontal = MARGEN_MISIONES,
        decoracion = { BurbujasDecorativas() },
        barraInferior = barra,
        overlay = {
            DialogoPerfilUi(
                dialogo = error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarAviso,
                onConfirmarEliminar = {},
            )
        },
    ) {
        TituloPixel(
            "Ajustes de misión",
            tamano = 24.sp,
            interlineado = 1f,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(46.dp))
        TituloPixel(
            // Mismo corte de línea que el diseño
            "Selecciona\ntu dificultad",
            tamano = 24.sp,
            interlineado = 1f,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(40.dp))

        Dificultad.entries.filter { it != Dificultad.UNICA }.forEach { dificultad ->
            TarjetaDificultadMision(
                dificultad = dificultad,
                seleccionada = dificultad == seleccion,
                onClick = { seleccion = dificultad },
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(17.dp))

        BotonPerfil(texto = "Guardar cambios", onClick = { onGuardar(seleccion) }, color = BotonGuardar, cargando = guardando)
    }
}
