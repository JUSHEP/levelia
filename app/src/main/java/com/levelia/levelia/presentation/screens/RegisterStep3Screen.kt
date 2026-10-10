package com.levelia.levelia.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelia.levelia.R
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.ui.theme.TextoSecundario
import com.levelia.levelia.ui.theme.TextoSobreClaro

/** Color del marco de cada personaje (el mismo de la imagen). */
fun Avatar.color(): Color = when (this) {
    Avatar.AVATAR_1 -> Color(0xFF3FBF4A)
    Avatar.AVATAR_2 -> Color(0xFF9B3FE0)
    Avatar.AVATAR_3 -> Color(0xFFE23A3A)
    Avatar.AVATAR_4 -> Color(0xFFF2B21E)
    Avatar.AVATAR_5 -> Color(0xFF1FD3E0)
    Avatar.AVATAR_6 -> Color(0xFFEC4F9B)
}

/** PNG de cada personaje (recortado de Characters.png) en res/drawable-nodpi. */
@DrawableRes
fun Avatar.recurso(): Int = when (this) {
    Avatar.AVATAR_1 -> R.drawable.avatar_1
    Avatar.AVATAR_2 -> R.drawable.avatar_2
    Avatar.AVATAR_3 -> R.drawable.avatar_3
    Avatar.AVATAR_4 -> R.drawable.avatar_4
    Avatar.AVATAR_5 -> R.drawable.avatar_5
    Avatar.AVATAR_6 -> R.drawable.avatar_6
}

fun Avatar.etiqueta(): String = "Avatar ${ordinal + 1}"

/** Dibuja el personaje sin suavizar para que el pixel art se vea nítido. */
@Composable
fun AvatarImagen(avatar: Avatar, modifier: Modifier = Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(avatar.recurso()),
        contentDescription = avatar.etiqueta(),
        filterQuality = FilterQuality.None,
        modifier = modifier,
    )
}

@Composable
fun RegisterStep3Screen(
    state: RegisterState,
    onAvatarSelected: (String) -> Unit,
    onCrearCuenta: () -> Unit,
    onBack: () -> Unit,
) {
    val habilitado = !state.isLoading

    AuthColumn(title = "Elige tu avatar", paso = 3, onBack = onBack.takeIf { habilitado }) {
        Text("Elige el que más te represente", color = TextoSecundario)

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 6 personajes en 2 filas de 3
            Avatar.entries.chunked(3).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    fila.forEach { avatar ->
                        AvatarOpcion(
                            avatar = avatar,
                            seleccionado = state.avatarSeleccionado == avatar.name,
                            habilitado = habilitado,
                            onClick = { onAvatarSelected(avatar.name) },
                        )
                    }
                }
            }
        }

        ErrorMessage(state.error)
        LoadingButton(
            text = "Crear cuenta",
            isLoading = state.isLoading,
            onClick = onCrearCuenta,
            enabled = state.avatarSeleccionado.isNotEmpty(),
        )
    }
}

@Composable
private fun AvatarOpcion(avatar: Avatar, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    val color = avatar.color()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            // Recorta el efecto de toque a una forma redondeada (sin esto se ve un rectángulo gris)
            .clip(RoundedCornerShape(16.dp))
            .selectable(
                selected = seleccionado,
                enabled = habilitado,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(8.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarImagen(
                avatar = avatar,
                // Tamaño fijo: si el seleccionado creciera, su etiqueta quedaría más abajo que las otras
                modifier = Modifier
                    .size(84.dp)
                    .alpha(if (seleccionado) 1f else 0.7f)
                    .then(
                        if (seleccionado) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier,
                    ),
            )
            if (seleccionado) {
                Surface(shape = CircleShape, color = color, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = TextoSobreClaro,
                        modifier = Modifier.padding(3.dp),
                    )
                }
            }
        }
        Text(
            avatar.etiqueta(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            color = if (seleccionado) color else TextoSecundario,
        )
    }
}
