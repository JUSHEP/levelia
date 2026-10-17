# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto

App Android **Levelia** (Kotlin, Jetpack Compose, Material 3, Hilt, Navigation Compose, StateFlow, Timber, Supabase auth + Postgrest). Paquete `com.levelia.levelia`. Estética pixel-art de videojuego, diseño de Figma en marco **412 × 917 dp**.

Documentación que hay que leer antes de cambios no triviales:
- `docs/CONTEXTO_LEVELIA.md` — fuente de verdad: valores exactos de branding (colores, medidas, textos), flujo de usuario, configuración de Supabase, pendientes y problemas ya resueltos.
- `REVISION_EMULADOR.md` — procedimiento para levantar el emulador (Pixel 6 = 412 dp de ancho), recorrer el módulo Perfil con `adb`, tomar capturas y compararlas con `docs/figma/`.
- `docs/PROMPT_SUPABASE_LEVELIA.md` — especificación del esquema de base de datos.

El `CLAUDE.md` del Escritorio (carpeta padre) describe proyectos web Next.js; sus partes de tema oscuro, logo, WhatsApp y GA4 **no aplican** aquí. Sí aplican sus reglas de proceso (no matar procesos genéricos, no hacer commit/push sin permiso, limpiar herramientas temporales).

## Cómo trabajar con este equipo

- Responder en español, corto y directo. Son estudiantes principiantes: explicar cada comando y qué debe aparecer al correrlo; dar los pasos en PowerShell (Windows) o bash (Ubuntu).
- No rediseñar por iniciativa propia ni inventar datos de diseño (color, tamaño, texto, imagen): preguntar o pedir la exportación de Figma.
- Dejar enlazadas las fuentes de lo que se consulte. La app usa **supabase-kt 3.0.2** (compatible con Kotlin 2.0.21): la documentación en línea es de versiones más nuevas, confirmar nombres de API para esa versión. No subir versiones de Supabase/Kotlin sin actualizar todo el toolchain.
- Cambios pequeños y revisados: buena parte del código se escribió sin compilar.

## Comandos

Desde la raíz del repo (esta carpeta). En Windows usar `.\gradlew.bat`; en Linux `./gradlew` (si falla con `sh\r`: `sed -i 's/\r$//' gradlew`). Requiere JDK 17–21 (AGP 8.13; Gradle 8.13 no corre con Java 25, que es el `jbr` de Android Studio en este PC: usar `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"`); compileSdk/targetSdk 36, minSdk 26. Versiones en `gradle/libs.versions.toml`.

```
./gradlew assembleDebug                 # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # instala en el dispositivo/emulador conectado
adb shell am start -n com.levelia.levelia/.MainActivity
./gradlew testDebugUnitTest             # tests unitarios (JUnit4 + MockK + coroutines-test, en app/src/test)
./gradlew testDebugUnitTest --tests "com.levelia.levelia.presentation.AuthViewModelTest"   # una clase
./gradlew testDebugUnitTest --tests "*AuthViewModelTest.login*"                          # por nombre
./gradlew lint
```

Esta carpeta suele ser una copia de trabajo **sin `.git`**: el repo real es `github.com/JUSHEP/levelia` y los cambios se copian encima de un clon (sección 7 de `docs/CONTEXTO_LEVELIA.md`). No asumir que hay historial de git aquí.

CI: `.github/workflows/build-apk.yml` compila `assembleDebug` (JDK 17) en cada push y publica el artefacto `levelia-apk`. Es la forma en que el equipo prueba builds cuando no tiene SDK local.

SQL: `supabase/setup.sql` (esquema + RLS, se pega en el SQL Editor). `supabase/pruebas.sql` prueba RLS y reglas y termina **a propósito** con un error que contiene `PRUEBAS_OK` (así revierte todo); cualquier `FALLO: ...` es un problema real.

## Arquitectura

Capas en `app/src/main/java/com/levelia/levelia/`: `data/` (Supabase: `SupabaseConfig`, DTOs, `AuthRepository`) → `domain/` (modelos, `ReglasPerfil`) → `presentation/` (ViewModels, NavGraph, pantallas) → `ui/theme/`.

