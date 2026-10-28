package com.levelia.levelia.data

import com.levelia.levelia.data.remote.LogroDto
import com.levelia.levelia.domain.models.CalidadArticulo
import com.levelia.levelia.domain.models.EspacioArticulo
import com.levelia.levelia.domain.models.FamiliaLogro
import com.levelia.levelia.domain.models.RequisitoLogro
import com.levelia.levelia.domain.models.TipoRequisito
import com.levelia.levelia.domain.models.textoFechaObtencion
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Filas de mis_logros() (supabase/logros.sql), textos de requisitos y fecha de obtención. */
class LogrosDatosTest {

    private val json = Json // sin ignoreUnknownKeys: una columna nueva sin mapear haría fallar la prueba

    @Test
    fun `fila bloqueada de mis_logros`() {
        val logro = json.decodeFromString<LogroDto>(
            """{"id":"EN_FORMA","titulo":"¡En forma!","descripcion":"Completa 10 misiones físicas y mantén una racha de 7 días.",
               "familia":"FISICO","nivel":2,"orden":5,"articulo_id":"PECHERA_PLATA","articulo_nombre":"Pechera de plata",
               "articulo_espacio":"PECHERA","articulo_calidad":"PLATA","obtenido_at":null,"visto":false,
               "requisitos":[
                 {"tipo":"MISIONES_CATEGORIA","categoria":"FISICO","logro_requerido":null,"titulo_requerido":null,"actual":3,"objetivo":10},
                 {"tipo":"RACHA_DIAS","categoria":null,"logro_requerido":null,"titulo_requerido":null,"actual":7,"objetivo":7},
                 {"tipo":"LOGRO","categoria":null,"logro_requerido":"CALENTAMIENTO","titulo_requerido":"¡Calentamiento!","actual":1,"objetivo":1}],
               "progreso":77,"siguiente_id":"UNIDAD_ABSOLUTA","avance_siguiente":0}""",
        ).aLogro()
        assertFalse(logro.obtenido)
        assertEquals(FamiliaLogro.FISICO, logro.familia)
        assertEquals("Físico", logro.familia.texto)
        assertEquals("Nivel 2", logro.etiquetaNivel)
        assertEquals(EspacioArticulo.PECHERA, logro.articulo?.espacio)
        assertEquals(CalidadArticulo.PLATA, logro.articulo?.calidad)
        assertEquals(77, logro.progreso)
        assertEquals("UNIDAD_ABSOLUTA", logro.siguienteId)
        assertEquals(listOf("3/10", "7/7", "1/1"), logro.requisitos.map { it.textoProgreso })
        assertEquals(listOf(false, true, true), logro.requisitos.map { it.cumplido })
    }

    @Test
    fun `fila desbloqueada y sin siguiente medalla`() {
        val logro = json.decodeFromString<LogroDto>(
            """{"id":"PRIMEROS_PASOS","titulo":"¡Primeros pasos!","descripcion":"Completa tu primera misión en la app.",
               "familia":"GENERAL","nivel":1,"orden":1,"articulo_id":"PECHERA_BASICA","articulo_nombre":"Pechera básica",
               "articulo_espacio":"PECHERA","articulo_calidad":"BASICA","obtenido_at":"2026-09-12T00:15:00.123456+00:00","visto":false,
               "requisitos":[{"tipo":"MISIONES","categoria":null,"logro_requerido":null,"titulo_requerido":null,"actual":1,"objetivo":1}],
               "progreso":100,"siguiente_id":null,"avance_siguiente":null}""",
        ).aLogro()
        assertTrue(logro.obtenido)
        assertTrue(logro.esNuevo)
        assertNull(logro.siguienteId)
        // 00:15 UTC del 12 de septiembre = 19:15 del 11 en Colombia, como en el diseño
        assertEquals("11 - sep - 2026 - 19:15", textoFechaObtencion(logro.obtenidoAt!!))
    }

    @Test
    fun `textos de los requisitos`() {
        fun req(tipo: TipoRequisito, objetivo: Int, categoria: String? = null, titulo: String? = null) =
            RequisitoLogro(tipo, categoria, titulo, actual = 0, objetivo = objetivo).texto
        assertEquals("Completar 1 misión", req(TipoRequisito.MISIONES, 1))
        assertEquals("Completar 10 misiones físicas", req(TipoRequisito.MISIONES_CATEGORIA, 10, "FISICO"))
        assertEquals("Completar 3 misiones de estudio", req(TipoRequisito.MISIONES_CATEGORIA, 3, "ESTUDIO"))
        assertEquals("Mantener una racha de 7 días", req(TipoRequisito.RACHA_DIAS, 7))
        assertEquals("Subir al nivel 10", req(TipoRequisito.NIVEL, 10))
        assertEquals("Desbloquear Estudioso", req(TipoRequisito.LOGRO, 1, titulo = "Estudioso"))
    }

    @Test
    fun `fecha de obtencion con dia y hora de un digito`() {
        assertEquals("5 - ene - 2027 - 08:05", textoFechaObtencion(Instant.parse("2027-01-05T13:05:00Z")))
    }
}
