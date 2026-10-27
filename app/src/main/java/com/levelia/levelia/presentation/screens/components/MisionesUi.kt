package com.levelia.levelia.presentation.screens.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.FiltroMisiones
import com.levelia.levelia.domain.models.IconoMision
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.CampoErrorFondo
import com.levelia.levelia.ui.theme.DificultadDificil
import com.levelia.levelia.ui.theme.DificultadFacil
import com.levelia.levelia.ui.theme.DificultadMedia
import com.levelia.levelia.ui.theme.DificultadUnica
import com.levelia.levelia.ui.theme.EtiquetaCampo
import com.levelia.levelia.ui.theme.MisionCumplidaBorde
import com.levelia.levelia.ui.theme.MisionCumplidaFondo
import com.levelia.levelia.ui.theme.MisionCumplidaMas
import com.levelia.levelia.ui.theme.MisionCumplidaProgreso
import com.levelia.levelia.ui.theme.MisionPestanaActiva
import com.levelia.levelia.ui.theme.MisionPestanasFondo
import com.levelia.levelia.ui.theme.MisionProgreso
import com.levelia.levelia.ui.theme.MisionTarjetaBorde
import com.levelia.levelia.ui.theme.MisionTarjetaFondo
import com.levelia.levelia.ui.theme.MisionXpMas
import com.levelia.levelia.ui.theme.PerfilPanelBorde
import com.levelia.levelia.ui.theme.PerfilPanelFondo
import com.levelia.levelia.ui.theme.RadioDificultadAro
import com.levelia.levelia.ui.theme.RadioDificultadVacio
import com.levelia.levelia.ui.theme.PlaceholderGris
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P
import com.levelia.levelia.ui.theme.TextoCampo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Medidas tomadas del diseño "Misiones" del equipo (marco de 412 dp de ancho).

/** Margen lateral de la lista y de las pestañas. */
val MARGEN_MISIONES = 27.dp

/** Ícono pixel-art de cada misión (recortes provisionales del diseño: reemplazar con el mismo nombre). */
@DrawableRes
fun IconoMision.recurso(): Int = when (this) {
    IconoMision.PESA -> R.drawable.ic_mision_pesa
    IconoMision.CEREBRO -> R.drawable.ic_mision_cerebro
    IconoMision.PORTAPAPELES -> R.drawable.ic_mision_portapapeles
    IconoMision.PERSONAS -> R.drawable.ic_mision_personas
}

/** "Activas · Completadas · Todas": tres partes iguales; la elegida es una píldora verde. */
@Composable
fun PestanasMisiones(seleccion: FiltroMisiones, onSeleccion: (FiltroMisiones) -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(forma)
            .background(MisionPestanasFondo),
    ) {
        FiltroMisiones.entries.forEach { filtro ->
            val elegida = filtro == seleccion
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(forma)
                    .background(if (elegida) MisionPestanaActiva else Color.Transparent)
                    .clickable(role = Role.Tab) { onSeleccion(filtro) },
                contentAlignment = Alignment.Center,
            ) {
                Text(filtro.texto(), color = Color.White, fontFamily = PlusJakartaSans, fontSize = 16.sp, maxLines = 1)
            }
        }
    }
}

private fun FiltroMisiones.texto() = when (this) {
    FiltroMisiones.ACTIVAS -> "Activas"
    FiltroMisiones.COMPLETADAS -> "Completadas"
    FiltroMisiones.TODAS -> "Todas"
}

/**
 * Tarjeta de la lista: ícono, título pixel, descripción, "+XP" y el progreso "0/3" abajo a la derecha.
 * Cumplida: verde con borde oscuro y la palomita a la derecha (pestaña "Completadas" del diseño).
 */
@Composable
fun TarjetaMision(item: MisionConProgreso, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(16.dp)
    val mision = item.mision
    val cumplida = item.estado == EstadoMision.COMPLETADA
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(forma)
            .background(if (cumplida) MisionCumplidaFondo else MisionTarjetaFondo)
            .border(if (cumplida) 3.dp else 1.dp, if (cumplida) MisionCumplidaBorde else MisionTarjetaBorde, forma)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (cumplida) {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.ic_mision_cumplida),
                contentDescription = "Cumplida",
                filterQuality = FilterQuality.None,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 35.dp, bottom = 8.dp).size(53.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxHeight().padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = ImageBitmap.imageResource(mision.icono.recurso()),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(50.dp),
            )
            Spacer(Modifier.width(18.dp))
            // Alto de línea = tamaño de letra: así las tres filas quedan tan juntas como en el diseño
            Column(modifier = Modifier.fillMaxHeight().padding(top = 14.dp)) {
                Text(
                    mision.titulo,
                    color = Color.White,
                    fontFamily = PressStart2P,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    mision.descripcion,
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = if (cumplida) MisionCumplidaMas else MisionXpMas)) { append("+") }
                        append("${item.aceptada?.xp ?: mision.xp} XP")
                    },
                    color = Color.White,
                    fontFamily = PressStart2P,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                )
            }
        }
        Text(
            item.aceptada?.textoProgreso ?: "0/${mision.meta}",
            color = if (cumplida) MisionCumplidaProgreso else MisionProgreso,
            fontFamily = PlusJakartaSans,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 27.dp, bottom = 8.dp),
        )
    }
}

