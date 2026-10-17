package com.levelia.levelia.presentation.screens.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.levelia.levelia.R
import com.levelia.levelia.ui.theme.PerfilTarjetaBorde
import com.levelia.levelia.ui.theme.PerfilTarjetaFondo

/** Las 5 pestañas de la barra inferior del diseño (la etiqueta ya viene dibujada en la imagen). */
enum class TabInferior(@DrawableRes val imagen: Int, val descripcion: String) {
    MISIONES(R.drawable.nav_misiones, "Misiones"),
    LOGROS(R.drawable.nav_logros, "Logros"),
    INICIO(R.drawable.nav_inicio, "Inicio"),
    PERSONALIZACION(R.drawable.nav_personalizacion, "Personalización"),
    PERFIL(R.drawable.nav_perfil, "Perfil"),
}

/**
 * Barra de navegación inferior. La pestaña [seleccion] es más ancha y más alta, como en el diseño.
 * Se usa en el parámetro `barraInferior` de LeveliaScreen, con `Modifier.align(Alignment.BottomCenter)`.
 */
@Composable
fun BarraInferior(
    seleccion: TabInferior,
    onTab: (TabInferior) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ALTO_BARRA_INFERIOR),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TabInferior.entries.forEach { tab ->
                val activa = tab == seleccion
                Box(
                    modifier = Modifier
                        .weight(if (activa) 1.25f else 1f)
                        .height(if (activa) ALTO_BARRA_INFERIOR else 62.dp)
                        .clip(forma)
                        .background(PerfilTarjetaFondo)
                        .border(2.dp, PerfilTarjetaBorde, forma)
                        .clickable(role = Role.Tab) { onTab(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = ImageBitmap.imageResource(tab.imagen),
                        contentDescription = tab.descripcion,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                    )
                }
            }
        }
        // Relleno bajo las pestañas, hasta el borde inferior (detrás de la barra de gestos)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(PerfilTarjetaFondo),
        )
    }
}
