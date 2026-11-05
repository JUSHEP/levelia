package com.levelia.levelia.data.repository

import com.levelia.levelia.data.remote.ModoDemoDto
import com.levelia.levelia.data.remote.SupabaseConfig
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.ModoDemo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modo demo contra Supabase (supabase/modo_demo.sql). Todo lo hace el servidor, y solo para cuentas que estén en
 * `cuentas_demo`: a las demás les responde NO_ES_DEMO. Como los demás repositorios, devuelve [AuthResult] y nunca
 * lanza excepciones.
 */
@Singleton
class ModoDemoRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** ¿La cuenta es demo? A una cuenta normal le responde activo = false. */
    suspend fun estado(): AuthResult<ModoDemo> = intentar("modoDemo") {
        supabase.postgrest.rpc("mi_modo_demo").decodeAs<ModoDemoDto>().aModoDemo()
    }

    /** "Avanzar 1 día": las fechas de las misiones de la cuenta se corren un día atrás. */
    suspend fun avanzarDia(): AuthResult<Unit> = llamar("demo_avanzar_dia")

    /** Deja la cuenta en el nivel [nivel] − 1 a 1 XP de subir. */
    suspend fun prepararNivel(nivel: Int): AuthResult<Unit> = intentar("demo_preparar_nivel") {
        supabase.postgrest.rpc("demo_preparar_nivel", buildJsonObject { put("p_nivel", nivel) })
        Unit
    }

    /** "+1 nivel". */
    suspend fun subirNivel(): AuthResult<Unit> = llamar("demo_subir_nivel")

    /** "+500 esmeraldas". */
    suspend fun sumarEsmeraldas(): AuthResult<Unit> = llamar("demo_sumar_esmeraldas")

    /** "Reiniciar cuenta demo": nivel 1, sin misiones, logros, skins ni esmeraldas. */
    suspend fun reiniciar(): AuthResult<Unit> = llamar("demo_reiniciar")

    private suspend fun llamar(funcion: String): AuthResult<Unit> = intentar(funcion) {
        supabase.postgrest.rpc(funcion)
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

    /** Traduce los códigos que lanzan las funciones de supabase/modo_demo.sql. */
    internal fun mensajeDeError(e: Throwable): String {
        if (esSinConexion(e)) return SIN_CONEXION
        val texto = generateSequence(e) { it.cause }.joinToString(" ") { it.message.orEmpty() }
        return MENSAJES.entries.firstOrNull { it.key in texto }?.value ?: ERROR_SERVIDOR
    }

    internal companion object {
        val MENSAJES = mapOf(
            "NO_ES_DEMO" to "Esta cuenta no tiene el modo demo.",
            "NIVEL_YA_ALCANZADO" to "Ya pasaste ese nivel. Usa \"Reiniciar cuenta demo\" para empezar de cero.",
            "NIVEL_MAXIMO" to "Ya estás en el nivel máximo (100).",
            "NIVEL_INVALIDO" to "Ese nivel no existe.",
            "SIN_SESION" to "Tu sesión expiró. Inicia sesión de nuevo.",
        )
    }
}