Reglas que requieren leer varios archivos para entenderlas:
- **ViewModels compartidos:** `AuthViewModel` y `PerfilViewModel` se crean una sola vez en `MainActivity` y se pasan a `NavGraph`. Nunca llamar `hiltViewModel()` dentro de un destino del NavHost (se perdería el estado entre pantallas).
- **La navegación sigue al estado:** las pantallas del registro no navegan entre pasos; llaman al ViewModel y `NavGraph` observa `registerState.pasoActual`. Igual con `isAuthenticated` (→ Home/Login vaciando el back stack) y `perfilViewModel.cuentaEliminada`. Las pantallas reciben estado + lambdas, no ViewModels.
- **Repositorio sin excepciones:** toda función de `AuthRepository` devuelve `AuthResult` (Success/Error con mensaje en español); los errores se traducen ahí. El ViewModel nunca ve excepciones.
- **Pruebas:** los ViewModels se prueban con `AuthRepository` simulado (MockK) y `MainDispatcherRule`. No usar clases de Android en la lógica (por eso el correo se valida con `ReglasPerfil.correoValido` y no con `Patterns`). `AuthRepository` contra Supabase se prueba en el emulador, no en unitarias.
- **Reglas duplicadas a propósito:** las reglas de nombre (3–20, letras sin tildes/números/`_`, único sin distinguir mayúsculas) y contraseña (≥ 8, mayúscula, minúscula, número) están en `domain/ReglasPerfil.kt` **y** como CHECK/funciones en `supabase/setup.sql`. Si se cambia una, cambiar la otra.
- **Supabase:** `SupabaseConfig.kt` trae valores de relleno; `configurado` es false hasta pegar la URL y la clave pública (anon/publishable). Nunca usar ni pedir la `service_role` ni la contraseña de la BD. Desde la app solo se actualizan `nombre` y `avatar` de `perfiles`; `progreso` es de solo lectura; borrar cuenta usa la RPC `eliminar_mi_cuenta`.
- **Cuenta = perfil:** el perfil se inserta en `perfiles` apenas se verifica el código (paso 2, avatar por defecto); el paso 3 solo actualiza el avatar. Una cuenta de Auth sin fila en `perfiles` no entra a la app (login y sesión guardada la rechazan); registrarse de nuevo con el mismo correo y contraseña la retoma (`retomarRegistro`).
- **Sin conexión:** `MainActivity` superpone `SinConexionScreen` sobre todo el NavGraph mientras no haya red (`Conexion.kt`).
- **Dos sistemas de UI:** `components/LeveliaUi.kt` (diseño nuevo) y `components/AuthComponents.kt` (viejo: `AuthColumn`, `AuthTextField`, `LoadingButton`, aún usado por los pasos 2–4 del registro y por `HomeScreen`). Lo nuevo siempre con el diseño nuevo; al rehacer una pantalla vieja, migrarla y borrar lo que quede sin uso. Componentes del módulo Perfil en `PerfilUi.kt` y `BarraInferior.kt`.

## Reglas de branding (ver sección 4 de `docs/CONTEXTO_LEVELIA.md`)

- Usar `LeveliaScreen`, `LeveliaTextField`, `LeveliaButton`, `LeveliaDialog`, `TituloPixel`, etc. No `OutlinedTextField`, `Button`, `Scaffold` ni hex sueltos en pantallas: colores en `ui/theme/Color.kt`, fuentes en `Type.kt` (Press Start 2P para títulos, Plus Jakarta Sans para el resto).
- App siempre oscura y de borde a borde (`SystemBarStyle.dark`); cada pantalla gestiona sus insets vía `LeveliaScreen`. Teclado: contenido con scroll + `imePadding`, el pie queda fijo.
- Pixel-art en `res/drawable-nodpi/`, dibujado con `ImageBitmap.imageResource` + `Image(bitmap = ..., filterQuality = FilterQuality.None)` (`Image(painter=…, filterQuality=…)` no existe). Los recursos provisionales se reemplazan por exportaciones de Figma con el **mismo nombre**, sin tocar código.
- Textos: los del módulo Perfil y del registro tienen la ortografía **corregida a propósito** respecto a Figma; no "arreglar" esa diferencia al comparar con `docs/figma/`.
