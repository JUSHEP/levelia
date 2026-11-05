package com.levelia.levelia.presentation.screens.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.SKIN_INICIAL
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.ContadorBorde
import com.levelia.levelia.ui.theme.ContadorFondo
import com.levelia.levelia.ui.theme.LogroDesbloqueadoBorde
import com.levelia.levelia.ui.theme.LogroDesbloqueadoFondo
import com.levelia.levelia.ui.theme.PerfilPanelBorde
import com.levelia.levelia.ui.theme.PerfilPanelFondo
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P
import com.levelia.levelia.ui.theme.SkinEquipadaBorde
import com.levelia.levelia.ui.theme.SkinEquipadaFondo
import com.levelia.levelia.ui.theme.SkinSilueta

// Medidas tomadas del diseño "Personalización" del equipo (marco de 412 dp de ancho).

/** Margen lateral de Personalización, su detalle e Inicio (en el diseño las tarjetas empiezan a 30 de 412). */
val MARGEN_PERSONALIZACION = 30.dp

/**
 * Dibujo de cada skin (provisionales: reemplazar con el MISMO nombre por la exportación definitiva). Los ids
 * son los del catálogo de supabase/personalizacion.sql.
 */
@DrawableRes
fun recursoSkin(id: String?): Int = when (id) {
    "NOVATO" -> R.drawable.skin_novato
    "ESCUDERO" -> R.drawable.skin_escudero
    "SOLDADO" -> R.drawable.skin_soldado
    "CABALLERO" -> R.drawable.skin_caballero
    "PALADIN" -> R.drawable.skin_paladin
    "LEYENDA" -> R.drawable.skin_leyenda
    "ARQUERO" -> R.drawable.skin_arquero
    "VIKINGO" -> R.drawable.skin_vikingo
    "SAMURAI" -> R.drawable.skin_samurai
    "MAGO" -> R.drawable.skin_mago
    "NINJA" -> R.drawable.skin_ninja
    "CABALLERO_OSCURO" -> R.drawable.skin_caballero_oscuro
    // Una skin nueva del servidor sin dibujo en esta versión de la app
    else -> R.drawable.skin_novato
}

/** La skin en pixel-art. [silueta] = bloqueada: solo la forma, en morado (como el diseño). */
@Composable
fun ImagenSkin(
    skinId: String?,
    modifier: Modifier = Modifier,
    silueta: Boolean = false,
    descripcion: String? = null,
) {
    Image(
        bitmap = ImageBitmap.imageResource(recursoSkin(skinId ?: SKIN_INICIAL)),
        contentDescription = descripcion,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        colorFilter = if (silueta) ColorFilter.tint(SkinSilueta, BlendMode.SrcIn) else null,
        modifier = modifier,
    )
}

/** Esmeralda de la moneda (recorte provisional de AppImagenes/recompensas.png). */
@Composable
private fun IconoEsmeralda(modifier: Modifier = Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(R.drawable.ic_esmeralda),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = modifier,
    )
}

// =============================================================================================
// Barra superior
// =============================================================================================

/**
 * Título pixel de la barra superior ("Personalización", el nombre de la skin). Press Start 2P mide lo mismo
 * en todas las letras: se achica lo justo para no chocar con el contador de esmeraldas.
 */
@Composable
fun RowScope.TituloBarra(texto: String) {
    BoxWithConstraints(
        modifier = Modifier.weight(1f).padding(end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tamano = (maxWidth.value / texto.length.coerceAtLeast(1)).coerceIn(9f, 16f).sp
        Text(
            texto,
            color = Color.White,
            fontFamily = PressStart2P,
            fontSize = tamano,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Esmeraldas del usuario (arriba a la derecha): borde dorado, la esmeralda y el número. */
@Composable
fun ContadorEsmeraldas(cantidad: Int, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(29.dp)
            .clip(forma)
            .background(ContadorFondo)
            .border(3.dp, ContadorBorde, forma)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoEsmeralda(Modifier.size(width = 13.dp, height = 15.dp))
        Spacer(Modifier.width(5.dp))
        Text("$cantidad", color = Color.White, fontFamily = PressStart2P, fontSize = 10.sp, maxLines = 1)
    }
}

// =============================================================================================
// Pantalla Personalización
// =============================================================================================

/** Tarjeta grande de arriba con la skin equipada. */
@Composable
fun TarjetaSkinActual(skin: Skin?, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(3.dp, PerfilPanelBorde, forma),
        contentAlignment = Alignment.Center,
    ) {
        ImagenSkin(skin?.id, Modifier.height(146.dp), descripcion = skin?.nombre)
    }
}

/** Texto pixel centrado de las fichas ("Escudero", "Nvl. 5"). */
@Composable
private fun TextoFicha(texto: String, tamano: TextUnit) {
    Text(
        texto,
        color = Color.White,
        fontFamily = PressStart2P,
        fontSize = tamano,
        lineHeight = tamano * 1.2f,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Clip,
    )
}

/** "💎 200" de las fichas de la tienda. */
@Composable
private fun PrecioFicha(precio: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconoEsmeralda(Modifier.size(width = 11.dp, height = 13.dp))
        Spacer(Modifier.width(4.dp))
        TextoFicha("$precio", tamano = 11.sp)
    }
}

/**
 * Ficha de la cuadrícula. Equipada: verde con borde verde claro. Del guerrero y bloqueada: el nivel que pide y el
 * candado (como el diseño). De la tienda sin comprar: el precio y cómo se ve. Las demás: nombre y dibujo.
 */
@Composable
fun TarjetaSkin(skin: Skin, equipada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(98.dp)
            .clip(forma)
            .background(if (equipada) SkinEquipadaFondo else PerfilPanelFondo)
            .border(3.dp, if (equipada) SkinEquipadaBorde else PerfilPanelBorde, forma)
            .clickable(role = Role.Button, onClickLabel = skin.nombre, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            skin.esSecreta -> {
                TextoFicha(skin.etiquetaNivel.orEmpty(), tamano = 12.sp)
                Spacer(Modifier.weight(1f))
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.ic_logro_bloqueado),
                    contentDescription = "Bloqueada",
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.height(33.dp),
                )
                Spacer(Modifier.weight(1f))
            }
            !skin.obtenida -> {
                PrecioFicha(skin.precio ?: 0)
                ImagenSkin(skin.id, Modifier.weight(1f).padding(top = 4.dp), descripcion = skin.nombre)
            }
            else -> {
                TextoFicha(skin.nombre, tamano = 10.sp)
                skin.etiquetaNivel?.let { TextoFicha(it, tamano = 10.sp) }
                ImagenSkin(skin.id, Modifier.weight(1f).padding(top = 4.dp), descripcion = skin.nombre)
            }
        }
    }
}

