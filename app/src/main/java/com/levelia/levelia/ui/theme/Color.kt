package com.levelia.levelia.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de marca
val FondoPrincipal = Color(0xFF1A1A2E)
val Superficie = Color(0xFF26264A) // cards y diálogos, un tono sobre el fondo
val VerdeBoton = Color(0xFF22C55E)
val MoradoAcento = Color(0xFFC084FC)
val MoradoInput = Color(0xFFE8D4F8)
val TextoPrincipal = Color(0xFFFFFFFF)
val TextoSecundario = Color(0xFFCCCCCC)

// Derivados para contraste: el texto sobre fondos claros (botón verde, inputs) debe ser oscuro
val TextoSobreClaro = FondoPrincipal
val PlaceholderInput = Color(0xFF6B5B7B)
val ErrorClaro = Color(0xFFF87171) // rojo legible sobre el fondo oscuro

// Bordes de los avatares
val AvatarMorado = MoradoAcento
val AvatarRojo = Color(0xFFEF4444)
val AvatarCian = Color(0xFF22D3EE)

// ---------------------------------------------------------------------------------------------
// Diseño pixel-art (valores medidos de las capturas de Figma)
// ---------------------------------------------------------------------------------------------

/** Degradado vertical del fondo: posición (0..1 del alto de la pantalla) y color. */
val FondoDegradadoParadas: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFF0D1031),
    0.09f to Color(0xFF18103E),
    0.18f to Color(0xFF24114C),
    0.26f to Color(0xFF331258),
    0.35f to Color(0xFF49165E),
    0.44f to Color(0xFF5E1A64),
    0.53f to Color(0xFF721D68),
    0.61f to Color(0xFF87246D),
    0.70f to Color(0xFF982E6F),
    0.79f to Color(0xFFAA3670),
    0.88f to Color(0xFFB23A70),
    1.00f to Color(0xFFB23A70),
)

val EtiquetaCampo = Color(0xFF7B61FF) // "correo", "contraseña"… dentro del campo
val PlaceholderGris = Color(0xFF979797)
val TextoCampo = Color(0xFF1A1A2E)
val CampoErrorFondo = Color(0xFF4F1126) // campo vacío o incorrecto
val EnlaceMenta = Color(0xFF81DEA5) // "¿Olvidaste tu contraseña?", "Regístrate"
val SombraTitulo = Color(0xFF852A7A) // desplazamiento rosado de los títulos pixel
val BotonVerdeInicio = Color(0xFF087D62)
val BotonVerdeFin = Color(0xFF0DA77E)
val BotonAtrasFondo = Color(0xFF3F3C5D)

// Diálogos
val ScrimDialogo = Color(0xB3241F22)
val DialogoDegradadoParadas: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFF0F0F36),
    0.35f to Color(0xFF1A1040),
    0.62f to Color(0xFF33134C),
    0.82f to Color(0xFF59195C),
    1.00f to Color(0xFF7E2672),
)
val BotonDialogoInicio = Color(0xFF0E6B55)
val BotonDialogoFin = Color(0xFF1C8A70)

// ---------------------------------------------------------------------------------------------
// Módulo Perfil (valores medidos de las capturas de Figma)
// ---------------------------------------------------------------------------------------------

val BotonCancelar = Color(0xFF320260) // "Cancelar" (pantallas y diálogos)
val BotonGuardar = Color(0xFF008A6A) // "Guardar", "Aceptar", "Volver al inicio"
val BotonPeligro = Color(0xFFFB3845) // "Eliminar cuenta" del diálogo de confirmación

val PerfilTarjetaFondo = Color(0xFF1D0249) // opciones del menú de perfil y barra inferior
val PerfilTarjetaBorde = Color(0xFF1D2F54)
val PerfilPanelFondo = Color(0xFF15033E) // tarjeta "Tu progreso" y campos de texto
val PerfilPanelBorde = Color(0xFF4C207D)
val PerfilCampoContrasena = Color(0xFF000434) // interior más oscuro de los campos de contraseña
val PerfilSubtitulo = Color(0xFF6F5C8E) // "Cambia tu nombre en la app"

val XpPistaFondo = Color(0xFF1C0859)
val XpPistaBorde = Color(0xFF6F4AE2)
val XpRellenoArriba = Color(0xFF2ED48A)
val XpRellenoMedio = Color(0xFF02B266)
val XpRellenoAbajo = Color(0xFF02975E)

val RequisitoCumplido = Color(0xFF038456)
val RequisitoFallido = Color(0xFFCB195D)

// ---------------------------------------------------------------------------------------------
// Módulo Misiones (valores medidos del diseño del equipo, pantalla "Misiones")
// ---------------------------------------------------------------------------------------------

val MisionTarjetaFondo = Color(0xFF20064F)
val MisionTarjetaBorde = Color(0xFF2A085A)
val MisionPestanasFondo = Color(0xFF1F054F) // contenedor "Activas · Completadas · Todas"
val MisionPestanaActiva = Color(0xFF039370)
val MisionXpMas = Color(0xFF018F6D) // el "+" de "+150 XP"
val MisionProgreso = Color(0xFF50ACC1) // "0/3"

// Nombre de la dificultad en "Detalles de misión"
val DificultadFacil = Color(0xFF3A945D)
val DificultadMedia = Color(0xFFD28B3A)
val DificultadDificil = Color(0xFFDD5F5F)
val DificultadUnica = Color(0xFFEA48F8)

// Círculo sin marcar de "Ajustes de misión" (el marcado usa ic_dialog_ok)
val RadioDificultadAro = Color(0xFF645980)
val RadioDificultadVacio = Color(0xFFD9D9D9)
