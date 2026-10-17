package com.levelia.levelia.domain.models

/** Progreso del usuario (tabla `progreso`). Solo se lee desde la app; lo escribe el servidor. */
data class ProgresoData(
    val nivel: Int = 1,
    val xp: Int = 0,
    val xpSiguienteNivel: Int = 1000,
) {
    /** 0f..1f para la barra de experiencia. */
    val fraccion: Float
        get() = if (xpSiguienteNivel <= 0) 0f else (xp.toFloat() / xpSiguienteNivel).coerceIn(0f, 1f)
}

/** Lo que muestra la pantalla Perfil: viene de la base de datos (perfiles + progreso). */
data class PerfilUiState(
    val cargando: Boolean = false,
    val usuario: UserData? = null,
    val progreso: ProgresoData = ProgresoData(),
)

data class EditarNombreState(
    val nuevoNombre: String = "",
    /** true = muestra la X roja dentro del campo (nombre vacío o inválido). */
    val error: Boolean = false,
    val guardando: Boolean = false,
)

data class CambiarAvatarState(
    val seleccionado: String = "",
    val guardando: Boolean = false,
)

data class CambiarContrasenaState(
    val actual: String = "",
    val nueva: String = "",
    val confirmar: String = "",
    val guardando: Boolean = false,
)

enum class TipoDialogoPerfil { USUARIO_INVALIDO, CONTRASENA_INVALIDA, CONTRASENA_ACTUALIZADA, CONFIRMAR_ELIMINAR, ERROR_SERVIDOR }

/** Un solo diálogo a la vez en todo el módulo Perfil. */
data class DialogoPerfil(val tipo: TipoDialogoPerfil, val mensaje: String)

/**
 * Recuperar contraseña después de enviar el correo: el código de 6 dígitos (pantalla "Verifica tu
 * email") y la contraseña nueva (pantalla "Restablecer contraseña"). Los diálogos son los de Perfil.
 */
data class RecuperarState(
    val codigo: String = "",
    val intentosFallidos: Int = 0,
    val tiempoReenvio: Int = 0,
    val cargando: Boolean = false,
    val error: String? = null,
    /** true = el código ya se verificó y hay una sesión temporal para cambiar la contraseña. */
    val codigoVerificado: Boolean = false,
    val nueva: String = "",
    val confirmar: String = "",
    val dialogo: DialogoPerfil? = null,
)
