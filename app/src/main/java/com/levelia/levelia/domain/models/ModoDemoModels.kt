package com.levelia.levelia.domain.models

// Modo demo (supabase/modo_demo.sql): solo las cuentas marcadas en la base lo tienen. Sirve para mostrar la app
// sin esperar días. Las reglas las aplica el servidor; aquí solo se describe lo que muestra la pantalla.

/** Respuesta de `mi_modo_demo()`: si la cuenta es demo y cuántas veces se usó "Avanzar 1 día". */
data class ModoDemo(val activo: Boolean, val dias: Int)

/**
 * Niveles que ofrece "Dejar listo para subir al nivel…" si todavía no llegó el catálogo de skins. Normalmente
 * salen de las skins de nivel (Personalización), así el modo demo sigue el catálogo si cambia.
 */
val NIVELES_DEMO_POR_DEFECTO = listOf(5, 10, 25, 50, 100)

/** Lo que pide confirmación antes de hacerse. */
sealed interface ConfirmacionDemo {
    data object Reiniciar : ConfirmacionDemo
}

data class ModoDemoUiState(
    /** false = cuenta normal (o el SQL del modo demo no está instalado): no se muestra nada. */
    val activo: Boolean = false,
    val dias: Int = 0,
    val enviando: Boolean = false,
    /** Lo último que se hizo ("Pasó 1 día."); null = nada todavía. */
    val mensaje: String? = null,
    val error: String? = null,
    val confirmacion: ConfirmacionDemo? = null,
    /** Sube con cada cambio hecho: NavGraph vuelve a leer nivel, misiones, logros y skins cuando cambia. */
    val cambios: Int = 0,
)
