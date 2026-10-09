package com.levelia.levelia.presentation.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.BuildConfig
import com.levelia.levelia.data.repository.AuthRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.DialogoAuth
import com.levelia.levelia.domain.models.ErroresLogin
import com.levelia.levelia.domain.models.ErroresPaso1
import com.levelia.levelia.domain.models.RegisterState
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

    // --- Registro ---

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    /** Solo en builds debug: no hay envío real de emails, así que se muestra el código en pantalla. */
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

    private var cuentaRegresivaJob: Job? = null

    init {
        checkCurrentSession()
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
        if (posicion !in 0 until LONGITUD_CODIGO) return
        val soloDigitos = digito.filter(Char::isDigit)

        // Pegar el código completo en cualquier casilla lo reparte entre las 6
        if (soloDigitos.length == LONGITUD_CODIGO) {
            actualizar { copy(codigoVerificacion = soloDigitos, error = null) }
            return
        }

        val casillas = _registerState.value.codigoVerificacion
            .padEnd(LONGITUD_CODIGO, ' ')
            .toCharArray()
        // Al escribir sobre una casilla llena llegan 2 caracteres: se queda el nuevo (el último).
        // digito = "" (borrado) deja la casilla vacía.
        casillas[posicion] = soloDigitos.lastOrNull() ?: ' '
        actualizar { copy(codigoVerificacion = String(casillas).trimEnd(), error = null) }
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
                    actualizarCodigoDePrueba(estado.email)
                    iniciarCuentaRegresiva()
                }
                is AuthResult.Error -> actualizar {
                    if (r.message.startsWith("Ya existe")) {
                        // El correo ya está registrado: se marca el campo, sin diálogo
                        copy(errores = errores.copy(email = r.message))
                    } else {
                        copy(error = r.message, dialogo = DialogoAuth.ERROR_SERVIDOR)
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
                    actualizar { copy(pasoActual = 3, emailVerificado = true, tiempoReenvio = 0) }
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
                    actualizarCodigoDePrueba(estado.email)
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
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo incorrecto."
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
                    // El repositorio solo falla en el login por credenciales: se marcan ambos campos
                    is AuthResult.Error -> _erroresLogin.value = ErroresLogin(
                        email = "Correo incorrecto.",
                        contraseña = "Contraseña incorrecta.",
                    )
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _user.value = null
        _isAuthenticated.value = false
        _loginPassword.value = ""
        _loginPasswordVisible.value = false
        _error.value = null
        _erroresLogin.value = ErroresLogin()
        resetRegistro()
        loadRememberedEmail()
    }

    fun checkCurrentSession() {
        val r = authRepository.getCurrentUser()
        if (r is AuthResult.Success && r.data != null) {
            _user.value = r.data
            _isAuthenticated.value = true
        }
    }

    fun loadRememberedEmail() {
        val email = authRepository.getRememberedEmail()
        _loginEmail.value = email.orEmpty()
        _rememberMe.value = email != null
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
            estado.nombre.trim().length < NOMBRE_MINIMO -> "Prueba con otro nombre."
            else -> null
        },
        edad = when {
            estado.edad == 0 -> OBLIGATORIO
            estado.edad !in EDAD_MINIMA..EDAD_MAXIMA -> "Introduce una edad válida."
            else -> null
        },
        email = when {
            estado.email.isEmpty() -> OBLIGATORIO
            !Patterns.EMAIL_ADDRESS.matcher(estado.email).matches() -> "Correo incorrecto."
            else -> null
        },
        contraseña = when {
            estado.contraseña.isEmpty() -> OBLIGATORIO
            estado.contraseña.length < CONTRASENA_MINIMA -> "Contraseña incorrecta."
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

    private fun codigoCompleto(estado: RegisterState) =
        estado.codigoVerificacion.length == LONGITUD_CODIGO && estado.codigoVerificacion.all(Char::isDigit)

    private fun actualizarCodigoDePrueba(email: String) {
        if (BuildConfig.DEBUG) _codigoDePrueba.value = authRepository.debugPendingCode(email)
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
        const val NOMBRE_MINIMO = 3
        const val EDAD_MINIMA = 13
        const val EDAD_MAXIMA = 120
        const val CONTRASENA_MINIMA = 8
        const val OBLIGATORIO = "Este campo es obligatorio."
    }
}
