package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.ModoDemoRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.ConfirmacionDemo
import com.levelia.levelia.domain.models.ModoDemoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modo demo. Una sola instancia creada en MainActivity: Inicio la usa para saber si muestra la opción en el menú
 * y la pantalla Modo demo para los botones. Después de cada cambio sube [ModoDemoUiState.cambios] y NavGraph
 * vuelve a leer el nivel, las misiones, los logros y las skins (así salen los avisos de logro y de skin).
 */
@HiltViewModel
class ModoDemoViewModel @Inject constructor(
    private val repo: ModoDemoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(ModoDemoUiState())
    val estado: StateFlow<ModoDemoUiState> = _estado.asStateFlow()

    private var cargaJob: Job? = null

    /** Al iniciar sesión y al abrir la pantalla. Si falla (cuenta normal o SQL sin instalar) no se muestra nada. */
    fun cargar() {
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            when (val r = repo.estado()) {
                is AuthResult.Success -> _estado.update { it.copy(activo = r.data.activo, dias = r.data.dias) }
                is AuthResult.Error -> _estado.update { it.copy(activo = false) }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun avanzarDia() = hacer("Pasó 1 día: ya puedes volver a registrar progreso.") { repo.avanzarDia() }

    fun prepararNivel(nivel: Int) =
        hacer("Listo: cumple y reclama cualquier misión para subir al nivel $nivel.") { repo.prepararNivel(nivel) }

    fun subirNivel() = hacer("Subiste un nivel.") { repo.subirNivel() }

    fun sumarEsmeraldas() = hacer("Sumaste 500 esmeraldas.") { repo.sumarEsmeraldas() }

    /** "Reiniciar cuenta demo" primero pregunta. */
    fun pedirReinicio() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(confirmacion = ConfirmacionDemo.Reiniciar) }
    }

    fun confirmarReinicio() {
        _estado.update { it.copy(confirmacion = null) }
        hacer("La cuenta demo quedó como nueva.") { repo.reiniciar() }
    }

    fun cancelarConfirmacion() = _estado.update { it.copy(confirmacion = null) }

    fun cerrarError() = _estado.update { it.copy(error = null) }

    /** Al cerrar sesión: la siguiente cuenta empieza sin modo demo hasta que el servidor diga lo contrario. */
    fun limpiar() {
        cargaJob?.cancel()
        _estado.value = ModoDemoUiState()
    }

    /** Una acción a la vez; si sale bien, avisa el cambio y relee los días adelantados. */
    private fun hacer(mensaje: String, accion: suspend () -> AuthResult<Unit>) {
        if (_estado.value.enviando || !_estado.value.activo) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                when (val r = accion()) {
                    is AuthResult.Success -> {
                        _estado.update { it.copy(mensaje = mensaje, cambios = it.cambios + 1) }
                        val demo = repo.estado()
                        if (demo is AuthResult.Success) _estado.update { it.copy(dias = demo.data.dias) }
                    }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }
}
