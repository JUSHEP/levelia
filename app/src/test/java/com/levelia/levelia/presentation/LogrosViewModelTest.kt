package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.LogrosRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.FamiliaLogro
import com.levelia.levelia.domain.models.FiltroLogros
import com.levelia.levelia.domain.models.Logro
import com.levelia.levelia.presentation.viewmodel.LogrosViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LogrosViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<LogrosRepository>()

    private fun logro(id: String, obtenido: Boolean, visto: Boolean = true) = Logro(
        id = id, titulo = id, descripcion = "", familia = FamiliaLogro.GENERAL, nivel = 1, articulo = null,
        obtenidoAt = if (obtenido) Instant.parse("2026-10-28T15:00:00Z") else null, visto = visto,
        requisitos = emptyList(), progreso = if (obtenido) 100 else 0, siguienteId = null, avanceSiguiente = null,
    )

    private val primeros = logro("PRIMEROS_PASOS", obtenido = true, visto = false)
    private val calentamiento = logro("CALENTAMIENTO", obtenido = true)
    private val enForma = logro("EN_FORMA", obtenido = false)

    @Before
    fun preparar() {
        coEvery { repo.obtenerLogros() } returns AuthResult.Success(listOf(primeros, calentamiento, enForma))
        coEvery { repo.marcarVistos() } returns AuthResult.Success(Unit)
    }

    private fun vm() = LogrosViewModel(repo)

    @Test
    fun `obtenidos muestra solo los desbloqueados y todos el catalogo`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        assertEquals(listOf(primeros, calentamiento), vm.estado.value.visibles) // pestaña por defecto: Obtenidos
        vm.onFiltro(FiltroLogros.TODOS)
        assertEquals(listOf(primeros, calentamiento, enForma), vm.estado.value.visibles)
        assertEquals(enForma, vm.logro("EN_FORMA"))
    }

    @Test
    fun `los nuevos se avisan una sola vez`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        assertEquals(listOf(primeros), vm.estado.value.nuevos)
        vm.cerrarNuevos()
        assertTrue(vm.estado.value.nuevos.isEmpty()) // se oculta sin esperar a la base
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.marcarVistos() }
        vm.cerrarNuevos() // sin nuevos no vuelve a llamar
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.marcarVistos() }
    }

    @Test
    fun `error al cargar se muestra y se cierra`() = runTest {
        coEvery { repo.obtenerLogros() } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        assertEquals("No hay conexión a internet. Inténtalo de nuevo.", vm.estado.value.error)
        vm.cerrarError()
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `limpiar al cerrar sesion borra lo del usuario anterior`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.limpiar()
        assertTrue(vm.estado.value.logros.isEmpty())
        assertEquals(FiltroLogros.OBTENIDOS, vm.estado.value.filtro)
    }
}
