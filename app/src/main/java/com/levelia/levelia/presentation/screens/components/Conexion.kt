package com.levelia.levelia.presentation.screens.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Estado de la conexión a internet del teléfono (se actualiza solo mientras la pantalla está en uso). */
class EstadoConexion(val cm: ConnectivityManager) {
    var hayConexion by mutableStateOf(consultar())
        private set

    /** Vuelve a preguntarle al teléfono (también lo usa el botón "Reintentar"). */
    fun revisar() {
        hayConexion = consultar()
    }

    fun marcarSinConexion() {
        hayConexion = false
    }

    // Solo se exige que haya una red con internet; no que Android ya la haya "validado",
    // para no bloquear la app en redes con portal cautivo (wifi de la universidad, por ejemplo)
    private fun consultar(): Boolean {
        val red = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

@Composable
fun rememberEstadoConexion(): EstadoConexion {
    val context = LocalContext.current
    val estado = remember {
        EstadoConexion(context.getSystemService(ConnectivityManager::class.java))
    }
    DisposableEffect(estado) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                estado.revisar()
            }

            override fun onLost(network: Network) {
                estado.marcarSinConexion()
            }
        }
        estado.cm.registerDefaultNetworkCallback(callback)
        onDispose { estado.cm.unregisterNetworkCallback(callback) }
    }
    return estado
}
