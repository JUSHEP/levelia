package com.levelia.levelia.presentation.screens.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.AvatarImagen
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.BotonPeligro
import com.levelia.levelia.ui.theme.PerfilCampoContrasena
import com.levelia.levelia.ui.theme.PerfilPanelBorde
import com.levelia.levelia.ui.theme.PerfilPanelFondo
import com.levelia.levelia.ui.theme.PerfilSubtitulo
import com.levelia.levelia.ui.theme.PerfilTarjetaBorde
import com.levelia.levelia.ui.theme.PerfilTarjetaFondo
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P
import com.levelia.levelia.ui.theme.XpPistaBorde
import com.levelia.levelia.ui.theme.XpPistaFondo
import com.levelia.levelia.ui.theme.XpRellenoAbajo
import com.levelia.levelia.ui.theme.XpRellenoArriba
import com.levelia.levelia.ui.theme.XpRellenoMedio

/** Margen lateral de las pantallas del módulo Perfil (en el diseño las tarjetas empiezan a 32 de 412). */
val MARGEN_PERFIL = 32.dp

/** Personaje guardado en la base de datos; si el texto no coincide con ninguno se usa el primero de la lista. */
fun avatarDe(nombre: String?): Avatar =
    Avatar.entries.firstOrNull { it.name == nombre } ?: Avatar.ordenSeleccion.first()

@Composable
fun AvatarPerfil(avatar: Avatar, modifier: Modifier = Modifier, tamano: Dp = 116.dp) {
    AvatarImagen(avatar = avatar, modifier = modifier.size(tamano))
}

/** Texto pixel blanco ("Nombre actual", "Contraseña actual"…). */
@Composable
fun EtiquetaPixel(texto: String, modifier: Modifier = Modifier, tamano: TextUnit = 14.sp) {
    Text(
        texto,
        color = Color.White,
        fontFamily = PressStart2P,
        fontSize = tamano,
        modifier = modifier,
    )
}

// =============================================================================================
// Pantalla Perfil
// =============================================================================================

/** Tarjeta "Tu progreso": nivel, barra de experiencia y XP. Los datos vienen de la tabla `progreso`. */
@Composable
fun TarjetaProgreso(progreso: ProgresoData, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(3.dp, PerfilPanelBorde, forma)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Tu progreso",
            color = Color.White,
            fontFamily = PressStart2P,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.ic_xp_brujula),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.height(32.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    "Nvl. ${progreso.nivel}",
                    color = Color.White,
                    fontFamily = PressStart2P,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                BarraXp(progreso.fraccion)
            }
        }
        Text(
            "${progreso.xp} / ${progreso.xpSiguienteNivel} XP",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun BarraXp(fraccion: Float, modifier: Modifier = Modifier, alto: Dp = 26.dp) {
    val forma = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(forma)
            .background(XpPistaFondo)
            .border(2.dp, XpPistaBorde, forma)
            .padding(3.dp),
    ) {
        if (fraccion > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraccion)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            0.0f to XpRellenoArriba,
                            0.35f to XpRellenoMedio,
                            1.0f to XpRellenoAbajo,
                        ),
                    ),
            )
        }
    }
}

