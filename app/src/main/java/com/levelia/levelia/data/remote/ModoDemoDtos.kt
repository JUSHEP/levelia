package com.levelia.levelia.data.remote

import com.levelia.levelia.domain.models.ModoDemo
import kotlinx.serialization.Serializable

/** Respuesta de `mi_modo_demo()` (supabase/modo_demo.sql). */
@Serializable
data class ModoDemoDto(
    val activo: Boolean = false,
    val dias: Int = 0,
) {
    fun aModoDemo() = ModoDemo(activo = activo, dias = dias)
}
