package com.levelia.levelia.data.remote

import com.levelia.levelia.domain.models.Personalizacion
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.domain.models.TipoSkin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/** Respuesta de `mi_personalizacion()` (supabase/personalizacion.sql). */
@Serializable
data class PersonalizacionDto(
    val esmeraldas: Int = 0,
    val nivel: Int = 1,
    @SerialName("skin_equipada") val skinEquipada: String? = null,
    val skins: List<SkinDto> = emptyList(),
) {
    fun aPersonalizacion() = Personalizacion(
        esmeraldas = esmeraldas,
        nivel = nivel,
        equipadaId = skinEquipada,
        skins = skins.mapNotNull(SkinDto::aSkin),
    )
}

/** Una skin del catálogo con lo que tiene el usuario. */
@Serializable
data class SkinDto(
    val id: String,
    val nombre: String,
    val habilidades: List<String> = emptyList(),
    val tipo: String,
    @SerialName("nivel_requerido") val nivelRequerido: Int? = null,
    val precio: Int? = null,
    val orden: Int = 0,
    @SerialName("obtenida_at") val obtenidaAt: String? = null,
    val visto: Boolean = false,
) {
    /** Un tipo que esta versión de la app no conoce se salta en vez de romper la pantalla. */
    fun aSkin(): Skin? {
        val tipoSkin = TipoSkin.entries.firstOrNull { it.name == tipo } ?: return null
        return Skin(
            id = id,
            nombre = nombre,
            habilidades = habilidades,
            tipo = tipoSkin,
            nivelRequerido = nivelRequerido,
            precio = precio,
            obtenidaAt = obtenidaAt?.let { OffsetDateTime.parse(it).toInstant() },
            visto = visto,
        )
    }
}
