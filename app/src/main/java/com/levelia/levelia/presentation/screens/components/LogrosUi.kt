package com.levelia.levelia.presentation.screens.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.Logro
import com.levelia.levelia.domain.models.RequisitoLogro
import com.levelia.levelia.ui.theme.LogroBloqueadoBorde
import com.levelia.levelia.ui.theme.LogroBloqueadoFondo
import com.levelia.levelia.ui.theme.LogroBloqueadoInsigniaBorde
import com.levelia.levelia.ui.theme.LogroBloqueadoInsigniaFondo
import com.levelia.levelia.ui.theme.LogroBloqueadoTexto
import com.levelia.levelia.ui.theme.LogroDesbloqueadoBorde
import com.levelia.levelia.ui.theme.LogroDesbloqueadoFondo
import com.levelia.levelia.ui.theme.LogroPanelBorde
import com.levelia.levelia.ui.theme.LogroPanelFondo
import com.levelia.levelia.ui.theme.LogroRequisitoCumplido
import com.levelia.levelia.ui.theme.LogroRequisitoPendiente
import com.levelia.levelia.ui.theme.LogroTarjetaBorde
import com.levelia.levelia.ui.theme.LogroTarjetaFondo
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P

// Medidas tomadas del diseño "Logros" del equipo (marco de 412 dp de ancho).

/** Margen lateral de la cuadrícula y de los paneles del detalle. */
val MARGEN_LOGROS = 31.dp

/**
 * Medalla de cada logro (recortes provisionales del diseño: reemplazar con el mismo nombre). Los ids son
 * los del catálogo de supabase/logros.sql; ¡¡Sabio!! todavía no tiene dibujo y usa el de ¡Pensador!.
 */
@DrawableRes
fun Logro.recurso(): Int = when (id) {
    "PRIMEROS_PASOS" -> R.drawable.logro_primeros_pasos
    "CALENTAMIENTO" -> R.drawable.logro_calentamiento
    "ESTUDIOSO" -> R.drawable.logro_estudioso
    "PENSADOR" -> R.drawable.logro_pensador
    "EN_FORMA" -> R.drawable.logro_en_forma
    "UNIDAD_ABSOLUTA" -> R.drawable.logro_unidad_absoluta
    "APRENDIZ" -> R.drawable.logro_aprendiz
    "JUNIOR" -> R.drawable.logro_junior
    "SENIOR" -> R.drawable.logro_senior
    "SABIO" -> R.drawable.logro_sabio
    // Un logro nuevo del servidor sin dibujo en esta versión de la app
    else -> R.drawable.logro_primeros_pasos
}

// Bloqueado: la medalla en gris y apagada, como la pestaña "Todos" del diseño
private val FILTRO_GRIS = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun MedallaLogro(logro: Logro, modifier: Modifier = Modifier, gris: Boolean = !logro.obtenido) {
    Image(
        bitmap = ImageBitmap.imageResource(logro.recurso()),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        colorFilter = FILTRO_GRIS.takeIf { gris },
        alpha = if (gris) 0.5f else 1f,
        modifier = modifier,
    )
}

/** Tarjeta de la cuadrícula: medalla y nombre. Bloqueado = gris, como en la pestaña "Todos". */
@Composable
fun TarjetaLogro(logro: Logro, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(129.dp)
            .clip(forma)
            .background(if (logro.obtenido) LogroTarjetaFondo else LogroBloqueadoFondo)
            .border(1.dp, if (logro.obtenido) LogroTarjetaBorde else LogroBloqueadoBorde, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedallaLogro(logro, Modifier.padding(top = 12.dp).size(width = 74.dp, height = 70.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                logro.titulo,
                color = if (logro.obtenido) Color.White else LogroBloqueadoTexto,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Cuadrícula de 3 columnas (dentro del scroll de LeveliaScreen, por eso no es una lista perezosa). */
@Composable
fun CuadriculaLogros(logros: List<Logro>, onLogro: (Logro) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        logros.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                fila.forEach { TarjetaLogro(it, onClick = { onLogro(it) }, modifier = Modifier.weight(1f)) }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** "Desbloqueado" (verde) o "Bloqueado" (morado) bajo la medalla del detalle. */
@Composable
fun InsigniaLogro(desbloqueado: Boolean, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(width = 199.dp, height = 46.dp)
            .clip(forma)
            .background(if (desbloqueado) LogroDesbloqueadoFondo else LogroBloqueadoInsigniaFondo)
            .border(2.dp, if (desbloqueado) LogroDesbloqueadoBorde else LogroBloqueadoInsigniaBorde, forma),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (desbloqueado) "Desbloqueado" else "Bloqueado",
            color = Color.White,
            fontFamily = PressStart2P,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

/** Etiquetas "Físico" y "Nivel 3" del detalle bloqueado. */
@Composable
fun EtiquetaLogro(texto: String, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(forma)
            .background(LogroBloqueadoInsigniaFondo)
            .border(2.dp, LogroBloqueadoInsigniaBorde, forma)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = Color.White, fontFamily = PressStart2P, fontSize = 11.sp, maxLines = 1)
    }
}

/** Panel oscuro con título pixel ("Descripción", "Requisitos", "Progreso general"…); null = sin título. */
@Composable
fun PanelLogro(
    titulo: String?,
    modifier: Modifier = Modifier,
    tituloCentrado: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(LogroPanelFondo)
            .border(2.dp, LogroPanelBorde, forma)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (titulo != null) {
            Text(
                titulo,
                color = Color.White,
                fontFamily = PressStart2P,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                textAlign = if (tituloCentrado) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

/** Fila de "Requisitos": palomita si ya se cumplió, el texto y "1/1" (verde) o "0/1" (gris). */
@Composable
fun FilaRequisitoLogro(requisito: RequisitoLogro, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (requisito.cumplido) {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.ic_req_ok),
                contentDescription = "Cumplido",
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            requisito.texto,
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            requisito.textoProgreso,
            color = if (requisito.cumplido) LogroRequisitoCumplido else LogroRequisitoPendiente,
            fontFamily = PlusJakartaSans,
            fontSize = 15.sp,
        )
    }
}

/** Barra verde de "Progreso general" / "Avance para el siguiente nivel" (la misma de la XP del Perfil). */
@Composable
fun BarraLogro(porcentaje: Int, modifier: Modifier = Modifier, alto: Dp = 22.dp) =
    BarraXp(porcentaje.coerceIn(0, 100) / 100f, modifier, alto)

/**
 * Copa del panel "Completado", del color de la medalla: bronce (nivel 1, la copa roja del diseño), plata (2)
 * u oro (3, la de plata teñida mientras no haya exportación de Figma: reemplazar con el mismo nombre).
 */
@Composable
fun CopaLogro(nivel: Int, modifier: Modifier = Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(if (nivel == 1) R.drawable.logro_primeros_pasos else R.drawable.ic_copa_logro),
        contentDescription = "Copa",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        colorFilter = ColorFilter.tint(Color(0xFFFFD34D), BlendMode.Modulate).takeIf { nivel >= 3 },
        modifier = modifier,
    )
}
