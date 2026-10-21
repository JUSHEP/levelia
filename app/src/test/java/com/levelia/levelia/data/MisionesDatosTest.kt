package com.levelia.levelia.data

import com.levelia.levelia.data.remote.MisionDto
import com.levelia.levelia.data.remote.MisionUsuarioDto
import com.levelia.levelia.data.remote.RecompensaDto
import com.levelia.levelia.data.repository.MisionesRepository
import com.levelia.levelia.domain.ReglasNivel
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.IconoMision
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.time.LocalDate

/** Filas de supabase/misiones.sql, curva de niveles y mensajes de error del módulo Misiones. */
class MisionesDatosTest {

    private val json = Json // sin ignoreUnknownKeys: una columna nueva sin mapear haría fallar la prueba

    @Test
    fun `fila de misiones predefinida y propia`() {
        val predefinida = json.decodeFromString<MisionDto>(
            """{"id":1,"user_id":null,"titulo":"Ir al gym","descripcion":"Mejora tu estado físico.","icono":"PESA",
               "dificultad":"MEDIA","meta":3,"xp":150,"fecha_limite":null,"created_at":"2026-10-21T15:00:00+00:00"}""",
        ).aMision()
        assertEquals(IconoMision.PESA, predefinida.icono)
        assertEquals(Dificultad.MEDIA, predefinida.dificultad)
        assertFalse(predefinida.propia)
        assertNull(predefinida.fechaLimite)

        val propia = json.decodeFromString<MisionDto>(
            """{"id":7,"user_id":"u1","titulo":"Tarea de cálculo #02","descripcion":"","icono":"ICONO_NUEVO",
               "dificultad":"UNICA","meta":1,"xp":50,"fecha_limite":"2026-09-08","created_at":"2026-10-21T15:00:00+00:00"}""",
        ).aMision()
        assertTrue(propia.propia)
        assertEquals(LocalDate.of(2026, 9, 8), propia.fechaLimite)
        assertEquals(IconoMision.PORTAPAPELES, propia.icono) // ícono desconocido no rompe la lista
    }

    @Test
    fun `fila de misiones_usuario`() {
        val aceptada = json.decodeFromString<MisionUsuarioDto>(
            """{"id":3,"user_id":"u1","mision_id":1,"dificultad":"MEDIA","meta":3,"xp":150,"progreso":1,
               "fecha_limite":"2026-10-27","ultimo_registro":"2026-10-21","estado":"ACTIVA",
               "recompensa_reclamada":false,"aceptada_at":"2026-10-21T15:00:00+00:00","terminada_at":null}""",
        ).aMisionAceptada()
        assertEquals("1/3", aceptada.textoProgreso)
        assertEquals(EstadoMision.ACTIVA, aceptada.estado)
        // Ya registró hoy: mañana sí puede; pasada la fecha límite ya no
        assertFalse(aceptada.puedeRegistrarHoy(LocalDate.of(2026, 10, 21)))
        assertTrue(aceptada.puedeRegistrarHoy(LocalDate.of(2026, 10, 22)))
        assertFalse(aceptada.puedeRegistrarHoy(LocalDate.of(2026, 10, 28)))
    }

    @Test
    fun `respuesta de reclamar_recompensas`() {
        val r = json.decodeFromString<RecompensaDto>("""{"xp_ganada":125,"niveles_subidos":1,"nivel":2,"xp":25}""").aRecompensa()
        assertEquals(125, r.xpGanada)
        assertEquals(2, r.nivel)
    }

    @Test
    fun `curva de niveles igual a la del servidor`() {
        // Mismos valores que prueba supabase/pruebas_misiones.sql con xp_para_subir()
        assertEquals(100, ReglasNivel.xpParaSubir(1))
        assertEquals(283, ReglasNivel.xpParaSubir(2))
        assertEquals(98_504, ReglasNivel.xpParaSubir(99))
        assertTrue((1 until ReglasNivel.NIVEL_MAX).all { ReglasNivel.xpParaSubir(it) < ReglasNivel.xpParaSubir(it + 1) })
    }

    @Test
    fun `codigos del servidor se traducen`() {
        val repo = MisionesRepository(mockk(relaxed = true))
        fun msg(texto: String) = repo.mensajeDeError(RuntimeException(texto))
        assertEquals("¡Tienes que darle un nombre a la misión!", msg("TITULO_VACIO"))
        assertEquals("Ya registraste tu progreso hoy. ¡Vuelve mañana!", msg("""{"code":"P0001","message":"YA_REGISTRADO_HOY"}"""))
        assertEquals("Ya aceptaste esta misión.", msg("MISION_YA_ACTIVA"))
        assertEquals("Ya aceptaste esta misión.", msg("duplicate key value violates unique constraint \"misiones_usuario_una_activa\""))
        assertEquals("Primero reclama la recompensa de esta misión.", msg("RECOMPENSA_PENDIENTE"))
        assertEquals("Ya hiciste esta misión. Podrás repetirla cuando termine su plazo.", msg("MISION_EN_CURSO"))
        assertEquals("Esta misión es única y ya la aceptaste.", msg("MISION_YA_HECHA"))
        assertEquals("No hay conexión a internet. Inténtalo de nuevo.", repo.mensajeDeError(UnknownHostException("x")))
        assertEquals("Algo salió mal con nuestros servidores. Inténtalo más tarde.", msg("boom"))
    }
}
