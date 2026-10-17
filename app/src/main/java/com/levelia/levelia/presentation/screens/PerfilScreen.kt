package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.levelia.levelia.domain.models.PerfilUiState
import com.levelia.levelia.presentation.screens.components.AvatarPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERFIL
import com.levelia.levelia.presentation.screens.components.OpcionPerfil
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaProgreso
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.presentation.screens.components.avatarDe
import com.levelia.levelia.ui.theme.PlusJakartaSans

@Composable
fun PerfilScreen(
    perfil: PerfilUiState,
    dialogo: DialogoPerfil?,
    onMenu: () -> Unit,
    onEditarNombre: () -> Unit,
    onCambiarAvatar: () -> Unit,
    onCambiarContrasena: () -> Unit,
    onEliminarCuenta: () -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCerrarDialogo: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onMenu = onMenu,
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
            DialogoPerfilUi(
                dialogo = dialogo,
                onCerrar = onCerrarDialogo,
                onConfirmarEliminar = onConfirmarEliminar,
            )
        },
    ) {
        TituloPixel(
            "Perfil",
            tamano = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        AvatarPerfil(avatarDe(perfil.usuario?.avatar), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Text(
            "@${perfil.usuario?.nombre.orEmpty()}",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))

        TarjetaProgreso(perfil.progreso)
        Spacer(Modifier.height(18.dp))

        OpcionPerfil(
            titulo = "Editar nombre de usuario",
            subtitulo = "Cambia tu nombre en la app",
            icono = R.drawable.ic_perfil_pluma,
            altoIcono = 28.dp,
            onClick = onEditarNombre,
        )
        Spacer(Modifier.height(12.dp))
        OpcionPerfil(
            titulo = "Cambiar avatar",
            subtitulo = "Elige una nueva imagen de perfil",
            icono = R.drawable.ic_perfil_avatar,
            altoIcono = 22.dp,
            onClick = onCambiarAvatar,
        )
        Spacer(Modifier.height(12.dp))
        OpcionPerfil(
            titulo = "Cambia la contraseña",
            subtitulo = "Mantén tu cuenta segura",
            icono = R.drawable.ic_perfil_candado,
            altoIcono = 34.dp,
            onClick = onCambiarContrasena,
        )
        Spacer(Modifier.height(12.dp))
        OpcionPerfil(
            titulo = "Eliminar cuenta",
            subtitulo = "Esta acción es irreversible",
            icono = R.drawable.ic_dialog_peligro,
            altoIcono = 30.dp,
            onClick = onEliminarCuenta,
        )
    }
}
