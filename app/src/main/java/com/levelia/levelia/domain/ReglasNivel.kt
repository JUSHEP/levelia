package com.levelia.levelia.domain

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Curva de niveles: del nivel n al n+1 hacen falta round(100 · n^1.5) XP (100, 283, 520 … ≈98 500 del
 * 99 al 100). Es la MISMA fórmula que `xp_para_subir()` en supabase/misiones.sql, que es quien sube de
 * nivel de verdad al reclamar recompensas: si se cambia una, hay que cambiar la otra.
 */
object ReglasNivel {
    const val NIVEL_MAX = 100

    fun xpParaSubir(nivel: Int): Int = (100 * nivel.coerceAtLeast(1).toDouble().pow(1.5)).roundToInt()
}
