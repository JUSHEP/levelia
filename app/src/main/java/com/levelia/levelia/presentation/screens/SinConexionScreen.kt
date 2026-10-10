package com.levelia.levelia.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TituloPixel

/** Pantalla "Sin conexión": tapa toda la app mientras el teléfono no tiene internet. */
@Composable
fun SinConexionScreen(onReintentar: () -> Unit) {
    // Mientras esté visible, "atrás" no debe mover la pantalla que quedó debajo
    BackHandler {}

    LeveliaScreen(
        pie = PieDePantalla.PASTO_LEGAL,
        espacioSuperior = 100.dp,
    ) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(20.dp))
        TituloPixel("SIN\nCONEXIÓN", tamano = 22.sp)
        Text(
            "Parece que te quedaste sin internet. Revisa tu conexión e inténtalo de nuevo.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(28.dp))
        LeveliaButton(text = "Reintentar", onClick = onReintentar)
    }
}
