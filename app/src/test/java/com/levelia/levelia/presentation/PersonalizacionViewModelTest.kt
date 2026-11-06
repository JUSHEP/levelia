package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.PersonalizacionRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.AvisoPersonalizacion
import com.levelia.levelia.domain.models.Personalizacion
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.domain.models.TipoSkin
import com.levelia.levelia.presentation.viewmodel.PersonalizacionViewModel
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
class PersonalizacionViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<PersonalizacionRepository>()

    private val ayer = Instant.parse("2026-11-04T15:00:00Z")

    private fun skin(id: String, tipo: TipoSkin, obtenida: Boolean, visto: Boolean = true, nivel: Int? = null, precio: Int? = null) = Skin(
        id = id, nombre = id, habilidades = listOf("Descripción de $id"), tipo = tipo, nivelRequerido = nivel, precio = precio,
        obtenidaAt = if (obtenida) ayer else null, visto = visto,
    )

    private val novato = skin("NOVATO", TipoSkin.NIVEL, obtenida = true, nivel = 1)
    private val escudero = skin("ESCUDERO", TipoSkin.NIVEL, obtenida = true, visto = false, nivel = 5)
    private val caballero = skin("CABALLERO", TipoSkin.NIVEL, obtenida = false, nivel = 25)
    private val arquero = skin("ARQUERO", TipoSkin.TIENDA, obtenida = false, precio = 200)
    private val vikingo = skin("VIKINGO", TipoSkin.TIENDA, obtenida = false, precio = 300)

    private fun respuesta(esmeraldas: Int = 250, equipada: String = "NOVATO") =
        AuthResult.Success(Personalizacion(esmeraldas, nivel = 5, equipadaId = equipada, skins = listOf(novato, escudero, caballero, arquero, vikingo)))

    @Before
    fun preparar() {
        coEvery { repo.obtener() } returns respuesta()
        coEvery { repo.equipar(any()) } returns AuthResult.Success(Unit)
        coEvery { repo.comprar(any()) } returns AuthResult.Success(Unit)
        coEvery { repo.marcarVistas() } returns AuthResult.Success(Unit)
    }

    private fun vm() = PersonalizacionViewModel(repo)

    @Test
    fun `cargar trae saldo, nivel, equipada y catalogo`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        val e = vm.estado.value
        assertEquals(250, e.esmeraldas)
        assertEquals(5, e.nivel)
        assertEquals(novato, e.equipada)
        assertEquals(listOf(novato, escudero, caballero), e.deNivel)
        assertEquals(listOf(arquero, vikingo), e.deTienda)
        assertEquals(arquero, vm.skin("ARQUERO"))
        assertTrue(!e.cargando)
    }

    @Test
    fun `equipar una desbloqueada la deja equipada y vuelve a leer`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        coEvery { repo.obtener() } returns respuesta(equipada = "ESCUDERO")
        vm.equipar("ESCUDERO")
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.equipar("ESCUDERO") }
        assertEquals("ESCUDERO", vm.estado.value.equipada?.id)
        coVerify(exactly = 2) { repo.obtener() }
    }

    @Test
    fun `no se equipa una bloqueada ni la que ya esta equipada`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.equipar("CABALLERO")
        vm.equipar("NOVATO")
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.equipar(any()) }
    }

    @Test
    fun `comprar pregunta primero y luego deja la skin equipada`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.pedirCompra("ARQUERO")
        assertEquals(AvisoPersonalizacion.ConfirmarCompra(arquero), vm.estado.value.aviso)
        coVerify(exactly = 0) { repo.comprar(any()) }

        // Después de comprar, el servidor ya la tiene como suya y equipada
        val comprado = arquero.copy(obtenidaAt = ayer)
        coEvery { repo.obtener() } returns AuthResult.Success(
            Personalizacion(50, nivel = 5, equipadaId = "ARQUERO", skins = listOf(novato, escudero, caballero, comprado, vikingo)),
        )
        vm.confirmarCompra()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.comprar("ARQUERO") }
        assertEquals(AvisoPersonalizacion.Comprada(arquero), vm.estado.value.aviso)
        assertEquals(50, vm.estado.value.esmeraldas)
        assertEquals("ARQUERO", vm.estado.value.equipada?.id)

        vm.cerrarAviso()
        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun `sin esmeraldas suficientes no se pide la compra`() = runTest {
        coEvery { repo.obtener() } returns respuesta(esmeraldas = 100)
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.pedirCompra("VIKINGO")
        assertNull(vm.estado.value.aviso)
        assertEquals("No tienes suficientes esmeraldas para esta skin.", vm.estado.value.error)
        vm.confirmarCompra() // sin diálogo de confirmación no compra nada
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.comprar(any()) }
    }

    @Test
    fun `una skin de nivel no se compra`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.pedirCompra("CABALLERO")
        assertNull(vm.estado.value.aviso)
    }

    @Test
    fun `error del servidor al comprar se muestra`() = runTest {
        coEvery { repo.comprar(any()) } returns AuthResult.Error("No tienes suficientes esmeraldas para esta skin.")
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.pedirCompra("ARQUERO")
        vm.confirmarCompra()
        advanceUntilIdle()
        assertEquals("No tienes suficientes esmeraldas para esta skin.", vm.estado.value.error)
        assertNull(vm.estado.value.aviso)
        assertEquals("NOVATO", vm.estado.value.equipada?.id)
    }

    @Test
    fun `las skins nuevas se avisan una sola vez`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        assertEquals(listOf(escudero), vm.estado.value.nuevas)
        vm.cerrarNuevas()
        assertTrue(vm.estado.value.nuevas.isEmpty()) // se oculta sin esperar a la base
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.marcarVistas() }
        vm.cerrarNuevas() // sin nuevas no vuelve a llamar
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.marcarVistas() }
    }

    @Test
    fun `limpiar al cerrar sesion borra lo del usuario anterior`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.limpiar()
        assertTrue(vm.estado.value.skins.isEmpty())
        assertEquals(0, vm.estado.value.esmeraldas)
        assertNull(vm.estado.value.equipada)
    }
}
