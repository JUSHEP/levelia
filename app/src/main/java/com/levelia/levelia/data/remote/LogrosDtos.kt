package com.levelia.levelia.data.remote

import com.levelia.levelia.domain.models.Articulo
import com.levelia.levelia.domain.models.CalidadArticulo
import com.levelia.levelia.domain.models.EspacioArticulo
import com.levelia.levelia.domain.models.FamiliaLogro
import com.levelia.levelia.domain.models.Logro
import com.levelia.levelia.domain.models.RequisitoLogro
import com.levelia.levelia.domain.models.TipoRequisito
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/** Una fila de `mis_logros()` (supabase/logros.sql). */
@Serializable
data class LogroDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val familia: String,
    val nivel: Int,
    val orden: Int,
    @SerialName("articulo_id") val articuloId: String? = null,
    @SerialName("articulo_nombre") val articuloNombre: String? = null,
    @SerialName("articulo_espacio") val articuloEspacio: String? = null,
    @SerialName("articulo_calidad") val articuloCalidad: String? = null,
    @SerialName("obtenido_at") val obtenidoAt: String? = null,
    val visto: Boolean = false,
    val requisitos: List<RequisitoDto> = emptyList(),
    val progreso: Int = 0,
    @SerialName("siguiente_id") val siguienteId: String? = null,
    @SerialName("avance_siguiente") val avanceSiguiente: Int? = null,
) {
    fun aLogro() = Logro(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        familia = FamiliaLogro.valueOf(familia),
        nivel = nivel,
        articulo = if (articuloId != null && articuloNombre != null && articuloEspacio != null && articuloCalidad != null) {
            Articulo(articuloId, articuloNombre, EspacioArticulo.valueOf(articuloEspacio), CalidadArticulo.valueOf(articuloCalidad))
        } else {
            null
        },
        obtenidoAt = obtenidoAt?.let { OffsetDateTime.parse(it).toInstant() },
        visto = visto,
        requisitos = requisitos.map(RequisitoDto::aRequisito),
        progreso = progreso,
        siguienteId = siguienteId,
        avanceSiguiente = avanceSiguiente,
    )
}

@Serializable
data class RequisitoDto(
    val tipo: String,
    val categoria: String? = null,
    @SerialName("logro_requerido") val logroRequerido: String? = null,
    @SerialName("titulo_requerido") val tituloRequerido: String? = null,
    val actual: Int,
    val objetivo: Int,
) {
    fun aRequisito() = RequisitoLogro(
        tipo = TipoRequisito.valueOf(tipo),
        categoria = categoria,
        tituloRequerido = tituloRequerido,
        actual = actual,
        objetivo = objetivo,
    )
}
