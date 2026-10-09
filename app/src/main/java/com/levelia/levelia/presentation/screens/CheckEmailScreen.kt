package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelia.levelia.presentation.screens.components.LeveliaButton
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TituloPixel

/** "Revisa tu correo": confirma que se envió la solicitud y vuelve al login. */
@Composable
fun CheckEmailScreen(
    email: String,
    onVolverAlLogin: () -> Unit,
    onBack: () -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.PASTO_LEGAL,
        onBack = onBack,
        espacioSuperior = 64.dp,
    ) {
        TituloPixel("REVISA TU\nCORREO", tamano = 20.sp)
        Text(
            "Si $email está registrado, te enviamos las instrucciones para crear una contraseña nueva.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Revisa también la carpeta de spam.",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(Modifier.height(32.dp))
        LeveliaButton(text = "Volver a iniciar sesión", onClick = onVolverAlLogin)
    }
}
