package com.levelia.levelia.domain

/**
 * Reglas de nombre de usuario y contraseña. Son las MISMAS que están como CHECK en
 * supabase/setup.sql: si se cambia una, hay que cambiar la otra.
 */
object ReglasPerfil {
    const val NOMBRE_MIN = 3
    const val NOMBRE_MAX = 20
    const val CONTRASENA_MIN = 8

    // Letras sin tildes ni ñ, números y guion bajo: igual que el CHECK perfiles_nombre_formato de la base
    private val NOMBRE_REGEX = Regex("^[A-Za-z0-9_]+$")

    // Mismo patrón que android.util.Patterns.EMAIL_ADDRESS, pero en Kotlin puro (se puede probar sin Android)
    private val CORREO_REGEX = Regex(
        """[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+"""
    )

    fun correoValido(correo: String): Boolean = CORREO_REGEX.matches(correo)

    fun nombreValido(nombre: String): Boolean =
        nombre.length in NOMBRE_MIN..NOMBRE_MAX && NOMBRE_REGEX.matches(nombre)

    fun requisitosContrasena(contrasena: String) = RequisitosContrasena(
        longitud = contrasena.length >= CONTRASENA_MIN,
        mayuscula = contrasena.any(Char::isUpperCase),
        minuscula = contrasena.any(Char::isLowerCase),
        numero = contrasena.any(Char::isDigit),
    )

    /**
     * Por qué no se acepta la contraseña nueva, para mostrarlo en el diálogo "Contraseña inválida"
     * (null = es válida). Lo usan "Cambiar contraseña" y "Restablecer contraseña".
     */
    fun motivoContrasenaInvalida(nueva: String, confirmar: String): String? {
        if (nueva.isEmpty()) return "Escribe tu nueva contraseña."
        val r = requisitosContrasena(nueva)
        val faltan = buildList {
            if (!r.longitud) add("mínimo $CONTRASENA_MIN caracteres")
            if (!r.mayuscula) add("una mayúscula")
            if (!r.minuscula) add("una minúscula")
            if (!r.numero) add("un número")
        }
        if (faltan.isNotEmpty()) return "La contraseña debe tener ${unirConY(faltan)}."
        if (confirmar.isEmpty()) return "Confirma tu nueva contraseña."
        if (nueva != confirmar) return "Las contraseñas no coinciden."
        return null
    }

    /** ["a", "b", "c"] → "a, b y c" */
    private fun unirConY(partes: List<String>): String =
        if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
}

/** Los cuatro requisitos que muestra la pantalla "Cambiar contraseña". */
data class RequisitosContrasena(
    val longitud: Boolean,
    val mayuscula: Boolean,
    val minuscula: Boolean,
    val numero: Boolean,
) {
    val cumpleTodos: Boolean get() = longitud && mayuscula && minuscula && numero
}
