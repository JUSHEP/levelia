package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.presentation.screens.components.ErrorMessage
import com.levelia.levelia.presentation.screens.components.LoadingButton
import com.levelia.levelia.ui.theme.AvatarCian
import com.levelia.levelia.ui.theme.AvatarMorado
import com.levelia.levelia.ui.theme.AvatarRojo
import com.levelia.levelia.ui.theme.TextoSecundario
import com.levelia.levelia.ui.theme.TextoSobreClaro

fun Avatar.color(): Color = when (this) {
    Avatar.AVATAR_1 -> AvatarMorado
    Avatar.AVATAR_2 -> AvatarRojo
    Avatar.AVATAR_3 -> AvatarCian
}

fun Avatar.etiqueta(): String = "Avatar ${ordinal + 1}"

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

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // TODO: reemplazar el ícono por las imágenes de personajes de AppImagenes/
            Avatar.entries.forEach { avatar ->
                AvatarCircular(
                    avatar = avatar,
                    seleccionado = state.avatarSeleccionado == avatar.name,
                    habilitado = habilitado,
                    onClick = { onAvatarSelected(avatar.name) },
                )
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
private fun AvatarCircular(avatar: Avatar, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
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
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = if (seleccionado) 0.35f else 0.12f),
                border = BorderStroke(if (seleccionado) 4.dp else 2.dp, color.copy(alpha = if (seleccionado) 1f else 0.6f)),
                // Tamaño fijo: si el seleccionado creciera, su etiqueta quedaría más abajo que las otras
                modifier = Modifier.size(84.dp),
            ) {
                Icon(
                    Icons.Filled.Face,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(16.dp),
                )
            }
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
