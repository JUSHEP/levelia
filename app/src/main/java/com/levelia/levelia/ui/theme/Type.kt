package com.levelia.levelia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.levelia.levelia.R

/** Fuente pixel de los títulos y diálogos (Press Start 2P: cada letra es un cuadrado). */
val PressStart2P = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

/** Fuente del texto normal. Un archivo estático por peso para que funcione igual desde Android 7. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

// Todo el texto usa Plus Jakarta Sans; los títulos (display/headline) usan la fuente pixel.
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PressStart2P),
    displayMedium = base.displayMedium.copy(fontFamily = PressStart2P),
    displaySmall = base.displaySmall.copy(fontFamily = PressStart2P),
    headlineLarge = base.headlineLarge.copy(fontFamily = PressStart2P),
    headlineMedium = base.headlineMedium.copy(fontFamily = PressStart2P),
    headlineSmall = base.headlineSmall.copy(fontFamily = PressStart2P),
    titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans),
    titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans),
    titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans),
    bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans),
)
