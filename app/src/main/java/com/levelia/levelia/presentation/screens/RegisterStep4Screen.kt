package com.levelia.levelia.presentation.screens

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.TextoSecundario

/** Sin flecha atrás: la cuenta ya existe (el back del sistema se bloquea en el NavGraph). */
@Composable
fun RegisterStep4Screen(onEmpezar: () -> Unit) {
    val latido = rememberInfiniteTransition(label = "latido")
    val escala by latido.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "escalaCorazon",
    )

    AuthColumn(title = "¡Cuenta creada!", paso = 4) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = MoradoAcento,
            modifier = Modifier.size(96.dp).scale(escala),
        )
        Text(
            "Tu cuenta ha sido creada exitosamente",
            color = TextoSecundario,
            textAlign = TextAlign.Center,
        )
        LoadingButton(text = "Empezar", isLoading = false, onClick = onEmpezar)
    }
}
