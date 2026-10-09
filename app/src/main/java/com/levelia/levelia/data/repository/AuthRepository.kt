package com.levelia.levelia.data.repository

import android.util.Patterns
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulación en memoria: los datos se pierden al cerrar la app.
 * TODO: reemplazar por Firebase Auth + Firestore (las firmas ya devuelven AuthResult).
 */
@Singleton
class AuthRepository @Inject constructor() {

    private val mutex = Mutex()

    /** email → código OTP pendiente */
    private val pendingCodes = mutableMapOf<String, String>()

    /** email → contraseña de cuentas en registro (aún sin perfil) */
    private val pendingPasswords = mutableMapOf<String, String>()

    /** email → usuario con perfil creado */
    private val registeredUsers = mutableMapOf<String, UserData>()
    private val passwords = mutableMapOf<String, String>()

    // TODO: guardar con almacenamiento cifrado; hoy se pierde al cerrar la app
    private var rememberedEmail: String? = null

    suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        delay(1500)
        val clave = normalizar(email)
        return mutex.withLock {
            when {
                !Patterns.EMAIL_ADDRESS.matcher(clave).matches() -> AuthResult.Error("El email no es válido.")
                password.length < 8 -> AuthResult.Error("La contraseña debe tener al menos 8 caracteres.")
                clave in registeredUsers -> AuthResult.Error("Ya existe una cuenta con ese email. Inicia sesión.")
                else -> {
                    pendingPasswords[clave] = password
                    pendingCodes[clave] = generarCodigo(clave)
                    AuthResult.Success(Unit)
                }
            }
        }
    }

    suspend fun verifyCode(email: String, token: String): AuthResult<Unit> {
        delay(1000)
        val clave = normalizar(email)
        return mutex.withLock {
            val esperado = pendingCodes[clave]
            when {
                esperado == null -> AuthResult.Error("El código expiró. Solicita uno nuevo.")
                esperado != token -> AuthResult.Error("Código incorrecto.")
                else -> {
                    pendingCodes.remove(clave)
                    AuthResult.Success(Unit)
                }
            }
        }
    }

    suspend fun createUserProfile(email: String, nombre: String, edad: Int, avatar: String): AuthResult<UserData> {
        delay(1500)
        val clave = normalizar(email)
        return mutex.withLock {
            val password = pendingPasswords[clave]
                ?: return@withLock AuthResult.Error("El registro expiró. Vuelve a empezar.")
            val user = UserData(nombre = nombre, email = clave, edad = edad, avatar = avatar)
            registeredUsers[clave] = user
            passwords[clave] = password
            pendingPasswords.remove(clave)
            Timber.d("Perfil creado: %s", user)
            AuthResult.Success(user)
        }
    }

    suspend fun login(email: String, password: String): AuthResult<UserData> {
        delay(1500)
        val clave = normalizar(email)
        return mutex.withLock {
            val user = registeredUsers[clave]
            // Mismo mensaje en ambos casos para no revelar qué emails están registrados
            if (user == null || passwords[clave] != password) {
                AuthResult.Error("Email o contraseña incorrectos.")
            } else {
                AuthResult.Success(user)
            }
        }
    }

    fun logout(): AuthResult<Unit> = AuthResult.Success(Unit)

    // TODO: devolver la sesión persistida cuando exista backend
    fun getCurrentUser(): AuthResult<UserData?> = AuthResult.Success(null)

    suspend fun resendCode(email: String): AuthResult<Unit> {
        delay(1000)
        val clave = normalizar(email)
        return mutex.withLock {
            if (clave !in pendingPasswords) {
                AuthResult.Error("El registro expiró. Vuelve a empezar.")
            } else {
                pendingCodes[clave] = generarCodigo(clave)
                AuthResult.Success(Unit)
            }
        }
    }

    /** Solo para pruebas: sin envío real de emails, la UI de debug muestra el código. */
    fun debugPendingCode(email: String): String? = pendingCodes[normalizar(email)]

    fun saveRememberedEmail(email: String) {
        rememberedEmail = normalizar(email)
    }

    fun getRememberedEmail(): String? = rememberedEmail

    fun clearRememberedEmail() {
        rememberedEmail = null
    }

    private fun generarCodigo(email: String): String {
        val codigo = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        Timber.d("Código OTP para %s: %s", email, codigo)
        return codigo
    }

    private fun normalizar(email: String) = email.trim().lowercase()
}
