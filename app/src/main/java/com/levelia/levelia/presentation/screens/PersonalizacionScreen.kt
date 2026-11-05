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
import com.levelia.levelia.domain.models.PersonalizacionUiState
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.ContadorEsmeraldas
import com.levelia.levelia.presentation.screens.components.CuadriculaSkins
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERSONALIZACION
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaSkinActual
import com.levelia.levelia.presentation.screens.components.TituloBarra
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.PlusJakartaSans

/**
 * "Personalización": la skin equipada arriba, las del guerrero por nivel (1, 5, 10, 25, 50, 100) y la tienda,
 * donde se canjean esmeraldas. En el diseño la tienda era el bloque "¡PRÓXIMAMENTE!". Recibe estado + lambdas;
 * los datos los trae PersonalizacionViewModel desde mi_personalizacion().
 */
@Composable
fun PersonalizacionScreen(
    estado: PersonalizacionUiState,
    onBack: () -> Unit,
    onSkin: (Skin) -> Unit,
    onCerrarError: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 31.dp,
        margenHorizontal = MARGEN_PERSONALIZACION,
        barraSuperior = {
            TituloBarra("Personalización")
            ContadorEsmeraldas(estado.esmeraldas)
        },
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(
                seleccion = TabInferior.PERSONALIZACION,
                onTab = onTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
        overlay = {
            DialogoPerfilUi(
                dialogo = estado.error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarError,
                onConfirmarEliminar = {},
            )
        },
    ) {
        TarjetaSkinActual(estado.equipada)
        Spacer(Modifier.height(21.dp))

        if (estado.cargando && estado.skins.isEmpty()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            CuadriculaSkins(estado.deNivel, estado.equipada, onSkin)

            if (estado.deTienda.isNotEmpty()) {
                Spacer(Modifier.height(34.dp))
                TituloPixel("Tienda", tamano = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                // Texto propio: el diseño no trae la tienda (allí estaba "¡PRÓXIMAMENTE!")
                Text(
                    "Canjea tus esmeraldas por skins exclusivas",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                CuadriculaSkins(estado.deTienda, estado.equipada, onSkin)
            }
        }
    }
}
