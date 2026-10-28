package com.levelia.levelia.data.repository

import com.levelia.levelia.data.remote.LogroDto
import com.levelia.levelia.data.remote.SupabaseConfig
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Logro
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Módulo Logros contra Supabase (supabase/logros.sql). El desbloqueo lo hace el servidor dentro de
 * registrar_progreso / reclamar_recompensas / mis_logros; la app solo lee y marca los avisos como vistos.
 * Como los demás repositorios, devuelve [AuthResult] y nunca lanza excepciones.
 */
@Singleton
class LogrosRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** Todos los logros con el avance del usuario, en el orden de la pestaña "Todos". */
    suspend fun obtenerLogros(): AuthResult<List<Logro>> = intentar("obtenerLogros") {
        supabase.postgrest.rpc("mis_logros").decodeList<LogroDto>().map(LogroDto::aLogro)
    }

    /** Ya se mostró "¡Logro desbloqueado!": esos logros dejan de contar como nuevos. */
    suspend fun marcarVistos(): AuthResult<Unit> = intentar("marcarVistos") {
        supabase.postgrest.rpc("marcar_logros_vistos")
        Unit
    }

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

    internal fun mensajeDeError(e: Throwable): String = when {
        esSinConexion(e) -> SIN_CONEXION
        generateSequence(e) { it.cause }.any { "SIN_SESION" in it.message.orEmpty() } -> "Tu sesión expiró. Inicia sesión de nuevo."
        else -> ERROR_SERVIDOR
    }
}
