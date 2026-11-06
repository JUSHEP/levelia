package com.levelia.levelia.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.levelia.levelia.domain.models.AvisoPersonalizacion
import com.levelia.levelia.domain.models.DialogoPerfil
import com.levelia.levelia.domain.models.PersonalizacionUiState
import com.levelia.levelia.domain.models.Skin
import com.levelia.levelia.domain.models.TipoDialogoPerfil
import com.levelia.levelia.domain.models.TipoSkin
import com.levelia.levelia.presentation.screens.components.BarraInferior
import com.levelia.levelia.presentation.screens.components.BotonComprarSkin
import com.levelia.levelia.presentation.screens.components.BotonPerfil
import com.levelia.levelia.presentation.screens.components.BurbujasDecorativas
import com.levelia.levelia.presentation.screens.components.ContadorEsmeraldas
import com.levelia.levelia.presentation.screens.components.DialogoPerfilUi
import com.levelia.levelia.presentation.screens.components.EtiquetaSkinEquipada
import com.levelia.levelia.presentation.screens.components.LeveliaDialog
import com.levelia.levelia.presentation.screens.components.LeveliaScreen
import com.levelia.levelia.presentation.screens.components.MARGEN_PERSONALIZACION
import com.levelia.levelia.presentation.screens.components.PieDePantalla
import com.levelia.levelia.presentation.screens.components.TabInferior
import com.levelia.levelia.presentation.screens.components.TarjetaDetalleSkin
import com.levelia.levelia.presentation.screens.components.TextoAyudaSkin
import com.levelia.levelia.presentation.screens.components.TituloBarra
import com.levelia.levelia.presentation.screens.components.recursoSkin
import com.levelia.levelia.ui.theme.BotonGuardar

/**
 * Detalle de una skin: la tarjeta del diseño (desbloqueada con sus habilidades; del guerrero y bloqueada, con
 * la silueta y "????") y debajo lo que se puede hacer: "Equipar", "Comprar por 💎 N" o por qué todavía no.
 * El diseño no trae ese botón ni los diálogos de compra (textos propios).
 */
@Composable
fun DetallesSkinScreen(
    skin: Skin,
    estado: PersonalizacionUiState,
    onBack: () -> Unit,
    onEquipar: () -> Unit,
    onComprar: () -> Unit,
    onConfirmarCompra: () -> Unit,
    onCerrarAviso: () -> Unit,
    onTab: (TabInferior) -> Unit,
) {
    LeveliaScreen(
        pie = PieDePantalla.NINGUNO,
        onBack = onBack,
        espacioSuperior = 31.dp,
        margenHorizontal = MARGEN_PERSONALIZACION,
        barraSuperior = {
            TituloBarra(skin.nombre)
            ContadorEsmeraldas(estado.esmeraldas)
        },
        decoracion = { BurbujasDecorativas() },
        barraInferior = {
            BarraInferior(
                seleccion = TabInferior.PERSONALIZACION,
                onTab = onTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
        overlay = {
            DialogoPerfilUi(
                dialogo = estado.error?.let { DialogoPerfil(TipoDialogoPerfil.ERROR_SERVIDOR, it) },
                onCerrar = onCerrarAviso,
                onConfirmarEliminar = {},
            )
            when (val aviso = estado.aviso) {
                is AvisoPersonalizacion.ConfirmarCompra -> {
                    val precio = aviso.skin.precio ?: 0
                    LeveliaDialog(
                        titulo = "¿Comprar la skin ${aviso.skin.nombre}?",
                        mensaje = "Cuesta $precio esmeraldas. Te quedarán ${(estado.esmeraldas - precio).coerceAtLeast(0)}.",
                        icono = recursoSkin(aviso.skin.id),
                        onEntendido = onConfirmarCompra,
                        textoBoton = "Comprar",
                        altoIcono = 96.dp,
                        colorBoton = BotonGuardar,
                        textoSecundario = "Cancelar",
                        onSecundario = onCerrarAviso,
                        onDescartar = onCerrarAviso,
                    )
                }
                is AvisoPersonalizacion.Comprada -> LeveliaDialog(
                    titulo = "¡Skin comprada!",
                    mensaje = "La skin ${aviso.skin.nombre} ya es tuya y quedó equipada.",
                    icono = recursoSkin(aviso.skin.id),
                    onEntendido = onCerrarAviso,
                    textoBoton = "Aceptar",
                    altoIcono = 96.dp,
                    colorBoton = BotonGuardar,
                )
                null -> Unit
            }
        },
    ) {
        TarjetaDetalleSkin(skin)
        Spacer(Modifier.height(20.dp))
        AccionSkin(skin = skin, estado = estado, onEquipar = onEquipar, onComprar = onComprar)
    }
}

/** Debajo de la tarjeta: lo que se puede hacer con la skin según si es suya, si es de nivel o de tienda. */
@Composable
private fun AccionSkin(skin: Skin, estado: PersonalizacionUiState, onEquipar: () -> Unit, onComprar: () -> Unit) {
    val faltan = estado.faltan(skin)
    when {
        estado.esEquipada(skin) -> EtiquetaSkinEquipada()
        skin.obtenida -> BotonPerfil(texto = "Equipar", onClick = onEquipar, cargando = estado.enviando)
        skin.tipo == TipoSkin.NIVEL -> TextoAyudaSkin("Se desbloquea al llegar al nivel ${skin.nivelRequerido ?: 1}.")
        faltan > 0 -> TextoAyudaSkin("Cuesta ${skin.precio ?: 0} esmeraldas: te faltan $faltan.")
        else -> BotonComprarSkin(precio = skin.precio ?: 0, onClick = onComprar, cargando = estado.enviando)
    }
}
