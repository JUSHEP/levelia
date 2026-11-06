package com.levelia.levelia.data.repository

import com.levelia.levelia.data.remote.PersonalizacionDto
import com.levelia.levelia.data.remote.SupabaseConfig
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Personalizacion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Módulo Personalización contra Supabase (supabase/personalizacion.sql). Las tablas solo se leen: pagar
 * esmeraldas, desbloquear skins, comprar y equipar lo hacen funciones del servidor, que aplican las reglas.
 * Como los demás repositorios, devuelve [AuthResult] y nunca lanza excepciones.
 */
@Singleton
class PersonalizacionRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** Saldo de esmeraldas, nivel, skin equipada y el catálogo (antes el servidor paga lo pendiente). */
    suspend fun obtener(): AuthResult<Personalizacion> = intentar("obtenerPersonalizacion") {
        supabase.postgrest.rpc("mi_personalizacion").decodeAs<PersonalizacionDto>().aPersonalizacion()
    }

    /** "Comprar" (tienda): descuenta las esmeraldas y la deja equipada. */
    suspend fun comprar(skinId: String): AuthResult<Unit> = intentar("comprarSkin") {
        supabase.postgrest.rpc("comprar_skin", buildJsonObject { put("p_skin_id", skinId) })
        Unit
    }

    /** "Equipar" una skin que ya tiene. */
    suspend fun equipar(skinId: String): AuthResult<Unit> = intentar("equiparSkin") {
        supabase.postgrest.rpc("equipar_skin", buildJsonObject { put("p_skin_id", skinId) })
        Unit
    }

    /** Ya se mostró "¡Skin desbloqueada!": esas skins dejan de contar como nuevas. */
    suspend fun marcarVistas(): AuthResult<Unit> = intentar("marcarSkinsVistas") {
        supabase.postgrest.rpc("marcar_skins_vistas")
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

    /** Traduce los códigos que lanzan las funciones de supabase/personalizacion.sql. */
    internal fun mensajeDeError(e: Throwable): String {
        if (esSinConexion(e)) return SIN_CONEXION
        val texto = generateSequence(e) { it.cause }.joinToString(" ") { it.message.orEmpty() }
        return MENSAJES.entries.firstOrNull { it.key in texto }?.value ?: ERROR_SERVIDOR
    }

    internal companion object {
        const val ESMERALDAS_INSUFICIENTES = "No tienes suficientes esmeraldas para esta skin."

        val MENSAJES = mapOf(
            "ESMERALDAS_INSUFICIENTES" to ESMERALDAS_INSUFICIENTES,
            "SKIN_YA_COMPRADA" to "Ya tienes esta skin.",
            "SKIN_NO_SE_VENDE" to "Esta skin no se compra: se desbloquea al subir de nivel.",
            "SKIN_BLOQUEADA" to "Todavía no tienes esta skin.",
            "SKIN_NO_EXISTE" to "Esta skin ya no existe.",
            "SIN_SESION" to "Tu sesión expiró. Inicia sesión de nuevo.",
        )
    }
}
