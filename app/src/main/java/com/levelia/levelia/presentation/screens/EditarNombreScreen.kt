package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.ReglasPerfil
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.EditarNombreState
import com.levelia.levelia.presentation.screens.components.AvatarPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CampoPerfil
import com.levelia.levelia.presentation.screens.components.ContadorCaracteres
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.EtiquetaPixel
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERFIL
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.presentation.screens.components.avatarDe
import com.levelia.levelia.ui.theme.BotonCancelar

@Composable
fun EditarNombreScreen(
    nombreActual: String,
    avatarActual: String,
    state: EditarNombreState,
    dialogo: DialogoPerfil?,
    onNuevoNombreChanged: (String) -> Unit,
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
            "Editar nombre de usuario",
            tamano = 20.sp,
            textAlign = TextAlign.Center,
            interlineado = 1.05f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        AvatarPerfil(avatarDe(avatarActual), modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(40.dp))
        EtiquetaPixel("Nombre actual", tamano = 16.sp, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
        CampoPerfil(value = nombreActual)

        Spacer(Modifier.height(56.dp))
        EtiquetaPixel("Nuevo nombre de usuario", tamano = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
        CampoPerfil(
            value = state.nuevoNombre,
            onValueChange = onNuevoNombreChanged,
            error = state.error,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions.Default,
        )
        ContadorCaracteres(actual = state.nuevoNombre.length, maximo = ReglasPerfil.NOMBRE_MAX)

        Spacer(Modifier.height(70.dp))
        BotonPerfil(texto = "Guardar", onClick = onGuardar, cargando = state.guardando)
        Spacer(Modifier.height(24.dp))
        BotonPerfil(texto = "Cancelar", onClick = onCancelar, color = BotonCancelar, enabled = habilitado)
    }
}
