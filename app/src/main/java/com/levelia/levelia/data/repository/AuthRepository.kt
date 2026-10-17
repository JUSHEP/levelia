package com.levelia.levelia.data.repository

import android.content.Context
import com.levelia.levelia.data.remote.AvatarUpdateDto
import com.levelia.levelia.data.remote.NombreUpdateDto
import com.levelia.levelia.data.remote.PerfilDto
import com.levelia.levelia.data.remote.PerfilNuevoDto
import com.levelia.levelia.data.remote.PerfilRegistroUpdateDto
import com.levelia.levelia.data.remote.ProgresoDto
import com.levelia.levelia.data.remote.SupabaseConfig
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.domain.models.UserData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Toda la lógica de cuentas y perfiles contra Supabase. Cada función devuelve [AuthResult]: el
 * ViewModel nunca ve excepciones. Las tablas y políticas están en supabase/setup.sql.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext context: Context,
) {

    // Solo guarda el correo recordado (nunca la contraseña)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ============================== REGISTRO ==============================

    /** Crea la cuenta y Supabase envía el código de 6 dígitos al correo. */
    suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        val clave = normalizar(email)
        val r = intentar("signUp") {
            supabase.auth.signUpWith(Email) {
                this.email = clave
                this.password = password
            }
        }
        return when (r) {
            is AuthResult.Success -> {
                // Con "Confirm email" activo, un correo ya registrado no da error: devuelve un
                // usuario sin identidades. Así se detecta.
                val usuario = r.data
                if (usuario != null && usuario.identities.isNullOrEmpty()) {
                    AuthResult.Error(CUENTA_EXISTENTE)
                } else {
                    AuthResult.Success(Unit)
                }
            }
            is AuthResult.Error -> AuthResult.Error(r.message)
            is AuthResult.Loading -> AuthResult.Error(ERROR_SERVIDOR)
        }
    }

    /** Verifica el código del correo; si es correcto deja la sesión iniciada. */
    suspend fun verifyCode(email: String, token: String): AuthResult<Unit> {
        val clave = normalizar(email)
        val r = intentar("verifyCode") {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = clave, token = token)
            Unit
        }
        // Supabase responde lo mismo si el código está vencido o es incorrecto
        return if (r is AuthResult.Error && r.message == ERROR_SERVIDOR) AuthResult.Error("Código incorrecto o vencido.") else r
    }

    suspend fun resendCode(email: String): AuthResult<Unit> {
        val clave = normalizar(email)
        return intentar("resendCode") {
            supabase.auth.resendEmail(OtpType.Email.SIGNUP, clave)
            Unit
        }
    }

    /**
     * El correo ya estaba registrado. Si esa cuenta quedó confirmada pero sin perfil (registro a
     * medias), se inicia sesión con la contraseña del formulario para que pueda terminarlo.
     * Si ya tiene perfil o la contraseña no coincide, es una cuenta existente: hay que iniciar sesión.
     */
    suspend fun retomarRegistro(email: String, password: String): AuthResult<Unit> {
        val clave = normalizar(email)
        val r = intentar("retomarRegistro") {
            supabase.auth.signInWith(Email) {
                this.email = clave
                this.password = password
            }
            val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
            leerPerfil(usuario.id) != null
        }
        return when {
            r is AuthResult.Success && !r.data -> AuthResult.Success(Unit)
            r is AuthResult.Success -> {
                logout()
                AuthResult.Error(CUENTA_EXISTENTE)
            }
            r is AuthResult.Error && r.message == CREDENCIALES_INCORRECTAS -> AuthResult.Error(CUENTA_EXISTENTE)
            r is AuthResult.Error -> AuthResult.Error(r.message)
            else -> AuthResult.Error(ERROR_SERVIDOR)
        }
    }

    /**
     * Guarda el perfil del usuario ya autenticado. Se llama al verificar el código (el registro queda
     * creado en la base de datos) y otra vez en el paso 3 para guardar el avatar elegido.
     * El progreso lo crea un trigger.
     */
    suspend fun createUserProfile(email: String, nombre: String, edad: Int, avatar: String): AuthResult<UserData> =
        intentar("createUserProfile") {
            val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
            val existente = leerPerfil(usuario.id)
            val perfil = if (existente == null) {
                supabase.from("perfiles")
                    .insert(PerfilNuevoDto(id = usuario.id, nombre = nombre, email = normalizar(email), edad = edad, avatar = avatar)) {
                        select()
                    }
                    .decodeSingle<PerfilDto>()
            } else {
                // Registro abandonado y retomado: se actualiza lo que se puede editar (nombre y avatar)
                supabase.from("perfiles")
                    .update(PerfilRegistroUpdateDto(nombre, avatar)) {
                        select()
                        filter { eq("id", usuario.id) }
                    }
                    .decodeSingle<PerfilDto>()
            }
            perfil.aUserData()
        }

    // ================================ SESIÓN ================================

    suspend fun login(email: String, password: String): AuthResult<UserData> {
        val clave = normalizar(email)
        val r = intentar("login") {
            supabase.auth.signInWith(Email) {
                this.email = clave
                this.password = password
            }
            Unit
        }
        if (r is AuthResult.Error) return AuthResult.Error(r.message)
        // Cuenta confirmada pero sin perfil: no se deja entrar, se termina desde el registro
        val perfil = intentar("login/perfil") {
            val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
            leerPerfil(usuario.id)?.aUserData()
        }
        return when (perfil) {
            is AuthResult.Success -> perfil.data?.let { AuthResult.Success(it) } ?: run {
                logout()
                AuthResult.Error(REGISTRO_INCOMPLETO)
            }
            is AuthResult.Error -> AuthResult.Error(perfil.message)
            is AuthResult.Loading -> AuthResult.Error(ERROR_SERVIDOR)
        }
    }

    suspend fun logout() {
        prefs.edit().remove(CLAVE_RECUPERACION_PENDIENTE).apply()
        try {
            supabase.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "No se pudo cerrar la sesión en el servidor")
        }
    }

    /**
     * Al abrir la app: si hay una sesión guardada, devuelve el usuario (leído de la base de datos).
     * Success(null) = no hay sesión.
     */
    suspend fun restaurarSesion(): AuthResult<UserData?> {
        if (!SupabaseConfig.configurado) return AuthResult.Success(null)
        return try {
            withTimeoutOrNull(TIEMPO_RESTAURAR_MS) { supabase.auth.awaitInitialization() }
            val usuario = supabase.auth.currentUserOrNull() ?: return AuthResult.Success(null)
            // Sesión que dejó un código de recuperación sin llegar a cambiar la contraseña: no se entra
            if (prefs.getBoolean(CLAVE_RECUPERACION_PENDIENTE, false)) {
                logout()
                return AuthResult.Success(null)
            }
            val perfil = try {
                leerPerfil(usuario.id)?.aUserData() ?: run {
                    // Registro a medias (sin perfil): no se entra a la app
                    logout()
                    return AuthResult.Success(null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Sesión restaurada, pero no se pudo leer el perfil")
                perfilMinimo(usuario.id, usuario.email)
            }
            AuthResult.Success(perfil)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Supabase: restaurarSesion")
            AuthResult.Success(null)
        }
    }

    // ============================ RECUPERAR CONTRASEÑA ============================

    /**
     * Supabase envía el correo "Reset password". Su plantilla usa {{ .Token }}: llega un código de
     * 6 dígitos (no un enlace). También sirve para reenviar el código.
     */
    suspend fun solicitarRecuperacion(email: String): AuthResult<Unit> {
        val clave = normalizar(email)
        return intentar("solicitarRecuperacion") {
            supabase.auth.resetPasswordForEmail(email = clave)
            Unit
        }
    }

    /** Verifica el código de recuperación; si es correcto deja una sesión iniciada para poder cambiar la contraseña. */
    suspend fun verificarCodigoRecuperacion(email: String, token: String): AuthResult<Unit> {
        val clave = normalizar(email)
        val r = intentar("verificarCodigoRecuperacion") {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = clave, token = token)
            // Si la app se cierra antes de guardar, esta sesión no debe abrir Home al volver (ver restaurarSesion)
            prefs.edit().putBoolean(CLAVE_RECUPERACION_PENDIENTE, true).apply()
        }
        // Supabase responde lo mismo si el código está vencido o es incorrecto
        return if (r is AuthResult.Error && r.message == ERROR_SERVIDOR) AuthResult.Error("Código incorrecto o vencido.") else r
    }

    /**
     * Pone la contraseña nueva (requiere la sesión que dejó [verificarCodigoRecuperacion]) y cierra
     * esa sesión: el usuario vuelve al login para entrar con la contraseña nueva.
     */
    suspend fun restablecerContrasena(nueva: String): AuthResult<Unit> {
        val r = intentar("restablecerContrasena") {
            supabase.auth.updateUser { password = nueva }
            Unit
        }
        if (r is AuthResult.Success) logout()
        return r
    }

    // =============================== PERFIL (lecturas) ===============================

    /** Lee el perfil directamente de la tabla `perfiles`. */
    suspend fun obtenerPerfil(): AuthResult<UserData> = intentar("obtenerPerfil") {
        val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
        leerPerfil(usuario.id)?.aUserData() ?: perfilMinimo(usuario.id, usuario.email)
    }

    /** Lee nivel y XP de la tabla `progreso` (si aún no hay fila, devuelve el valor inicial). */
    suspend fun obtenerProgreso(): AuthResult<ProgresoData> = intentar("obtenerProgreso") {
        val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
        supabase.from("progreso")
            .select { filter { eq("user_id", usuario.id) } }
            .decodeSingleOrNull<ProgresoDto>()
            ?.aProgresoData()
            ?: ProgresoData()
    }

    /** true si nadie más usa ese nombre de usuario (sin distinguir mayúsculas). Función SQL `nombre_disponible`. */
    suspend fun nombreDisponible(nombre: String): AuthResult<Boolean> = intentar("nombreDisponible") {
        supabase.postgrest
            .rpc("nombre_disponible", buildJsonObject { put("p_nombre", nombre.trim()) })
            .decodeAs<Boolean>()
    }

    // ============================== PERFIL (cambios) ==============================

    suspend fun actualizarNombre(nombre: String): AuthResult<UserData> = intentar("actualizarNombre") {
        val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
        supabase.from("perfiles")
            .update(NombreUpdateDto(nombre.trim())) {
                select()
                filter { eq("id", usuario.id) }
            }
            .decodeSingle<PerfilDto>()
            .aUserData()
    }

    suspend fun actualizarAvatar(avatar: String): AuthResult<UserData> = intentar("actualizarAvatar") {
        val usuario = supabase.auth.currentUserOrNull() ?: throw SesionExpiradaException()
        supabase.from("perfiles")
            .update(AvatarUpdateDto(avatar)) {
                select()
                filter { eq("id", usuario.id) }
            }
            .decodeSingle<PerfilDto>()
            .aUserData()
    }

    /**
     * Comprueba la contraseña actual iniciando sesión con ella y, si es correcta, la cambia.
     * La contraseña nueva nunca se guarda en tablas propias: solo vive en Supabase Auth.
     */
    suspend fun cambiarContrasena(actual: String, nueva: String): AuthResult<Unit> {
        val correo = supabase.auth.currentUserOrNull()?.email
            ?: return AuthResult.Error("Tu sesión expiró. Inicia sesión de nuevo.")

        val verificacion = intentar("cambiarContrasena/verificar") {
            supabase.auth.signInWith(Email) {
                this.email = correo
                this.password = actual
            }
            Unit
        }
        if (verificacion is AuthResult.Error) {
            return if (verificacion.message == CREDENCIALES_INCORRECTAS) {
                AuthResult.Error("La contraseña actual no es correcta.")
            } else {
                AuthResult.Error(verificacion.message)
            }
        }

        return intentar("cambiarContrasena/actualizar") {
            supabase.auth.updateUser { password = nueva }
            Unit
        }
    }

    /**
     * Borra la cuenta: la función SQL `eliminar_mi_cuenta` elimina al usuario de Auth y, en cascada,
     * su perfil y su progreso. Es irreversible.
     */
    suspend fun eliminarCuenta(): AuthResult<Unit> {
        val r = intentar("eliminarCuenta") {
            supabase.postgrest.rpc("eliminar_mi_cuenta")
            Unit
        }
        if (r is AuthResult.Success) {
            logout()
            clearRememberedEmail()
        }
        return r
    }

    // ============================ CORREO RECORDADO ============================

    fun saveRememberedEmail(email: String) {
        prefs.edit().putString(CLAVE_CORREO, normalizar(email)).apply()
    }

    fun getRememberedEmail(): String? = prefs.getString(CLAVE_CORREO, null)

    fun clearRememberedEmail() {
        prefs.edit().remove(CLAVE_CORREO).apply()
    }

    // ================================ INTERNOS ================================

    private suspend fun leerPerfil(id: String): PerfilDto? =
        supabase.from("perfiles")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilDto>()

    /** Cuenta que abandonó el registro antes del paso 3: no tiene fila en `perfiles`. */
    private fun perfilMinimo(id: String, email: String?) = UserData(
        id = id,
        nombre = email.orEmpty().substringBefore('@'),
        email = email.orEmpty(),
    )

    private suspend fun <T> intentar(accion: String, bloque: suspend () -> T): AuthResult<T> {
        if (!SupabaseConfig.configurado) {
            return AuthResult.Error("Falta configurar Supabase: pega la URL y la clave en SupabaseConfig.kt.")
        }
        return try {
            AuthResult.Success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Supabase: %s", accion)
            AuthResult.Error(mensajeDeError(e))
        }
    }

    /** Traduce los errores de red, Auth y Postgres a mensajes en español. */
    internal fun mensajeDeError(e: Throwable): String {
        if (e is SesionExpiradaException) return "Tu sesión expiró. Inicia sesión de nuevo."
        val causas = generateSequence(e) { it.cause }.toList()
        val texto = causas.joinToString(" ") { it.message.orEmpty() }.lowercase()
        return when {
            causas.any { it is IOException || it::class.simpleName.orEmpty().contains("HttpRequest") } ||
                "unable to resolve host" in texto || "timed out" in texto || "timeout" in texto ->
                "No hay conexión a internet. Inténtalo de nuevo."
            "invalid login credentials" in texto -> CREDENCIALES_INCORRECTAS
            "email not confirmed" in texto -> "Debes verificar tu correo antes de iniciar sesión."
            "already registered" in texto -> CUENTA_EXISTENTE
            "rate limit" in texto || "only request this after" in texto || "too many requests" in texto ->
                "Has hecho demasiados intentos. Espera un momento e inténtalo de nuevo."
            "different from the old password" in texto || "same_password" in texto ->
                "La nueva contraseña debe ser diferente a la actual."
            // Supabase Auth rechaza la contraseña débil (va después de la regla anterior, que también dice "password should")
            causas.any { it is AuthWeakPasswordException } || "weak_password" in texto ||
                "password should" in texto || "known to be weak" in texto ->
                motivoContrasenaDebil(texto, causas.filterIsInstance<AuthWeakPasswordException>().flatMap { it.reasons })
            "duplicate key" in texto || "23505" in texto -> "Ese nombre de usuario ya está en uso."
            else -> ERROR_SERVIDOR
        }
    }

    /**
     * El motivo exacto por el que Supabase rechazó la contraseña ([razones]: "length", "characters",
     * "pwned"). Empiezan con "La contraseña" para que los ViewModels muestren "Contraseña inválida".
     */
    private fun motivoContrasenaDebil(texto: String, razones: List<String>): String = when {
        "pwned" in razones || "known to be weak" in texto || "pwned" in texto ->
            "La contraseña aparece en filtraciones de datos y no es segura. Elige otra."
        // Password Requirements del panel con símbolos: la lista de caracteres exigidos incluye "!@#"
        "!@#" in texto ->
            "La contraseña también debe tener un carácter especial, por ejemplo * ! @ #."
        else -> Regex("""at least (\d+) characters""").find(texto)?.let {
            "La contraseña debe tener mínimo ${it.groupValues[1]} caracteres."
        } ?: "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número."
    }

    private fun normalizar(email: String) = email.trim().lowercase()

    private class SesionExpiradaException : Exception("sesion_expirada")

    private companion object {
        const val PREFS = "levelia_prefs"
        const val CLAVE_CORREO = "correo_recordado"
        const val CLAVE_RECUPERACION_PENDIENTE = "recuperacion_pendiente"
        const val TIEMPO_RESTAURAR_MS = 8_000L
        const val CREDENCIALES_INCORRECTAS = "Email o contraseña incorrectos."
        const val CUENTA_EXISTENTE = "Ya existe una cuenta con ese email. Inicia sesión."
        const val REGISTRO_INCOMPLETO =
            "Tu registro no se terminó. Regístrate de nuevo con este correo y la misma contraseña."
        const val ERROR_SERVIDOR = "Algo salió mal con nuestros servidores. Inténtalo más tarde."
    }
}
