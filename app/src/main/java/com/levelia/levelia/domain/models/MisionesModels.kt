package com.levelia.levelia.domain.models

import java.time.LocalDate
import java.time.ZoneId

/** "Hoy" en Colombia: el mismo que usa el servidor (función hoy() de supabase/misiones.sql). */
fun hoyLevelia(): LocalDate = LocalDate.now(ZoneId.of("America/Bogota"))

/**
 * Veces por semana y XP de cada dificultad. Mismos valores que `datos_dificultad()` en
 * supabase/misiones.sql (el servidor es quien los aplica). FACIL es "Suave" en el formulario del diseño.
 */
enum class Dificultad(val veces: Int, val xp: Int) {
    FACIL(1, 75),
    MEDIA(3, 150),
    DIFICIL(5, 300),

    /** Una sola vez, antes de su fecha límite (es la única con fecha). */
    UNICA(1, 50),
}

enum class EstadoMision { ACTIVA, COMPLETADA, PERDIDA }

/** Ícono pixel-art de la tarjeta. Las misiones creadas por el usuario usan PORTAPAPELES. */
enum class IconoMision { PESA, CEREBRO, PORTAPAPELES, PERSONAS }

/** Misión del catálogo (tabla `misiones`): predefinida o creada por el usuario. */
data class Mision(
    val id: Long,
    val titulo: String,
    val descripcion: String,
    val icono: IconoMision,
    val dificultad: Dificultad,
    /** Veces que hay que registrar progreso para cumplirla. */
    val meta: Int,
    val xp: Int,
    /** Solo en UNICA. */
    val fechaLimite: LocalDate?,
    /** true = la creó el usuario ("Crea tu propia misión"). */
    val propia: Boolean,
) {
    /**
     * La misión con la dificultad que el usuario guardó en "Ajustes de misión": veces y XP de la tabla de
     * dificultades. null o la misma de la misión = sin cambios (conserva sus veces y XP originales).
     */
    fun ajustada(guardada: Dificultad?): Mision =
        if (guardada == null || guardada == dificultad) this
        else copy(dificultad = guardada, meta = guardada.veces, xp = guardada.xp)
}

/** Una misión aceptada (tabla `misiones_usuario`), con su progreso. */
data class MisionAceptada(
    val id: Long,
    val misionId: Long,
    /** La elegida al aceptar ("Ajustar dificultad"); puede no ser la de la misión. */
    val dificultad: Dificultad,
    val meta: Int,
    val xp: Int,
    val progreso: Int,
    /** UNICA: la de la misión. Por veces: 7 días desde que se aceptó. */
    val fechaLimite: LocalDate,
    val ultimoRegistro: LocalDate?,
    val estado: EstadoMision,
    val recompensaReclamada: Boolean,
) {
    /** "0/3" de la tarjeta. */
    val textoProgreso: String get() = "$progreso/$meta"

    /** Por veces solo se registra una vez al día (lo hace cumplir el servidor). */
    fun puedeRegistrarHoy(hoy: LocalDate): Boolean =
        estado == EstadoMision.ACTIVA && ultimoRegistro != hoy && !hoy.isAfter(fechaLimite)
}

/**
 * Una tarjeta de la lista: la misión y su aceptación más reciente. [aceptada] null = está pendiente
 * de aceptar ("Aceptar misión"); una PERDIDA o ya reclamada se puede volver a aceptar.
 */
data class MisionConProgreso(val mision: Mision, val aceptada: MisionAceptada?) {
    val estado: EstadoMision? get() = aceptada?.estado

    /**
     * Misma regla que `aceptar_mision()`: la UNICA una sola vez; las demás, cuando terminó el plazo de
     * la anterior y su XP ya se reclamó (si no, se podría sumar XP varias veces el mismo día).
     */
    fun sePuedeAceptar(hoy: LocalDate): Boolean {
        val anterior = aceptada ?: return true
        if (mision.dificultad == Dificultad.UNICA) return false
        return anterior.estado != EstadoMision.ACTIVA && !porReclamar && hoy.isAfter(anterior.fechaLimite)
    }

