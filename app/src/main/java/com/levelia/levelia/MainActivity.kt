package com.levelia.levelia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.levelia.levelia.presentation.navigation.NavGraph
import com.levelia.levelia.presentation.viewmodel.AuthViewModel
import com.levelia.levelia.ui.theme.LeveliaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app siempre es oscura: íconos claros en las barras del sistema aunque el teléfono esté en modo claro
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LeveliaTheme {
                // Una sola instancia, ligada a la Activity y compartida por todas las pantallas
                val authViewModel: AuthViewModel = hiltViewModel()

                // Sin Scaffold ni padding de barras: el fondo del diseño llega hasta los bordes y cada
                // pantalla se encarga de dejar libre la barra de estado (ver LeveliaScreen)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    NavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}
