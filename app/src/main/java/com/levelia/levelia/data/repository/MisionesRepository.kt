package com.levelia.levelia.data.repository

import com.levelia.levelia.data.remote.MisionDto
import com.levelia.levelia.data.remote.MisionUsuarioDto
import com.levelia.levelia.data.remote.PreferenciaDto
import com.levelia.levelia.data.remote.RecompensaDto
import com.levelia.levelia.data.remote.SupabaseConfig
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.Mision
import com.levelia.levelia.domain.models.MensajesMision
import com.levelia.levelia.domain.models.MisionAceptada
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.domain.models.Recompensa
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Módulo Misiones contra Supabase (supabase/misiones.sql). Las tablas solo se leen; todo cambio pasa
 * por funciones del servidor, que aplican las reglas (1 registro por día, plazos, XP y niveles).
 * Como [AuthRepository], devuelve [AuthResult] y nunca lanza excepciones.
 */
@Singleton
class MisionesRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** Catálogo visible (predefinidas + propias) con la aceptación más reciente de cada misión. */
    suspend fun obtenerMisiones(): AuthResult<List<MisionConProgreso>> = intentar("obtenerMisiones") {
        // Antes de leer: las que pasaron su fecha límite quedan PERDIDA
        supabase.postgrest.rpc("vencer_mis_misiones")
        val misiones = supabase.from("misiones")
            .select { order("id", Order.ASCENDING) }
            .decodeList<MisionDto>()
            .map(MisionDto::aMision)
        val ultimaAceptada = supabase.from("misiones_usuario")
            .select { order("aceptada_at", Order.DESCENDING) }
            .decodeList<MisionUsuarioDto>()
            .map(MisionUsuarioDto::aMisionAceptada)
            .groupBy(MisionAceptada::misionId)
            .mapValues { it.value.first() }
        // La dificultad guardada en "Ajustes de misión" (sin aceptar) cambia las veces y XP que se muestran
        val guardadas = supabase.from("misiones_preferencias")
            .select()
            .decodeList<PreferenciaDto>()
            .associate { it.misionId to Dificultad.valueOf(it.dificultad) }
        misiones.map { MisionConProgreso(it.ajustada(guardadas[it.id]), ultimaAceptada[it.id]) }
    }

    /** "Guardar cambios" de Ajustes de misión: guarda la dificultad en la base SIN aceptar la misión. */
    suspend fun ajustarDificultad(misionId: Long, dificultad: Dificultad): AuthResult<Unit> =
        intentar("ajustarDificultad") {
            supabase.postgrest.rpc(
                "ajustar_dificultad",
                buildJsonObject {
                    put("p_mision_id", misionId)
                    put("p_dificultad", dificultad.name)
                },
            )
            Unit
        }

    /** "Crea tu propia misión". [fechaLimite] solo se usa (y es obligatoria) en UNICA. */
    suspend fun crearMision(
        titulo: String,
        descripcion: String,
        dificultad: Dificultad,
        fechaLimite: LocalDate?,
    ): AuthResult<Mision> = intentar("crearMision") {
        supabase.postgrest.rpc(
            "crear_mision",
            buildJsonObject {
                put("p_titulo", titulo.trim())
                put("p_descripcion", descripcion.trim())
                put("p_dificultad", dificultad.name)
                put("p_fecha_limite", fechaLimite?.takeIf { dificultad == Dificultad.UNICA }?.toString())
            },
        ).decodeAs<MisionDto>().aMision()
    }

    /** "Aceptar misión". [dificultad] null = la guardada en "Ajustes de misión" o, si no hay, la de la misión. */
    suspend fun aceptarMision(misionId: Long, dificultad: Dificultad? = null): AuthResult<MisionAceptada> =
        intentar("aceptarMision") {
            supabase.postgrest.rpc(
                "aceptar_mision",
                buildJsonObject {
                    put("p_mision_id", misionId)
                    put("p_dificultad", dificultad?.name)
                },
            ).decodeAs<MisionUsuarioDto>().aMisionAceptada()
        }

    /**
     * "Registrar progreso +". Devuelve la misión actualizada: COMPLETADA si llegó a la meta, PERDIDA si
     * ya había pasado la fecha límite (así se valida la UNICA contra su fecha objetivo).
     */
    suspend fun registrarProgreso(aceptadaId: Long): AuthResult<MisionAceptada> = intentar("registrarProgreso") {
        supabase.postgrest.rpc("registrar_progreso", buildJsonObject { put("p_id", aceptadaId) })
            .decodeAs<MisionUsuarioDto>()
            .aMisionAceptada()
    }

    /** "¡Reclamar recompensa!": suma la XP de todas las cumplidas sin reclamar y sube de nivel. */
    suspend fun reclamarRecompensas(): AuthResult<Recompensa> = intentar("reclamarRecompensas") {
        supabase.postgrest.rpc("reclamar_recompensas").decodeAs<RecompensaDto>().aRecompensa()
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

    /** Traduce los códigos que lanzan las funciones de supabase/misiones.sql. */
    internal fun mensajeDeError(e: Throwable): String {
        if (esSinConexion(e)) return SIN_CONEXION
        val texto = generateSequence(e) { it.cause }.joinToString(" ") { it.message.orEmpty() }
        return MENSAJES.entries.firstOrNull { it.key in texto }?.value ?: ERROR_SERVIDOR
    }

    internal companion object {
        // Los dos primeros son textos del diseño ("Crea tu propia misión")
        const val TITULO_VACIO = "¡Tienes que darle un nombre a la misión!"
        const val DIFICULTAD_VACIA = "¡No has definido la dificultad!"
        const val FECHA_REQUERIDA = "¡Tienes que elegir una fecha límite!"
        const val FECHA_PASADA = "La fecha límite no puede ser anterior a hoy."
        const val YA_REGISTRADO_HOY = "Ya registraste tu progreso hoy. ¡Vuelve mañana!"
        const val YA_ACEPTADA = MensajesMision.YA_ACEPTADA
        const val TITULO_MAX = 40
        const val TITULO_LARGO = "El nombre puede tener máximo $TITULO_MAX caracteres."

        val MENSAJES = mapOf(
            "TITULO_VACIO" to TITULO_VACIO,
            "TITULO_LARGO" to TITULO_LARGO,
            "DESCRIPCION_LARGA" to "La descripción puede tener máximo 300 caracteres.",
            "DIFICULTAD_INVALIDA" to DIFICULTAD_VACIA,
            "FECHA_REQUERIDA" to FECHA_REQUERIDA,
            "FECHA_PASADA" to FECHA_PASADA,
            "MISION_YA_ACTIVA" to YA_ACEPTADA,
            // Doble toque o dos teléfonos a la vez: lo frena el índice único de la tabla
            "misiones_usuario_una_activa" to YA_ACEPTADA,
            "RECOMPENSA_PENDIENTE" to MensajesMision.RECOMPENSA_PENDIENTE,
            "MISION_YA_HECHA" to MensajesMision.UNICA_HECHA,
            "MISION_EN_CURSO" to MensajesMision.EN_CURSO,
            "MISION_NO_EXISTE" to "Esta misión ya no existe.",
            "MISION_TERMINADA" to "Esta misión ya terminó.",
            "YA_REGISTRADO_HOY" to YA_REGISTRADO_HOY,
            "SIN_SESION" to "Tu sesión expiró. Inicia sesión de nuevo.",
        )
    }
}
