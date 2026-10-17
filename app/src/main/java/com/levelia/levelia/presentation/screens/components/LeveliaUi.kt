package com.levelia.levelia.presentation.screens.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.ui.theme.BotonDialogoFin
import com.levelia.levelia.ui.theme.BotonCancelar
import com.levelia.levelia.ui.theme.BotonDialogoInicio
import com.levelia.levelia.ui.theme.BotonVerdeFin
import com.levelia.levelia.ui.theme.BotonVerdeInicio
import com.levelia.levelia.ui.theme.CampoErrorFondo
import com.levelia.levelia.ui.theme.DialogoDegradadoParadas
import com.levelia.levelia.ui.theme.EnlaceMenta
import com.levelia.levelia.ui.theme.EtiquetaCampo
import com.levelia.levelia.ui.theme.FondoDegradadoParadas
import com.levelia.levelia.ui.theme.PlaceholderGris
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P
import com.levelia.levelia.ui.theme.ScrimDialogo
import com.levelia.levelia.ui.theme.SombraTitulo
import com.levelia.levelia.ui.theme.TextoCampo

// =============================================================================================
// Fondo
// =============================================================================================

/** Cada cuadro de la cuadrícula del fondo mide 412 / 14 ≈ 29.4 en el diseño. */
private val CELDA_FONDO = 29.4.dp
private val CELDA_DIALOGO = 24.4.dp

/** Líneas finas y translúcidas, como la cuadrícula del diseño. */
private fun DrawScope.dibujarCuadricula(celda: Float, color: Color, grosor: Float) {
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), grosor)
        x += celda
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), grosor)
        y += celda
    }
}

/** Degradado morado → magenta con cuadrícula. Ocupa toda la pantalla, también bajo las barras del sistema. */
@Composable
fun LeveliaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(*FondoDegradadoParadas))
            .drawBehind {
                dibujarCuadricula(CELDA_FONDO.toPx(), Color.White.copy(alpha = 0.10f), 1.dp.toPx())
            },
        content = content,
    )
}

/** Círculos translúcidos de las esquinas del login (posiciones medidas sobre el diseño de 412 de ancho). */
@Composable
fun BurbujasDecorativas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val k = size.width / 412f
        fun burbuja(cx: Float, cy: Float, radio: Float, alfa: Float) {
            drawCircle(Color.White.copy(alpha = alfa), radius = radio * k, center = Offset(cx * k, cy * k))
        }
        burbuja(410f, 105f, 70f, 0.12f)
        burbuja(380f, 115f, 11f, 0.35f)
        burbuja(30f, 170f, 52f, 0.12f)
        burbuja(8f, 208f, 10f, 0.30f)
        burbuja(412f, 590f, 46f, 0.12f)
        burbuja(388f, 598f, 11f, 0.30f)
        burbuja(404f, 48f, 4f, 0.35f)
    }
}

// =============================================================================================
// Contenedor de pantalla
// =============================================================================================

enum class PieDePantalla {
    /** Banner "TU PRÓXIMO NIVEL TE ESPERA" (login y recuperar contraseña). */
    BANNER,

    /** Pasto con el texto legal (registro). */
    PASTO_LEGAL,

    /** Sin imagen abajo (módulo Perfil: ahí va la barra inferior). */
    NINGUNO,
}

@DrawableRes
private fun PieDePantalla.recurso(): Int? = when (this) {
    PieDePantalla.BANNER -> R.drawable.bg_banner_nivel
    PieDePantalla.PASTO_LEGAL -> R.drawable.bg_pasto
    PieDePantalla.NINGUNO -> null
}

/** Alto de la barra de navegación inferior (la pestaña seleccionada es la más alta). */
val ALTO_BARRA_INFERIOR = 74.dp

/**
 * Pantalla completa del diseño: fondo, pie de pasto/banner abajo, flecha atrás e indicador de paso
 * arriba, y el contenido con scroll en medio. [overlay] se dibuja encima de todo (diálogos).
 */
