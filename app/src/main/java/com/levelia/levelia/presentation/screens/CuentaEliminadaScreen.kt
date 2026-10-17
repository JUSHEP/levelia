package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.R
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERFIL
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TituloPixel
import com.levelia.levelia.ui.theme.PlusJakartaSans

/** Se muestra después de borrar la cuenta: no tiene flecha atrás ni barra inferior (ya no hay sesión). */
@Composable
fun CuentaEliminadaScreen(onVolverAlInicio: () -> Unit) {
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        espacioSuperior = 90.dp,
        margenHorizontal = MARGEN_PERFIL,
        decoracion = { BurbujasDecorativas() },
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.ic_candado_x),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier.height(109.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(22.dp))
        TituloPixel(
            "Cuenta eliminada",
            tamano = 24.sp,
            textAlign = TextAlign.Center,
            interlineado = 1.0f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "Tu cuenta ha sido eliminada satisfactoriamente",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontSize = 28.sp,
            lineHeight = 41.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(100.dp))
        BotonPerfil(texto = "Volver al inicio", onClick = onVolverAlInicio)
    }
}
