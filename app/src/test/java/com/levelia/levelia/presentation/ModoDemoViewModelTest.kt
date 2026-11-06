package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.ModoDemoRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.ConfirmacionDemo
import com.levelia.levelia.domain.models.ModoDemo
import com.levelia.levelia.presentation.viewmodel.ModoDemoViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModoDemoViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<ModoDemoRepository>()

    @Before
    fun preparar() {
        coEvery { repo.estado() } returns AuthResult.Success(ModoDemo(activo = true, dias = 0))
        coEvery { repo.avanzarDia() } returns AuthResult.Success(Unit)
        coEvery { repo.prepararNivel(any()) } returns AuthResult.Success(Unit)
        coEvery { repo.subirNivel() } returns AuthResult.Success(Unit)
        coEvery { repo.sumarEsmeraldas() } returns AuthResult.Success(Unit)
        coEvery { repo.reiniciar() } returns AuthResult.Success(Unit)
    }

    /** Un ViewModel que ya sabe si la cuenta es demo. */
    private fun TestScope.vmCargado(): ModoDemoViewModel {
        val vm = ModoDemoViewModel(repo)
        vm.cargar()
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `cargar dice si la cuenta es demo`() = runTest {
        coEvery { repo.estado() } returns AuthResult.Success(ModoDemo(activo = true, dias = 4))
        val e = vmCargado().estado.value
        assertTrue(e.activo)
        assertEquals(4, e.dias)
    }

    @Test
    fun `si no puede preguntar no muestra el modo demo`() = runTest {
        // Cuenta normal o el SQL del modo demo sin instalar
        coEvery { repo.estado() } returns AuthResult.Error("Algo salió mal con nuestros servidores. Inténtalo más tarde.")
        val vm = vmCargado()
        assertFalse(vm.estado.value.activo)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `una cuenta normal no llama al servidor`() = runTest {
        coEvery { repo.estado() } returns AuthResult.Success(ModoDemo(activo = false, dias = 0))
        val vm = vmCargado()
        vm.avanzarDia()
        vm.subirNivel()
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.avanzarDia() }
        coVerify(exactly = 0) { repo.subirNivel() }
        assertEquals(0, vm.estado.value.cambios)
    }

    @Test
    fun `avanzar un dia avisa el cambio y relee los dias`() = runTest {
        val vm = vmCargado()
        coEvery { repo.estado() } returns AuthResult.Success(ModoDemo(activo = true, dias = 1))
        vm.avanzarDia()
        advanceUntilIdle()
        val e = vm.estado.value
        assertEquals(1, e.cambios)
        assertEquals(1, e.dias)
        assertEquals("Pasó 1 día: ya puedes volver a registrar progreso.", e.mensaje)
        assertFalse(e.enviando)
    }

    @Test
    fun `preparar nivel manda el nivel elegido`() = runTest {
        val vm = vmCargado()
        vm.prepararNivel(25)
        advanceUntilIdle()
        coVerify { repo.prepararNivel(25) }
        assertEquals("Listo: cumple y reclama cualquier misión para subir al nivel 25.", vm.estado.value.mensaje)
    }

    @Test
    fun `un error se muestra y no cuenta como cambio`() = runTest {
        coEvery { repo.subirNivel() } returns AuthResult.Error("Ya estás en el nivel máximo (100).")
        val vm = vmCargado()
        vm.subirNivel()
        advanceUntilIdle()
        assertEquals("Ya estás en el nivel máximo (100).", vm.estado.value.error)
        assertEquals(0, vm.estado.value.cambios)
        vm.cerrarError()
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `reiniciar primero pregunta`() = runTest {
        val vm = vmCargado()
        vm.pedirReinicio()
        assertEquals(ConfirmacionDemo.Reiniciar, vm.estado.value.confirmacion)
        vm.cancelarConfirmacion()
        advanceUntilIdle()
        assertNull(vm.estado.value.confirmacion)
        coVerify(exactly = 0) { repo.reiniciar() }

        vm.pedirReinicio()
        vm.confirmarReinicio()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.reiniciar() }
        assertNull(vm.estado.value.confirmacion)
        assertEquals(1, vm.estado.value.cambios)
    }

    @Test
    fun `sumar esmeraldas avisa el cambio`() = runTest {
        val vm = vmCargado()
        vm.sumarEsmeraldas()
        advanceUntilIdle()
        coVerify { repo.sumarEsmeraldas() }
        assertEquals(1, vm.estado.value.cambios)
    }

    @Test
    fun `limpiar al cerrar sesion`() = runTest {
        val vm = vmCargado()
        vm.avanzarDia()
        advanceUntilIdle()
        vm.limpiar()
        val e = vm.estado.value
        assertFalse(e.activo)
        assertEquals(0, e.cambios)
        assertNull(e.mensaje)
    }
}
