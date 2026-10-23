package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.IconoMision
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_MISIONES
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaDetalleMision
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.presentation.screens.components.recurso
import com.levelia.levelia.ui.theme.BotonCancelar
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.PlusJakartaSans
import java.time.LocalDate

/**
 * "Detalles de misión": se abre desde la lista de Misiones y, más adelante, justo después de crear una
 * misión propia ([desdeCrear] = true: imagen siempre amarilla, sin "Ajustar dificultad" y sin barra
 * inferior). Ya aceptada y activa, en lugar de "Aceptar misión" / "Ajustar dificultad" muestra solo
 * "Aumentar progreso". Recibe estado + lambdas; los datos y las reglas viven en MisionesViewModel.
 */
@Composable
fun DetallesMisionScreen(
    item: MisionConProgreso,
    enviando: Boolean,
    error: String?,
    onBack: () -> Unit,
    onAceptar: () -> Unit,
    onAjustarDificultad: () -> Unit,
    onAumentarProgreso: () -> Unit,
    onCerrarAviso: () -> Unit,
    onTab: (TabInferior) -> Unit,
    /** La que se eligió en "Ajustes de misión" y aún no se guardó en la base (se envía al aceptar). */
    dificultadElegida: Dificultad? = null,
    desdeCrear: Boolean = false,
    /** Para saber si hoy todavía se puede registrar progreso (una vez al día). */
    hoy: LocalDate = LocalDate.now(),
) {
    val mision = item.mision
    val aceptada = item.aceptada
    // Las misiones creadas por el usuario usan el portapapeles amarillo
    val icono = if (desdeCrear) IconoMision.PORTAPAPELES else mision.icono
    // Una misión "Única" no se puede cambiar de dificultad (lo rechaza aceptar_mision en el servidor)
    val puedeAjustar = !desdeCrear && mision.dificultad != Dificultad.UNICA
    // Ya aceptada y en curso: no se vuelve a aceptar ni a ajustar, solo se suma progreso
    val enCurso = aceptada?.estado == EstadoMision.ACTIVA

    val barra: @Composable BoxScope.() -> Unit = {
        BarraInferior(
            seleccion = TabInferior.MISIONES,
            onTab = onTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 16.dp,
        margenHorizontal = MARGEN_MISIONES,
        decoracion = { BurbujasDecorativas() },
        barraInferior = barra.takeIf { !desdeCrear },
        overlay = {
            DialogoPerfilUi(
                dialogo = error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarAviso,
                onConfirmarEliminar = {},
            )
        },
    ) {
        TituloPixel(
            "Detalles de misión",
            tamano = 24.sp,
            interlineado = 1f,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(60.dp))
        TituloPixel(
            mision.titulo,
            tamano = 24.sp,
            interlineado = 1.2f,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(36.dp))
        Image(
            bitmap = ImageBitmap.imageResource(icono.recurso()),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        if (mision.descripcion.isNotBlank()) {
            Text(
                mision.descripcion,
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(27.dp))

        TarjetaDetalleMision(
            dificultad = dificultadElegida ?: aceptada?.dificultad ?: mision.dificultad,
            xp = dificultadElegida?.xp ?: aceptada?.xp ?: mision.xp,
            meta = dificultadElegida?.veces ?: aceptada?.meta ?: mision.meta,
            fechaLimite = mision.fechaLimite ?: aceptada?.fechaLimite,
            estado = aceptada?.estado,
            progreso = aceptada?.textoProgreso.takeIf { enCurso },
        )
        Spacer(Modifier.height(34.dp))

        if (enCurso) {
            val puedeRegistrar = aceptada!!.puedeRegistrarHoy(hoy)
            BotonPerfil(
                texto = "Aumentar progreso",
                onClick = onAumentarProgreso,
                color = BotonGuardar,
                enabled = puedeRegistrar,
                cargando = enviando,
            )
            if (!puedeRegistrar) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (hoy.isAfter(aceptada.fechaLimite)) "Se venció el plazo de esta misión."
                    else "Ya registraste tu progreso hoy. ¡Vuelve mañana!",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            BotonPerfil(texto = "Aceptar misión", onClick = onAceptar, color = BotonGuardar, cargando = enviando)
            if (puedeAjustar) {
                Spacer(Modifier.height(24.dp))
                BotonPerfil(
                    texto = "Ajustar dificultad",
                    onClick = onAjustarDificultad,
                    color = BotonCancelar,
                    enabled = !enviando,
                )
            }
        }
    }
}
