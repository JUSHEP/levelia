package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.FiltroMisiones
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.domain.models.MisionesUiState
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.ALTO_BARRA_INFERIOR
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonCrearMision
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_MISIONES
import com.levelia.levelia.presentation.screens.components.PestanasMisiones
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaMision
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.PlusJakartaSans

/**
 * "Misiones": las predefinidas y las propias, con pestañas y el botón para crear una. Recibe estado +
 * lambdas (los datos los trae MisionesViewModel). El diseño de las tarjetas cumplidas (verdes, con
 * palomita) y el botón "¡Reclamar recompensa!" todavía no están: por ahora todas usan la tarjeta normal.
 */
@Composable
fun MisionesScreen(
    estado: MisionesUiState,
    onBack: () -> Unit,
    onFiltro: (FiltroMisiones) -> Unit,
    onMision: (MisionConProgreso) -> Unit,
    onCrear: () -> Unit,
    onCerrarAviso: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    val altoBarra = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 16.dp,
        margenHorizontal = MARGEN_MISIONES,
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            // Fijo sobre la barra: la lista se desplaza por debajo
            BotonCrearMision(
                onClick = onCrear,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = altoBarra + 16.dp),
            )
            BarraInferior(
                seleccion = TabInferior.MISIONES,
                onTab = onTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
        overlay = {
            DialogoPerfilUi(
                dialogo = estado.error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarAviso,
                onConfirmarEliminar = {},
            )
        },
    ) {
        TituloPixel("Misiones", tamano = 24.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(13.dp))
        Text(
            // Mismo corte de línea que el diseño
            "Completa tareas, gana XP y desbloquea\nmás contenido",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(36.dp))

        PestanasMisiones(seleccion = estado.filtro, onSeleccion = onFiltro)
        Spacer(Modifier.height(26.dp))

        if (estado.cargando && estado.misiones.isEmpty()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        estado.visibles.forEach { item ->
            TarjetaMision(item = item, onClick = { onMision(item) })
            Spacer(Modifier.height(28.dp))
        }
        // Que la última tarjeta no quede tapada por el botón de crear
        Spacer(Modifier.height(56.dp))
    }
}
