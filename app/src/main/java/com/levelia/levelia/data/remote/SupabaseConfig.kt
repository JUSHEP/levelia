package com.levelia.levelia.data.remote

/**
 * Datos de conexión del proyecto de Supabase (Project Settings → API).
 *
 * - [URL]: "Project URL", por ejemplo https://abcdefgh.supabase.co
 * - [CLAVE_PUBLICA]: la clave pública (anon o publishable). NUNCA la service_role.
 *
 * La clave pública puede ir en el repositorio porque las políticas de seguridad (RLS) de
 * supabase/setup.sql son las que protegen los datos.
 */
object SupabaseConfig {
    const val URL = "https://ikpggszhejomdpgtujfc.supabase.co"
    const val CLAVE_PUBLICA = "sb_publishable_GRShvr1KMxAxp5yi03Q0OQ_0jMKDrQT"

    /** false mientras sigan los valores de ejemplo: el repositorio avisa en vez de intentar conectarse. */
    val configurado: Boolean
        get() = !URL.contains("TU-PROYECTO") && !CLAVE_PUBLICA.startsWith("PEGAR")
}