@Composable
fun LeveliaScreen(
    modifier: Modifier = Modifier,
    pie: PieDePantalla = PieDePantalla.PASTO_LEGAL,
    onBack: (() -> Unit)? = null,
    indicadorPaso: String? = null,
    espacioSuperior: Dp = 80.dp,
    margenHorizontal: Dp = 43.dp,
    /** Si no es null, la esquina superior izquierda muestra el botón de menú (hamburguesa) en vez de la flecha. */
    onMenu: (() -> Unit)? = null,
    /** Barra de navegación fija abajo (módulo Perfil / Home). */
    barraInferior: (@Composable BoxScope.() -> Unit)? = null,
    decoracion: @Composable BoxScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val imagenPie = pie.recurso()?.let { ImageBitmap.imageResource(it) }
    val altoPie = if (imagenPie == null) {
        0.dp
    } else {
        val proporcionPie = imagenPie.height.toFloat() / imagenPie.width
        (LocalConfiguration.current.screenWidthDp * proporcionPie).dp
    }
    val altoBarra = if (barraInferior == null) {
        0.dp
    } else {
        ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

    LeveliaBackground(modifier = modifier) {
        decoracion()

        // Pie fijo abajo; el teclado lo tapa en vez de empujarlo
        if (imagenPie != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                Image(
                    bitmap = imagenPie,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pie == PieDePantalla.PASTO_LEGAL) {
                    Text(
                        "Al registrarte aceptas nuestros\nTérminos y Política de privacidad.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = margenHorizontal),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    BotonEsquina(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onBack)
                } else if (onMenu != null) {
                    BotonEsquina(Icons.Filled.Menu, "Menú", onMenu)
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                if (indicadorPaso != null) {
                    Text(indicadorPaso, color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(espacioSuperior))
            content()
            Spacer(Modifier.height(altoPie + altoBarra + 16.dp))
        }

        if (barraInferior != null) barraInferior()

        overlay()
    }
}

@Composable
private fun BotonEsquina(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icono,
            contentDescription = descripcion,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

// =============================================================================================
// Textos
// =============================================================================================

/** Título pixel con el desplazamiento rosado del diseño. */
@Composable
fun TituloPixel(
    texto: String,
    tamano: TextUnit = 28.sp,
    textAlign: TextAlign = TextAlign.Unspecified,
    interlineado: Float = 1.3f,
    modifier: Modifier = Modifier,
) {
    Text(
        texto,
        color = Color.White,
        textAlign = textAlign,
        modifier = modifier,
        style = TextStyle(
            fontFamily = PressStart2P,
            fontSize = tamano,
            lineHeight = tamano * interlineado,
            shadow = Shadow(color = SombraTitulo, offset = Offset(6f, 6f), blurRadius = 0f),
        ),
    )
}

/** "Comienza tu **aventura**." */
@Composable
fun SubtituloAventura(prefijo: String, destacado: String) {
    Text(
        buildAnnotatedString {
            append(prefijo)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(destacado) }
            append(".")
        },
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** "¿No tienes cuenta? **Regístrate**": solo el enlace es pulsable. */
@Composable
fun EnlaceInferior(
    pregunta: String,
    enlace: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(pregunta, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        Text(
            enlace,
            color = EnlaceMenta,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
        )
    }
}

// =============================================================================================
// Campos y botones
// =============================================================================================

private val FormaCampo = RoundedCornerShape(20.dp)

/**
 * Campo blanco con la etiqueta morada dentro. Con [error] el campo se pinta rojo oscuro con borde
 * blanco, la etiqueta lleva "*" y el mensaje aparece debajo.
 */
@Composable
fun LeveliaTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Si es true, oculta el texto y muestra el ojo para verlo / ocultarlo. */
    esContrasena: Boolean = false,
) {
    val hayError = error != null
    val colorTexto = if (hayError) Color.White else TextoCampo
    val focus = remember { FocusRequester() }
    var contrasenaVisible by remember { mutableStateOf(false) }
    val transformacion =
        if (esContrasena && !contrasenaVisible) PasswordVisualTransformation() else visualTransformation

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(FormaCampo)
                .background(if (hayError) CampoErrorFondo else Color.White)
                .then(if (hayError) Modifier.border(1.dp, Color.White, FormaCampo) else Modifier)
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { focus.requestFocus() }
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        ) {
            Column(modifier = Modifier.padding(end = if (esContrasena) 44.dp else 0.dp)) {
                Text(
                    if (hayError) "*$label" else label,
                    color = if (hayError) Color.White else EtiquetaCampo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, color = colorTexto),
                    cursorBrush = SolidColor(colorTexto),
                    keyboardOptions = keyboardOptions,
                    visualTransformation = transformacion,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { campo ->
                        Box(modifier = Modifier.padding(top = 3.dp)) {
                            if (value.isEmpty() && !hayError) {
                                Text(placeholder, color = PlaceholderGris, fontSize = 16.sp)
                            }
                            campo()
                        }
                    },
                )
            }
            if (esContrasena) {
                IconButton(
                    onClick = { contrasenaVisible = !contrasenaVisible },
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.CenterEnd).size(40.dp),
                ) {
                    Icon(
                        imageVector = if (contrasenaVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = colorTexto,
                    )
                }
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

/** Botón verde del diseño (degradado horizontal, esquinas redondeadas y sombra). */
@Composable
fun LeveliaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val forma = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(8.dp, forma)
            .clip(forma)
            .background(Brush.horizontalGradient(listOf(BotonVerdeInicio, BotonVerdeFin)))
            .clickable(enabled = enabled && !isLoading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Text(
                text,
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
        }
    }
}

/** Casilla blanca de contorno + texto; toda la fila alterna el valor. */
@Composable
fun CasillaRecordarme(
    marcada: Boolean,
    onChange: (Boolean) -> Unit,
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, role = Role.Checkbox) { onChange(!marcada) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.5.dp, Color.White, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (marcada) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Text(texto, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

// =============================================================================================
// Diálogo pixel-art
// =============================================================================================

/**
 * Tarjeta de aviso del diseño (título pixel, ícono, mensaje y botón "Entendido") sobre un velo que
 * oscurece la pantalla. Se pone en el parámetro `overlay` de [LeveliaScreen].
 */
@Composable
fun LeveliaDialog(
    titulo: String,
    mensaje: String,
    @DrawableRes icono: Int,
    onEntendido: () -> Unit,
    modifier: Modifier = Modifier,
    textoBoton: String = "Entendido",
    /** Alto del ícono; los íconos del módulo Perfil tienen otro tamaño que los de registro. */
    altoIcono: Dp = 64.dp,
    /** Color plano del botón principal (null = degradado verde de siempre). */
    colorBoton: Color? = null,
    /** Segundo botón opcional (p. ej. "Cancelar"), en morado oscuro bajo el principal. */
    textoSecundario: String? = null,
    onSecundario: (() -> Unit)? = null,
    /** Qué hace el botón "atrás" del teléfono; por defecto lo mismo que el botón principal. */
    onDescartar: (() -> Unit)? = null,
) {
    BackHandler(onBack = onDescartar ?: onEntendido)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrimDialogo)
            // Absorbe los toques para que no lleguen a la pantalla de abajo
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center,
    ) {
        val forma = RoundedCornerShape(38.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 38.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(forma)
                .background(Brush.verticalGradient(*DialogoDegradadoParadas))
                .drawBehind {
                    dibujarCuadricula(CELDA_DIALOGO.toPx(), Color.White.copy(alpha = 0.09f), 1.dp.toPx())
                }
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                titulo,
                color = Color.White,
                fontFamily = PressStart2P,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Image(
                bitmap = ImageBitmap.imageResource(icono),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.padding(top = 22.dp, bottom = 18.dp).height(altoIcono),
            )
            Text(
                mensaje,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .height(38.dp)
                    .widthIn(min = if (colorBoton != null) 158.dp else 118.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (colorBoton != null) SolidColor(colorBoton)
                        else Brush.horizontalGradient(listOf(BotonDialogoInicio, BotonDialogoFin)),
                    )
                    .clickable(role = Role.Button, onClick = onEntendido)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    textoBoton,
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (textoSecundario != null && onSecundario != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .height(38.dp)
                        .widthIn(min = 158.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BotonCancelar)
                        .clickable(role = Role.Button, onClick = onSecundario)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        textoSecundario,
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