    /**
     * Por qué no se puede aceptar (null = sí se puede). Mismos textos que traduce MisionesRepository
     * cuando el servidor la rechaza, para que "Detalles de misión" no muestre un botón que va a fallar.
     */
    fun motivoNoAceptable(hoy: LocalDate): String? = when {
        sePuedeAceptar(hoy) -> null
        aceptada?.estado == EstadoMision.ACTIVA -> MensajesMision.YA_ACEPTADA
        porReclamar -> MensajesMision.RECOMPENSA_PENDIENTE
        mision.dificultad == Dificultad.UNICA -> MensajesMision.UNICA_HECHA
        else -> MensajesMision.EN_CURSO
    }

    /** Cumplida y su XP todavía no se sumó (cuenta para "¡Reclamar recompensa!"). */
    val porReclamar: Boolean get() = aceptada?.estado == EstadoMision.COMPLETADA && !aceptada.recompensaReclamada
}

/** Textos de las reglas de aceptar (los usan la pantalla y MisionesRepository). */
object MensajesMision {
    const val YA_ACEPTADA = "Ya aceptaste esta misión."
    const val RECOMPENSA_PENDIENTE = "Primero reclama la recompensa de esta misión."
    const val UNICA_HECHA = "Esta misión es única y ya la aceptaste."
    const val EN_CURSO = "Ya hiciste esta misión. Podrás repetirla cuando termine su plazo."
}

/** Pestañas de la pantalla Misiones. */
enum class FiltroMisiones { ACTIVAS, COMPLETADAS, TODAS }

/** Resultado de "¡Reclamar recompensa!". [nivel] y [xp] son el progreso nuevo del usuario. */
data class Recompensa(val xpGanada: Int, val nivelesSubidos: Int, val nivel: Int, val xp: Int)

/** Pantalla Misiones (lista con pestañas) y las acciones de Detalle / Ajustar dificultad. */
data class MisionesUiState(
    val cargando: Boolean = false,
    val misiones: List<MisionConProgreso> = emptyList(),
    val filtro: FiltroMisiones = FiltroMisiones.ACTIVAS,
    /** Aceptar, registrar progreso o reclamar en curso (deshabilita los botones). */
    val enviando: Boolean = false,
    val error: String? = null,
    /** Última recompensa reclamada, para mostrar el aviso (null = ninguno). */
    val recompensa: Recompensa? = null,
    /** Para [MisionConProgreso.sePuedeAceptar] y [MisionAceptada.puedeRegistrarHoy]; se actualiza al cargar. */
    val hoy: LocalDate = hoyLevelia(),
) {
    /**
     * Activas = aceptadas y en curso (vacía hasta aceptar una). Completadas = cumplidas, con la XP
     * reclamada o no. Todas = el catálogo completo, primero las que aún no están cumplidas.
     */
    val visibles: List<MisionConProgreso>
        get() = when (filtro) {
            FiltroMisiones.ACTIVAS -> misiones.filter { it.estado == EstadoMision.ACTIVA }
            FiltroMisiones.COMPLETADAS -> misiones.filter { it.estado == EstadoMision.COMPLETADA }
            FiltroMisiones.TODAS -> misiones.sortedBy { it.estado == EstadoMision.COMPLETADA }
        }

    /** Muestra el botón "¡Reclamar recompensa!". */
    val hayRecompensas: Boolean get() = misiones.any { it.porReclamar }
}

/** "Crea tu propia misión". Cada error es el texto bajo su campo (campo en rojo). */
data class CrearMisionState(
    val titulo: String = "",
    val descripcion: String = "",
    val dificultad: Dificultad? = null,
    /** Solo se pide en UNICA. */
    val fechaLimite: LocalDate? = null,
    val errorTitulo: String? = null,
    val errorDificultad: String? = null,
    val errorFecha: String? = null,
    val guardando: Boolean = false,
    /** Error del servidor o de red. */
    val error: String? = null,
)
