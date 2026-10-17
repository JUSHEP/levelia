package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.domain.RequisitosContrasena
import com.levelia.levelia.domain.models.RecuperarState
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.CampoPerfil
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.EtiquetaPixel
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERFIL
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.RequisitoContrasena
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.BotonCancelar

/**
 * Igual que "Cambiar contraseña" del Perfil, pero sin la contraseña actual (ya se comprobó el código)
 * y sin la barra inferior (no hay sesión abierta).
 */
@Composable
fun RestablecerContrasenaScreen(
    state: RecuperarState,
    requisitos: RequisitosContrasena,
    onNuevaChanged: (String) -> Unit,
    onConfirmarChanged: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    onCerrarDialogo: () -> Unit,
) {
    val habilitado = !state.cargando
    val teclado = KeyboardOptions(keyboardType = KeyboardType.Password)

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onCancelar.takeIf { habilitado },
        espacioSuperior = 0.dp,
        margenHorizontal = MARGEN_PERFIL,
        decoracion = { BurbujasDecorativas() },
        overlay = {
            DialogoPerfilUi(dialogo = state.dialogo, onCerrar = onCerrarDialogo, onConfirmarEliminar = {})
        },
    ) {
        TituloPixel(
            "Restablecer\ncontraseña",
            tamano = 20.sp,
            textAlign = TextAlign.Center,
            interlineado = 1.05f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ic_candado_morado),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier.height(106.dp).align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(36.dp))
        EtiquetaPixel("Nueva contraseña", tamano = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        CampoPerfil(
            value = state.nueva,
            onValueChange = onNuevaChanged,
            esContrasena = true,
            enabled = habilitado,
            keyboardOptions = teclado,
        )

        // Lista de requisitos: se actualiza mientras se escribe
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            RequisitoContrasena("Mínimo 8 caracteres", requisitos.longitud)
            RequisitoContrasena("Una mayúscula", requisitos.mayuscula)
            RequisitoContrasena("Una minúscula", requisitos.minuscula)
            RequisitoContrasena("Un número", requisitos.numero)
        }

        Spacer(Modifier.height(16.dp))
        EtiquetaPixel("Confirmar contraseña", tamano = 14.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        CampoPerfil(
            value = state.confirmar,
            onValueChange = onConfirmarChanged,
            esContrasena = true,
            enabled = habilitado,
            keyboardOptions = teclado,
        )

        Spacer(Modifier.height(16.dp))
        BotonPerfil(texto = "Guardar", onClick = onGuardar, cargando = state.cargando)
        Spacer(Modifier.height(16.dp))
        BotonPerfil(texto = "Cancelar", onClick = onCancelar, color = BotonCancelar, enabled = habilitado)
    }
}
