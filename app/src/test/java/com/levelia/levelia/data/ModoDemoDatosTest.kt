package com.levelia.levelia.data

import com.levelia.levelia.data.remote.ModoDemoDto
import com.levelia.levelia.data.repository.ModoDemoRepository
import com.levelia.levelia.domain.models.ModoDemo
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Respuesta de mi_modo_demo() (supabase/modo_demo.sql) y errores del servidor. */
class ModoDemoDatosTest {

    private val json = Json // sin ignoreUnknownKeys: una columna nueva sin mapear haría fallar la prueba

    @Test
    fun `respuesta de mi_modo_demo`() {
        assertEquals(ModoDemo(activo = true, dias = 3), json.decodeFromString<ModoDemoDto>("""{"activo":true,"dias":3}""").aModoDemo())
        assertEquals(ModoDemo(activo = false, dias = 0), json.decodeFromString<ModoDemoDto>("""{"activo":false,"dias":0}""").aModoDemo())
    }

    @Test
    fun `codigos del servidor se traducen`() {
        val repo = ModoDemoRepository(mockk(relaxed = true))
        fun msg(texto: String) = repo.mensajeDeError(RuntimeException(texto))
        assertEquals("Esta cuenta no tiene el modo demo.", msg("""{"code":"P0001","message":"NO_ES_DEMO"}"""))
        assertEquals("Ya estás en el nivel máximo (100).", msg("NIVEL_MAXIMO"))
        assertEquals("Ese nivel no existe.", msg("NIVEL_INVALIDO"))
        assertEquals(
            "Ya pasaste ese nivel. Usa \"Reiniciar cuenta demo\" para empezar de cero.",
            msg("NIVEL_YA_ALCANZADO"),
        )
        // Lo desconocido (por ejemplo, el SQL del modo demo sin instalar) es un error del servidor
        assertEquals("Algo salió mal con nuestros servidores. Inténtalo más tarde.", msg("Could not find the function public.demo_subir_nivel"))
    }
}