/** "¡Reclamar recompensa!" (pestaña Completadas): verde, texto grande y la gema a la derecha. */
@Composable
fun BotonReclamarRecompensa(onClick: () -> Unit, cargando: Boolean, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(forma)
            .background(BotonGuardar)
            .clickable(enabled = !cargando, role = Role.Button, onClick = onClick)
            .padding(start = 11.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (cargando) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp, color = Color.White)
            }
        } else {
            Text(
                "¡Reclamar recompensa!",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                maxLines = 1,
            )
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ic_recompensa_gema),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(width = 36.dp, height = 40.dp),
        )
    }
}

/** Botón cuadrado verde con la pluma (sale por arriba a la derecha): abre "Crea tu propia misión". */
@Composable
fun BotonCrearMision(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Box(modifier = modifier.size(56.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(forma)
                .background(BotonGuardar)
                .border(2.dp, PerfilPanelBorde, forma)
                .clickable(role = Role.Button, onClickLabel = "Crear misión", onClick = onClick),
        )
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ic_perfil_pluma),
            contentDescription = "Crear misión",
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(30.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
        )
    }
}

// =============================================================================================
// Formulario "Crea tu propia misión"
// =============================================================================================

private val FormaCampoMision = RoundedCornerShape(20.dp)

/** Alto del cuadro de descripción (medido del diseño; los demás campos miden 65 dp, igual que LeveliaTextField). */
private val ALTO_DESCRIPCION_MISION = 177.dp

/**
 * Caja blanca con la etiqueta morada arriba, igual que LeveliaTextField. Con [error] se pinta rojo oscuro
 * con borde blanco, la etiqueta lleva "*" al principio y el mensaje aparece debajo. [obligatorio] agrega el
 * "*" al final de la etiqueta cuando no hay error ("dificultad*").
 */
@Composable
private fun CajaCampoMision(
    etiqueta: String,
    alto: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    obligatorio: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    contenido: @Composable ColumnScope.(colorTexto: Color) -> Unit,
) {
    val hayError = error != null
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(alto)
                .clip(FormaCampoMision)
                .background(if (hayError) CampoErrorFondo else Color.White)
                .then(if (hayError) Modifier.border(1.dp, Color.White, FormaCampoMision) else Modifier)
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                )
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        ) {
            Column {
                Text(
                    when {
                        hayError -> "*$etiqueta"
                        obligatorio -> "$etiqueta*"
                        else -> etiqueta
                    },
                    color = if (hayError) Color.White else EtiquetaCampo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                contenido(if (hayError) Color.White else TextoCampo)
            }
        }
        if (error != null) {
            Text(
                error,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

/** "descripción de la misión": caja blanca grande de varias líneas. */
@Composable
fun CampoDescripcionMision(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    CajaCampoMision(
        etiqueta = "descripción de la misión",
        alto = ALTO_DESCRIPCION_MISION,
        onClick = { focus.requestFocus() },
        modifier = modifier,
        enabled = enabled,
    ) { colorTexto ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, color = colorTexto),
            cursorBrush = SolidColor(colorTexto),
            modifier = Modifier.fillMaxSize().focusRequester(focus),
            decorationBox = { campo ->
                Box(modifier = Modifier.padding(top = 3.dp)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = PlaceholderGris, fontSize = 16.sp)
                    }
                    campo()
                }
            },
        )
    }
}

/** Texto de cada dificultad en el formulario. */
private fun Dificultad.textoFormulario() = when (this) {
    Dificultad.FACIL -> "Fácil"
    Dificultad.MEDIA -> "Media"
    Dificultad.DIFICIL -> "Difícil"
    Dificultad.UNICA -> "Única"
}

/**
 * "dificultad*": muestra "--" hasta elegir. Al tocarla se abre la lista Fácil · Media · Difícil · Única
 * justo debajo; al elegir una se cierra.
 */
