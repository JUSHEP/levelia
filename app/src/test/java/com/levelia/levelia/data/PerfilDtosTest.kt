package com.levelia.levelia.data

import com.levelia.levelia.data.remote.PerfilDto
import com.levelia.levelia.data.remote.ProgresoDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las filas tal como las devuelve hoy la base real deben poder leerse sin errores. */
class PerfilDtosTest {

    private val json = Json // sin ignoreUnknownKeys: una columna nueva sin mapear haría fallar la prueba

    @Test
    fun `fila de perfiles se convierte en UserData`() {
        val fila = """
            {"id":"60ace839-85dc-4fe8-beb9-a766531e491e","nombre":"Ronald1031","email":"rontorres304@gmail.com",
             "edad":26,"avatar":"AVATAR_7","created_at":"2026-10-15T17:40:00.123456+00:00",
             "updated_at":"2026-10-15T17:40:00.123456+00:00"}
        """.trimIndent()
        val usuario = json.decodeFromString<PerfilDto>(fila).aUserData()
        assertEquals("Ronald1031", usuario.nombre)
        assertEquals(26, usuario.edad)
        assertEquals("AVATAR_7", usuario.avatar)
        assertEquals(1_792_086_000_123L, usuario.createdAt)
    }

    @Test
    fun `fecha ilegible no rompe la conversion`() {
        val dto = PerfilDto("id", "Ronald1031", "a@b.co", 20, "AVATAR_1", createdAt = "no-es-fecha")
        assertTrue(dto.aUserData().createdAt > 0)
    }

    @Test
    fun `fila de progreso real calcula la meta del nivel con la curva`() {
        val fila = """{"user_id":"u1","nivel":2,"xp":650,"updated_at":"2026-10-15T17:40:00+00:00"}"""
        val progreso = json.decodeFromString<ProgresoDto>(fila).aProgresoData()
        assertEquals(2, progreso.nivel)
        assertEquals(650, progreso.xp)
        assertEquals(283, progreso.xpSiguienteNivel) // nivel 2 → round(100 · 2^1.5)
    }
}
