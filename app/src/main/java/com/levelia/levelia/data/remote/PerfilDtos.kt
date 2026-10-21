package com.levelia.levelia.data.remote

import com.levelia.levelia.domain.ReglasNivel
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.domain.models.UserData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/** Fila de la tabla `perfiles` tal como la devuelve Supabase (todas las columnas). */
@Serializable
data class PerfilDto(
    val id: String,
    val nombre: String,
    val email: String,
    val edad: Int,
    val avatar: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun aUserData() = UserData(
        id = id,
        nombre = nombre,
        email = email,
        edad = edad,
        avatar = avatar,
        createdAt = createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
            ?: System.currentTimeMillis(),
    )
}

/** Lo que la app envía al crear el perfil (created_at y updated_at los pone la base de datos). */
@Serializable
data class PerfilNuevoDto(
    val id: String,
    val nombre: String,
    val email: String,
    val edad: Int,
    val avatar: String,
)

/** Solo estos campos se pueden modificar desde la app (también lo limita GRANT UPDATE en SQL). */
@Serializable
data class NombreUpdateDto(val nombre: String)

@Serializable
data class AvatarUpdateDto(val avatar: String)

/** Registro retomado: se actualizan juntos el nombre y el avatar elegidos. */
@Serializable
data class PerfilRegistroUpdateDto(val nombre: String, val avatar: String)

/**
 * Fila de la tabla `progreso` (user_id, nivel, xp, updated_at). `xp` es la del nivel actual; la meta
 * del nivel sale de la curva de [ReglasNivel] (la base no la guarda).
 */
@Serializable
data class ProgresoDto(
    @SerialName("user_id") val userId: String,
    val nivel: Int = 1,
    val xp: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun aProgresoData() = ProgresoData(nivel = nivel, xp = xp, xpSiguienteNivel = ReglasNivel.xpParaSubir(nivel))
}