/** Opción del menú de perfil: ícono, título pixel y subtítulo. */
@Composable
fun OpcionPerfil(
    titulo: String,
    subtitulo: String,
    @DrawableRes icono: Int,
    altoIcono: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(forma)
            .background(PerfilTarjetaFondo)
            .border(2.dp, PerfilTarjetaBorde, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Image(
                bitmap = ImageBitmap.imageResource(icono),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.height(altoIcono),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                titulo,
                color = Color.White,
                fontFamily = PressStart2P,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                subtitulo,
                color = PerfilSubtitulo,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// =============================================================================================
// Campos y botones
// =============================================================================================

/**
 * Campo oscuro con borde morado del módulo Perfil. Con [onValueChange] = null es de solo lectura
 * ("Nombre actual"). [error] muestra la X roja a la derecha; [esContrasena] oculta el texto y
 * muestra el ojo.
 */
@Composable
fun CampoPerfil(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    esContrasena: Boolean = false,
    error: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val forma = RoundedCornerShape(14.dp)
    val focus = remember { FocusRequester() }
    var visible by remember { mutableStateOf(false) }
    val editable = onValueChange != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(3.dp, PerfilPanelBorde, forma)
            .then(
                if (editable) {
                    Modifier.clickable(
                        enabled = enabled,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { focus.requestFocus() }
                } else {
                    Modifier
                },
            )
            .padding(if (esContrasena) 4.dp else 0.dp)
            .clip(RoundedCornerShape(9.dp))
            .then(if (esContrasena) Modifier.background(PerfilCampoContrasena) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onValueChange == null) {
                Text(
                    value,
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = keyboardOptions,
                    visualTransformation =
                        if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
            }
            if (esContrasena) {
                IconButton(
                    onClick = { visible = !visible },
                    enabled = enabled,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = Color(0xFFB8B0CC),
                    )
                }
            } else if (error) {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.ic_req_x),
                    contentDescription = "Dato inválido",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Contador "0/20" debajo del campo, alineado a la derecha. */
@Composable
fun ContadorCaracteres(actual: Int, maximo: Int, modifier: Modifier = Modifier) {
    Text(
        "$actual/$maximo",
        color = Color.White,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        textAlign = TextAlign.End,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
    )
}

/**
 * Fila de la lista de requisitos de la contraseña: casilla verde con palomita o roja con X. El texto
 * siempre blanco y en negrita (pedido del equipo): solo la casilla cambia al cumplirse.
 */
@Composable
fun RequisitoContrasena(texto: String, cumplido: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            bitmap = ImageBitmap.imageResource(if (cumplido) R.drawable.ic_req_ok else R.drawable.ic_req_x),
            contentDescription = if (cumplido) "Cumplido" else "No cumplido",
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(24.dp),
        )
        Text(
            texto,
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Botón plano del módulo Perfil ("Guardar" verde, "Cancelar" morado oscuro). */
@Composable
fun BotonPerfil(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BotonGuardar,
    enabled: Boolean = true,
    cargando: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable(enabled = enabled && !cargando, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Text(
                texto,
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
    }
}

// =============================================================================================
// Diálogos
// =============================================================================================

/**
 * Diálogo que corresponde a [dialogo] (null = no dibuja nada). Los textos fijos son los del diseño;
 * el mensaje viene del ViewModel. Se pone en el parámetro `overlay` de LeveliaScreen.
 */
@Composable
fun DialogoPerfilUi(
    dialogo: DialogoPerfil?,
    onCerrar: () -> Unit,
    onConfirmarEliminar: () -> Unit,
) {
    if (dialogo == null) return
    val colorAceptar = BotonGuardar
    when (dialogo.tipo) {
        TipoDialogoPerfil.USUARIO_INVALIDO -> LeveliaDialog(
            titulo = "Usuario inválido",
            mensaje = dialogo.mensaje,
            icono = R.drawable.ic_dialog_info,
            onEntendido = onCerrar,
            textoBoton = "Aceptar",
            altoIcono = 52.dp,
            colorBoton = colorAceptar,
        )
        TipoDialogoPerfil.CONTRASENA_INVALIDA -> LeveliaDialog(
            titulo = "Contraseña inválida",
            mensaje = dialogo.mensaje,
            icono = R.drawable.ic_dialog_info,
            onEntendido = onCerrar,
            textoBoton = "Aceptar",
            altoIcono = 52.dp,
            colorBoton = colorAceptar,
        )
        TipoDialogoPerfil.CONTRASENA_ACTUALIZADA -> LeveliaDialog(
            titulo = "Contraseña actualizada",
            mensaje = dialogo.mensaje,
            icono = R.drawable.ic_dialog_ok,
            onEntendido = onCerrar,
            textoBoton = "Aceptar",
            altoIcono = 50.dp,
            colorBoton = colorAceptar,
        )
        TipoDialogoPerfil.CONFIRMAR_ELIMINAR -> LeveliaDialog(
            titulo = "¿Estás seguro?",
            mensaje = dialogo.mensaje,
            icono = R.drawable.ic_dialog_peligro,
            onEntendido = onConfirmarEliminar,
            textoBoton = "Eliminar cuenta",
            altoIcono = 58.dp,
            colorBoton = BotonPeligro,
            textoSecundario = "Cancelar",
            onSecundario = onCerrar,
            onDescartar = onCerrar,
        )
        TipoDialogoPerfil.ERROR_SERVIDOR -> LeveliaDialog(
            titulo = "Error en el servidor",
            mensaje = dialogo.mensaje,
            icono = R.drawable.ic_dialog_error_servidor,
            onEntendido = onCerrar,
            textoBoton = "Aceptar",
            colorBoton = colorAceptar,
        )
    }
}
