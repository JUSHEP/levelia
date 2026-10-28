package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.Logro
import com.levelia.levelia.domain.models.textoFechaObtencion
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BarraLogro
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CopaLogro
import com.levelia.levelia.presentation.screens.components.EtiquetaLogro
import com.levelia.levelia.presentation.screens.components.FilaRequisitoLogro
import com.levelia.levelia.presentation.screens.components.InsigniaLogro
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_LOGROS
import com.levelia.levelia.presentation.screens.components.MedallaLogro
import com.levelia.levelia.presentation.screens.components.PanelLogro
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.LogroFecha
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P

/**
 * "Detalles del logro". Desbloqueado: medalla, requisitos cumplidos, fecha de obtención y "Completado".
 * Bloqueado: candado, familia y nivel, requisitos con su avance, "Progreso general" y "Avance para el
 * siguiente nivel" (la medalla [siguiente] de la misma familia; null = es la última).
 */
@Composable
fun DetallesLogroScreen(
    logro: Logro,
    siguiente: Logro?,
    onBack: () -> Unit,
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
    ) {
        TituloPixel("Detalles del logro", tamano = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        if (logro.obtenido) {
            MedallaLogro(logro, Modifier.size(110.dp).align(Alignment.CenterHorizontally))
        } else {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.ic_logro_bloqueado),
                contentDescription = "Bloqueado",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.height(108.dp).align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(12.dp))
        InsigniaLogro(logro.obtenido, Modifier.align(Alignment.CenterHorizontally))
        if (!logro.obtenido) {
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                EtiquetaLogro(logro.familia.texto)
                EtiquetaLogro(logro.etiquetaNivel)
            }
        }
        Spacer(Modifier.height(16.dp))
        TituloPixel(logro.titulo, tamano = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        val espacio = if (logro.obtenido) 23.dp else 12.dp
        PanelLogro("Descripción", tituloCentrado = !logro.obtenido) { TextoPanel(logro.descripcion) }
        Spacer(Modifier.height(espacio))
        PanelLogro("Requisitos") { logro.requisitos.forEach { FilaRequisitoLogro(it) } }
        Spacer(Modifier.height(espacio))

        if (logro.obtenido) {
            FechaObtencion(textoFechaObtencion(logro.obtenidoAt!!))
            Spacer(Modifier.height(espacio))
            PanelCompletado(logro.nivel)
        } else {
            PanelLogro("Progreso general") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarraLogro(logro.progreso, Modifier.weight(1f))
                    Text(
                        "${logro.progreso}%",
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontSize = 20.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
            if (siguiente != null) {
                Spacer(Modifier.height(espacio))
                PanelLogro("Avance para el siguiente nivel", tituloCentrado = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BarraLogro(logro.avanceSiguiente ?: 0, Modifier.weight(1f))
                        Spacer(Modifier.width(24.dp))
                        MedallaLogro(siguiente, Modifier.size(44.dp), gris = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextoPanel(texto: String) {
    Text(texto, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 15.sp, lineHeight = 19.sp)
}

@Composable
private fun FechaObtencion(fecha: String) {
    PanelLogro(null) {
        // El calendario va a la izquierda del título y la fecha
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.ic_logro_calendario),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Fecha de obtención", color = Color.White, fontFamily = PressStart2P, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text(fecha, color = LogroFecha, fontFamily = PlusJakartaSans, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun PanelCompletado(nivel: Int) {
    PanelLogro(null) {
        // Texto y copa centrados en el alto del panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 16.dp),
        ) {
            Text(
                "Completado",
                color = Color.White,
                fontFamily = PressStart2P,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            CopaLogro(nivel, Modifier.size(52.dp))
        }
    }
}
