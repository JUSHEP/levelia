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
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.FiltroMisiones
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.domain.models.MisionesUiState
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.ALTO_BARRA_INFERIOR
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonCrearMision
import com.levelia.levelia.presentation.screens.components.BotonReclamarRecompensa
import com.levelia.levelia.presentation.screens.components.LeveliaDialog
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_MISIONES
import com.levelia.levelia.presentation.screens.components.PestanasMisiones
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaMision
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.PlusJakartaSans

/**
 * "Misiones": las predefinidas y las propias, con pestañas y el botón para crear una. Recibe estado +
 * lambdas (los datos los trae MisionesViewModel). Las cumplidas se ven verdes con palomita y, en
 * "Completadas", "¡Reclamar recompensa!" suma la XP y muestra el nivel alcanzado.
 */
@Composable
fun MisionesScreen(
    estado: MisionesUiState,
    onBack: () -> Unit,
    onFiltro: (FiltroMisiones) -> Unit,
    onMision: (MisionConProgreso) -> Unit,
    onCrear: () -> Unit,
    onReclamar: () -> Unit,
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
            // Textos provisionales: el diseño no trae este aviso (confirmar con el equipo)
            estado.recompensa?.let { r ->
                LeveliaDialog(
                    titulo = "¡Recompensa reclamada!",
                    mensaje = "Ganaste +${r.xpGanada} XP." +
                        if (r.nivelesSubidos > 0) " ¡Subiste al nivel ${r.nivel}!" else " Sigues en el nivel ${r.nivel}.",
                    icono = R.drawable.ic_dialog_ok,
                    onEntendido = onCerrarAviso,
                    textoBoton = "Aceptar",
                    altoIcono = 50.dp,
                    colorBoton = BotonGuardar,
                )
            }
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
        // En "Completadas", con XP por reclamar (en el diseño va justo debajo de la lista)
        if (estado.filtro == FiltroMisiones.COMPLETADAS && estado.hayRecompensas) {
            BotonReclamarRecompensa(onClick = onReclamar, cargando = estado.enviando)
            Spacer(Modifier.height(16.dp))
        }
        // Que la última tarjeta no quede tapada por el botón de crear
        Spacer(Modifier.height(56.dp))
    }
}
