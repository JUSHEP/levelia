package com.levelia.levelia.data.repository

import java.io.IOException

internal const val SIN_CONEXION = "No hay conexión a internet. Inténtalo de nuevo."
internal const val ERROR_SERVIDOR = "Algo salió mal con nuestros servidores. Inténtalo más tarde."

/** El error (o alguna de sus causas) es de red: sin internet, DNS o tiempo agotado. */
internal fun esSinConexion(e: Throwable): Boolean {
    val causas = generateSequence(e) { it.cause }.toList()
    val texto = causas.joinToString(" ") { it.message.orEmpty() }.lowercase()
    return causas.any { it is IOException || it::class.simpleName.orEmpty().contains("HttpRequest") } ||
        "unable to resolve host" in texto || "timed out" in texto || "timeout" in texto
}