/** Cuadrícula de 3 columnas (dentro del scroll de LeveliaScreen, por eso no es una lista perezosa). */
@Composable
fun CuadriculaSkins(skins: List<Skin>, equipada: Skin?, onSkin: (Skin) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(30.dp)) {
        skins.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                fila.forEach { skin ->
                    TarjetaSkin(
                        skin = skin,
                        equipada = skin.id == equipada?.id,
                        onClick = { onSkin(skin) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// =============================================================================================
// Detalle de la skin
// =============================================================================================

/**
 * Tarjeta del detalle: título, la skin grande y "Habilidades". Las del guerrero que aún no se alcanzan se
 * muestran como en el diseño: "????", la silueta y signos de pregunta en las habilidades.
 */
@Composable
fun TarjetaDetalleSkin(skin: Skin, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(3.dp, PerfilPanelBorde, forma)
            .padding(start = 17.dp, end = 17.dp, top = 64.dp, bottom = 33.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            skin.tituloDetalle,
            color = Color.White,
            fontFamily = PressStart2P,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        ImagenSkin(
            skinId = skin.id,
            modifier = Modifier.height(280.dp),
            silueta = skin.esSecreta,
            descripcion = if (skin.esSecreta) "Skin bloqueada" else skin.nombre,
        )
        Spacer(Modifier.height(40.dp))
        PanelHabilidades(if (skin.esSecreta) null else skin.habilidades)
    }
}

/** Signos de pregunta del diseño para una skin bloqueada. */
private val HABILIDADES_SECRETAS = listOf(
    "?????????????????",
    "??????????????????????",
    "????????????????",
    "????????????????",
)

/** Panel "Habilidades" (solo texto descriptivo). [lineas] null = bloqueada. */
@Composable
fun PanelHabilidades(lineas: List<String>?, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(3.dp, PerfilPanelBorde, forma)
            .padding(start = 19.dp, end = 16.dp, top = 25.dp, bottom = 20.dp),
    ) {
        Text("Habilidades", color = Color.White, fontFamily = PressStart2P, fontSize = 14.sp)
        (lineas ?: HABILIDADES_SECRETAS).forEach { linea ->
            Spacer(Modifier.height(16.dp))
            Text(linea, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 18.sp)
        }
    }
}

/** "Comprar por 💎 200" (verde, igual que "Guardar"). */
@Composable
fun BotonComprarSkin(precio: Int, onClick: () -> Unit, cargando: Boolean, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(forma)
            .background(BotonGuardar)
            .clickable(enabled = !cargando, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Text("Comprar por", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            IconoEsmeralda(Modifier.size(width = 20.dp, height = 23.dp))
            Spacer(Modifier.width(6.dp))
            Text("$precio", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

/** "Equipada" (la que se ve en Inicio): misma píldora verde de "Desbloqueado" en Logros. */
@Composable
fun EtiquetaSkinEquipada(modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 199.dp, height = 46.dp)
                .clip(forma)
                .background(LogroDesbloqueadoFondo)
                .border(2.dp, LogroDesbloqueadoBorde, forma),
            contentAlignment = Alignment.Center,
        ) {
            Text("Equipada", color = Color.White, fontFamily = PressStart2P, fontSize = 14.sp, maxLines = 1)
        }
    }
}

/** Por qué todavía no se puede usar ("Se desbloquea al llegar al nivel 25", "te faltan 150"). */
@Composable
fun TextoAyudaSkin(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        color = Color.White,
        fontFamily = PlusJakartaSans,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
