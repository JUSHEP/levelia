package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.MisionesRepository
import com.levelia.levelia.data.repository.MisionesRepository.Companion.DIFICULTAD_VACIA
import com.levelia.levelia.data.repository.MisionesRepository.Companion.FECHA_PASADA
import com.levelia.levelia.data.repository.MisionesRepository.Companion.FECHA_REQUERIDA
import com.levelia.levelia.data.repository.MisionesRepository.Companion.TITULO_LARGO
import com.levelia.levelia.data.repository.MisionesRepository.Companion.TITULO_MAX
import com.levelia.levelia.data.repository.MisionesRepository.Companion.TITULO_VACIO
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.CrearMisionState
import com.levelia.levelia.domain.models.Dificultad
import com.levelia.levelia.domain.models.EstadoMision
import com.levelia.levelia.domain.models.FiltroMisiones
import com.levelia.levelia.domain.models.MisionAceptada
import com.levelia.levelia.domain.models.MisionesUiState
import com.levelia.levelia.domain.models.hoyLevelia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * Módulo Misiones. Igual que PerfilViewModel: una sola instancia creada en MainActivity y compartida
 * por sus pantallas (Misiones, Detalle, Ajustar dificultad, Crea tu propia misión).
 *
 * Las reglas (1 registro por día, plazo de 7 días, fecha de la UNICA, XP y niveles) las aplica el
 * servidor; aquí solo se valida el formulario y se refleja lo que responde. Después de cada cambio se
 * vuelve a leer la lista para mostrar el progreso real.
 */
