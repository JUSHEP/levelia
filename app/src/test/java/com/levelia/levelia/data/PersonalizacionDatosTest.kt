package com.levelia.levelia.data

import com.levelia.levelia.data.remote.PersonalizacionDto
import com.levelia.levelia.data.repository.PersonalizacionRepository
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.PersonalizacionUiState
import com.levelia.levelia.domain.models.TipoSkin
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Respuesta de mi_personalizacion() (supabase/personalizacion.sql), textos de las skins y errores del servidor. */
class PersonalizacionDatosTest {

    private val json = Json // sin ignoreUnknownKeys: una columna nueva sin mapear haría fallar la prueba

    private val respuesta = """
        {"esmeraldas":60,"nivel":10,"skin_equipada":"SOLDADO","skins":[
          {"id":"NOVATO","nombre":"Novato","habilidades":["Acaba de empezar su aventura."],"tipo":"NIVEL",
           "nivel_requerido":1,"precio":null,"orden":1,"obtenida_at":"2026-11-05T21:00:00.123456+00:00","visto":true},
          {"id":"SOLDADO","nombre":"Soldado","habilidades":["Constante y disciplinado.","Casco y cota de malla de hierro."],
           "tipo":"NIVEL","nivel_requerido":10,"precio":null,"orden":3,"obtenida_at":"2026-11-05T21:05:00+00:00","visto":false},
          {"id":"CABALLERO","nombre":"Caballero","habilidades":["Un ejemplo para los demás aventureros."],"tipo":"NIVEL",
           "nivel_requerido":25,"precio":null,"orden":4,"obtenida_at":null,"visto":false},
          {"id":"ARQUERO","nombre":"Arquero","habilidades":["Vigila el bosque desde los árboles."],"tipo":"TIENDA",
           "nivel_requerido":null,"precio":200,"orden":7,"obtenida_at":null,"visto":false},
          {"id":"FUTURA","nombre":"Futura","habilidades":[],"tipo":"EVENTO","nivel_requerido":null,"precio":null,
           "orden":13,"obtenida_at":null,"visto":false}
        ]}
    """.trimIndent()

    private fun estado() = json.decodeFromString<PersonalizacionDto>(respuesta).aPersonalizacion().let {
        PersonalizacionUiState(esmeraldas = it.esmeraldas, nivel = it.nivel, equipadaId = it.equipadaId, skins = it.skins)
    }

    @Test
    fun `respuesta de mi_personalizacion`() {
        val p = json.decodeFromString<PersonalizacionDto>(respuesta).aPersonalizacion()
        assertEquals(60, p.esmeraldas)
        assertEquals(10, p.nivel)
        assertEquals("SOLDADO", p.equipadaId)
        // Un tipo que esta versión no conoce se salta en vez de romper la pantalla
        assertEquals(listOf("NOVATO", "SOLDADO", "CABALLERO", "ARQUERO"), p.skins.map { it.id })
        val novato = p.skins.first()
        assertEquals(Instant.parse("2026-11-05T21:00:00.123456Z"), novato.obtenidaAt)
        assertEquals(TipoSkin.NIVEL, novato.tipo)
        assertEquals(listOf("Constante y disciplinado.", "Casco y cota de malla de hierro."), p.skins[1].habilidades)
    }

    @Test
    fun `nuevas, secretas, equipada y textos`() {
        val e = estado()
        val (novato, soldado, caballero, arquero) = e.skins
        assertEquals(listOf(novato, soldado, caballero), e.deNivel)
        assertEquals(listOf(arquero), e.deTienda)
        assertEquals(soldado, e.equipada)
        assertTrue(e.esEquipada(soldado))
        assertFalse(e.esEquipada(novato))
        // Soldado se desbloqueó al subir y no se ha avisado; Novato es la inicial (no se anuncia)
        assertEquals(listOf(soldado), e.nuevas)

        assertTrue(caballero.esSecreta)
        assertEquals("????", caballero.tituloDetalle)
        assertEquals("Nvl. 25", caballero.etiquetaNivel)
        assertEquals("Soldado Nvl.10", soldado.tituloDetalle)
        // La de tienda sin comprar no es secreta: se ve para poder comprarla
        assertFalse(arquero.esSecreta)
        assertEquals("Arquero", arquero.tituloDetalle)
        assertNull(arquero.etiquetaNivel)
        assertEquals(140, e.faltan(arquero))
        assertEquals(0, e.copy(esmeraldas = 250).faltan(arquero))
    }

    @Test
    fun `sin skin equipada se usa la primera del guerrero y sin catalogo no hay ninguna`() {
        val e = estado()
        assertEquals("NOVATO", e.copy(equipadaId = null).equipada?.id)
        assertEquals("NOVATO", e.copy(equipadaId = "BORRADA").equipada?.id)
        assertNull(PersonalizacionUiState().equipada)
    }

    @Test
    fun `esmeraldas por dificultad iguales a esmeraldas_dificultad del servidor`() {
        assertEquals(listOf(10, 20, 40, 10), listOf(Dificultad.FACIL, Dificultad.MEDIA, Dificultad.DIFICIL, Dificultad.UNICA).map { it.esmeraldas })
    }

    @Test
    fun `errores del servidor en espanol`() {
        val repo = PersonalizacionRepository(mockk(relaxed = true))
        assertEquals(
            "No tienes suficientes esmeraldas para esta skin.",
            repo.mensajeDeError(RuntimeException("P0001: ESMERALDAS_INSUFICIENTES")),
        )
        assertEquals("Ya tienes esta skin.", repo.mensajeDeError(RuntimeException("SKIN_YA_COMPRADA")))
        assertEquals("Todavía no tienes esta skin.", repo.mensajeDeError(RuntimeException("SKIN_BLOQUEADA")))
        assertEquals(
            "Algo salió mal con nuestros servidores. Inténtalo más tarde.",
            repo.mensajeDeError(RuntimeException("otra cosa")),
        )
    }
}
