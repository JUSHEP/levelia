package com.levelia.levelia.data.remote

import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.IconoMision
import com.levelia.levelia.domain.models.Mision
import com.levelia.levelia.domain.models.MisionAceptada
import com.levelia.levelia.domain.models.Recompensa
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// Filas de supabase/misiones.sql tal como las devuelve Supabase. Las fechas llegan como "AAAA-MM-DD".

@Serializable
data class MisionDto(
    val id: Long,
    @SerialName("user_id") val userId: String? = null,
    val titulo: String,
    val descripcion: String = "",
    val icono: String,
    val dificultad: String,
    val meta: Int,
    val xp: Int,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun aMision() = Mision(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        // Un ícono que la app aún no conoce no rompe la lista
        icono = IconoMision.entries.firstOrNull { it.name == icono } ?: IconoMision.PORTAPAPELES,
        dificultad = Dificultad.valueOf(dificultad),
        meta = meta,
        xp = xp,
        fechaLimite = fechaLimite?.let(LocalDate::parse),
        propia = userId != null,
    )
}

@Serializable
data class MisionUsuarioDto(
    val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("mision_id") val misionId: Long,
    val dificultad: String,
    val meta: Int,
    val xp: Int,
    val progreso: Int,
    @SerialName("fecha_limite") val fechaLimite: String,
    @SerialName("ultimo_registro") val ultimoRegistro: String? = null,
    val estado: String,
    @SerialName("recompensa_reclamada") val recompensaReclamada: Boolean,
    @SerialName("aceptada_at") val aceptadaAt: String,
    @SerialName("terminada_at") val terminadaAt: String? = null,
) {
    fun aMisionAceptada() = MisionAceptada(
        id = id,
        misionId = misionId,
        dificultad = Dificultad.valueOf(dificultad),
        meta = meta,
        xp = xp,
        progreso = progreso,
        fechaLimite = LocalDate.parse(fechaLimite),
        ultimoRegistro = ultimoRegistro?.let(LocalDate::parse),
        estado = EstadoMision.valueOf(estado),
        recompensaReclamada = recompensaReclamada,
    )
}

/** Fila de `misiones_preferencias`: la dificultad guardada en "Ajustes de misión" (sin aceptar). */
@Serializable
data class PreferenciaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("mision_id") val misionId: Long,
    val dificultad: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Respuesta de la función `reclamar_recompensas()`. */
@Serializable
data class RecompensaDto(
    @SerialName("xp_ganada") val xpGanada: Int,
    @SerialName("niveles_subidos") val nivelesSubidos: Int,
    val nivel: Int,
    val xp: Int,
) {
    fun aRecompensa() = Recompensa(xpGanada = xpGanada, nivelesSubidos = nivelesSubidos, nivel = nivel, xp = xp)
}