@HiltViewModel
class MisionesViewModel @Inject constructor(
    private val repo: MisionesRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(MisionesUiState())
    val estado: StateFlow<MisionesUiState> = _estado.asStateFlow()

    private val _crear = MutableStateFlow(CrearMisionState())
    val crear: StateFlow<CrearMisionState> = _crear.asStateFlow()

    // ============================== LISTA ==============================

    private var cargaJob: Job? = null

    fun cargar() {
        // Solo cuenta la última lectura: una respuesta vieja no pisa una más nueva
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            try {
                when (val r = repo.obtenerMisiones()) {
                    // No borra el aviso: tras una acción (p. ej. "plazo vencido") se recarga y el aviso debe seguir
                    is AuthResult.Success -> _estado.update { it.copy(misiones = r.data, hoy = hoyLevelia()) }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    fun onFiltro(filtro: FiltroMisiones) = _estado.update { it.copy(filtro = filtro) }

    /** Cierra el aviso de error o de recompensa. */
    fun cerrarAviso() = _estado.update { it.copy(error = null, recompensa = null) }

    /** Al cerrar sesión (como PerfilViewModel.limpiar): no debe quedar nada del usuario anterior. */
    fun limpiar() {
        cargaJob?.cancel()
        _estado.value = MisionesUiState()
        _crear.value = CrearMisionState()
    }

    // ============================== ACCIONES ==============================

    /**
     * "Aceptar misión" (Detalle). [dificultad] null = la de la misión; si no, la elegida en "Ajustar
     * dificultad". [onAceptada] se llama en el hilo principal (para pasar al detalle con "Registrar progreso").
     */
    /**
     * "Guardar cambios" de Ajustes de misión: guarda la dificultad en la base sin aceptar la misión.
     * [onGuardada]: volver a la lista, que se vuelve a leer con las veces y XP nuevas.
     */
    fun ajustarDificultad(misionId: Long, dificultad: Dificultad, onGuardada: () -> Unit = {}) =
        enviar(accion = { repo.ajustarDificultad(misionId, dificultad) }, alTerminar = { onGuardada() })

    fun aceptar(misionId: Long, dificultad: Dificultad? = null, onAceptada: () -> Unit = {}) =
        enviar(
            accion = { repo.aceptarMision(misionId, dificultad) },
            alTerminar = { reflejar(it); onAceptada() },
        )

    /**
     * "Registrar progreso +". [onRegistrado]: volver a la lista, que ya trae el progreso nuevo. Si el
     * plazo ya había pasado, el servidor la deja PERDIDA: se avisa y no se llama a [onRegistrado].
     */
    fun registrarProgreso(aceptadaId: Long, onRegistrado: () -> Unit = {}) =
        enviar(
            accion = { repo.registrarProgreso(aceptadaId) },
            alTerminar = {
                reflejar(it)
                if (it.estado == EstadoMision.PERDIDA) {
                    _estado.update { e -> e.copy(error = PLAZO_VENCIDO) }
                } else {
                    onRegistrado()
                }
            },
        )

    /** "¡Reclamar recompensa!": la XP ganada y el nivel nuevo quedan en [MisionesUiState.recompensa]. */
    fun reclamar() {
        if (!_estado.value.hayRecompensas) return
        enviar(accion = { repo.reclamarRecompensas() }, alTerminar = { r -> _estado.update { it.copy(recompensa = r) } })
    }

    // ========================= CREA TU PROPIA MISIÓN =========================

    /** Al abrir el formulario: vacío y sin errores. */
    fun iniciarCrear() {
        _crear.value = CrearMisionState()
    }

    // Al editar un campo se le quita su error (deja de verse en rojo)
    fun onTitulo(texto: String) = _crear.update { it.copy(titulo = texto, errorTitulo = null, error = null) }

    fun onDescripcion(texto: String) = _crear.update { it.copy(descripcion = texto, error = null) }

    fun onDificultad(dificultad: Dificultad) =
        _crear.update { it.copy(dificultad = dificultad, errorDificultad = null, errorFecha = null, error = null) }

    fun onFechaLimite(fecha: LocalDate?) = _crear.update { it.copy(fechaLimite = fecha, errorFecha = null, error = null) }

    /** "CREAR". [onCreada] se llama en el hilo principal (para volver a la lista). */
    fun crearMision(onCreada: () -> Unit = {}) {
        val f = _crear.value
        if (f.guardando) return
        val errores = f.copy(
            errorTitulo = when {
                f.titulo.isBlank() -> TITULO_VACIO
                f.titulo.trim().length > TITULO_MAX -> TITULO_LARGO
                else -> null
            },
            errorDificultad = if (f.dificultad == null) DIFICULTAD_VACIA else null,
            errorFecha = when {
                f.dificultad != Dificultad.UNICA -> null
                f.fechaLimite == null -> FECHA_REQUERIDA
                f.fechaLimite.isBefore(hoyLevelia()) -> FECHA_PASADA
                else -> null
            },
        )
        if (errores.errorTitulo != null || errores.errorDificultad != null || errores.errorFecha != null) {
            _crear.value = errores
            return
        }

        _crear.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                when (val r = repo.crearMision(f.titulo, f.descripcion, f.dificultad!!, f.fechaLimite)) {
                    is AuthResult.Success -> {
                        _crear.value = CrearMisionState()
                        cargar()
                        onCreada()
                    }
                    is AuthResult.Error -> _crear.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _crear.update { it.copy(guardando = false) }
            }
        }
    }

    // ============================== INTERNOS ==============================

    private companion object {
        const val PLAZO_VENCIDO = "Se venció el plazo de esta misión."
    }

    /** La respuesta del servidor se ve en la lista de una vez, sin esperar a que se vuelva a leer. */
    private fun reflejar(aceptada: MisionAceptada) = _estado.update { e ->
        e.copy(misiones = e.misiones.map { if (it.mision.id == aceptada.misionId) it.copy(aceptada = aceptada) else it })
    }

    /** Ejecuta una acción del servidor; si sale bien llama a [alTerminar] con la respuesta y vuelve a leer la lista. */
    private fun <T> enviar(accion: suspend () -> AuthResult<T>, alTerminar: (T) -> Unit) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                when (val r = accion()) {
                    is AuthResult.Success -> {
                        alTerminar(r.data)
                        cargar()
                    }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error en misiones")
                _estado.update { it.copy(error = "Ocurrió un error inesperado. Inténtalo de nuevo.") }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }
}
