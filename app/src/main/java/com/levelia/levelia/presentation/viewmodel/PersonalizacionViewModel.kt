package com.levelia.levelia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelia.levelia.data.repository.PersonalizacionRepository
import com.levelia.levelia.domain.models.AuthResult
import com.levelia.levelia.domain.models.AvisoPersonalizacion
import com.levelia.levelia.domain.models.PersonalizacionUiState
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.domain.models.TipoSkin
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
 * Módulo Personalización. Igual que los otros módulos: una sola instancia creada en MainActivity y compartida
 * por sus pantallas (Personalización, Detalle de la skin) y por Inicio, que dibuja la skin equipada.
 *
 * Las reglas (cuántas esmeraldas da cada cosa, qué skin se desbloquea en qué nivel, si alcanza para comprar)
 * las aplica el servidor; aquí solo se refleja lo que responde y se vuelve a leer después de cada cambio.
 */
@HiltViewModel
class PersonalizacionViewModel @Inject constructor(
    private val repo: PersonalizacionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PersonalizacionUiState())
    val estado: StateFlow<PersonalizacionUiState> = _estado.asStateFlow()

    private var cargaJob: Job? = null

    /** Al abrir Inicio o Personalización, al iniciar sesión y después de reclamar recompensas. */
    fun cargar() {
        // Solo cuenta la última lectura: una respuesta vieja no pisa una más nueva
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            try {
                when (val r = repo.obtener()) {
                    is AuthResult.Success -> _estado.update {
                        it.copy(esmeraldas = r.data.esmeraldas, nivel = r.data.nivel, equipadaId = r.data.equipadaId, skins = r.data.skins)
                    }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    /** Busca una skin del catálogo (pantalla Detalle). */
    fun skin(id: String): Skin? = _estado.value.skins.firstOrNull { it.id == id }

    /** "Equipar": se ve de una vez y el servidor lo guarda. */
    fun equipar(skinId: String) {
        val skin = skin(skinId) ?: return
        if (!skin.obtenida || _estado.value.esEquipada(skin)) return
        enviar(accion = { repo.equipar(skinId) }) { _estado.update { it.copy(equipadaId = skinId) } }
    }

    /** "Comprar": primero pregunta. Solo skins de tienda que no tiene y que le alcanzan. */
    fun pedirCompra(skinId: String) {
        val skin = skin(skinId) ?: return
        val e = _estado.value
        if (skin.tipo != TipoSkin.TIENDA || skin.obtenida || e.enviando) return
        if (e.faltan(skin) > 0) {
            _estado.update { it.copy(error = PersonalizacionRepository.ESMERALDAS_INSUFICIENTES) }
            return
        }
        _estado.update { it.copy(aviso = AvisoPersonalizacion.ConfirmarCompra(skin)) }
    }

    /** "Comprar" del diálogo de confirmación. Al terminar, la skin queda equipada y se avisa "¡Skin comprada!". */
    fun confirmarCompra() {
        val skin = (_estado.value.aviso as? AvisoPersonalizacion.ConfirmarCompra)?.skin ?: return
        _estado.update { it.copy(aviso = null) }
        enviar(accion = { repo.comprar(skin.id) }) {
            _estado.update {
                it.copy(
                    aviso = AvisoPersonalizacion.Comprada(skin),
                    equipadaId = skin.id,
                    esmeraldas = (it.esmeraldas - (skin.precio ?: 0)).coerceAtLeast(0),
                )
            }
        }
    }

    /** Cierra el diálogo de compra o el de error. */
    fun cerrarAviso() = _estado.update { it.copy(aviso = null, error = null) }

    /**
     * Se cerró "¡Skin desbloqueada!": se marca en la base y en la lista local de una vez, así el aviso no
     * vuelve a salir aunque la base tarde en responder.
     */
    fun cerrarNuevas() {
        if (_estado.value.nuevas.isEmpty()) return
        _estado.update { e -> e.copy(skins = e.skins.map { if (it.esNueva) it.copy(visto = true) else it }) }
        viewModelScope.launch {
            val r = repo.marcarVistas()
            if (r is AuthResult.Error) Timber.w("No se pudieron marcar las skins como vistas: %s", r.message)
        }
    }

    /** Al cerrar sesión (como los demás módulos): no debe quedar nada del usuario anterior. */
    fun limpiar() {
        cargaJob?.cancel()
        _estado.value = PersonalizacionUiState()
    }

    /** Ejecuta una acción del servidor; si sale bien llama a [alTerminar] y vuelve a leer todo. */
    private fun enviar(accion: suspend () -> AuthResult<Unit>, alTerminar: () -> Unit) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                when (val r = accion()) {
                    is AuthResult.Success -> {
                        alTerminar()
                        cargar()
                    }
                    is AuthResult.Error -> _estado.update { it.copy(error = r.message) }
                    is AuthResult.Loading -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error en personalización")
                _estado.update { it.copy(error = "Ocurrió un error inesperado. Inténtalo de nuevo.") }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }
}
