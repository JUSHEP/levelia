package com.levelia.levelia.domain

import com.levelia.levelia.domain.models.ProgresoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las reglas deben coincidir con los CHECK de `perfiles` en Supabase. */
class ReglasPerfilTest {

    // ---------- Nombre: CHECK char_length 3..20 y nombre ~ '^[A-Za-z0-9_]+$' ----------

    @Test
    fun `nombre valido con letras numeros y guion bajo`() {
        assertTrue(ReglasPerfil.nombreValido("Ronald1031"))
        assertTrue(ReglasPerfil.nombreValido("ron_123"))
        assertTrue(ReglasPerfil.nombreValido("abc"))
        assertTrue(ReglasPerfil.nombreValido("a".repeat(20)))
    }

    @Test
    fun `nombre fuera de largo es invalido`() {
        assertFalse(ReglasPerfil.nombreValido("ab"))
        assertFalse(ReglasPerfil.nombreValido("a".repeat(21)))
        assertFalse(ReglasPerfil.nombreValido(""))
    }

    @Test
    fun `nombre con espacios o simbolos es invalido`() {
        assertFalse(ReglasPerfil.nombreValido("ab c"))
        assertFalse(ReglasPerfil.nombreValido("ron-123"))
        assertFalse(ReglasPerfil.nombreValido("ron.123"))
        assertFalse(ReglasPerfil.nombreValido("@ronald"))
    }

    @Test
    fun `nombre con tildes o ene es invalido porque la base de datos lo rechaza`() {
        assertFalse(ReglasPerfil.nombreValido("Peña"))
        assertFalse(ReglasPerfil.nombreValido("José_1"))
        assertFalse(ReglasPerfil.nombreValido("Ángel"))
    }

    // ---------- Contraseña: mínimo 8, mayúscula, minúscula y número ----------

    @Test
    fun `contrasena que cumple todo`() {
        val r = ReglasPerfil.requisitosContrasena("Abcdefg1")
        assertTrue(r.longitud && r.mayuscula && r.minuscula && r.numero)
        assertTrue(r.cumpleTodos)
    }

    @Test
    fun `cada requisito de contrasena se detecta por separado`() {
        assertFalse(ReglasPerfil.requisitosContrasena("Abc1").longitud)
        assertFalse(ReglasPerfil.requisitosContrasena("abcdefg1").mayuscula)
        assertFalse(ReglasPerfil.requisitosContrasena("ABCDEFG1").minuscula)
        assertFalse(ReglasPerfil.requisitosContrasena("Abcdefgh").numero)
        assertFalse(ReglasPerfil.requisitosContrasena("123456789").cumpleTodos)
        assertFalse(ReglasPerfil.requisitosContrasena("").cumpleTodos)
    }

    @Test
    fun `exactamente 8 caracteres cumple la longitud`() {
        assertTrue(ReglasPerfil.requisitosContrasena("Abcdef12").longitud)
        assertFalse(ReglasPerfil.requisitosContrasena("Abcde12").longitud)
    }

    @Test
    fun `motivo exacto de contrasena invalida`() {
        assertEquals("Escribe tu nueva contraseña.", ReglasPerfil.motivoContrasenaInvalida("", ""))
        assertEquals(
            "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.",
            ReglasPerfil.motivoContrasenaInvalida("abc", "abc"),
        )
        assertEquals("La contraseña debe tener un número.", ReglasPerfil.motivoContrasenaInvalida("Levelianueva", "Levelianueva"))
        assertEquals("Confirma tu nueva contraseña.", ReglasPerfil.motivoContrasenaInvalida("Levelia2026", ""))
        assertEquals("Las contraseñas no coinciden.", ReglasPerfil.motivoContrasenaInvalida("Levelia2026", "Levelia2027"))
        // Sin carácter especial es válida: la app solo pide los 4 requisitos del diseño
        assertNull(ReglasPerfil.motivoContrasenaInvalida("Levelia2026", "Levelia2026"))
    }

    // ---------- Correo ----------

    @Test
    fun `correos validos`() {
        assertTrue(ReglasPerfil.correoValido("rontorres304@gmail.com"))
        assertTrue(ReglasPerfil.correoValido("nombre.apellido+test@dominio.co"))
    }

    @Test
    fun `correos invalidos`() {
        assertFalse(ReglasPerfil.correoValido(""))
        assertFalse(ReglasPerfil.correoValido("correo-malo"))
        assertFalse(ReglasPerfil.correoValido("sin@dominio"))
        assertFalse(ReglasPerfil.correoValido("con espacio@gmail.com"))
        assertFalse(ReglasPerfil.correoValido("@gmail.com"))
    }

    // ---------- Barra de experiencia ----------

    @Test
    fun `fraccion de experiencia`() {
        assertEquals(0.65f, ProgresoData(xp = 650, xpSiguienteNivel = 1000).fraccion, 0.0001f)
        assertEquals(0f, ProgresoData(xp = 0).fraccion, 0f)
        assertEquals(1f, ProgresoData(xp = 5000, xpSiguienteNivel = 1000).fraccion, 0f)
        assertEquals(0f, ProgresoData(xp = 10, xpSiguienteNivel = 0).fraccion, 0f)
    }
}
