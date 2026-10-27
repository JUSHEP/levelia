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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.levelia.levelia.domain.ReglasPerfil
import com.levelia.levelia.domain.models.Destinations
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.presentation.screens.CambiarAvatarScreen
import com.levelia.levelia.presentation.screens.CambiarContrasenaScreen
import com.levelia.levelia.presentation.screens.RecoverCodeScreen
import com.levelia.levelia.presentation.screens.CrearMisionScreen
import com.levelia.levelia.presentation.screens.AjustarDificultadScreen
import com.levelia.levelia.presentation.screens.CuentaEliminadaScreen
import com.levelia.levelia.presentation.screens.DetallesMisionScreen
import com.levelia.levelia.presentation.screens.EditarNombreScreen
import com.levelia.levelia.presentation.screens.HomeScreen
import com.levelia.levelia.presentation.screens.LoginScreen
import com.levelia.levelia.presentation.screens.MisionesScreen
import com.levelia.levelia.presentation.screens.PerfilScreen
import com.levelia.levelia.presentation.screens.RecoverPasswordScreen
import com.levelia.levelia.presentation.screens.RestablecerContrasenaScreen
import com.levelia.levelia.presentation.screens.RegisterStep1Screen
import com.levelia.levelia.presentation.screens.RegisterStep2Screen
import com.levelia.levelia.presentation.screens.RegisterStep3Screen
import com.levelia.levelia.presentation.screens.RegisterStep4Screen
import com.levelia.levelia.presentation.screens.components.LeveliaBackground
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.viewmodel.AuthViewModel
import com.levelia.levelia.presentation.viewmodel.MisionesViewModel
import com.levelia.levelia.presentation.viewmodel.PerfilViewModel
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

// Pantallas que solo se ven con sesión iniciada
private val RUTAS_PROTEGIDAS = listOf(
    Destinations.HomeScreen.route,
    Destinations.PerfilScreen.route,
    Destinations.EditarNombreScreen.route,
    Destinations.CambiarAvatarScreen.route,
    Destinations.CambiarContrasenaScreen.route,
    Destinations.MisionesScreen.route,
    Destinations.CrearMisionScreen.route,
    Destinations.DetallesMisionScreen.route,
    Destinations.AjustarDificultadScreen.route,
)

/**
 * [authViewModel], [perfilViewModel] y [misionesViewModel] deben crearse fuera del NavHost (ver MainActivity): si cada
 * destino llamara a hiltViewModel() tendría su propia instancia y el estado se perdería entre pantallas.
 */
@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    perfilViewModel: PerfilViewModel,
    misionesViewModel: MisionesViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Mientras se revisa si quedó una sesión guardada se muestra solo el fondo, para no parpadear el login
    val restaurando by authViewModel.restaurandoSesion.collectAsStateWithLifecycle()
    if (restaurando) {
        LeveliaBackground {}
    } else {
        NavGraphContenido(authViewModel, perfilViewModel, misionesViewModel, modifier, navController)
    }
}

