package com.levelia.levelia.presentation

import com.levelia.levelia.data.repository.AuthRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.DialogoAuth
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.domain.models.UserData
import com.levelia.levelia.presentation.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = mockk<AuthRepository>(relaxUnitFun = true)
    private val usuario = UserData(id = "u1", nombre = "Ronald1031", email = "rontorres304@gmail.com", edad = 26, avatar = "AVATAR_1")

    @Before
    fun preparar() {
        coEvery { repo.restaurarSesion() } returns AuthResult.Success(null)
        every { repo.getRememberedEmail() } returns null
        coEvery { repo.nombreDisponible(any()) } returns AuthResult.Success(true)
        coEvery { repo.signUp(any(), any()) } returns AuthResult.Success(Unit)
        coEvery { repo.verifyCode(any(), any()) } returns AuthResult.Success(Unit)
        coEvery { repo.createUserProfile(any(), any(), any(), any()) } returns AuthResult.Success(usuario)
    }

    private fun vm() = AuthViewModel(repo)

    private fun AuthViewModel.llenarPaso1(
        nombre: String = "Ronald1031",
        edad: Int = 26,
        email: String = "rontorres304@gmail.com",
        contrasena: String = "Levelia2026",
    ) {
        onNombreChanged(nombre)
        onEdadChanged(edad)
        onEmailChanged(email)
        onContraseñaChanged(contrasena)
    }

    // ============================== Sesión al abrir ==============================

    @Test
    fun `sesion guardada con perfil entra directo`() = runTest {
        coEvery { repo.restaurarSesion() } returns AuthResult.Success(usuario)
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.isAuthenticated.value)
        assertEquals(usuario, vm.user.value)
        assertFalse(vm.restaurandoSesion.value)
    }

    @Test
    fun `sin sesion guardada se queda en login y recuerda el correo`() = runTest {
        every { repo.getRememberedEmail() } returns "rontorres304@gmail.com"
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.isAuthenticated.value)
        assertEquals("rontorres304@gmail.com", vm.loginEmail.value)
        assertTrue(vm.rememberMe.value)
    }

    // ============================== Registro paso 1 ==============================

    @Test
    fun `formulario vacio muestra el dialogo de validacion y no llama al servidor`() = runTest {
        val vm = vm()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        val estado = vm.registerState.value
        assertEquals(DialogoAuth.VALIDACION, estado.dialogo)
        assertNotNull(estado.errores.nombre)
        assertNotNull(estado.errores.edad)
        assertNotNull(estado.errores.email)
        assertNotNull(estado.errores.contraseña)
        coVerify(exactly = 0) { repo.signUp(any(), any()) }
    }

    @Test
    fun `correo invalido marca el campo`() = runTest {
        val vm = vm()
        vm.llenarPaso1(email = "correo-malo")
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals("Correo incorrecto.", vm.registerState.value.errores.email)
        assertEquals(DialogoAuth.CORREO_INVALIDO, vm.registerState.value.dialogo)
    }

    @Test
    fun `edad fuera de rango marca el campo`() = runTest {
        val vm = vm()
        vm.llenarPaso1(edad = 10)
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertNotNull(vm.registerState.value.errores.edad)
        coVerify(exactly = 0) { repo.signUp(any(), any()) }
    }

    @Test
    fun `contrasena sin mayuscula ni numero se rechaza antes de crear la cuenta`() = runTest {
        val vm = vm()
        vm.llenarPaso1(contrasena = "abcdefgh")
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertNotNull(vm.registerState.value.errores.contraseña)
        assertEquals(DialogoAuth.CONTRASENA_DEBIL, vm.registerState.value.dialogo)
        coVerify(exactly = 0) { repo.signUp(any(), any()) }
    }

    @Test
    fun `nombre con tilde se rechaza antes de crear la cuenta`() = runTest {
        val vm = vm()
        vm.llenarPaso1(nombre = "Peña")
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertNotNull(vm.registerState.value.errores.nombre)
        coVerify(exactly = 0) { repo.signUp(any(), any()) }
    }

    @Test
    fun `nombre ocupado no crea la cuenta`() = runTest {
        coEvery { repo.nombreDisponible("Ronald1031") } returns AuthResult.Success(false)
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals("Prueba con otro nombre.", vm.registerState.value.errores.nombre)
        assertEquals(1, vm.registerState.value.pasoActual)
        coVerify(exactly = 0) { repo.signUp(any(), any()) }
    }

    @Test
    fun `registro correcto pasa al paso 2 y arranca la cuenta regresiva`() = runTest {
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        main.dispatcher.scheduler.advanceTimeBy(1_500)
        assertEquals(2, vm.registerState.value.pasoActual)
        assertTrue(vm.registerState.value.tiempoReenvio in 1..60)
        assertFalse(vm.registerState.value.isLoading)
        coVerify(exactly = 0) { repo.createUserProfile(any(), any(), any(), any()) }
    }

    @Test
    fun `correo con registro a medias se retoma y guarda el perfil`() = runTest {
        coEvery { repo.signUp(any(), any()) } returns AuthResult.Error("Ya existe una cuenta con ese email. Inicia sesión.")
        coEvery { repo.retomarRegistro(any(), any()) } returns AuthResult.Success(Unit)
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals(3, vm.registerState.value.pasoActual)
        assertTrue(vm.registerState.value.emailVerificado)
        coVerify { repo.createUserProfile("rontorres304@gmail.com", "Ronald1031", 26, "AVATAR_1") }
    }

    @Test
    fun `correo de una cuenta completa marca el campo correo`() = runTest {
        val mensaje = "Ya existe una cuenta con ese email. Inicia sesión."
        coEvery { repo.signUp(any(), any()) } returns AuthResult.Error(mensaje)
        coEvery { repo.retomarRegistro(any(), any()) } returns AuthResult.Error(mensaje)
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals(mensaje, vm.registerState.value.errores.email)
        assertEquals(1, vm.registerState.value.pasoActual)
        coVerify(exactly = 0) { repo.createUserProfile(any(), any(), any(), any()) }
    }

    @Test
    fun `error del servidor al registrar muestra el dialogo de servidor`() = runTest {
        coEvery { repo.signUp(any(), any()) } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals(DialogoAuth.ERROR_SERVIDOR, vm.registerState.value.dialogo)
        assertEquals(1, vm.registerState.value.pasoActual)
    }

    // ============================== Registro paso 2 ==============================

    private suspend fun kotlinx.coroutines.test.TestScope.vmEnPaso2(): AuthViewModel {
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `pegar el codigo completo llena las 6 casillas`() = runTest {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "123456")
        assertEquals("123456", vm.registerState.value.codigoVerificacion)
    }

    @Test
    fun `escribir y borrar casillas sueltas`() = runTest {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "1")
        vm.onCodigoDigitChanged(2, "3")
        assertEquals("1 3", vm.registerState.value.codigoVerificacion)
        vm.onCodigoDigitChanged(2, "")
        assertEquals("1", vm.registerState.value.codigoVerificacion)
        vm.onCodigoDigitChanged(0, "a")
        assertEquals("", vm.registerState.value.codigoVerificacion)
    }

    @Test
    fun `codigo incompleto no se envia`() = runTest {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "12345")
        vm.verificarCodigo()
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.verifyCode(any(), any()) }
    }

    @Test
    fun `codigo correcto guarda el perfil en la base y pasa al paso 3`() = runTest {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "123456")
        vm.verificarCodigo()
        advanceUntilIdle()
        assertEquals(3, vm.registerState.value.pasoActual)
        assertTrue(vm.registerState.value.emailVerificado)
        assertEquals(usuario, vm.user.value)
        coVerify { repo.createUserProfile("rontorres304@gmail.com", "Ronald1031", 26, "AVATAR_1") }
    }

    @Test
    fun `si el perfil falla tras verificar se queda en el paso 2 con el correo verificado`() = runTest {
        coEvery { repo.createUserProfile(any(), any(), any(), any()) } returns AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "123456")
        vm.verificarCodigo()
        advanceUntilIdle()
        assertEquals(2, vm.registerState.value.pasoActual)
        assertTrue(vm.registerState.value.emailVerificado)
        assertNotNull(vm.registerState.value.error)
        vm.continuarConEmailVerificado()
        assertEquals(3, vm.registerState.value.pasoActual)
    }

    @Test
    fun `tres codigos incorrectos bloquean hasta reenviar`() = runTest {
        coEvery { repo.verifyCode(any(), any()) } returns AuthResult.Error("Código incorrecto o vencido.")
        coEvery { repo.resendCode(any()) } returns AuthResult.Success(Unit)
        val vm = vmEnPaso2()
        repeat(3) {
            vm.onCodigoDigitChanged(0, "000000")
            vm.verificarCodigo()
            advanceUntilIdle()
        }
        assertEquals("Demasiados intentos. Solicita un nuevo código.", vm.registerState.value.error)
        vm.onCodigoDigitChanged(0, "000000")
        vm.verificarCodigo()
        advanceUntilIdle()
        coVerify(exactly = 3) { repo.verifyCode(any(), any()) }

        vm.onReenviarCodigo()
        advanceUntilIdle()
        assertEquals(0, vm.registerState.value.intentosFallidos)
        coVerify(exactly = 1) { repo.resendCode("rontorres304@gmail.com") }
    }

    @Test
    fun `no se puede reenviar mientras corre la cuenta regresiva`() = runTest {
        coEvery { repo.resendCode(any()) } returns AuthResult.Success(Unit)
        val vm = vm()
        vm.llenarPaso1()
        vm.enviarFormularioRegistro()
        main.dispatcher.scheduler.advanceTimeBy(5_000)
        vm.onReenviarCodigo()
        main.dispatcher.scheduler.advanceTimeBy(1_000)
        coVerify(exactly = 0) { repo.resendCode(any()) }
    }

    @Test
    fun `volver del paso 2 al 1 limpia el codigo`() = runTest {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "12")
        vm.volverPaso()
        assertEquals(1, vm.registerState.value.pasoActual)
        assertEquals("", vm.registerState.value.codigoVerificacion)
    }

    // ============================== Registro pasos 3 y 4 ==============================

    private suspend fun kotlinx.coroutines.test.TestScope.vmEnPaso3(): AuthViewModel {
        val vm = vmEnPaso2()
        vm.onCodigoDigitChanged(0, "123456")
        vm.verificarCodigo()
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `volver al paso 1 con el correo verificado corrige el perfil sin crear otra cuenta`() = runTest {
        val vm = vmEnPaso3()
        vm.volverPaso() // 3 -> 2
        vm.volverPaso() // 2 -> 1
        vm.onNombreChanged("Ronald_2")
        vm.enviarFormularioRegistro()
        advanceUntilIdle()
        assertEquals(3, vm.registerState.value.pasoActual)
        coVerify(exactly = 1) { repo.signUp(any(), any()) }
        coVerify { repo.createUserProfile(any(), "Ronald_2", any(), any()) }
    }

    @Test
    fun `avatar desconocido se ignora`() = runTest {
        val vm = vmEnPaso3()
        vm.onAvatarSelected("AVATAR_99")
        assertEquals("", vm.registerState.value.avatarSeleccionado)
    }

    @Test
    fun `elegir avatar y terminar lleva al paso 4 y luego a la app`() = runTest {
        val conAvatar = usuario.copy(avatar = "AVATAR_5")
        val vm = vmEnPaso3()
        coEvery { repo.createUserProfile(any(), any(), any(), "AVATAR_5") } returns AuthResult.Success(conAvatar)
        vm.onAvatarSelected("AVATAR_5")
        vm.completarRegistro()
        advanceUntilIdle()
        assertEquals(4, vm.registerState.value.pasoActual)
        assertEquals(conAvatar, vm.user.value)
        assertFalse(vm.isAuthenticated.value)
        vm.empezar()
        assertTrue(vm.isAuthenticated.value)
    }

    @Test
    fun `terminar sin avatar no hace nada`() = runTest {
        val vm = vmEnPaso3()
        vm.completarRegistro()
        advanceUntilIdle()
        assertEquals(3, vm.registerState.value.pasoActual)
    }

    // ============================== Login ==============================

    @Test
    fun `login vacio marca ambos campos sin llamar al servidor`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.login()
        advanceUntilIdle()
        assertEquals("Este campo es obligatorio.", vm.erroresLogin.value.email)
        assertEquals("Este campo es obligatorio.", vm.erroresLogin.value.contraseña)
        coVerify(exactly = 0) { repo.login(any(), any()) }
    }

    @Test
    fun `login correcto entra y recuerda el correo si se pidio`() = runTest {
        coEvery { repo.login(any(), any()) } returns AuthResult.Success(usuario)
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginEmailChanged(" rontorres304@gmail.com ")
        vm.onLoginPasswordChanged("Levelia2026")
        vm.onRememberMeChanged(true)
        vm.login()
        advanceUntilIdle()
        assertTrue(vm.isAuthenticated.value)
        assertEquals("", vm.loginPassword.value)
        verify { repo.saveRememberedEmail("rontorres304@gmail.com") }
    }

    @Test
    fun `credenciales malas marcan ambos campos`() = runTest {
        coEvery { repo.login(any(), any()) } returns AuthResult.Error("Email o contraseña incorrectos.")
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginEmailChanged("rontorres304@gmail.com")
        vm.onLoginPasswordChanged("Mala1234")
        vm.login()
        advanceUntilIdle()
        assertFalse(vm.isAuthenticated.value)
        assertEquals("Correo incorrecto.", vm.erroresLogin.value.email)
        assertEquals("Contraseña incorrecta.", vm.erroresLogin.value.contraseña)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `cuenta sin perfil no entra y explica como terminar`() = runTest {
        val mensaje = "Tu registro no se terminó. Regístrate de nuevo con este correo y la misma contraseña."
        coEvery { repo.login(any(), any()) } returns AuthResult.Error(mensaje)
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginEmailChanged("rontorres304@gmail.com")
        vm.onLoginPasswordChanged("Levelia2026")
        vm.login()
        advanceUntilIdle()
        assertFalse(vm.isAuthenticated.value)
        assertEquals(mensaje, vm.erroresLogin.value.email)
        assertNull(vm.erroresLogin.value.contraseña)
    }

    @Test
    fun `cerrar sesion borra el usuario y el registro`() = runTest {
        coEvery { repo.restaurarSesion() } returns AuthResult.Success(usuario)
        val vm = vm()
        advanceUntilIdle()
        vm.logout()
        advanceUntilIdle()
        assertFalse(vm.isAuthenticated.value)
        assertNull(vm.user.value)
        assertEquals(1, vm.registerState.value.pasoActual)
        coVerify { repo.logout() }
    }

    // ============================== Recuperar contraseña ==============================

    @Test
    fun `recuperar con correo invalido no llama al servidor`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        var enviado = false
        vm.onRecuperarEmailChanged("correo-malo")
        vm.enviarRecuperacion { enviado = true }
        advanceUntilIdle()
        assertEquals("Correo incorrecto.", vm.recuperarError.value)
        assertFalse(enviado)
        coVerify(exactly = 0) { repo.solicitarRecuperacion(any()) }
    }

    @Test
    fun `recuperar con correo valido avisa que se envio`() = runTest {
        coEvery { repo.solicitarRecuperacion(any()) } returns AuthResult.Success(Unit)
        val vm = vm()
        advanceUntilIdle()
        var enviado = false
        vm.onRecuperarEmailChanged("rontorres304@gmail.com")
        vm.enviarRecuperacion { enviado = true }
        advanceUntilIdle()
        assertTrue(enviado)
        assertFalse(vm.recuperarLoading.value)
    }

    // ============================ Recuperar: código y contraseña nueva ============================

    /** Envía el correo y escribe el código, como en la pantalla. */
    private fun AuthViewModel.enviarYEscribirCodigo(codigo: String = "123456") {
        coEvery { repo.solicitarRecuperacion(any()) } returns AuthResult.Success(Unit)
        onRecuperarEmailChanged("rontorres304@gmail.com")
        enviarRecuperacion {}
        onRecuperarCodigoChanged(0, codigo)
    }

    /** VM con el código ya verificado (pantalla "Restablecer contraseña"). */
    private fun TestScope.vmConCodigoVerificado(): AuthViewModel {
        coEvery { repo.verificarCodigoRecuperacion(any(), any()) } returns AuthResult.Success(Unit)
        val vm = vm()
        advanceUntilIdle()
        vm.enviarYEscribirCodigo()
        vm.verificarCodigoRecuperacion {}
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `al enviar el correo de recuperacion arranca la espera para reenviar`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        coEvery { repo.solicitarRecuperacion(any()) } returns AuthResult.Success(Unit)
        vm.onRecuperarEmailChanged("rontorres304@gmail.com")
        vm.enviarRecuperacion {}
        main.dispatcher.scheduler.advanceTimeBy(1_500)
        assertTrue(vm.recuperacion.value.tiempoReenvio in 1..60)
    }

    @Test
    fun `codigo de recuperacion correcto lleva a restablecer`() = runTest {
        coEvery { repo.verificarCodigoRecuperacion(any(), any()) } returns AuthResult.Success(Unit)
        val vm = vm()
        advanceUntilIdle()
        vm.enviarYEscribirCodigo()
        var verificado = false
        vm.verificarCodigoRecuperacion { verificado = true }
        advanceUntilIdle()
        assertTrue(verificado)
        assertTrue(vm.recuperacion.value.codigoVerificado)
        coVerify { repo.verificarCodigoRecuperacion("rontorres304@gmail.com", "123456") }
    }

    @Test
    fun `codigo de recuperacion incompleto no llama al servidor`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.enviarYEscribirCodigo("123")
        vm.verificarCodigoRecuperacion {}
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.verificarCodigoRecuperacion(any(), any()) }
    }

    @Test
    fun `codigo de recuperacion incorrecto descuenta intentos y bloquea al tercero`() = runTest {
        coEvery { repo.verificarCodigoRecuperacion(any(), any()) } returns AuthResult.Error("Código incorrecto o vencido.")
        val vm = vm()
        advanceUntilIdle()
        vm.enviarYEscribirCodigo()
        var verificado = false
        vm.verificarCodigoRecuperacion { verificado = true }
        advanceUntilIdle()
        assertEquals("Código incorrecto o vencido. Te quedan 2 intentos.", vm.recuperacion.value.error)
        assertEquals("", vm.recuperacion.value.codigo)

        repeat(2) {
            vm.onRecuperarCodigoChanged(0, "123456")
            vm.verificarCodigoRecuperacion { verificado = true }
            advanceUntilIdle()
        }
        assertFalse(verificado)
        assertEquals("Demasiados intentos. Solicita un nuevo código.", vm.recuperacion.value.error)
        assertEquals(0, vm.recuperacion.value.tiempoReenvio)

        // Sin intentos ya no se llama al servidor
        vm.onRecuperarCodigoChanged(0, "123456")
        vm.verificarCodigoRecuperacion { verificado = true }
        advanceUntilIdle()
        coVerify(exactly = 3) { repo.verificarCodigoRecuperacion(any(), any()) }
    }

    @Test
    fun `reenviar el codigo de recuperacion reinicia los intentos`() = runTest {
        coEvery { repo.verificarCodigoRecuperacion(any(), any()) } returns AuthResult.Error("Código incorrecto o vencido.")
        val vm = vm()
        advanceUntilIdle()
        vm.enviarYEscribirCodigo()
        vm.verificarCodigoRecuperacion {}
        advanceUntilIdle() // también termina la espera de 60 s
        vm.reenviarCodigoRecuperacion()
        advanceUntilIdle()
        assertEquals(0, vm.recuperacion.value.intentosFallidos)
        coVerify(exactly = 2) { repo.solicitarRecuperacion("rontorres304@gmail.com") }
    }

    @Test
    fun `restablecer con contrasena debil o distinta muestra contrasena invalida`() = runTest {
        val vm = vmConCodigoVerificado()
        vm.onRecuperarNuevaChanged("debil")
        vm.onRecuperarConfirmarChanged("debil")
        vm.restablecerContrasena()
        assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, vm.recuperacion.value.dialogo?.tipo)
        assertEquals("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.", vm.recuperacion.value.dialogo?.mensaje)

        vm.cerrarDialogoRecuperacion()
        vm.onRecuperarNuevaChanged("Levelia2026")
        vm.onRecuperarConfirmarChanged("Levelia2027")
        vm.restablecerContrasena()
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, vm.recuperacion.value.dialogo?.tipo)
        assertEquals("Las contraseñas no coinciden.", vm.recuperacion.value.dialogo?.mensaje)
        coVerify(exactly = 0) { repo.restablecerContrasena(any()) }
    }

    @Test
    fun `restablecer sin codigo verificado no llama al servidor`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onRecuperarNuevaChanged("Levelia2026")
        vm.onRecuperarConfirmarChanged("Levelia2026")
        vm.restablecerContrasena()
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.restablecerContrasena(any()) }
    }

    @Test
    fun `restablecer valido guarda y avisa contrasena actualizada`() = runTest {
        coEvery { repo.restablecerContrasena(any()) } returns AuthResult.Success(Unit)
        val vm = vmConCodigoVerificado()
        vm.onRecuperarNuevaChanged("Levelia2026")
        vm.onRecuperarConfirmarChanged("Levelia2026")
        vm.restablecerContrasena()
        advanceUntilIdle()
        coVerify { repo.restablecerContrasena("Levelia2026") }
        val estado = vm.recuperacion.value
        assertEquals(TipoDialogoPerfil.CONTRASENA_ACTUALIZADA, estado.dialogo?.tipo)
        // Se borra la contraseña escrita y no queda sesión abierta en la app
        assertEquals("", estado.nueva)
        assertFalse(estado.codigoVerificado)
        assertFalse(vm.isAuthenticated.value)
    }

    @Test
    fun `restablecer con la misma contrasena de antes muestra contrasena invalida`() = runTest {
        coEvery { repo.restablecerContrasena(any()) } returns
            AuthResult.Error("La nueva contraseña debe ser diferente a la actual.")
        val vm = vmConCodigoVerificado()
        vm.onRecuperarNuevaChanged("Levelia2026")
        vm.onRecuperarConfirmarChanged("Levelia2026")
        vm.restablecerContrasena()
        advanceUntilIdle()
        val dialogo = vm.recuperacion.value.dialogo
        assertEquals(TipoDialogoPerfil.CONTRASENA_INVALIDA, dialogo?.tipo)
        assertEquals("La nueva contraseña debe ser diferente a la actual.", dialogo?.mensaje)
        // El código sigue verificado: puede intentar con otra contraseña
        assertTrue(vm.recuperacion.value.codigoVerificado)
    }

    @Test
    fun `restablecer sin internet muestra error del servidor`() = runTest {
        coEvery { repo.restablecerContrasena(any()) } returns
            AuthResult.Error("No hay conexión a internet. Inténtalo de nuevo.")
        val vm = vmConCodigoVerificado()
        vm.onRecuperarNuevaChanged("Levelia2026")
        vm.onRecuperarConfirmarChanged("Levelia2026")
        vm.restablecerContrasena()
        advanceUntilIdle()
        assertEquals(TipoDialogoPerfil.ERROR_SERVIDOR, vm.recuperacion.value.dialogo?.tipo)
    }

    @Test
    fun `cancelar despues del codigo cierra la sesion temporal`() = runTest {
        val vm = vmConCodigoVerificado()
        vm.cancelarRecuperacion()
        advanceUntilIdle()
        coVerify { repo.logout() }
        assertFalse(vm.recuperacion.value.codigoVerificado)
    }
}
