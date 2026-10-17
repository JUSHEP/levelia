package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.AuthRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.ProgresoData
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.domain.models.UserData
import com.levelia.levelia.presentation.viewmodel.PerfilViewModel
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
class PerfilViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<AuthRepository>()
    private val usuario = UserData(id = "u1", nombre = "Ronald1031", email = "rontorres304@gmail.com", edad = 26, avatar = "AVATAR_7")
    private val progreso = ProgresoData(nivel = 2, xp = 650)

    @Before
    fun preparar() {
        coEvery { repo.obtenerPerfil() } returns AuthResult.Success(usuario)
        coEvery { repo.obtenerProgreso() } returns AuthResult.Success(progreso)
        coEvery { repo.nombreDisponible(any()) } returns AuthResult.Success(true)
    }

    private suspend fun TestScope.vmCargado(): PerfilViewModel {
        val vm = PerfilViewModel(repo)
        vm.cargarPerfil()
        advanceUntilIdle()
        return vm
    }

    // ============================== Cargar ==============================

    @Test
    fun `carga nombre avatar y progreso de la base de datos`() = runTest {
        val vm = vmCargado()
        assertEquals(usuario, vm.perfil.value.usuario)
        assertEquals(progreso, vm.perfil.value.progreso)
        assertFalse(vm.perfil.value.cargando)
    }

    @Test
    fun `si falla la carga se conserva lo que ya se mostraba`() = runTest {
        coEvery { repo.obtenerPerfil() } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        coEvery { repo.obtenerProgreso() } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = PerfilViewModel(repo)
        vm.cargarPerfil(usuarioInicial = usuario)
        advanceUntilIdle()
        assertEquals(usuario, vm.perfil.value.usuario)
        assertEquals(ProgresoData(), vm.perfil.value.progreso)
    }

    // ============================== Editar nombre ==============================

    @Test
    fun `nombre vacio muestra usuario invalido sin llamar al servidor`() = runTest {
        val vm = vmCargado()
        vm.iniciarEditarNombre()
        var listo = false
        vm.guardarNombre { listo = true }
        advanceUntilIdle()
        assertTrue(vm.editarNombre.value.error)
        assertEquals(TipoDialogoPerfil.USUARIO_INVALIDO, vm.dialogo.value?.tipo)
        assertFalse(listo)
        coVerify(exactly = 0) { repo.actualizarNombre(any()) }
    }

    @Test
    fun `nombre con espacio o tilde es invalido`() = runTest {
        val vm = vmCargado()
        for (malo in listOf("ab c", "Peña_1")) {
            vm.iniciarEditarNombre()
            vm.onNuevoNombreChanged(malo)
            vm.guardarNombre {}
            advanceUntilIdle()
            assertEquals(TipoDialogoPerfil.USUARIO_INVALIDO, vm.dialogo.value?.tipo)
        }
        coVerify(exactly = 0) { repo.actualizarNombre(any()) }
    }

    @Test
    fun `el campo no acepta mas de 20 caracteres`() = runTest {
        val vm = vmCargado()
        vm.onNuevoNombreChanged("a".repeat(30))
        assertEquals(20, vm.editarNombre.value.nuevoNombre.length)
    }

    @Test
    fun `el mismo nombre actual no se guarda`() = runTest {
        val vm = vmCargado()
        vm.onNuevoNombreChanged("Ronald1031")
        vm.guardarNombre {}
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.USUARIO_INVALIDO, vm.dialogo.value?.tipo)
        coVerify(exactly = 0) { repo.actualizarNombre(any()) }
    }

    @Test
    fun `nombre ocupado avisa que ya esta en uso`() = runTest {
        coEvery { repo.nombreDisponible("Demo_Dos") } returns AuthResult.Success(false)
        val vm = vmCargado()
        vm.onNuevoNombreChanged("Demo_Dos")
        vm.guardarNombre {}
        advanceUntilIdle()
        assertEquals(PerfilViewModel.MENSAJE_NOMBRE_EN_USO, vm.dialogo.value?.mensaje)
        assertFalse(vm.editarNombre.value.guardando)
        coVerify(exactly = 0) { repo.actualizarNombre(any()) }
    }

    @Test
    fun `nombre valido se guarda y el perfil muestra el nuevo`() = runTest {
        val nuevo = usuario.copy(nombre = "Ronald_456")
        coEvery { repo.actualizarNombre("Ronald_456") } returns AuthResult.Success(nuevo)
        val vm = vmCargado()
        vm.onNuevoNombreChanged("  Ronald_456 ")
        var listo = false
        vm.guardarNombre { listo = true }
        advanceUntilIdle()
        assertTrue(listo)
        assertEquals("Ronald_456", vm.perfil.value.usuario?.nombre)
        assertEquals("", vm.editarNombre.value.nuevoNombre)
        assertNull(vm.dialogo.value)
    }

    @Test
    fun `cambiar solo mayusculas del propio nombre se permite`() = runTest {
        coEvery { repo.actualizarNombre("RONALD1031") } returns AuthResult.Success(usuario.copy(nombre = "RONALD1031"))
        val vm = vmCargado()
        vm.onNuevoNombreChanged("RONALD1031")
        vm.guardarNombre {}
        advanceUntilIdle()
        assertEquals("RONALD1031", vm.perfil.value.usuario?.nombre)
    }

    @Test
    fun `nombre tomado justo al guardar muestra usuario invalido`() = runTest {
        coEvery { repo.actualizarNombre(any()) } returns AuthResult.Error(PerfilViewModel.MENSAJE_NOMBRE_EN_USO)
        val vm = vmCargado()
        vm.onNuevoNombreChanged("Otro_123")
        vm.guardarNombre {}
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.USUARIO_INVALIDO, vm.dialogo.value?.tipo)
    }

    // ============================== Cambiar avatar ==============================

    @Test
    fun `al abrir cambiar avatar queda marcado el actual`() = runTest {
        val vm = vmCargado()
        vm.iniciarCambiarAvatar()
        assertEquals("AVATAR_7", vm.cambiarAvatar.value.seleccionado)
    }

    @Test
    fun `guardar el mismo avatar no llama al servidor`() = runTest {
        val vm = vmCargado()
        vm.iniciarCambiarAvatar()
        var listo = false
        vm.guardarAvatar { listo = true }
        advanceUntilIdle()
        assertTrue(listo)
        coVerify(exactly = 0) { repo.actualizarAvatar(any()) }
    }

    @Test
    fun `nuevo avatar se guarda`() = runTest {
        coEvery { repo.actualizarAvatar("AVATAR_2") } returns AuthResult.Success(usuario.copy(avatar = "AVATAR_2"))
        val vm = vmCargado()
        vm.iniciarCambiarAvatar()
        vm.onAvatarSeleccionado(Avatar.AVATAR_2)
        var listo = false
        vm.guardarAvatar { listo = true }
        advanceUntilIdle()
        assertTrue(listo)
        assertEquals("AVATAR_2", vm.perfil.value.usuario?.avatar)
    }

    @Test
    fun `error al guardar avatar muestra dialogo y no sale`() = runTest {
        coEvery { repo.actualizarAvatar(any()) } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vmCargado()
        vm.onAvatarSeleccionado(Avatar.AVATAR_2)
        var listo = false
        vm.guardarAvatar { listo = true }
        advanceUntilIdle()
        assertFalse(listo)
        assertEquals(TipoDialogoPerfil.ERROR_SERVIDOR, vm.dialogo.value?.tipo)
        assertFalse(vm.cambiarAvatar.value.guardando)
    }

    // ============================== Cambiar contraseña ==============================

    private fun PerfilViewModel.llenarContrasena(actual: String, nueva: String, confirmar: String) {
        iniciarCambiarContrasena()
        onContrasenaActualChanged(actual)
        onContrasenaNuevaChanged(nueva)
        onContrasenaConfirmarChanged(confirmar)
    }

    @Test
    fun `contrasenas invalidas no llaman al servidor`() = runTest {
        val vm = vmCargado()
        // El diálogo dice exactamente qué falta
        val casos = listOf(
            listOf("", "Abcdefg1", "Abcdefg1", "Escribe tu contraseña actual."),
            listOf("Levelia2026", "Abc1", "Abc1", "La contraseña debe tener mínimo 8 caracteres."),
            listOf("Levelia2026", "abcdefg1", "abcdefg1", "La contraseña debe tener una mayúscula."),
            listOf("Levelia2026", "Abcdefg1", "Zzz", "Las contraseñas no coinciden."),
        )
        for ((a, n, c, mensaje) in casos) {
            vm.llenarContrasena(a, n, c)
            vm.guardarContrasena()
            advanceUntilIdle()
            assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, vm.dialogo.value?.tipo)
            assertEquals(mensaje, vm.dialogo.value?.mensaje)
        }
        coVerify(exactly = 0) { repo.cambiarContrasena(any(), any()) }
    }

    @Test
    fun `contrasena correcta se actualiza y limpia el formulario`() = runTest {
        coEvery { repo.cambiarContrasena("Levelia2026", "Abcdefg1") } returns AuthResult.Success(Unit)
        val vm = vmCargado()
        vm.llenarContrasena("Levelia2026", "Abcdefg1", "Abcdefg1")
        vm.guardarContrasena()
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.CONTRASENA_ACTUALIZADA, vm.dialogo.value?.tipo)
        assertEquals("", vm.contrasena.value.actual)
        assertEquals("", vm.contrasena.value.nueva)
        assertFalse(vm.contrasena.value.guardando)
    }

    @Test
    fun `contrasena actual equivocada muestra contrasena invalida`() = runTest {
        coEvery { repo.cambiarContrasena(any(), any()) } returns AuthResult.Error("La contraseña actual no es correcta.")
        val vm = vmCargado()
        vm.llenarContrasena("Mala1234", "Abcdefg1", "Abcdefg1")
        vm.guardarContrasena()
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, vm.dialogo.value?.tipo)
        assertEquals("La contraseña actual no es correcta.", vm.dialogo.value?.mensaje)
    }

    @Test
    fun `contrasena debil rechazada por el servidor muestra contrasena invalida`() = runTest {
        val mensaje = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número."
        coEvery { repo.cambiarContrasena(any(), any()) } returns AuthResult.Error(mensaje)
        val vm = vmCargado()
        vm.llenarContrasena("Levelia2026", "Abcdefg1", "Abcdefg1")
        vm.guardarContrasena()
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, vm.dialogo.value?.tipo)
    }

    // ============================== Eliminar cuenta ==============================

    @Test
    fun `eliminar pide confirmacion y luego borra`() = runTest {
        coEvery { repo.eliminarCuenta() } returns AuthResult.Success(Unit)
        val vm = vmCargado()
        vm.solicitarEliminarCuenta()
        assertEquals(TipoDialogoPerfil.CONFIRMAR_ELIMINAR, vm.dialogo.value?.tipo)
        coVerify(exactly = 0) { repo.eliminarCuenta() }
        vm.confirmarEliminarCuenta()
        advanceUntilIdle()
        assertTrue(vm.cuentaEliminada.value)
        assertFalse(vm.eliminando.value)
    }

    @Test
    fun `error al eliminar muestra dialogo y la cuenta sigue`() = runTest {
        coEvery { repo.eliminarCuenta() } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vmCargado()
        vm.confirmarEliminarCuenta()
        advanceUntilIdle()
        assertFalse(vm.cuentaEliminada.value)
        assertEquals(TipoDialogoPerfil.ERROR_SERVIDOR, vm.dialogo.value?.tipo)
    }

    @Test
    fun `limpiar no deja datos del usuario anterior`() = runTest {
        val vm = vmCargado()
        vm.onNuevoNombreChanged("algo")
        vm.limpiar()
        assertNull(vm.perfil.value.usuario)
        assertEquals("", vm.editarNombre.value.nuevoNombre)
        assertFalse(vm.cuentaEliminada.value)
    }
}
