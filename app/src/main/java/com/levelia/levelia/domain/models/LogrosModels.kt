package com.levelia.levelia.domain.models

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Módulo Logros (supabase/logros.sql). Las reglas y el desbloqueo los aplica el servidor; aquí solo se
// describe lo que devuelve mis_logros() y los textos que muestran las pantallas.

enum class FamiliaLogro(val texto: String) { GENERAL("General"), FISICO("Físico"), ESTUDIO("Estudio"), NIVEL("Experiencia") }

enum class TipoRequisito { MISIONES, MISIONES_CATEGORIA, RACHA_DIAS, NIVEL, LOGRO }

/** Los 4 espacios de equipamiento básico del personaje (módulo Personalización). */
enum class EspacioArticulo { CASCO, PECHERA, ESPADA, BOTAS }

enum class CalidadArticulo { BASICA, PLATA, ORO }

/** Recompensa de un logro: un artículo de equipamiento. */
data class Articulo(val id: String, val nombre: String, val espacio: EspacioArticulo, val calidad: CalidadArticulo)

/** Un requisito con el avance del usuario ([actual] nunca pasa de [objetivo]). */
data class RequisitoLogro(
    val tipo: TipoRequisito,
    /** Solo en MISIONES_CATEGORIA: FISICO, ESTUDIO, SOCIAL u OTRO. */
    val categoria: String?,
    /** Solo en LOGRO: el nombre del logro que hay que desbloquear antes. */
    val tituloRequerido: String?,
    val actual: Int,
    val objetivo: Int,
) {
    val cumplido: Boolean get() = actual >= objetivo

    /** "0/1" de la tarjeta Requisitos. */
    val textoProgreso: String get() = "$actual/$objetivo"

    /** "Completar 10 misiones físicas", "Subir al nivel 10", "Desbloquear Estudioso"… */
    val texto: String
        get() = when (tipo) {
            TipoRequisito.MISIONES -> "Completar $objetivo ${if (objetivo == 1) "misión" else "misiones"}"
            TipoRequisito.MISIONES_CATEGORIA -> "Completar $objetivo ${if (objetivo == 1) "misión" else "misiones"} ${
                when (categoria) {
                    "FISICO" -> if (objetivo == 1) "física" else "físicas"
                    "ESTUDIO" -> "de estudio"
                    "SOCIAL" -> if (objetivo == 1) "social" else "sociales"
                    else -> "de otro tipo"
                }
            }"
            TipoRequisito.RACHA_DIAS -> "Mantener una racha de $objetivo ${if (objetivo == 1) "día" else "días"}"
            TipoRequisito.NIVEL -> "Subir al nivel $objetivo"
            TipoRequisito.LOGRO -> "Desbloquear ${tituloRequerido.orEmpty()}"
        }
}

data class Logro(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val familia: FamiliaLogro,
    /** 1 = bronce, 2 = plata, 3 = oro. */
    val nivel: Int,
    val articulo: Articulo?,
    /** null = bloqueado. */
    val obtenidoAt: Instant?,
    /** false = desbloqueado y todavía no se le avisó al usuario ("¡Logro desbloqueado!"). */
    val visto: Boolean,
    val requisitos: List<RequisitoLogro>,
    /** "Progreso general" (0–100). */
    val progreso: Int,
    /** La siguiente medalla de la misma familia y su avance ("Avance para el siguiente nivel"); null = es la última. */
    val siguienteId: String?,
    val avanceSiguiente: Int?,
) {
    val obtenido: Boolean get() = obtenidoAt != null
    val esNuevo: Boolean get() = obtenido && !visto

    /** Etiquetas del detalle bloqueado: "Físico", "Nivel 3". */
    val etiquetaNivel: String get() = "Nivel $nivel"
}

/** Pestañas de la pantalla Logros. */
enum class FiltroLogros { OBTENIDOS, TODOS }

data class LogrosUiState(
    val cargando: Boolean = false,
    val logros: List<Logro> = emptyList(),
    val filtro: FiltroLogros = FiltroLogros.OBTENIDOS,
    val error: String? = null,
) {
    /** Obtenidos = desbloqueados; Todos = el catálogo (los bloqueados se dibujan en gris). */
    val visibles: List<Logro>
        get() = when (filtro) {
            FiltroLogros.OBTENIDOS -> logros.filter { it.obtenido }
            FiltroLogros.TODOS -> logros
        }

    /** Desbloqueados que la app aún no avisó (para el aviso "¡Logro desbloqueado!"). */
    val nuevos: List<Logro> get() = logros.filter { it.esNuevo }
}

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val HORA = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/** "Fecha de obtención" del diseño: "11 - sep - 2026 - 19:15" (hora de Colombia, la misma del servidor). */
fun textoFechaObtencion(instante: Instant): String {
    val local = instante.atZone(ZoneId.of("America/Bogota"))
    return "${local.dayOfMonth} - ${MESES[local.monthValue - 1]} - ${local.year} - ${HORA.format(local)}"
}
