package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.levelia.levelia.R
import com.levelia.levelia.domain.ReglasNivel
import com.levelia.levelia.domain.models.ConfirmacionDemo
import com.levelia.levelia.domain.models.ModoDemoUiState
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.ContadorEsmeraldas
import com.levelia.levelia.presentation.screens.components.EtiquetaPixel
import com.levelia.levelia.presentation.screens.components.LeveliaDialog
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERSONALIZACION
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaProgreso
import com.levelia.levelia.presentation.screens.components.TextoAyudaSkin
import com.levelia.levelia.presentation.screens.components.TituloBarra
import com.levelia.levelia.ui.theme.BotonGuardar
import com.levelia.levelia.ui.theme.BotonPeligro
import com.levelia.levelia.ui.theme.PerfilPanelBorde
import com.levelia.levelia.ui.theme.PerfilPanelFondo
import com.levelia.levelia.ui.theme.PressStart2P
import com.levelia.levelia.ui.theme.XpPistaBorde

/**
 * "Modo demo" (solo cuentas marcadas en supabase/modo_demo.sql): botones para mostrar la app sin esperar días.
 * No está en el diseño de Figma: usa los componentes que ya tiene la app. Recibe estado + lambdas.
 *
 * Los avisos "¡Logro desbloqueado!" y "¡Skin desbloqueada!" no se dibujan aquí: salen solos encima (NavGraph)
 * cuando el cambio desbloquea algo, igual que al reclamar una misión.
 */
@Composable
fun ModoDemoScreen(
    estado: ModoDemoUiState,
    progreso: ProgresoData,
    esmeraldas: Int,
    /** Los niveles de las skins (5, 10, 25, 50, 100) para "Dejar listo para subir al nivel…". */
    niveles: List<Int>,
    onBack: () -> Unit,
    onAvanzarDia: () -> Unit,
    onPrepararNivel: (Int) -> Unit,
    onSubirNivel: () -> Unit,
    onSumarEsmeraldas: () -> Unit,
    onReiniciar: () -> Unit,
    onConfirmarReinicio: () -> Unit,
    onCancelarConfirmacion: () -> Unit,
    onCerrarError: () -> Unit,
    /** La barra de abajo, para ir rápido a Misiones a cumplir y reclamar después de preparar un nivel. */
    onTab: (TabInferior) -> Unit,
) {
    val libre = estado.activo && !estado.enviando
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 24.dp,
        margenHorizontal = MARGEN_PERSONALIZACION,
        barraSuperior = {
            TituloBarra("Modo demo")
            ContadorEsmeraldas(esmeraldas)
        },
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(seleccion = TabInferior.INICIO, onTab = onTab, modifier = Modifier.align(Alignment.BottomCenter))
        },
        overlay = {
            if (estado.confirmacion == ConfirmacionDemo.Reiniciar) {
                LeveliaDialog(
                    titulo = "¿Reiniciar la cuenta demo?",
                    mensaje = "Vuelve al nivel 1 y se borran sus misiones, logros, skins y esmeraldas.",
                    icono = R.drawable.ic_dialog_peligro,
                    onEntendido = onConfirmarReinicio,
                    textoBoton = "Reiniciar",
                    altoIcono = 58.dp,
                    colorBoton = BotonPeligro,
                    textoSecundario = "Cancelar",
                    onSecundario = onCancelarConfirmacion,
                    onDescartar = onCancelarConfirmacion,
                )
            }
            estado.error?.let {
                LeveliaDialog(
                    titulo = "No se pudo",
                    mensaje = it,
                    icono = R.drawable.ic_dialog_info,
                    onEntendido = onCerrarError,
                    textoBoton = "Aceptar",
                    altoIcono = 52.dp,
                    colorBoton = BotonGuardar,
                )
            }
        },
    ) {
        if (estado.activo) {
            BotonesDemo(estado, progreso, niveles, libre, onAvanzarDia, onPrepararNivel, onSubirNivel, onSumarEsmeraldas, onReiniciar)
        } else {
            TextoAyudaSkin("Esta cuenta no tiene el modo demo.")
        }
    }
}

/** Lo que se ve si la cuenta es demo: el progreso y los grupos de botones. */
@Composable
private fun BotonesDemo(
    estado: ModoDemoUiState,
    progreso: ProgresoData,
    niveles: List<Int>,
    libre: Boolean,
    onAvanzarDia: () -> Unit,
    onPrepararNivel: (Int) -> Unit,
    onSubirNivel: () -> Unit,
    onSumarEsmeraldas: () -> Unit,
    onReiniciar: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        TextoAyudaSkin("Para mostrar la app sin esperar días. Lo que hagas aquí cambia esta cuenta de verdad.")
        Spacer(Modifier.height(18.dp))
        TarjetaProgreso(progreso)
        Spacer(Modifier.height(10.dp))
        TextoAyudaSkin(
            buildString {
                append("Días adelantados: ${estado.dias}")
                estado.mensaje?.let { append("\n$it") }
            },
        )

        SeccionDemo("Tiempo")
        BotonPerfil("Avanzar 1 día", onAvanzarDia, enabled = libre)
        AyudaDemo("Como si fuera mañana: puedes volver a registrar progreso y vencen los plazos.")

        SeccionDemo("Nivel")
        EtiquetaPixel("Dejar listo para subir al nivel:", tamano = 10.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            niveles.forEach { nivel ->
                FichaNivelDemo(
                    nivel = nivel,
                    enabled = libre && nivel > progreso.nivel,
                    onClick = { onPrepararNivel(nivel) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AyudaDemo("Quedas a 1 XP: cumple y reclama una misión para ver la subida, el logro y la skin.")
        Spacer(Modifier.height(12.dp))
        BotonPerfil("+1 nivel", onSubirNivel, enabled = libre && progreso.nivel < ReglasNivel.NIVEL_MAX)

        SeccionDemo("Esmeraldas")
        BotonPerfil("+500 esmeraldas", onSumarEsmeraldas, enabled = libre)
        AyudaDemo("Para comprar skins en la tienda de Personalización.")

        Spacer(Modifier.height(30.dp))
        BotonPerfil("Reiniciar cuenta demo", onReiniciar, color = BotonPeligro, enabled = libre)
        AyudaDemo("Vuelve al nivel 1 para repetir la demostración.")
    }
}

/** Título pixel de cada grupo de botones. */
@Composable
private fun SeccionDemo(titulo: String) {
    Spacer(Modifier.height(28.dp))
    EtiquetaPixel(titulo)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AyudaDemo(texto: String) {
    Spacer(Modifier.height(8.dp))
    TextoAyudaSkin(texto)
}

/** Ficha de un nivel ("25"). Apagada si la cuenta ya llegó a ese nivel. */
@Composable
private fun FichaNivelDemo(nivel: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(forma)
            .background(PerfilPanelFondo)
            .border(2.dp, if (enabled) XpPistaBorde else PerfilPanelBorde, forma)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Nivel $nivel", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$nivel", color = Color.White, fontFamily = PressStart2P, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
