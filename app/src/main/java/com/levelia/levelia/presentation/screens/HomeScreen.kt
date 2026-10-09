package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.UserData
import com.levelia.levelia.presentation.screens.components.AuthColumn
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.TextoSecundario

@Composable
fun HomeScreen(user: UserData?, onLogout: () -> Unit) {
    AuthColumn(title = "¡Hola${user?.nombre?.let { ", $it" }.orEmpty()}!") {
        if (user != null) {
            TarjetaUsuario(user)
        }
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MoradoAcento),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MoradoAcento),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun TarjetaUsuario(user: UserData) {
    val avatar = Avatar.entries.firstOrNull { it.name == user.avatar }
    val colorAvatar = avatar?.color() ?: MoradoAcento

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = colorAvatar.copy(alpha = 0.2f),
                    border = BorderStroke(3.dp, colorAvatar),
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null, tint = colorAvatar, modifier = Modifier.padding(12.dp))
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(user.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(avatar?.etiqueta() ?: user.avatar, color = colorAvatar)
                }
            }
            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.3f))
            DatoUsuario("Email", user.email)
            DatoUsuario("Edad", "${user.edad} años")
        }
    }
}

@Composable
private fun DatoUsuario(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
