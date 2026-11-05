package com.levelia.levelia.domain.models

import java.time.Instant

// Módulo Personalización (supabase/personalizacion.sql). El servidor paga las esmeraldas, desbloquea las skins de
// nivel y valida las compras; aquí solo se describe lo que devuelve mi_personalizacion() y los textos de las pantallas.

/** Skin con la que empieza todo el mundo (nivel 1). También se dibuja si el servidor todavía no respondió. */
const val SKIN_INICIAL = "NOVATO"
const val NOMBRE_SKIN_INICIAL = "Novato"

/** NIVEL = el guerrero que se desbloquea solo al subir de nivel; TIENDA = se compra con esmeraldas. */
enum class TipoSkin { NIVEL, TIENDA }

data class Skin(
    val id: String,
    val nombre: String,
    /** Panel "Habilidades": solo describen la skin (no cambian la XP ni las misiones). */
    val habilidades: List<String>,
    val tipo: TipoSkin,
    /** Solo en NIVEL. */
    val nivelRequerido: Int?,
    /** Solo en TIENDA (esmeraldas). */
    val precio: Int?,
    /** null = el usuario todavía no la tiene. */
    val obtenidaAt: Instant?,
    /** false = se desbloqueó al subir de nivel y todavía no se avisó ("¡Skin desbloqueada!"). */
    val visto: Boolean,
) {
    val obtenida: Boolean get() = obtenidaAt != null
    val esNueva: Boolean get() = obtenida && !visto

    /** Bloqueada por nivel: en el detalle se muestra la silueta y "????" (como el diseño). */
    val esSecreta: Boolean get() = tipo == TipoSkin.NIVEL && !obtenida

    /** "Nvl. 25" de la cuadrícula. */
    val etiquetaNivel: String? get() = nivelRequerido?.let { "Nvl. $it" }

    /** Título de la tarjeta del detalle: "Escudero Nvl.5", "Arquero" o "????" si es secreta. */
    val tituloDetalle: String
        get() = when {
            esSecreta -> "????"
            nivelRequerido != null -> "$nombre Nvl.$nivelRequerido"
            else -> nombre
        }
}

/** Lo que devuelve mi_personalizacion(). */
data class Personalizacion(
    val esmeraldas: Int,
    val nivel: Int,
    /** La skin equipada (null = el servidor todavía no le asignó ninguna). */
    val equipadaId: String?,
    val skins: List<Skin>,
)

/** Diálogos de la compra. */
sealed interface AvisoPersonalizacion {
    /** "¿Comprar …?" antes de gastar las esmeraldas. */
    data class ConfirmarCompra(val skin: Skin) : AvisoPersonalizacion

    /** "¡Skin comprada!" (ya quedó equipada). */
    data class Comprada(val skin: Skin) : AvisoPersonalizacion
}

/** Pantallas Personalización, Detalle de la skin e Inicio (la skin equipada). */
data class PersonalizacionUiState(
    val cargando: Boolean = false,
    val esmeraldas: Int = 0,
    val nivel: Int = 1,
    val equipadaId: String? = null,
    val skins: List<Skin> = emptyList(),
    /** Comprar o equipar en curso (deshabilita los botones). */
    val enviando: Boolean = false,
    val error: String? = null,
    val aviso: AvisoPersonalizacion? = null,
) {
    /** Las del guerrero por nivel (cuadrícula de arriba) y las de la tienda (cuadrícula de abajo). */
    val deNivel: List<Skin> get() = skins.filter { it.tipo == TipoSkin.NIVEL }
    val deTienda: List<Skin> get() = skins.filter { it.tipo == TipoSkin.TIENDA }

    /** La equipada; si el servidor no la mandó, la primera del guerrero (la inicial). null = aún no hay catálogo. */
    val equipada: Skin? get() = skins.firstOrNull { it.id == equipadaId } ?: deNivel.firstOrNull()

    /** Desbloqueadas que la app aún no avisó (para "¡Skin desbloqueada!"). */
    val nuevas: List<Skin> get() = skins.filter { it.esNueva }

    fun esEquipada(skin: Skin): Boolean = skin.id == equipada?.id

    /** Esmeraldas que le faltan para comprarla (0 = le alcanza). */
    fun faltan(skin: Skin): Int = ((skin.precio ?: 0) - esmeraldas).coerceAtLeast(0)
}
