package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.levelia.levelia.domain.models.FiltroLogros
import com.levelia.levelia.domain.models.Logro
import com.levelia.levelia.domain.models.LogrosUiState
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CuadriculaLogros
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_LOGROS
import com.levelia.levelia.presentation.screens.components.PestanasPildora
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.PlusJakartaSans

/**
 * "Logros": pestañas Obtenidos / Todos y la cuadrícula de medallas (las bloqueadas en gris). Recibe
 * estado + lambdas; los datos los trae LogrosViewModel desde mis_logros().
 */
@Composable
fun LogrosScreen(
    estado: LogrosUiState,
    onBack: () -> Unit,
    onFiltro: (FiltroLogros) -> Unit,
    onLogro: (Logro) -> Unit,
    onCerrarError: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 16.dp,
        margenHorizontal = MARGEN_LOGROS,
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(seleccion = TabInferior.LOGROS, onTab = onTab, modifier = Modifier.align(Alignment.BottomCenter))
        },
        overlay = {
            DialogoPerfilUi(
                dialogo = estado.error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarError,
                onConfirmarEliminar = {},
            )
        },
    ) {
        TituloPixel("Logros", tamano = 30.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Text(
            // Mismo corte de línea que el diseño (ortografía corregida: "a través", "¡…!")
            "¡Mira tu progreso a través de medallas y\ntrofeos!",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))

        PestanasPildora(
            opciones = FiltroLogros.entries,
            seleccion = estado.filtro,
            texto = { if (it == FiltroLogros.OBTENIDOS) "Obtenidos" else "Todos" },
            onSeleccion = onFiltro,
            alto = 46.dp,
            tamanoTexto = 19.sp,
        )
        Spacer(Modifier.height(36.dp))

        when {
            estado.cargando && estado.logros.isEmpty() ->
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
            // Texto provisional: el diseño no trae la pestaña vacía (confirmar con el equipo)
            estado.visibles.isEmpty() && estado.filtro == FiltroLogros.OBTENIDOS && !estado.cargando -> Text(
                "Todavía no tienes logros. ¡Completa tu primera misión para ganar el primero!",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> CuadriculaLogros(estado.visibles, onLogro, Modifier.fillMaxWidth())
        }
    }
}
