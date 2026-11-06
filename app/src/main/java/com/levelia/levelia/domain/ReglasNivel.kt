package com.levelia.levelia.domain

/**
 * Curva de niveles: del nivel n al n+1 hacen falta 100 + 10·(n − 1) XP (100, 110, 120 … 1 080 del 99 al
 * 100; unos 58 400 XP para llegar al 100). Cambiada el 2026-11-05: antes era round(100 · n^1.5) y el nivel
 * 100 pedía casi 4 millones de XP. Es la MISMA fórmula que `xp_para_subir()` en supabase/misiones.sql, que es
 * quien sube de nivel de verdad al reclamar recompensas: si se cambia una, hay que cambiar la otra.
 */
object ReglasNivel {
    const val NIVEL_MAX = 100

    fun xpParaSubir(nivel: Int): Int = 100 + 10 * (nivel.coerceAtLeast(1) - 1)

    /** XP total que hay que juntar para llegar a [nivel] empezando en el nivel 1 con 0 XP. */
    fun xpTotalHasta(nivel: Int): Int = (1 until nivel.coerceIn(1, NIVEL_MAX)).sumOf { xpParaSubir(it) }
}
