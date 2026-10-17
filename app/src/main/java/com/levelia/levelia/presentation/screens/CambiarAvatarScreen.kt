package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.CambiarAvatarState
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.presentation.screens.components.AvatarPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERFIL
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.presentation.screens.components.avatarDe
import com.levelia.levelia.ui.theme.BotonCancelar

@Composable
fun CambiarAvatarScreen(
    avatarActual: String,
    state: CambiarAvatarState,
    dialogo: DialogoPerfil?,
    onAvatarSeleccionado: (Avatar) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    val habilitado = !state.guardando

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onCancelar.takeIf { habilitado },
        espacioSuperior = 0.dp,
        margenHorizontal = MARGEN_PERFIL,
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(
                seleccion = TabInferior.PERFIL,
                onTab = onTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
        overlay = {
            DialogoPerfilUi(dialogo = dialogo, onCerrar = onCerrarDialogo, onConfirmarEliminar = {})
        },
    ) {
        TituloPixel(
            "Cambiar avatar",
            tamano = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        AvatarPerfil(avatarDe(avatarActual), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(26.dp))
        TituloPixel(
            "Selecciona un\nnuevo avatar",
            tamano = 16.sp,
            textAlign = TextAlign.Center,
            interlineado = 1.1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        // 9 personajes en 3 filas de 3, en el orden del diseño
        Avatar.ordenSeleccion.chunked(3).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                fila.forEach { avatar ->
                    OpcionAvatar(
                        avatar = avatar,
                        seleccionado = state.seleccionado == avatar.name,
                        habilitado = habilitado,
                        onClick = { onAvatarSeleccionado(avatar) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(26.dp))
        BotonPerfil(texto = "Guardar", onClick = onGuardar, cargando = state.guardando)
        Spacer(Modifier.height(16.dp))
        BotonPerfil(texto = "Cancelar", onClick = onCancelar, color = BotonCancelar, enabled = habilitado)
    }
}

/** El personaje elegido se ve apagado con una palomita encima, como en el diseño. */
@Composable
private fun OpcionAvatar(avatar: Avatar, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = seleccionado, enabled = habilitado, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AvatarImagen(
            avatar = avatar,
            modifier = Modifier.size(90.dp).alpha(if (seleccionado) 0.45f else 1f),
        )
        if (seleccionado) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}