@Composable
private fun NavGraphContenido(
    authViewModel: AuthViewModel,
    perfilViewModel: PerfilViewModel,
    misionesViewModel: MisionesViewModel,
    modifier: Modifier,
    navController: NavHostController,
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
            // Sin sesión no debe quedar nada del usuario anterior en el módulo Perfil
            if (!autenticado) {
                perfilViewModel.limpiar()
                misionesViewModel.limpiar()
            }
            val rutaActual = navController.currentDestination?.route ?: return@collect
            if (autenticado && rutaActual != Destinations.HomeScreen.route) {
                navController.navigateToHomeClear()
                // Después de navegar: con Home en pantalla, el reset no dispara la sincronización de pasos
                authViewModel.resetRegistro()
            } else if (!autenticado && rutaActual in RUTAS_PROTEGIDAS) {
                navController.navigateToLoginClear()
            }
        }
    }

    // Lo que cambia el Perfil (nombre, avatar) se copia a la sesión para que Home lo muestre al volver
    LaunchedEffect(authViewModel, perfilViewModel) {
        perfilViewModel.perfil
            .map { it.usuario }
            .distinctUntilChanged()
            .collect { usuario ->
                if (usuario != null && authViewModel.isAuthenticated.value) authViewModel.actualizarUsuario(usuario)
            }
    }

    // Cuenta eliminada: primero se abre "Cuenta eliminada" y después se cierra la sesión local
    LaunchedEffect(navController, authViewModel, perfilViewModel) {
        perfilViewModel.cuentaEliminada.collect { eliminada ->
            if (eliminada) {
                navController.navigateToCuentaEliminadaClear()
                authViewModel.logout()
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
                onForgotPasswordClick = {
                    authViewModel.iniciarRecuperacion()
                    navController.navigate(Destinations.RecoverPasswordScreen.route)
                },
            )
        }

        composable(Destinations.RecoverPasswordScreen.route) {
            val email by authViewModel.recuperarEmail.collectAsStateWithLifecycle()
            val error by authViewModel.recuperarError.collectAsStateWithLifecycle()
            val cargando by authViewModel.recuperarLoading.collectAsStateWithLifecycle()
            RecoverPasswordScreen(
                email = email,
                error = error,
                isLoading = cargando,
                onEmailChanged = authViewModel::onRecuperarEmailChanged,
                onEnviar = {
                    authViewModel.enviarRecuperacion {
                        navController.navigate(Destinations.RecoverCodeScreen.route)
                    }
                },
                onRegistrate = {
                    authViewModel.resetRegistro()
                    navController.navigate(Destinations.RegisterStep1Screen.route) {
                        popUpTo(Destinations.LoginScreen.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.RecoverCodeScreen.route) {
            val email by authViewModel.recuperarEmail.collectAsStateWithLifecycle()
            val estado by authViewModel.recuperacion.collectAsStateWithLifecycle()
            RecoverCodeScreen(
                email = email,
                state = estado,
                onCodigoDigitChanged = authViewModel::onRecuperarCodigoChanged,
                onVerificar = {
                    authViewModel.verificarCodigoRecuperacion {
                        // Sin volver atrás al código: ya se usó
                        navController.navigate(Destinations.RestablecerContrasenaScreen.route) {
                            popUpTo(Destinations.LoginScreen.route) { inclusive = false }
                        }
                    }
                },
                onReenviar = authViewModel::reenviarCodigoRecuperacion,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.RestablecerContrasenaScreen.route) {
            val estado by authViewModel.recuperacion.collectAsStateWithLifecycle()
            val salirAlLogin = {
                authViewModel.cancelarRecuperacion()
                navController.popBackStack(Destinations.LoginScreen.route, inclusive = false)
            }
            BackHandler(enabled = !estado.cargando) { salirAlLogin() }
            RestablecerContrasenaScreen(
                state = estado,
                requisitos = ReglasPerfil.requisitosContrasena(estado.nueva),
                onNuevaChanged = authViewModel::onRecuperarNuevaChanged,
                onConfirmarChanged = authViewModel::onRecuperarConfirmarChanged,
                onGuardar = authViewModel::restablecerContrasena,
                onCancelar = { salirAlLogin() },
                onCerrarDialogo = {
                    // Tras "Contraseña actualizada" se vuelve al login; los demás avisos solo se cierran
                    if (estado.dialogo?.tipo == TipoDialogoPerfil.CONTRASENA_ACTUALIZADA) {
                        salirAlLogin()
                    } else {
                        authViewModel.cerrarDialogoRecuperacion()
                    }
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
            HomeScreen(
                user = user,
                onLogout = authViewModel::logout,
                onTab = { navController.irATab(it) },
            )
        }

        // ------------------------------ Módulo Misiones ------------------------------

        composable(Destinations.MisionesScreen.route) {
            val estado by misionesViewModel.estado.collectAsStateWithLifecycle()

            // Cada vez que se abre se vuelve a leer de la base de datos
            LaunchedEffect(Unit) { misionesViewModel.cargar() }

            MisionesScreen(
                estado = estado,
                onBack = { navController.popBackStack() },
                onFiltro = misionesViewModel::onFiltro,
                onMision = { navController.navigate(Destinations.DetallesMisionScreen.conId(it.mision.id)) },
                onCrear = {
                    misionesViewModel.iniciarCrear()
                    navController.navigate(Destinations.CrearMisionScreen.route)
                },
                onReclamar = misionesViewModel::reclamar,
                onCerrarAviso = misionesViewModel::cerrarAviso,
                onTab = { navController.irATab(it) },
            )
        }

        composable(
            route = Destinations.DetallesMisionScreen.route,
            arguments = listOf(navArgument("misionId") { type = NavType.LongType }),
        ) { entrada ->
            val misionId = entrada.arguments?.getLong("misionId") ?: 0L
            val estado by misionesViewModel.estado.collectAsStateWithLifecycle()
            val item = estado.misiones.firstOrNull { it.mision.id == misionId }
            if (item == null) {
                // La misión ya no está en la lista: no hay nada que mostrar
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                DetallesMisionScreen(
                    item = item,
                    enviando = estado.enviando,
                    error = estado.error,
                    onBack = { navController.popBackStack() },
                    // Acepta con la dificultad guardada en "Ajustes de misión" (la aplica el servidor) y se
                    // queda en el detalle, que ahora muestra "Aumentar progreso"
                    onAceptar = { misionesViewModel.aceptar(misionId) },
                    onAjustarDificultad = { navController.navigate(Destinations.AjustarDificultadScreen.conId(misionId)) },
                    // Suma 1 al progreso y se queda en el detalle; si con eso se cumplió, vuelve a la lista
                    // para reclamar la recompensa
                    onAumentarProgreso = {
                        item.aceptada?.let { aceptada ->
                            misionesViewModel.registrarProgreso(aceptada.id) {
                                val actual = misionesViewModel.estado.value.misiones.firstOrNull { it.mision.id == misionId }
                                if (actual?.estado == EstadoMision.COMPLETADA) navController.popBackStack()
                            }
                        }
                    },
                    onCerrarAviso = misionesViewModel::cerrarAviso,
                    onTab = { navController.irATab(it) },
                    hoy = estado.hoy,
                )
            }
        }

        composable(
            route = Destinations.AjustarDificultadScreen.route,
            arguments = listOf(navArgument("misionId") { type = NavType.LongType }),
        ) { entrada ->
            val misionId = entrada.arguments?.getLong("misionId") ?: 0L
            val estado by misionesViewModel.estado.collectAsStateWithLifecycle()
            val item = estado.misiones.firstOrNull { it.mision.id == misionId }
            if (item == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                AjustarDificultadScreen(
                    // La guardada en la base (la lista ya la trae aplicada) o la de la misión
                    dificultadInicial = item.mision.dificultad,
                    // Guarda en la base sin aceptar y vuelve a la lista, que se relee con las veces y XP nuevas
                    onGuardar = { dificultad ->
                        misionesViewModel.ajustarDificultad(misionId, dificultad) {
                            navController.popBackStack(Destinations.MisionesScreen.route, inclusive = false)
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onTab = { navController.irATab(it) },
                    guardando = estado.enviando,
                    error = estado.error,
                    onCerrarAviso = misionesViewModel::cerrarAviso,
                )
            }
        }

        composable(Destinations.CrearMisionScreen.route) {
            val crear by misionesViewModel.crear.collectAsStateWithLifecycle()
            CrearMisionScreen(
                state = crear,
                onBack = { navController.popBackStack() },
                onTitulo = misionesViewModel::onTitulo,
                onDescripcion = misionesViewModel::onDescripcion,
                onDificultad = misionesViewModel::onDificultad,
                onFechaLimite = misionesViewModel::onFechaLimite,
                // Valida el formulario y, si todo está bien, crea la misión y vuelve a la lista
                onCrear = { misionesViewModel.crearMision { navController.popBackStack() } },
            )
        }

        // ------------------------------- Módulo Perfil -------------------------------

        composable(Destinations.PerfilScreen.route) {
            val perfil by perfilViewModel.perfil.collectAsStateWithLifecycle()
            val dialogo by perfilViewModel.dialogo.collectAsStateWithLifecycle()
            val usuarioSesion by authViewModel.user.collectAsStateWithLifecycle()

            // Cada vez que se abre Perfil se vuelve a leer de la base de datos
            LaunchedEffect(Unit) { perfilViewModel.cargarPerfil(usuarioSesion) }

            PerfilScreen(
                perfil = perfil,
                dialogo = dialogo,
                // El menú (hamburguesa) del diseño todavía no tiene destino: no hace nada
                onMenu = {},
                onEditarNombre = {
                    perfilViewModel.iniciarEditarNombre()
                    navController.navigate(Destinations.EditarNombreScreen.route)
                },
                onCambiarAvatar = {
                    perfilViewModel.iniciarCambiarAvatar()
                    navController.navigate(Destinations.CambiarAvatarScreen.route)
                },
                onCambiarContrasena = {
                    perfilViewModel.iniciarCambiarContrasena()
                    navController.navigate(Destinations.CambiarContrasenaScreen.route)
                },
                onEliminarCuenta = perfilViewModel::solicitarEliminarCuenta,
                onConfirmarEliminar = perfilViewModel::confirmarEliminarCuenta,
                onCerrarDialogo = perfilViewModel::cerrarDialogo,
                onTab = { navController.irATab(it) },
            )
        }

        composable(Destinations.EditarNombreScreen.route) {
            val perfil by perfilViewModel.perfil.collectAsStateWithLifecycle()
            val estado by perfilViewModel.editarNombre.collectAsStateWithLifecycle()
            val dialogo by perfilViewModel.dialogo.collectAsStateWithLifecycle()
            EditarNombreScreen(
                nombreActual = perfil.usuario?.nombre.orEmpty(),
                avatarActual = perfil.usuario?.avatar.orEmpty(),
                state = estado,
                dialogo = dialogo,
                onNuevoNombreChanged = perfilViewModel::onNuevoNombreChanged,
                onGuardar = { perfilViewModel.guardarNombre { navController.popBackStack() } },
                onCancelar = { navController.popBackStack() },
                onCerrarDialogo = perfilViewModel::cerrarDialogo,
                onTab = { navController.irATab(it) },
            )
        }

        composable(Destinations.CambiarAvatarScreen.route) {
            val perfil by perfilViewModel.perfil.collectAsStateWithLifecycle()
            val estado by perfilViewModel.cambiarAvatar.collectAsStateWithLifecycle()
            val dialogo by perfilViewModel.dialogo.collectAsStateWithLifecycle()
            CambiarAvatarScreen(
                avatarActual = perfil.usuario?.avatar.orEmpty(),
                state = estado,
                dialogo = dialogo,
                onAvatarSeleccionado = perfilViewModel::onAvatarSeleccionado,
                onGuardar = { perfilViewModel.guardarAvatar { navController.popBackStack() } },
                onCancelar = { navController.popBackStack() },
                onCerrarDialogo = perfilViewModel::cerrarDialogo,
                onTab = { navController.irATab(it) },
            )
        }

        composable(Destinations.CambiarContrasenaScreen.route) {
            val estado by perfilViewModel.contrasena.collectAsStateWithLifecycle()
            val dialogo by perfilViewModel.dialogo.collectAsStateWithLifecycle()
            CambiarContrasenaScreen(
                state = estado,
                requisitos = perfilViewModel.requisitos(estado.nueva),
                dialogo = dialogo,
                onActualChanged = perfilViewModel::onContrasenaActualChanged,
                onNuevaChanged = perfilViewModel::onContrasenaNuevaChanged,
                onConfirmarChanged = perfilViewModel::onContrasenaConfirmarChanged,
                onGuardar = perfilViewModel::guardarContrasena,
                onCancelar = { navController.popBackStack() },
                onCerrarDialogo = {
                    // Tras "Contraseña actualizada" se vuelve a Perfil; los demás avisos solo se cierran
                    val fueExito = dialogo?.tipo == TipoDialogoPerfil.CONTRASENA_ACTUALIZADA
                    perfilViewModel.cerrarDialogo()
                    if (fueExito) navController.popBackStack()
                },
                onTab = { navController.irATab(it) },
            )
        }

        composable(Destinations.CuentaEliminadaScreen.route) {
            // La cuenta ya no existe: "atrás" no hace nada, solo se sale con "Volver al inicio"
            BackHandler {}
            CuentaEliminadaScreen(onVolverAlInicio = { navController.navigateToLoginClear() })
        }
    }
}

/** Barra inferior: Misiones, Inicio y Perfil tienen pantalla; Logros y Personalización todavía no hacen nada. */
private fun NavHostController.irATab(tab: TabInferior) {
    val destino = when (tab) {
        TabInferior.MISIONES -> Destinations.MisionesScreen.route
        TabInferior.INICIO -> Destinations.HomeScreen.route
        TabInferior.PERFIL -> Destinations.PerfilScreen.route
        else -> return
    }
    if (currentDestination?.route == destino) return
    navigate(destino) {
        popUpTo(Destinations.HomeScreen.route) { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToCuentaEliminadaClear() {
    navigate(Destinations.CuentaEliminadaScreen.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
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
