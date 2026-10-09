package com.levelia.levelia.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.levelia.levelia.domain.models.Destinations
import com.levelia.levelia.presentation.screens.HomeScreen
import com.levelia.levelia.presentation.screens.LoginScreen
import com.levelia.levelia.presentation.screens.RegisterStep1Screen
import com.levelia.levelia.presentation.screens.RegisterStep2Screen
import com.levelia.levelia.presentation.screens.RegisterStep3Screen
import com.levelia.levelia.presentation.screens.RegisterStep4Screen
import com.levelia.levelia.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Índice = pasoActual - 1
private val RUTAS_REGISTRO = listOf(
    Destinations.RegisterStep1Screen.route,
    Destinations.RegisterStep2Screen.route,
    Destinations.RegisterStep3Screen.route,
    Destinations.RegisterStep4Screen.route,
)

private const val DURACION_TRANSICION = 300

/**
 * [authViewModel] debe crearse fuera del NavHost (ver MainActivity): si cada destino llamara a
 * hiltViewModel() tendría su propia instancia y el estado del registro se perdería entre pasos.
 */
@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Se calcula una sola vez: cambiar startDestination después de construir el grafo lo reconstruye
    val startDestination = remember {
        if (authViewModel.isAuthenticated.value) Destinations.HomeScreen.route else Destinations.LoginScreen.route
    }

    // El paso del registro vive en el ViewModel y la navegación lo sigue: las pantallas solo
    // llaman funciones del ViewModel y nunca navegan por su cuenta entre pasos.
    LaunchedEffect(navController, authViewModel) {
        authViewModel.registerState
            .map { it.pasoActual }
            .distinctUntilChanged()
            .collect { paso ->
                val rutaActual = navController.currentDestination?.route
                if (rutaActual !in RUTAS_REGISTRO) return@collect
                val destino = RUTAS_REGISTRO.getOrNull(paso - 1) ?: return@collect
                if (destino == rutaActual) return@collect
                // Retroceder = volver a la pantalla que ya está en el back stack; avanzar = navegar
                if (!navController.popBackStack(destino, inclusive = false)) {
                    navController.navigate(destino)
                }
            }
    }

    // Login exitoso / "Empezar" del paso 4 → Home; logout → Login. Ambos vacían el back stack.
    LaunchedEffect(navController, authViewModel) {
        authViewModel.isAuthenticated.collect { autenticado ->
            val rutaActual = navController.currentDestination?.route ?: return@collect
            if (autenticado && rutaActual != Destinations.HomeScreen.route) {
                navController.navigateToHomeClear()
                // Después de navegar: con Home en pantalla, el reset no dispara la sincronización de pasos
                authViewModel.resetRegistro()
            } else if (!autenticado && rutaActual == Destinations.HomeScreen.route) {
                navController.navigateToLoginClear()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(DURACION_TRANSICION)) + fadeIn() },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(DURACION_TRANSICION)) + fadeOut() },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(DURACION_TRANSICION)) + fadeIn() },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(DURACION_TRANSICION)) + fadeOut() },
    ) {
        composable(Destinations.LoginScreen.route) {
            val email by authViewModel.loginEmail.collectAsStateWithLifecycle()
            val password by authViewModel.loginPassword.collectAsStateWithLifecycle()
            val rememberMe by authViewModel.rememberMe.collectAsStateWithLifecycle()
            val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
            val erroresLogin by authViewModel.erroresLogin.collectAsStateWithLifecycle()

            LoginScreen(
                email = email,
                password = password,
                rememberMe = rememberMe,
                isLoading = isLoading,
                errores = erroresLogin,
                onEmailChanged = authViewModel::onLoginEmailChanged,
                onPasswordChanged = authViewModel::onLoginPasswordChanged,
                onRememberMeChanged = authViewModel::onRememberMeChanged,
                onLogin = authViewModel::login,
                onRegisterClick = {
                    authViewModel.limpiarError()
                    authViewModel.resetRegistro()
                    navController.navigate(Destinations.RegisterStep1Screen.route)
                },
            )
        }

        composable(Destinations.RegisterStep1Screen.route) {
            val state by authViewModel.registerState.collectAsStateWithLifecycle()
            RegisterStep1Screen(
                state = state,
                onNombreChanged = authViewModel::onNombreChanged,
                onEdadChanged = authViewModel::onEdadChanged,
                onEmailChanged = authViewModel::onEmailChanged,
                onContraseñaChanged = authViewModel::onContraseñaChanged,
                onCrearCuenta = authViewModel::enviarFormularioRegistro,
                onCerrarDialogo = authViewModel::cerrarDialogo,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.RegisterStep2Screen.route) {
            val state by authViewModel.registerState.collectAsStateWithLifecycle()
            val codigoDePrueba by authViewModel.codigoDePrueba.collectAsStateWithLifecycle()
            BackHandler { authViewModel.volverPaso() }
            RegisterStep2Screen(
                state = state,
                codigoDePrueba = codigoDePrueba,
                onCodigoDigitChanged = authViewModel::onCodigoDigitChanged,
                onVerificar = authViewModel::verificarCodigo,
                onReenviar = authViewModel::onReenviarCodigo,
                onContinuar = authViewModel::continuarConEmailVerificado,
                onBack = authViewModel::volverPaso,
            )
        }

        composable(Destinations.RegisterStep3Screen.route) {
            val state by authViewModel.registerState.collectAsStateWithLifecycle()
            BackHandler { authViewModel.volverPaso() }
            RegisterStep3Screen(
                state = state,
                onAvatarSelected = authViewModel::onAvatarSelected,
                onCrearCuenta = authViewModel::completarRegistro,
                onBack = authViewModel::volverPaso,
            )
        }

        composable(Destinations.RegisterStep4Screen.route) {
            // La cuenta ya existe: "atrás" no hace nada, solo se sale con "Empezar"
            BackHandler {}
            RegisterStep4Screen(onEmpezar = authViewModel::empezar)
        }

        composable(Destinations.HomeScreen.route) {
            val user by authViewModel.user.collectAsStateWithLifecycle()
            HomeScreen(user = user, onLogout = authViewModel::logout)
        }
    }
}

private fun NavHostController.navigateToHomeClear() {
    navigate(Destinations.HomeScreen.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToLoginClear() {
    navigate(Destinations.LoginScreen.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
