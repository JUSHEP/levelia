package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.AuthRepository
import com.levelia.levelia.domain.ReglasPerfil
import com.levelia.levelia.domain.RequisitosContrasena
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.Avatar
import com.levelia.levelia.domain.models.CambiarAvatarState
import com.levelia.levelia.domain.models.CambiarContrasenaState
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.EditarNombreState
import com.levelia.levelia.domain.models.PerfilUiState
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.domain.models.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Módulo Perfil. Igual que AuthViewModel: una sola instancia creada en MainActivity y compartida por
 * todas las pantallas del módulo (Perfil, Editar nombre, Cambiar avatar, Cambiar contraseña).
 *
 * Los datos NO se inventan aquí: [cargarPerfil] los lee de las tablas `perfiles` y `progreso`, y cada
 * cambio se guarda en la base de datos antes de reflejarse en pantalla.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _perfil = MutableStateFlow(PerfilUiState())
    val perfil: StateFlow<PerfilUiState> = _perfil.asStateFlow()

    private val _editarNombre = MutableStateFlow(EditarNombreState())
    val editarNombre: StateFlow<EditarNombreState> = _editarNombre.asStateFlow()

    private val _cambiarAvatar = MutableStateFlow(CambiarAvatarState())
    val cambiarAvatar: StateFlow<CambiarAvatarState> = _cambiarAvatar.asStateFlow()

    private val _contrasena = MutableStateFlow(CambiarContrasenaState())
    val contrasena: StateFlow<CambiarContrasenaState> = _contrasena.asStateFlow()

    /** Diálogo visible (null = ninguno). Solo hay uno a la vez en todo el módulo. */
    private val _dialogo = MutableStateFlow<DialogoPerfil?>(null)
    val dialogo: StateFlow<DialogoPerfil?> = _dialogo.asStateFlow()

    /** true cuando la cuenta ya se borró: el NavGraph abre "Cuenta eliminada". */
    private val _cuentaEliminada = MutableStateFlow(false)
    val cuentaEliminada: StateFlow<Boolean> = _cuentaEliminada.asStateFlow()

    private val _eliminando = MutableStateFlow(false)
    val eliminando: StateFlow<Boolean> = _eliminando.asStateFlow()

    private var cargaJob: Job? = null

    // ============================== PERFIL ==============================

    /**
     * Lee nombre, avatar, nivel y XP de la base de datos. [usuarioInicial] (el que ya conoce la
     * sesión) solo se muestra mientras llega la respuesta.
     */
    fun cargarPerfil(usuarioInicial: UserData? = null) {
        if (_perfil.value.usuario == null && usuarioInicial != null) {
            _perfil.update { it.copy(usuario = usuarioInicial) }
        }
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            _perfil.update { it.copy(cargando = true) }
            try {
                val usuario = repo.obtenerPerfil()
                val progreso = repo.obtenerProgreso()
                _perfil.update { estado ->
                    estado.copy(
                        usuario = if (usuario is AuthResult.Success) usuario.data else estado.usuario,
                        progreso = if (progreso is AuthResult.Success) progreso.data else estado.progreso,
                    )
                }
            } finally {
                _perfil.update { it.copy(cargando = false) }
            }
        }
    }

    // =========================== EDITAR NOMBRE ===========================

    fun iniciarEditarNombre() {
        _editarNombre.value = EditarNombreState()
        _dialogo.value = null
    }

    fun onNuevoNombreChanged(texto: String) {
        _editarNombre.update { it.copy(nuevoNombre = texto.take(ReglasPerfil.NOMBRE_MAX), error = false) }
    }

    /** [onListo] se llama cuando el nombre quedó guardado en la base de datos. */
    fun guardarNombre(onListo: () -> Unit) {
        val estado = _editarNombre.value
        if (estado.guardando) return
        val nuevo = estado.nuevoNombre.trim()
        val actual = _perfil.value.usuario?.nombre.orEmpty()

        if (!ReglasPerfil.nombreValido(nuevo) || nuevo == actual) {
            _editarNombre.update { it.copy(error = true) }
            _dialogo.value = DialogoPerfil(TipoDialogoPerfil.USUARIO_INVALIDO, MENSAJE_NOMBRE_INVALIDO)
            return
        }

        _editarNombre.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                // El nombre de usuario es único (sin distinguir mayúsculas)
                when (val d = repo.nombreDisponible(nuevo)) {
                    is AuthResult.Success -> if (!d.data) {
                        _editarNombre.update { it.copy(error = true) }
                        _dialogo.value = DialogoPerfil(TipoDialogoPerfil.USUARIO_INVALIDO, MENSAJE_NOMBRE_EN_USO)
                        return@launch
                    }
                    is AuthResult.Error -> {
                        _dialogo.value = DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, d.message)
                        return@launch
                    }
                    is AuthResult.Loading -> Unit
                }

                when (val r = repo.actualizarNombre(nuevo)) {
                    is AuthResult.Success -> {
                        // Perfil ya muestra el nombre nuevo (viene de la respuesta de la base de datos)
                        _perfil.update { it.copy(usuario = r.data) }
                        _editarNombre.update { it.copy(nuevoNombre = "", error = false) }
                        onListo()
                    }
                    is AuthResult.Error -> {
                        _editarNombre.update { it.copy(error = true) }
                        _dialogo.value =
                            if (r.message == MENSAJE_NOMBRE_EN_USO) {
                                DialogoPerfil(TipoDialogoPerfil.USUARIO_INVALIDO, r.message)
                            } else {
                                DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, r.message)
                            }
                    }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _editarNombre.update { it.copy(guardando = false) }
            }
        }
    }

    // ============================ CAMBIAR AVATAR ============================

    fun iniciarCambiarAvatar() {
        val actual = _perfil.value.usuario?.avatar.orEmpty()
        _cambiarAvatar.value = CambiarAvatarState(
            seleccionado = if (Avatar.entries.any { it.name == actual }) actual else "",
        )
        _dialogo.value = null
    }

    fun onAvatarSeleccionado(avatar: Avatar) {
        _cambiarAvatar.update { it.copy(seleccionado = avatar.name) }
    }

    /** [onListo] se llama cuando el avatar quedó guardado (o no había nada que cambiar). */
    fun guardarAvatar(onListo: () -> Unit) {
        val estado = _cambiarAvatar.value
        if (estado.guardando) return
        if (estado.seleccionado.isEmpty() || estado.seleccionado == _perfil.value.usuario?.avatar) {
            onListo()
            return
        }

        _cambiarAvatar.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                when (val r = repo.actualizarAvatar(estado.seleccionado)) {
                    is AuthResult.Success -> {
                        _perfil.update { it.copy(usuario = r.data) }
                        onListo()
                    }
                    is AuthResult.Error ->
                        _dialogo.value = DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, r.message)
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _cambiarAvatar.update { it.copy(guardando = false) }
            }
        }
    }

    // ========================== CAMBIAR CONTRASEÑA ==========================

    fun iniciarCambiarContrasena() {
        _contrasena.value = CambiarContrasenaState()
        _dialogo.value = null
    }

    fun onContrasenaActualChanged(texto: String) = _contrasena.update { it.copy(actual = texto) }
    fun onContrasenaNuevaChanged(texto: String) = _contrasena.update { it.copy(nueva = texto) }
    fun onContrasenaConfirmarChanged(texto: String) = _contrasena.update { it.copy(confirmar = texto) }

    /** Los 4 requisitos de la contraseña nueva, para pintar las palomitas / X en vivo. */
    fun requisitos(nueva: String): RequisitosContrasena = ReglasPerfil.requisitosContrasena(nueva)

    fun guardarContrasena() {
        val estado = _contrasena.value
        if (estado.guardando) return

        // El diálogo dice exactamente qué falta
        val motivo = if (estado.actual.isEmpty()) {
            "Escribe tu contraseña actual."
        } else {
            ReglasPerfil.motivoContrasenaInvalida(estado.nueva, estado.confirmar)
        }
        if (motivo != null) {
            _dialogo.value = DialogoPerfil(TipoDialogoPerfil.CONTRASENA_INVALIDA, motivo)
            return
        }

        _contrasena.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                when (val r = repo.cambiarContrasena(estado.actual, estado.nueva)) {
                    is AuthResult.Success -> {
                        _contrasena.value = CambiarContrasenaState()
                        _dialogo.value =
                            DialogoPerfil(TipoDialogoPerfil.CONTRASENA_ACTUALIZADA, MENSAJE_CONTRASENA_ACTUALIZADA)
                    }
                    is AuthResult.Error -> _dialogo.value =
                        if (r.message.startsWith("La contraseña") || r.message.startsWith("La nueva contraseña")) {
                            DialogoPerfil(TipoDialogoPerfil.CONTRASENA_INVALIDA, r.message)
                        } else {
                            DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, r.message)
                        }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _contrasena.update { it.copy(guardando = false) }
            }
        }
    }

    // ============================ ELIMINAR CUENTA ============================

    fun solicitarEliminarCuenta() {
        _dialogo.value = DialogoPerfil(TipoDialogoPerfil.CONFIRMAR_ELIMINAR, MENSAJE_CONFIRMAR_ELIMINAR)
    }

    fun confirmarEliminarCuenta() {
        if (_eliminando.value) return
        _dialogo.value = null
        _eliminando.value = true
        viewModelScope.launch {
            try {
                when (val r = repo.eliminarCuenta()) {
                    is AuthResult.Success -> _cuentaEliminada.value = true
                    is AuthResult.Error ->
                        _dialogo.value = DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, r.message)
                    is AuthResult.Loading -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error al eliminar la cuenta")
                _dialogo.value = DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, MENSAJE_ERROR_SERVIDOR)
            } finally {
                _eliminando.value = false
            }
        }
    }

    // ================================ COMÚN ================================

    fun cerrarDialogo() {
        _dialogo.value = null
    }

    /** Al cerrar sesión o borrar la cuenta: no debe quedar nada del usuario anterior. */
    fun limpiar() {
        cargaJob?.cancel()
        _perfil.value = PerfilUiState()
        _editarNombre.value = EditarNombreState()
        _cambiarAvatar.value = CambiarAvatarState()
        _contrasena.value = CambiarContrasenaState()
        _dialogo.value = null
        _cuentaEliminada.value = false
        _eliminando.value = false
    }

    companion object {
        // Textos del diseño de Figma con la ortografía corregida
        const val MENSAJE_NOMBRE_INVALIDO =
            "Por favor, ingresa un nombre válido: de 3 a 20 caracteres, solo letras, números o guion bajo (sin espacios)."
        // El diseño repetía aquí el texto del diálogo de error; se escribió un mensaje de éxito acorde
        const val MENSAJE_CONTRASENA_ACTUALIZADA = "Tu contraseña se actualizó correctamente."
        const val MENSAJE_CONFIRMAR_ELIMINAR = "Esta acción no se puede revertir. ¿Estás seguro de que deseas eliminar la cuenta?"

        // Mensajes que el diseño no tiene (casos que solo existen con una base de datos real)
        const val MENSAJE_NOMBRE_EN_USO = "Ese nombre de usuario ya está en uso."
        const val MENSAJE_ERROR_SERVIDOR = "Algo salió mal con nuestros servidores. Inténtalo más tarde."
    }
}
