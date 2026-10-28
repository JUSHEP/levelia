package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.LogrosRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.FiltroLogros
import com.levelia.levelia.domain.models.LogrosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Módulo Logros. Igual que los otros módulos: una sola instancia creada en MainActivity y compartida por
 * sus pantallas (Logros con pestañas y Detalles del logro). Los logros se desbloquean en el servidor al
 * registrar progreso o reclamar XP; [cargar] trae el estado real y [LogrosUiState.nuevos] lo que falta avisar.
 */
@HiltViewModel
class LogrosViewModel @Inject constructor(
    private val repo: LogrosRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(LogrosUiState())
    val estado: StateFlow<LogrosUiState> = _estado.asStateFlow()

    private var cargaJob: Job? = null

    /** Al abrir Logros (y después de registrar progreso o reclamar, para saber si hay logros nuevos). */
    fun cargar() {
        // Solo cuenta la última lectura: una respuesta vieja no pisa una más nueva
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            try {
                when (val r = repo.obtenerLogros()) {
                    is AuthResult.Success -> _estado.update { it.copy(logros = r.data, error = null) }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    fun onFiltro(filtro: FiltroLogros) = _estado.update { it.copy(filtro = filtro) }

    /** Busca un logro de la lista (pantalla Detalles del logro). */
    fun logro(id: String) = _estado.value.logros.firstOrNull { it.id == id }

    /**
     * Se cerró el aviso "¡Logro desbloqueado!": se marca en la base y en la lista local de una vez, así el
     * aviso no vuelve a salir aunque la base tarde en responder.
     */
    fun cerrarNuevos() {
        if (_estado.value.nuevos.isEmpty()) return
        _estado.update { e -> e.copy(logros = e.logros.map { if (it.esNuevo) it.copy(visto = true) else it }) }
        viewModelScope.launch {
            val r = repo.marcarVistos()
            if (r is AuthResult.Error) _estado.update { it.copy(error = r.message) }
        }
    }

    fun cerrarError() = _estado.update { it.copy(error = null) }

    /** Al cerrar sesión (como PerfilViewModel.limpiar): no debe quedar nada del usuario anterior. */
    fun limpiar() {
        cargaJob?.cancel()
        _estado.value = LogrosUiState()
    }
}
