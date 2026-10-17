package com.levelia.levelia.data

import com.levelia.levelia.data.repository.AuthRepository
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/** Cada error de Supabase / red se traduce a un mensaje en español que entiende el usuario. */
class MensajesDeErrorTest {

    private val repo = AuthRepository(mockk(relaxed = true), mockk(relaxed = true))

    private fun msg(texto: String) = repo.mensajeDeError(RuntimeException(texto))

    @Test
    fun `sin internet`() {
        assertEquals("No hay conexión a internet. Inténtalo de nuevo.", repo.mensajeDeError(UnknownHostException("x")))
        assertEquals("No hay conexión a internet. Inténtalo de nuevo.", repo.mensajeDeError(RuntimeException("envuelto", IOException("timeout"))))
    }

    @Test
    fun `errores de Auth`() {
        assertEquals("Email o contraseña incorrectos.", msg("Invalid login credentials"))
        assertEquals("Debes verificar tu correo antes de iniciar sesión.", msg("Email not confirmed"))
        assertEquals("Ya existe una cuenta con ese email. Inicia sesión.", msg("User already registered"))
        assertEquals("Has hecho demasiados intentos. Espera un momento e inténtalo de nuevo.", msg("email rate limit exceeded"))
        assertEquals("La nueva contraseña debe ser diferente a la actual.", msg("New password should be different from the old password."))
    }

    @Test
    fun `contrasena debil del servidor dice el motivo exacto`() {
        // Password Requirements con símbolos activado en Supabase
        assertEquals(
            "La contraseña también debe tener un carácter especial, por ejemplo * ! @ #.",
            msg("Password should contain at least one character of each: abcdefghijklmnopqrstuvwxyz, ABCDEFGHIJKLMNOPQRSTUVWXYZ, 0123456789, !@#\$%^&*()_+-=[]{};':\"|<>?,./`~."),
        )
        assertEquals("La contraseña debe tener mínimo 10 caracteres.", msg("Password should be at least 10 characters."))
        assertEquals(
            "La contraseña aparece en filtraciones de datos y no es segura. Elige otra.",
            msg("Password is known to be weak and easy to guess, please choose a different one."),
        )
        assertEquals(
            "La contraseña aparece en filtraciones de datos y no es segura. Elige otra.",
            repo.mensajeDeError(AuthWeakPasswordException("Password is weak", 422, listOf("pwned"))),
        )
        assertEquals(
            "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número.",
            msg("weak_password: Password should contain at least one character of each: abc..."),
        )
    }

    @Test
    fun `errores de la base de datos`() {
        assertEquals("Ese nombre de usuario ya está en uso.", msg("duplicate key value violates unique constraint \"perfiles_nombre_unico\" (23505)"))
    }

    @Test
    fun `cualquier otro error es de servidor`() {
        assertEquals("Algo salió mal con nuestros servidores. Inténtalo más tarde.", msg("boom"))
    }
}
