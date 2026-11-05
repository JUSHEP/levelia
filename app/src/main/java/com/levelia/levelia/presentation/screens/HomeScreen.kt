package com.levelia.levelia.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.domain.models.NOMBRE_SKIN_INICIAL
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.ImagenSkin
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERSONALIZACION
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaProgreso
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.NivelPildoraBorde
import com.levelia.levelia.ui.theme.NivelPildoraFondo
import com.levelia.levelia.ui.theme.PerfilPanelBorde
import com.levelia.levelia.ui.theme.PerfilPanelFondo
import com.levelia.levelia.ui.theme.PlusJakartaSans
import com.levelia.levelia.ui.theme.PressStart2P

/**
 * "Inicio" del diseño: nombre de la skin equipada, la píldora del nivel, la skin grande, el usuario y "Tu
 * progreso". El botón de menú abre "menú" (Cerrar sesión, Perfil, Configuraciones). Recibe estado + lambdas:
 * el nivel y la XP vienen de PerfilViewModel y la skin de PersonalizacionViewModel.
 */
@Composable
fun HomeScreen(
    nombreUsuario: String,
    progreso: ProgresoData,
    /** La skin equipada; null = todavía no llegó (se dibuja la inicial). */
    skin: Skin?,
    onPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    var menuAbierto by rememberSaveable { mutableStateOf(false) }

    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onMenu = { menuAbierto = true },
        espacioSuperior = 50.dp,
        margenHorizontal = MARGEN_PERSONALIZACION,
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(seleccion = TabInferior.INICIO, onTab = onTab, modifier = Modifier.align(Alignment.BottomCenter))
        },
        overlay = {
            if (menuAbierto) {
                MenuInicio(
                    onCerrar = { menuAbierto = false },
                    onCerrarSesion = {
                        menuAbierto = false
                        onCerrarSesion()
                    },
                    onPerfil = {
                        menuAbierto = false
                        onPerfil()
                    },
                )
            }
        },
    ) {
        TituloPixel(
            (skin?.nombre ?: NOMBRE_SKIN_INICIAL).uppercase(),
            tamano = 31.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(17.dp))
        PildoraNivel(progreso.nivel, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        ImagenSkin(
            skinId = skin?.id,
            modifier = Modifier.height(330.dp).align(Alignment.CenterHorizontally),
            descripcion = skin?.nombre ?: NOMBRE_SKIN_INICIAL,
        )
        Spacer(Modifier.height(25.dp))
        Text(
            if (nombreUsuario.isBlank()) "" else "@$nombreUsuario",
            color = Color.White,
            fontFamily = PressStart2P,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        TarjetaProgreso(progreso)
    }
}

/** "NVL. 10" amarillo bajo el nombre de la skin. */
@Composable
private fun PildoraNivel(nivel: Int, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(width = 200.dp, height = 46.dp)
            .clip(forma)
            .background(NivelPildoraFondo)
            .border(2.dp, NivelPildoraBorde, forma),
        contentAlignment = Alignment.Center,
    ) {
        Text("NVL. $nivel", color = Color.White, fontFamily = PressStart2P, fontSize = 14.sp, maxLines = 1)
    }
}

/**
 * "menú" del diseño, sobre el botón que lo abre. "Login" del diseño quedó como "Cerrar sesión" (es lo que
 * hace); "Configuraciones" todavía no tiene pantalla y se ve apagada. Tocar fuera o "atrás" lo cierra.
 */
@Composable
private fun MenuInicio(onCerrar: () -> Unit, onCerrarSesion: () -> Unit, onPerfil: () -> Unit) {
    BackHandler(onBack = onCerrar)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onCerrar),
    ) {
        val forma = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = MARGEN_PERSONALIZACION, top = 10.dp)
                .width(181.dp)
                .clip(forma)
                .background(PerfilPanelFondo)
                .border(2.dp, PerfilPanelBorde, forma)
                // Los toques dentro del panel no lo cierran
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "menú",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            )
            OpcionMenuInicio("Cerrar sesión", onCerrarSesion)
            OpcionMenuInicio("Perfil", onPerfil)
            OpcionMenuInicio("Configuraciones", onClick = null)
        }
    }
}

/** Opción morada del menú; [onClick] null = todavía no disponible (apagada). */
@Composable
private fun OpcionMenuInicio(texto: String, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(31.dp)
            .alpha(if (onClick != null) 1f else 0.45f)
            .clip(RoundedCornerShape(8.dp))
            .background(PerfilPanelBorde)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
    }
}