@Composable
fun SelectorDificultadMision(
    seleccion: Dificultad?,
    onSeleccion: (Dificultad) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        CajaCampoMision(
            etiqueta = "dificultad",
            alto = 65.dp,
            onClick = { abierto = !abierto },
            obligatorio = true,
            error = error,
            enabled = enabled,
        ) { colorTexto ->
            Box(modifier = Modifier.padding(top = 3.dp)) {
                if (seleccion != null) {
                    Text(seleccion.textoFormulario(), color = colorTexto, fontFamily = PlusJakartaSans, fontSize = 16.sp)
                } else if (error == null) {
                    Text("--", color = PlaceholderGris, fontSize = 16.sp)
                }
            }
        }
        if (abierto && enabled) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FormaCampoMision)
                    .background(Color.White),
            ) {
                Dificultad.entries.forEach { dificultad ->
                    Text(
                        dificultad.textoFormulario(),
                        color = TextoCampo,
                        fontFamily = PlusJakartaSans,
                        fontSize = 16.sp,
                        fontWeight = if (dificultad == seleccion) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                abierto = false
                                onSeleccion(dificultad)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================================
// "Detalles de misión"
// =============================================================================================

private fun Dificultad.colorDetalle() = when (this) {
    Dificultad.FACIL -> DificultadFacil
    Dificultad.MEDIA -> DificultadMedia
    Dificultad.DIFICIL -> DificultadDificil
    Dificultad.UNICA -> DificultadUnica
}

private fun EstadoMision.textoDetalle() = when (this) {
    EstadoMision.ACTIVA -> "En progreso"
    EstadoMision.COMPLETADA -> "Completada"
    EstadoMision.PERDIDA -> "Fallida"
}

/**
 * Cuadro oscuro con Dificultad, Recompensa, Frecuencia y, solo si la misión ya se aceptó ([estado] no es
 * null), Estado. La dificultad va del color de su nivel.
 */
@Composable
fun TarjetaDetalleMision(
    dificultad: Dificultad,
    xp: Int,
    meta: Int,
    fechaLimite: LocalDate?,
    estado: EstadoMision?,
    modifier: Modifier = Modifier,
    /** "1/3" mientras la misión está en curso (null = no se muestra). */
    progreso: String? = null,
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(2.dp, PerfilPanelBorde, forma)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        FilaDetalleMision("Dificultad", dificultad.textoFormulario(), dificultad.colorDetalle())
        FilaDetalleMision("Recompensa", "+$xp XP")
        // Por veces: "3/Semana". La única se cumple una sola vez: se muestra su fecha límite en negrilla
        if (dificultad == Dificultad.UNICA) {
            FilaDetalleMision(
                "Frecuencia",
                fechaLimite?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "--",
                negrilla = true,
            )
        } else {
            FilaDetalleMision("Frecuencia", "$meta/Semana")
        }
        if (estado != null) FilaDetalleMision("Estado", estado.textoDetalle())
        if (progreso != null) FilaDetalleMision("Progreso", progreso)
    }
}

@Composable
private fun FilaDetalleMision(
    etiqueta: String,
    valor: String,
    colorValor: Color = Color.White,
    negrilla: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, color = Color.White, fontFamily = PressStart2P, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            valor,
            color = colorValor,
            fontFamily = PlusJakartaSans,
            fontSize = 20.sp,
            fontWeight = if (negrilla) FontWeight.Bold else null,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

// =============================================================================================
// "Ajustes de misión"
// =============================================================================================

/**
 * Opción de dificultad: círculo a la izquierda (marcado o vacío), nombre con el color del nivel, veces por
 * semana y XP. Toda la tarjeta es tocable. Los números salen de [Dificultad] (iguales a los del servidor).
 */
@Composable
fun TarjetaDificultadMision(
    dificultad: Dificultad,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(2.dp, PerfilPanelBorde, forma)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
            RadioDificultad(seleccionada)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 61.dp, end = 18.dp, top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                dificultad.textoFormulario(),
                color = dificultad.colorDetalle(),
                fontFamily = PlusJakartaSans,
                fontSize = 22.sp,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (dificultad.veces == 1) "1 vez/Semana" else "${dificultad.veces} veces/Semana",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 22.sp,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "+${dificultad.xp} XP",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 18.sp,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun RadioDificultad(seleccionada: Boolean) {
    if (seleccionada) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ic_dialog_ok),
            contentDescription = "Seleccionada",
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(36.dp),
        )
    } else {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(RadioDificultadAro),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(RadioDificultadVacio))
        }
    }
}
