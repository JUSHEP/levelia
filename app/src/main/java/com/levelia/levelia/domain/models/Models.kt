package com.levelia.levelia.domain.models

import java.util.UUID

/** pasoActual: 1 formulario, 2 código OTP, 3 avatar, 4 cuenta creada. */
data class RegisterState(
    val pasoActual: Int = 1,
    val nombre: String = "",
    val edad: Int = 0,
    val email: String = "",
    val contraseña: String = "",
    /** Un carácter por casilla; las casillas vacías intermedias son espacios. */
    val codigoVerificacion: String = "",
    val avatarSeleccionado: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val intentosFallidos: Int = 0,
    val tiempoReenvio: Int = 0,
    val emailVerificado: Boolean = false,
    val perfilCreado: Boolean = false,
    /** Mensajes bajo cada campo del paso 1 (campo en rojo). */
    val errores: ErroresPaso1 = ErroresPaso1(),
    /** Diálogo pixel-art que se muestra encima del paso 1; null = ninguno. */
    val dialogo: DialogoAuth? = null,
)

/** Mensaje de error por campo del formulario del paso 1; null = campo sin error. */
data class ErroresPaso1(
    val nombre: String? = null,
    val edad: String? = null,
    val email: String? = null,
    val contraseña: String? = null,
) {
    val hayErrores: Boolean get() = nombre != null || edad != null || email != null || contraseña != null
}

/** Mensaje de error por campo del login; null = campo sin error. */
data class ErroresLogin(
    val email: String? = null,
    val contraseña: String? = null,
)

/** Diálogos de error del diseño (título, ícono y texto los define la UI). */
enum class DialogoAuth { VALIDACION, CORREO_INVALIDO, CONTRASENA_DEBIL, ERROR_SERVIDOR }

data class UserData(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val email: String = "",
    val edad: Int = 0,
    val avatar: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

sealed class AuthResult<T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error<T>(val message: String) : AuthResult<T>()
    class Loading<T> : AuthResult<T>()
}

sealed class Destinations(val route: String) {
    data object LoginScreen : Destinations("login")
    data object RegisterStep1Screen : Destinations("register_step1")
    data object RegisterStep2Screen : Destinations("register_step2")
    data object RegisterStep3Screen : Destinations("register_step3")
    data object RegisterStep4Screen : Destinations("register_step4")
    data object HomeScreen : Destinations("home")
}

enum class Avatar { AVATAR_1, AVATAR_2, AVATAR_3 }
