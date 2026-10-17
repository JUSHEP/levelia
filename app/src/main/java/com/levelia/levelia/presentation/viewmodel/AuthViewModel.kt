package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.AuthRepository
import com.levelia.levelia.domain.ReglasPerfil
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.DialogoAuth
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.ErroresLogin
import com.levelia.levelia.domain.models.ErroresPaso1
import com.levelia.levelia.domain.models.RecuperarState
import com.levelia.levelia.domain.models.RegisterState
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.domain.models.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // --- Sesión ---

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _user = MutableStateFlow<UserData?>(null)
    val user: StateFlow<UserData?> = _user.asStateFlow()

    /** true mientras se revisa si quedó una sesión guardada (el NavGraph espera para elegir la pantalla de inicio). */
    private val _restaurandoSesion = MutableStateFlow(true)
    val restaurandoSesion: StateFlow<Boolean> = _restaurandoSesion.asStateFlow()

    // --- Registro ---

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    /** Ya no se usa (el código llega por correo desde Supabase); la pantalla del paso 2 todavía lo recibe. */
    private val _codigoDePrueba = MutableStateFlow<String?>(null)
    val codigoDePrueba: StateFlow<String?> = _codigoDePrueba.asStateFlow()

    // --- Login ---

    private val _loginEmail = MutableStateFlow("")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    private val _loginPasswordVisible = MutableStateFlow(false)
    val loginPasswordVisible: StateFlow<Boolean> = _loginPasswordVisible.asStateFlow()

    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    /** Carga del login (el registro usa RegisterState.isLoading). */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Error del login (el registro usa RegisterState.error). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Mensajes bajo cada campo del login (campo en rojo). */
    private val _erroresLogin = MutableStateFlow(ErroresLogin())
    val erroresLogin: StateFlow<ErroresLogin> = _erroresLogin.asStateFlow()

    // --- Recuperar contraseña ---

    private val _recuperarEmail = MutableStateFlow("")
    val recuperarEmail: StateFlow<String> = _recuperarEmail.asStateFlow()

    private val _recuperarError = MutableStateFlow<String?>(null)
    val recuperarError: StateFlow<String?> = _recuperarError.asStateFlow()

    private val _recuperarLoading = MutableStateFlow(false)
    val recuperarLoading: StateFlow<Boolean> = _recuperarLoading.asStateFlow()

    /** Código y contraseña nueva, después de enviar el correo de recuperación. */
    private val _recuperacion = MutableStateFlow(RecuperarState())
    val recuperacion: StateFlow<RecuperarState> = _recuperacion.asStateFlow()

    private var cuentaRegresivaJob: Job? = null
    private var cuentaRegresivaRecuperacionJob: Job? = null

    init {
        restaurarSesion()
        loadRememberedEmail()
    }

    // ============================== REGISTRO ==============================

    // Al editar un campo se le quita su mensaje de error (el campo deja de verse en rojo)
    fun onNombreChanged(nombre: String) =
        actualizar { copy(nombre = nombre, error = null, errores = errores.copy(nombre = null)) }

    fun onEdadChanged(edad: Int) =
        actualizar { copy(edad = edad, error = null, errores = errores.copy(edad = null)) }

    fun onEmailChanged(email: String) =
        actualizar { copy(email = email.trim(), error = null, errores = errores.copy(email = null)) }

    fun onContraseñaChanged(contraseña: String) =
        actualizar { copy(contraseña = contraseña, error = null, errores = errores.copy(contraseña = null)) }

    fun cerrarDialogo() = actualizar { copy(dialogo = null) }

    fun onCodigoDigitChanged(posicion: Int, digito: String) {
        val codigo = codigoConDigito(_registerState.value.codigoVerificacion, posicion, digito) ?: return
        actualizar { copy(codigoVerificacion = codigo, error = null) }
    }

    fun onAvatarSelected(avatar: String) {
        if (Avatar.entries.none { it.name == avatar }) return
        actualizar { copy(avatarSeleccionado = avatar, error = null) }
    }

    fun enviarFormularioRegistro() {
        val estado = _registerState.value
        if (estado.isLoading) return
        val erroresValidacion = validarPaso1(estado)
        if (erroresValidacion.hayErrores) {
            actualizar { copy(errores = erroresValidacion, dialogo = dialogoPara(estado, erroresValidacion)) }
            return
        }

        ejecutarRegistro {
            // El nombre de usuario es único: se revisa antes de crear la cuenta
            when (val d = authRepository.nombreDisponible(estado.nombre.trim())) {
                is AuthResult.Success -> if (!d.data) {
                    actualizar { copy(errores = errores.copy(nombre = NOMBRE_NO_DISPONIBLE)) }
                    return@ejecutarRegistro
                }
                is AuthResult.Error -> {
                    actualizar { copy(error = d.message, dialogo = DialogoAuth.ERROR_SERVIDOR) }
                    return@ejecutarRegistro
                }
                is AuthResult.Loading -> Unit
            }

            // Volvió al paso 1 con el correo ya verificado en este registro: solo se corrige el perfil
            if (estado.emailVerificado) {
                guardarPerfilYSeguir()
                return@ejecutarRegistro
            }

            when (val r = authRepository.signUp(estado.email, estado.contraseña)) {
                is AuthResult.Success -> {
                    actualizar {
                        copy(
                            pasoActual = 2,
                            codigoVerificacion = "",
                            intentosFallidos = 0,
                            emailVerificado = false,
                        )
                    }
                    iniciarCuentaRegresiva()
                }
                is AuthResult.Error -> {
                    if (!r.message.startsWith("Ya existe")) {
                        actualizar { copy(error = r.message, dialogo = DialogoAuth.ERROR_SERVIDOR) }
                        return@ejecutarRegistro
                    }
                    // Correo ya registrado: si quedó confirmado sin perfil (registro a medias) se retoma
                    when (val retomar = authRepository.retomarRegistro(estado.email, estado.contraseña)) {
                        is AuthResult.Success -> {
                            actualizar { copy(emailVerificado = true) }
                            guardarPerfilYSeguir()
                        }
                        // Cuenta completa (o contraseña distinta): se marca el campo, sin diálogo
                        is AuthResult.Error -> actualizar {
                            if (retomar.message.startsWith("Ya existe")) {
                                copy(errores = errores.copy(email = retomar.message))
                            } else {
                                copy(error = retomar.message, dialogo = DialogoAuth.ERROR_SERVIDOR)
                            }
                        }
                        is AuthResult.Loading -> Unit
                    }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun verificarCodigo() {
        val estado = _registerState.value
        if (estado.isLoading || !codigoCompleto(estado) || estado.intentosFallidos >= MAX_INTENTOS) return

        ejecutarRegistro {
            when (val r = authRepository.verifyCode(estado.email, estado.codigoVerificacion)) {
                is AuthResult.Success -> {
                    cuentaRegresivaJob?.cancel()
                    _codigoDePrueba.value = null
                    actualizar { copy(emailVerificado = true, tiempoReenvio = 0) }
                    // El código confirma la cuenta: el perfil se guarda ya, no al terminar el paso 3
                    guardarPerfilYSeguir()
                }
                is AuthResult.Error -> {
                    val intentos = estado.intentosFallidos + 1
                    if (intentos >= MAX_INTENTOS) {
                        // Sin intentos: el reenvío queda disponible de inmediato
                        cuentaRegresivaJob?.cancel()
                        actualizar {
                            copy(
                                intentosFallidos = intentos,
                                codigoVerificacion = "",
                                tiempoReenvio = 0,
                                error = "Demasiados intentos. Solicita un nuevo código.",
                            )
                        }
                    } else {
                        val restantes = MAX_INTENTOS - intentos
                        actualizar {
                            copy(
                                intentosFallidos = intentos,
                                codigoVerificacion = "",
                                error = "${r.message} Te queda${if (restantes == 1) "" else "n"} $restantes " +
                                    "intento${if (restantes == 1) "" else "s"}.",
                            )
                        }
                    }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun onReenviarCodigo() {
        val estado = _registerState.value
        if (estado.isLoading || estado.tiempoReenvio > 0) return

        ejecutarRegistro {
            when (val r = authRepository.resendCode(estado.email)) {
                is AuthResult.Success -> {
                    actualizar { copy(intentosFallidos = 0, codigoVerificacion = "") }
                    iniciarCuentaRegresiva()
                }
                is AuthResult.Error -> actualizar { copy(error = r.message) }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun completarRegistro() {
        val estado = _registerState.value
        if (estado.isLoading || estado.avatarSeleccionado.isEmpty()) return

        ejecutarRegistro {
            val r = authRepository.createUserProfile(
                email = estado.email,
                nombre = estado.nombre.trim(),
                edad = estado.edad,
                avatar = estado.avatarSeleccionado,
            )
            when (r) {
                is AuthResult.Success -> {
                    _user.value = r.data
                    actualizar { copy(pasoActual = 4, perfilCreado = true) }
                }
                is AuthResult.Error -> actualizar { copy(error = r.message) }
                is AuthResult.Loading -> Unit
            }
        }
    }

    /**
     * Crea (o corrige) el perfil con los datos del paso 1 y pasa a elegir avatar. Mientras no se elija,
     * queda el primer avatar. Si falla, el paso 2 muestra "Email verificado" y el botón Continuar.
     */
    private suspend fun guardarPerfilYSeguir() {
        val estado = _registerState.value
        val r = authRepository.createUserProfile(
            email = estado.email,
            nombre = estado.nombre.trim(),
            edad = estado.edad,
            avatar = estado.avatarSeleccionado.ifEmpty { Avatar.entries.first().name },
        )
        when (r) {
            is AuthResult.Success -> {
                _user.value = r.data
                actualizar { copy(pasoActual = 3, error = null, dialogo = null) }
            }
            is AuthResult.Error -> actualizar {
                if (r.message.startsWith("Ese nombre")) {
                    // Alguien tomó el nombre mientras tanto: se corrige en el paso 1
                    copy(pasoActual = 1, errores = errores.copy(nombre = NOMBRE_NO_DISPONIBLE))
                } else {
                    copy(error = r.message)
                }
            }
            is AuthResult.Loading -> Unit
        }
    }

    /** El perfil cambió en la pantalla Perfil (nombre o avatar): se refleja en el resto de la app. */
    fun actualizarUsuario(usuario: UserData) {
        _user.value = usuario
    }

    /** Botón "Empezar" del paso 4: con isAuthenticated = true el NavGraph lleva a Home. */
    fun empezar() {
        if (_registerState.value.perfilCreado && _user.value != null) {
            _isAuthenticated.value = true
        }
    }

    /** Paso 2 → 1 (corregir datos) y 3 → 2. Del 4 no se retrocede: la cuenta ya existe. */
    fun volverPaso() {
        val estado = _registerState.value
        if (estado.isLoading) return
        when (estado.pasoActual) {
            2 -> {
                cuentaRegresivaJob?.cancel()
                _codigoDePrueba.value = null
                actualizar {
                    copy(pasoActual = 1, codigoVerificacion = "", tiempoReenvio = 0, error = null, dialogo = null)
                }
            }
            3 -> actualizar { copy(pasoActual = 2, error = null) }
        }
    }

    /** Paso 2 con el email ya verificado (se volvió desde el paso 3): continuar sin otro código. */
    fun continuarConEmailVerificado() {
        if (_registerState.value.emailVerificado) actualizar { copy(pasoActual = 3, error = null) }
    }

    fun resetRegistro() {
        cuentaRegresivaJob?.cancel()
        _codigoDePrueba.value = null
        _registerState.value = RegisterState()
    }

    // =============================== LOGIN ===============================

    fun onLoginEmailChanged(email: String) {
        _loginEmail.value = email.trim()
        _error.value = null
        _erroresLogin.update { it.copy(email = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _loginPassword.value = password
        _error.value = null
        _erroresLogin.update { it.copy(contraseña = null) }
    }

    fun togglePasswordVisibility() {
        _loginPasswordVisible.update { !it }
    }

    fun onRememberMeChanged(recordar: Boolean) {
        _rememberMe.value = recordar
    }

    fun login() {
        if (_isLoading.value) return
        val email = _loginEmail.value
        val password = _loginPassword.value
        val erroresCampos = ErroresLogin(
            email = when {
                email.isEmpty() -> OBLIGATORIO
                !ReglasPerfil.correoValido(email) -> "Correo incorrecto."
                else -> null
            },
            contraseña = if (password.isEmpty()) OBLIGATORIO else null,
        )
        if (erroresCampos.email != null || erroresCampos.contraseña != null) {
            _erroresLogin.value = erroresCampos
            return
        }

        _isLoading.value = true
        _error.value = null
        _erroresLogin.value = ErroresLogin()
        viewModelScope.launch {
            try {
                when (val r = authRepository.login(email, password)) {
                    is AuthResult.Success -> {
                        if (_rememberMe.value) {
                            authRepository.saveRememberedEmail(email)
                        } else {
                            authRepository.clearRememberedEmail()
                        }
                        _user.value = r.data
                        _loginPassword.value = ""
                        _loginPasswordVisible.value = false
                        _isAuthenticated.value = true
                    }
                    // Credenciales malas: se marcan ambos campos. Otros fallos (sin internet, servidor,
                    // correo sin verificar) se muestran como mensaje bajo el correo.
                    is AuthResult.Error -> _erroresLogin.value =
                        if (r.message.startsWith("Email o contraseña")) {
                            ErroresLogin(email = "Correo incorrecto.", contraseña = "Contraseña incorrecta.")
                        } else {
                            ErroresLogin(email = r.message)
                        }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
        _user.value = null
        _isAuthenticated.value = false
        _loginPassword.value = ""
        _loginPasswordVisible.value = false
        _error.value = null
        _erroresLogin.value = ErroresLogin()
        resetRegistro()
        loadRememberedEmail()
    }

    /** Si Supabase guardó una sesión, entra directo a Home con el perfil leído de la base de datos. */
    private fun restaurarSesion() {
        viewModelScope.launch {
            try {
                val r = authRepository.restaurarSesion()
                if (r is AuthResult.Success) {
                    val usuario = r.data
                    if (usuario != null) {
                        _user.value = usuario
                        _isAuthenticated.value = true
                    }
                }
            } finally {
                _restaurandoSesion.value = false
            }
        }
    }

    fun loadRememberedEmail() {
        val email = authRepository.getRememberedEmail()
        _loginEmail.value = email.orEmpty()
        _rememberMe.value = email != null
    }

    /** Al abrir "Recupera tu contraseña": arranca con el correo del login si ya lo escribió. */
    fun iniciarRecuperacion() {
        _recuperarEmail.value = _loginEmail.value
        _recuperarError.value = null
        _recuperarLoading.value = false
        cuentaRegresivaRecuperacionJob?.cancel()
        _recuperacion.value = RecuperarState()
    }

    fun onRecuperarEmailChanged(email: String) {
        _recuperarEmail.value = email.trim()
        _recuperarError.value = null
    }

    /** [onEnviado] se llama en el hilo principal cuando la solicitud salió bien. */
    fun enviarRecuperacion(onEnviado: () -> Unit) {
        if (_recuperarLoading.value) return
        val email = _recuperarEmail.value
        _recuperarError.value = when {
            email.isEmpty() -> OBLIGATORIO
            !ReglasPerfil.correoValido(email) -> "Correo incorrecto."
            else -> null
        }
        if (_recuperarError.value != null) return

        _recuperarLoading.value = true
        viewModelScope.launch {
            try {
                when (val r = authRepository.solicitarRecuperacion(email)) {
                    is AuthResult.Success -> {
                        _recuperacion.value = RecuperarState()
                        iniciarCuentaRegresivaRecuperacion()
                        onEnviado()
                    }
                    is AuthResult.Error -> _recuperarError.value = r.message
                    is AuthResult.Loading -> Unit
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "Error al solicitar la recuperación")
                _recuperarError.value = "No pudimos enviar el correo. Inténtalo más tarde."
            } finally {
                _recuperarLoading.value = false
            }
        }
    }

    fun onRecuperarCodigoChanged(posicion: Int, digito: String) {
        val codigo = codigoConDigito(_recuperacion.value.codigo, posicion, digito) ?: return
        _recuperacion.update { it.copy(codigo = codigo, error = null) }
    }

    /** Igual que el paso 2 del registro: 3 intentos y luego hay que pedir otro código. */
    fun verificarCodigoRecuperacion(onVerificado: () -> Unit) {
        val estado = _recuperacion.value
        if (estado.cargando || !codigoCompleto(estado.codigo) || estado.intentosFallidos >= MAX_INTENTOS) return

        ejecutarRecuperacion {
            when (val r = authRepository.verificarCodigoRecuperacion(_recuperarEmail.value, estado.codigo)) {
                is AuthResult.Success -> {
                    cuentaRegresivaRecuperacionJob?.cancel()
                    _recuperacion.update { it.copy(codigoVerificado = true, tiempoReenvio = 0) }
                    onVerificado()
                }
                is AuthResult.Error -> {
                    val intentos = estado.intentosFallidos + 1
                    val restantes = MAX_INTENTOS - intentos
                    if (intentos >= MAX_INTENTOS) cuentaRegresivaRecuperacionJob?.cancel()
                    _recuperacion.update {
                        it.copy(
                            intentosFallidos = intentos,
                            codigo = "",
                            tiempoReenvio = if (intentos >= MAX_INTENTOS) 0 else it.tiempoReenvio,
                            error = if (intentos >= MAX_INTENTOS) {
                                "Demasiados intentos. Solicita un nuevo código."
                            } else {
                                "${r.message} Te queda${if (restantes == 1) "" else "n"} $restantes " +
                                    "intento${if (restantes == 1) "" else "s"}."
                            },
                        )
                    }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    /** Vuelve a pedir el correo "Reset password" (Supabase manda un código nuevo). */
    fun reenviarCodigoRecuperacion() {
        val estado = _recuperacion.value
        if (estado.cargando || estado.tiempoReenvio > 0) return

        ejecutarRecuperacion {
            when (val r = authRepository.solicitarRecuperacion(_recuperarEmail.value)) {
                is AuthResult.Success -> {
                    _recuperacion.update { it.copy(intentosFallidos = 0, codigo = "") }
                    iniciarCuentaRegresivaRecuperacion()
                }
                is AuthResult.Error -> _recuperacion.update { it.copy(error = r.message) }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun onRecuperarNuevaChanged(texto: String) = _recuperacion.update { it.copy(nueva = texto) }
    fun onRecuperarConfirmarChanged(texto: String) = _recuperacion.update { it.copy(confirmar = texto) }

    /** Mismas reglas y diálogos que "Cambiar contraseña" del Perfil, sin pedir la contraseña actual. */
    fun restablecerContrasena() {
        val estado = _recuperacion.value
        if (estado.cargando || !estado.codigoVerificado) return

        // El diálogo dice exactamente qué falta
        val motivo = ReglasPerfil.motivoContrasenaInvalida(estado.nueva, estado.confirmar)
        if (motivo != null) {
            _recuperacion.update { it.copy(dialogo = DialogoPerfil(TipoDialogoPerfil.CONTRASENA_INVALIDA, motivo)) }
            return
        }

        ejecutarRecuperacion {
            when (val r = authRepository.restablecerContrasena(estado.nueva)) {
                // El repositorio ya cerró la sesión temporal: se entra con la contraseña nueva desde el login
                is AuthResult.Success -> _recuperacion.update {
                    RecuperarState(
                        dialogo = DialogoPerfil(
                            TipoDialogoPerfil.CONTRASENA_ACTUALIZADA,
                            PerfilViewModel.MENSAJE_CONTRASENA_ACTUALIZADA,
                        ),
                    )
                }
                is AuthResult.Error -> _recuperacion.update {
                    val tipo = if (r.message.startsWith("La contraseña") || r.message.startsWith("La nueva contraseña")) {
                        TipoDialogoPerfil.CONTRASENA_INVALIDA
                    } else {
                        TipoDialogoPerfil.ERROR_SERVIDOR
                    }
                    it.copy(dialogo = DialogoPerfil(tipo, r.message))
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun cerrarDialogoRecuperacion() = _recuperacion.update { it.copy(dialogo = null) }

    /** Salir de "Restablecer contraseña" sin guardar: se cierra la sesión temporal del código. */
    fun cancelarRecuperacion() {
        if (_recuperacion.value.codigoVerificado) viewModelScope.launch { authRepository.logout() }
        cuentaRegresivaRecuperacionJob?.cancel()
        _recuperacion.value = RecuperarState()
    }

    fun limpiarError() {
        _error.value = null
        _erroresLogin.value = ErroresLogin()
    }

    // ============================== INTERNOS ==============================

    /** Un mensaje por campo con problema (los textos son los del diseño). */
    private fun validarPaso1(estado: RegisterState): ErroresPaso1 = ErroresPaso1(
        nombre = when {
            estado.nombre.isBlank() -> OBLIGATORIO
            !ReglasPerfil.nombreValido(estado.nombre.trim()) -> NOMBRE_NO_DISPONIBLE
            else -> null
        },
        edad = when {
            estado.edad == 0 -> OBLIGATORIO
            estado.edad !in EDAD_MINIMA..EDAD_MAXIMA -> "Introduce una edad válida."
            else -> null
        },
        email = when {
            estado.email.isEmpty() -> OBLIGATORIO
            !ReglasPerfil.correoValido(estado.email) -> "Correo incorrecto."
            else -> null
        },
        contraseña = when {
            estado.contraseña.isEmpty() -> OBLIGATORIO
            // Las mismas 4 reglas que "Cambiar contraseña" y que exige Supabase Auth
            !ReglasPerfil.requisitosContrasena(estado.contraseña).cumpleTodos -> "Contraseña incorrecta."
            else -> null
        },
    )

    /** Qué diálogo acompaña a los errores: campos vacíos > correo > contraseña; nombre/edad solo van bajo el campo. */
    private fun dialogoPara(estado: RegisterState, errores: ErroresPaso1): DialogoAuth? = when {
        estado.nombre.isBlank() || estado.edad == 0 || estado.email.isEmpty() || estado.contraseña.isEmpty() ->
            DialogoAuth.VALIDACION
        errores.email != null -> DialogoAuth.CORREO_INVALIDO
        errores.contraseña != null -> DialogoAuth.CONTRASENA_DEBIL
        else -> null
    }

    private fun codigoCompleto(estado: RegisterState) = codigoCompleto(estado.codigoVerificacion)

    private fun codigoCompleto(codigo: String) = codigo.length == LONGITUD_CODIGO && codigo.all(Char::isDigit)

    /** El código después de escribir [digito] en la casilla [posicion] (null = posición inválida). */
    private fun codigoConDigito(actual: String, posicion: Int, digito: String): String? {
        if (posicion !in 0 until LONGITUD_CODIGO) return null
        val soloDigitos = digito.filter(Char::isDigit)

        // Pegar el código completo en cualquier casilla lo reparte entre las 6
        if (soloDigitos.length == LONGITUD_CODIGO) return soloDigitos

        val casillas = actual.padEnd(LONGITUD_CODIGO, ' ').toCharArray()
        // Al escribir sobre una casilla llena llegan 2 caracteres: se queda el nuevo (el último).
        // digito = "" (borrado) deja la casilla vacía.
        casillas[posicion] = soloDigitos.lastOrNull() ?: ' '
        return String(casillas).trimEnd()
    }

    private fun iniciarCuentaRegresiva() {
        cuentaRegresivaJob?.cancel()
        cuentaRegresivaJob = viewModelScope.launch {
            for (segundos in COOLDOWN_REENVIO_SEGUNDOS downTo 0) {
                actualizar { copy(tiempoReenvio = segundos) }
                if (segundos > 0) delay(1_000)
            }
        }
    }

    private fun iniciarCuentaRegresivaRecuperacion() {
        cuentaRegresivaRecuperacionJob?.cancel()
        cuentaRegresivaRecuperacionJob = viewModelScope.launch {
            for (segundos in COOLDOWN_REENVIO_SEGUNDOS downTo 0) {
                _recuperacion.update { it.copy(tiempoReenvio = segundos) }
                if (segundos > 0) delay(1_000)
            }
        }
    }

    private fun ejecutarRecuperacion(bloque: suspend () -> Unit) {
        _recuperacion.update { it.copy(cargando = true, error = null, dialogo = null) }
        viewModelScope.launch {
            try {
                bloque()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "Error al recuperar la contraseña")
                _recuperacion.update {
                    it.copy(dialogo = DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, "Ocurrió un error inesperado. Inténtalo de nuevo."))
                }
            } finally {
                _recuperacion.update { it.copy(cargando = false) }
            }
        }
    }

    private fun ejecutarRegistro(bloque: suspend () -> Unit) {
        actualizar { copy(isLoading = true, error = null, dialogo = null) }
        viewModelScope.launch {
            try {
                bloque()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "Error en el registro")
                actualizar {
                    copy(error = "Ocurrió un error inesperado. Inténtalo de nuevo.", dialogo = DialogoAuth.ERROR_SERVIDOR)
                }
            } finally {
                actualizar { copy(isLoading = false) }
            }
        }
    }

    private inline fun actualizar(cambio: RegisterState.() -> RegisterState) {
        _registerState.update { it.cambio() }
    }

    companion object {
        const val LONGITUD_CODIGO = 6
        const val MAX_INTENTOS = 3
        const val COOLDOWN_REENVIO_SEGUNDOS = 60
        const val EDAD_MINIMA = 13
        const val EDAD_MAXIMA = 120
        const val OBLIGATORIO = "Este campo es obligatorio."
        const val NOMBRE_NO_DISPONIBLE = "Prueba con otro nombre."
    }
}
