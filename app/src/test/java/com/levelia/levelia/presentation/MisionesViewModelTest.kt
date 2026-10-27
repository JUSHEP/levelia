package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.MisionesRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.FiltroMisiones
import com.levelia.levelia.domain.models.IconoMision
import com.levelia.levelia.domain.models.Mision
import com.levelia.levelia.domain.models.MisionAceptada
import com.levelia.levelia.domain.models.MisionConProgreso
import com.levelia.levelia.domain.models.Recompensa
import com.levelia.levelia.presentation.viewmodel.MisionesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MisionesViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<MisionesRepository>()

    private val gym = Mision(1, "Ir al gym", "Mejora tu estado físico.", IconoMision.PESA, Dificultad.MEDIA, 3, 150, null, propia = false)
    private val clases = Mision(2, "A clases", "Asiste a la universidad.", IconoMision.CEREBRO, Dificultad.MEDIA, 2, 100, null, propia = false)

    private fun aceptada(estado: EstadoMision, reclamada: Boolean = false, progreso: Int = 0) = MisionAceptada(
        id = 10, misionId = 1, dificultad = Dificultad.MEDIA, meta = 3, xp = 150, progreso = progreso,
        fechaLimite = LocalDate.of(2026, 10, 27), ultimoRegistro = null, estado = estado, recompensaReclamada = reclamada,
    )

    @Before
    fun preparar() {
        coEvery { repo.obtenerMisiones() } returns AuthResult.Success(
            listOf(MisionConProgreso(gym, null), MisionConProgreso(clases, null)),
        )
    }

    private fun vm() = MisionesViewModel(repo)

    // ============================== Lista ==============================

    @Test
    fun `cargar trae las misiones y activas queda vacia hasta aceptar una`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        val estado = vm.estado.value
        assertEquals(2, estado.misiones.size)
        assertTrue(estado.misiones.all { it.sePuedeAceptar(estado.hoy) })
        assertTrue(estado.visibles.isEmpty()) // pestaña Activas: ninguna aceptada todavía
        assertFalse(estado.hayRecompensas)
        assertFalse(estado.cargando)
    }

    @Test
    fun `cada pestana muestra lo suyo y todas pone primero las no cumplidas`() = runTest {
        val enfoque = gym.copy(id = 3, titulo = "Modo enfoque")
        val sigueme = gym.copy(id = 4, titulo = "Sígueme y te sigo")
        val perdida = gym.copy(id = 5, titulo = "Perdida")
        coEvery { repo.obtenerMisiones() } returns AuthResult.Success(
            listOf(
                MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, progreso = 3)), // por reclamar
                MisionConProgreso(clases, null), // sin aceptar
                MisionConProgreso(enfoque, aceptada(EstadoMision.ACTIVA, progreso = 1)), // en curso
                MisionConProgreso(sigueme, aceptada(EstadoMision.COMPLETADA, reclamada = true, progreso = 3)), // ya reclamada
                MisionConProgreso(perdida, aceptada(EstadoMision.PERDIDA)),
            ),
        )
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()

        vm.onFiltro(FiltroMisiones.ACTIVAS)
        assertEquals(listOf(enfoque), vm.estado.value.visibles.map { it.mision })

        // Completadas: las cumplidas, reclamadas o no (el botón Reclamar sale si falta alguna)
        vm.onFiltro(FiltroMisiones.COMPLETADAS)
        assertEquals(listOf(gym, sigueme), vm.estado.value.visibles.map { it.mision })
        assertTrue(vm.estado.value.hayRecompensas)

        // Todas: el catálogo completo, primero las que aún no están cumplidas (en su orden original)
        vm.onFiltro(FiltroMisiones.TODAS)
        assertEquals(listOf(clases, enfoque, perdida, gym, sigueme), vm.estado.value.visibles.map { it.mision })
    }

    @Test
    fun `volver a aceptar sigue la misma regla que el servidor`() {
        val dentroDelPlazo = LocalDate.of(2026, 10, 27) // fechaLimite de aceptada()
        val pasadoElPlazo = LocalDate.of(2026, 10, 28)
        // Perdida o reclamada: solo cuando terminó su semana (si no, se sumaría XP varias veces el mismo día)
        assertTrue(MisionConProgreso(gym, aceptada(EstadoMision.PERDIDA)).sePuedeAceptar(pasadoElPlazo))
        assertTrue(MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, reclamada = true)).sePuedeAceptar(pasadoElPlazo))
        assertFalse(MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, reclamada = true)).sePuedeAceptar(dentroDelPlazo))
        // Activa, o cumplida con la XP sin reclamar: no
        assertFalse(MisionConProgreso(gym, aceptada(EstadoMision.ACTIVA)).sePuedeAceptar(pasadoElPlazo))
        assertFalse(MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA)).sePuedeAceptar(pasadoElPlazo))
        // UNICA: una sola vez
        val unica = gym.copy(dificultad = Dificultad.UNICA, fechaLimite = LocalDate.of(2026, 12, 1))
        assertTrue(MisionConProgreso(unica, null).sePuedeAceptar(pasadoElPlazo))
        assertFalse(MisionConProgreso(unica, aceptada(EstadoMision.COMPLETADA, reclamada = true)).sePuedeAceptar(pasadoElPlazo))
    }

    @Test
    fun `guardar dificultad no acepta y vuelve a la lista`() = runTest {
        coEvery { repo.ajustarDificultad(1, Dificultad.DIFICIL) } returns AuthResult.Success(Unit)
        val vm = vm()
        var volvio = false
        vm.ajustarDificultad(1, Dificultad.DIFICIL) { volvio = true }
        advanceUntilIdle()
        assertTrue(volvio)
        coVerify { repo.ajustarDificultad(1, Dificultad.DIFICIL) }
        coVerify(exactly = 0) { repo.aceptarMision(any(), any()) }
        coVerify { repo.obtenerMisiones() } // la lista se relee con las veces y XP nuevas
    }

    @Test
    fun `guardar dificultad con error se queda y lo muestra`() = runTest {
        coEvery { repo.ajustarDificultad(any(), any()) } returns AuthResult.Error("Ya aceptaste esta misión.")
        val vm = vm()
        var volvio = false
        vm.ajustarDificultad(1, Dificultad.FACIL) { volvio = true }
        advanceUntilIdle()
        assertFalse(volvio)
        assertEquals("Ya aceptaste esta misión.", vm.estado.value.error)
    }

    @Test
    fun `la dificultad guardada cambia veces y xp de la mision`() {
        val ajustada = gym.ajustada(Dificultad.DIFICIL)
        assertEquals(Dificultad.DIFICIL, ajustada.dificultad)
        assertEquals(5, ajustada.meta)
        assertEquals(300, ajustada.xp)
        // Sin guardar, o guardada la misma: conserva sus valores originales (A clases es MEDIA 2/100)
        assertEquals(clases, clases.ajustada(null))
        assertEquals(clases, clases.ajustada(Dificultad.MEDIA))
    }

    @Test
    fun `detalles dice por que no se puede aceptar`() {
        val dentro = LocalDate.of(2026, 10, 27)
        val despues = LocalDate.of(2026, 10, 28)
        assertNull(MisionConProgreso(gym, null).motivoNoAceptable(dentro))
        assertEquals("Ya aceptaste esta misión.", MisionConProgreso(gym, aceptada(EstadoMision.ACTIVA)).motivoNoAceptable(dentro))
        assertEquals(
            "Primero reclama la recompensa de esta misión.",
            MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA)).motivoNoAceptable(despues),
        )
        assertEquals(
            "Ya hiciste esta misión. Podrás repetirla cuando termine su plazo.",
            MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, reclamada = true)).motivoNoAceptable(dentro),
        )
        assertNull(MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, reclamada = true)).motivoNoAceptable(despues))
        val unica = gym.copy(dificultad = Dificultad.UNICA, fechaLimite = LocalDate.of(2026, 12, 1))
        assertEquals("Esta misión es única y ya la aceptaste.", MisionConProgreso(unica, aceptada(EstadoMision.PERDIDA)).motivoNoAceptable(despues))
    }

    @Test
    fun `limpiar al cerrar sesion borra lo del usuario anterior`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.onTitulo("Leer")
        vm.limpiar()
        assertTrue(vm.estado.value.misiones.isEmpty())
        assertEquals("", vm.crear.value.titulo)
    }

    @Test
    fun `error al cargar se muestra`() = runTest {
        coEvery { repo.obtenerMisiones() } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        assertEquals("No hay conexión a internet. Inténtalo de nuevo.", vm.estado.value.error)
        vm.cerrarAviso()
        assertNull(vm.estado.value.error)
    }

    // ============================== Acciones ==============================

    @Test
    fun `aceptar con dificultad ajustada recarga y avisa`() = runTest {
        coEvery { repo.aceptarMision(1, Dificultad.DIFICIL) } returns AuthResult.Success(aceptada(EstadoMision.ACTIVA))
        val vm = vm()
        var aceptadaOk = false
        vm.aceptar(1, Dificultad.DIFICIL) { aceptadaOk = true }
        advanceUntilIdle()
        assertTrue(aceptadaOk)
        coVerify { repo.aceptarMision(1, Dificultad.DIFICIL) }
        coVerify { repo.obtenerMisiones() }
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun `al aceptar la lista muestra de una vez la mision aceptada`() = runTest {
        coEvery { repo.aceptarMision(1, null) } returns AuthResult.Success(aceptada(EstadoMision.ACTIVA))
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        var vistaEnElDetalle: MisionAceptada? = null
        vm.aceptar(1) { vistaEnElDetalle = vm.estado.value.misiones.first { it.mision.id == 1L }.aceptada }
        advanceUntilIdle()
        // Al pasar al detalle ya tiene la aceptada (para "Registrar progreso"), sin esperar la recarga
        assertEquals(10L, vistaEnElDetalle?.id)
    }

    @Test
    fun `registrar con el plazo vencido avisa y no vuelve a la lista`() = runTest {
        coEvery { repo.registrarProgreso(10) } returns AuthResult.Success(aceptada(EstadoMision.PERDIDA))
        val vm = vm()
        var registrado = false
        vm.registrarProgreso(10) { registrado = true }
        advanceUntilIdle()
        assertFalse(registrado)
        // El aviso sigue después de recargar la lista
        assertEquals("Se venció el plazo de esta misión.", vm.estado.value.error)
    }

    @Test
    fun `registrar dos veces el mismo dia muestra el mensaje del servidor`() = runTest {
        coEvery { repo.registrarProgreso(10) } returns AuthResult.Error("Ya registraste tu progreso hoy. ¡Vuelve mañana!")
        val vm = vm()
        var registrado = false
        vm.registrarProgreso(10) { registrado = true }
        advanceUntilIdle()
        assertFalse(registrado)
        assertEquals("Ya registraste tu progreso hoy. ¡Vuelve mañana!", vm.estado.value.error)
    }

    @Test
    fun `registrar progreso vuelve a la lista con el progreso nuevo`() = runTest {
        coEvery { repo.registrarProgreso(10) } returns AuthResult.Success(aceptada(EstadoMision.ACTIVA, progreso = 1))
        val vm = vm()
        var registrado = false
        vm.registrarProgreso(10) { registrado = true }
        advanceUntilIdle()
        assertTrue(registrado)
        coVerify { repo.obtenerMisiones() }
    }

    @Test
    fun `reclamar guarda la recompensa y no llama si no hay nada`() = runTest {
        val vm = vm()
        vm.cargar()
        advanceUntilIdle()
        vm.reclamar()
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.reclamarRecompensas() }

        coEvery { repo.obtenerMisiones() } returns AuthResult.Success(
            listOf(MisionConProgreso(gym, aceptada(EstadoMision.COMPLETADA, progreso = 3))),
        )
        coEvery { repo.reclamarRecompensas() } returns AuthResult.Success(Recompensa(150, 1, 2, 50))
        vm.cargar()
        advanceUntilIdle()
        vm.reclamar()
        advanceUntilIdle()
        assertEquals(Recompensa(150, 1, 2, 50), vm.estado.value.recompensa)
    }

    // ========================= Crea tu propia misión =========================

    @Test
    fun `crear sin nombre ni dificultad marca los dos campos`() = runTest {
        val vm = vm()
        vm.crearMision()
        val f = vm.crear.value
        assertEquals("¡Tienes que darle un nombre a la misión!", f.errorTitulo)
        assertEquals("¡No has definido la dificultad!", f.errorDificultad)
        coVerify(exactly = 0) { repo.crearMision(any(), any(), any(), any()) }

        vm.onTitulo("Tarea de cálculo #02")
        assertNull(vm.crear.value.errorTitulo)
    }

    @Test
    fun `nombre de mas de 40 caracteres se marca en el campo`() = runTest {
        val vm = vm()
        vm.onTitulo("a".repeat(41))
        vm.onDificultad(Dificultad.MEDIA)
        vm.crearMision()
        assertEquals("El nombre puede tener máximo 40 caracteres.", vm.crear.value.errorTitulo)
        coVerify(exactly = 0) { repo.crearMision(any(), any(), any(), any()) }
    }

    @Test
    fun `unica pide una fecha que no sea pasada`() = runTest {
        val vm = vm()
        vm.onTitulo("Tarea de cálculo #02")
        vm.onDificultad(Dificultad.UNICA)
        vm.crearMision()
        assertEquals("¡Tienes que elegir una fecha límite!", vm.crear.value.errorFecha)

        vm.onFechaLimite(LocalDate.now().minusYears(1))
        vm.crearMision()
        assertEquals("La fecha límite no puede ser anterior a hoy.", vm.crear.value.errorFecha)
        coVerify(exactly = 0) { repo.crearMision(any(), any(), any(), any()) }
    }

    @Test
    fun `por veces no pide fecha y crea la mision`() = runTest {
        coEvery { repo.crearMision(any(), any(), any(), any()) } returns AuthResult.Success(
            Mision(9, "Leer", "", IconoMision.PORTAPAPELES, Dificultad.FACIL, 1, 75, null, propia = true),
        )
        val vm = vm()
        vm.onTitulo("Leer")
        vm.onDescripcion("Un capítulo")
        vm.onDificultad(Dificultad.FACIL)
        var creada = false
        vm.crearMision { creada = true }
        advanceUntilIdle()
        assertTrue(creada)
        coVerify { repo.crearMision("Leer", "Un capítulo", Dificultad.FACIL, null) }
        assertEquals("", vm.crear.value.titulo) // el formulario queda limpio
    }

    @Test
    fun `unica con fecha futura se envia con la fecha`() = runTest {
        val fecha = LocalDate.now().plusYears(1)
        coEvery { repo.crearMision(any(), any(), any(), any()) } returns AuthResult.Success(
            Mision(9, "Tarea", "", IconoMision.PORTAPAPELES, Dificultad.UNICA, 1, 50, fecha, propia = true),
        )
        val vm = vm()
        vm.onTitulo("Tarea")
        vm.onDificultad(Dificultad.UNICA)
        vm.onFechaLimite(fecha)
        vm.crearMision()
        advanceUntilIdle()
        coVerify { repo.crearMision("Tarea", "", Dificultad.UNICA, fecha) }
    }

    @Test
    fun `error del servidor al crear queda en el formulario`() = runTest {
        coEvery { repo.crearMision(any(), any(), any(), any()) } returns AuthResult.Error("Algo salió mal con nuestros servidores. Inténtalo más tarde.")
        val vm = vm()
        vm.onTitulo("Leer")
        vm.onDificultad(Dificultad.MEDIA)
        vm.crearMision()
        advanceUntilIdle()
        assertEquals("Algo salió mal con nuestros servidores. Inténtalo más tarde.", vm.crear.value.error)
        assertFalse(vm.crear.value.guardando)
        assertEquals("Leer", vm.crear.value.titulo) // no se pierde lo escrito
    }
}
