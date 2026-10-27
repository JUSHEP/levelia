# Reporte QA — pruebas unitarias (2026-10-15)

**Resultado final: 82 pruebas, 0 fallos** (70 + 11 de recuperar contraseña + 1 del motivo de contraseña inválida, 2026-10-15). `./gradlew testDebugUnitTest` y `assembleDebug` pasan.

## Qué se prueba

| Archivo (`app/src/test/...`) | Pruebas | Cubre |
|---|---|---|
| `domain/ReglasPerfilTest` | 11 | Nombre (largo, símbolos, tildes), contraseña (4 requisitos), correo, barra de XP, motivo exacto de contraseña inválida |
| `data/PerfilDtosTest` | 3 | Filas reales de `perfiles` y `progreso` se leen bien (fecha, falta de `xp_siguiente_nivel`) |
| `data/MensajesDeErrorTest` | 5 | Traducción de errores de red, Auth y Postgres a mensajes en español |
| `presentation/AuthViewModelTest` | 42 | Sesión guardada, registro pasos 1–4, código OTP (pegar, borrar, 3 intentos, reenvío), registro a medias, login, logout, recuperar contraseña (correo, código con 3 intentos y reenvío, contraseña nueva, cancelar) |
| `presentation/PerfilViewModelTest` | 21 | Cargar perfil, editar nombre, cambiar avatar, cambiar contraseña, eliminar cuenta, limpiar al salir |

Los ViewModels usan un `AuthRepository` simulado (MockK): no tocan Supabase ni crean cuentas.

## Iteraciones

| Pasada | Resultado | Qué se hizo |
|---|---|---|
| 1 | No compila | La lógica usaba `android.util.Patterns` (no existe fuera de Android) y el traductor de errores era privado |
| 2 | 70 pruebas, **6 fallos** (4 bugs) | Se corrigieron F1–F4 |
| 3 | **70 / 70** | Revisión de código: F5 |
| 4 | **81 / 81** | P1: recuperar contraseña con código (11 pruebas nuevas) |
| 5 | **82 / 82** | "Contraseña inválida" dice el motivo exacto (qué requisito falta, no coinciden, o el motivo con que Supabase la rechazó) |

## Hallazgos corregidos

| # | Severidad | Bug | Cómo pasaba | Corrección |
|---|---|---|---|---|
| F0 | Media | Validación de correo con `android.util.Patterns`: imposible probar registro, login y recuperar | Pasada 1 no compilaba | `ReglasPerfil.correoValido` (mismo patrón, Kotlin puro) — `AuthViewModel.kt` |
| F1 | **Alta** | La app aceptaba nombres con tildes o ñ (`Peña`, `José_1`) pero la base los rechaza (`perfiles_nombre_formato`) | El usuario veía "Algo salió mal con nuestros servidores" al terminar el registro o editar el nombre | Regla `^[A-Za-z0-9_]+$` igual a la base — `ReglasPerfil.kt` |
| F2 | **Alta** | El registro solo pedía 8 caracteres; `abcdefgh` pasaba aunque "Cambiar contraseña" exige mayúscula, minúscula y número | Con los requisitos activados en Supabase, el registro fallaba con error genérico | Paso 1 usa `ReglasPerfil.requisitosContrasena(...).cumpleTodos` |
| F3 | Media | Poner como nueva la misma contraseña actual se traducía como "contraseña débil" (el texto de Supabase también contiene "password should") | Mensaje equivocado en "Cambiar contraseña" | Orden de las reglas en `AuthRepository.mensajeDeError` |
| F4 | Baja | Contraseña débil rechazada por el servidor mostraba el diálogo de error de servidor | Diálogo equivocado | `PerfilViewModel.guardarContrasena` la trata como "Contraseña inválida" |
| F5 | Media | `supabase/setup.sql` permitía tildes en el nombre y no coincide con la base real (`xp_siguiente_nivel`) | Correrlo sobre el proyecto lo dejaría distinto a la app | Regla del nombre alineada + aviso al inicio del archivo |

## Pendientes (no se corrigen aquí: requieren decisión o serían sobreingeniería)

| # | Qué | Por qué no se corrigió |
|---|---|---|
| ~~P1~~ | ✅ **Resuelto (2026-10-15)**: recuperar contraseña ahora usa un código de 6 dígitos. "Recupera tu contraseña" (Figma) → "Verifica tu email" (igual al paso 2 del registro, `verifyEmailOtp(OtpType.Email.RECOVERY)`) → "Restablecer contraseña" (igual a Cambiar contraseña sin la actual ni barra inferior, `updateUser { password }`) → diálogo "Contraseña actualizada" → login. Pruebas en `AuthViewModelTest`. **Falta en el panel de Supabase:** plantilla "Reset password" con `{{ .Token }}` | — |
| ~~P2~~ | ✅ **Resuelto (2026-10-15)**: Supabase **Password Requirements** quedó en "Lowercase, uppercase letters and digits" con mínimo 8 (sin símbolos), igual que los 4 requisitos de la app y del diseño. Confirmado por el equipo en el panel | — |
| P3 | La edad no se puede corregir después de verificar el código | La base solo permite a la app actualizar `nombre` y `avatar`; cambiarlo es decisión de diseño |
| P4 | `AuthRepository` (llamadas reales a Supabase) sin pruebas unitarias | Simular el SDK de Supabase es frágil; se prueba en el emulador (`REVISION_EMULADOR.md`) |
| P5 | Sin pruebas de interfaz (Compose) | Necesitan emulador; la revisión visual está en `REVISION_EMULADOR.md` |
| P6 | En el paso 1, un nombre con formato inválido muestra "Prueba con otro nombre." (mismo texto que nombre ocupado) | Texto del diseño de Figma |

## Cómo correrlas

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
.\gradlew.bat testDebugUnitTest
```

Reporte HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Módulo Misiones — revisión QA de backend (2026-10-21)

Qué se prueba: `supabase/pruebas_misiones.sql` (termina con `PRUEBAS_OK`, corrido contra el proyecto real sin dejar datos), `data/MisionesDatosTest` (5), `presentation/MisionesViewModelTest` (17). Total de la app: **105 pruebas, 0 fallos**.

| # | Severidad | Hallazgo | Corrección |
|---|---|---|---|
| M1 | **Alta** | XP infinita: cumplir, reclamar y volver a aceptar la misma misión el mismo día (y la UNICA las veces que se quisiera) | `aceptar_mision()`: la UNICA una sola vez; las demás solo cuando terminó el plazo de la anterior (`MISION_EN_CURSO`, `MISION_YA_HECHA`); misma regla en `MisionConProgreso.sePuedeAceptar(hoy)` |
| M2 | Media | Volver a aceptar con la XP sin reclamar escondía el botón "¡Reclamar recompensa!" | Hay que reclamar antes (`RECOMPENSA_PENDIENTE`) |
| M3 | Media | Aceptar dos veces a la vez (doble toque / dos teléfonos) daba "error del servidor" | El índice único se traduce a "Ya aceptaste esta misión." |
| M4 | Media | El ViewModel compartido no se limpiaba al cerrar sesión (el siguiente usuario vería misiones del anterior) | `MisionesViewModel.limpiar()`, igual que `PerfilViewModel.limpiar()` |
| M5 | Media | Registrar progreso con el plazo vencido se trataba como éxito | Aviso "Se venció el plazo de esta misión." (y no vuelve a la lista) |
| M6 | Baja | Dos lecturas seguidas podían pisar datos nuevos con viejos | `cargaJob` cancela la lectura anterior |
| M7 | Baja | Tras aceptar, el detalle no tenía la misión aceptada hasta recargar | La respuesta del servidor se refleja en la lista de inmediato |
| M8 | Baja | Nombre > 40 caracteres solo lo detectaba el servidor | Se marca bajo el campo |
| M9 | Baja | Editar las predefinidas en el SQL y volver a correrlo no las actualizaba | `on conflict … do update` |
| M10 | Prueba | `select (f()).*` en las pruebas SQL ejecutaba la función una vez por columna | Se usa `select * from f()` (la app ya llama así vía Supabase) |

Pendientes (requieren reingeniería o decisión, no se corrigen aquí):
- La lista lee todo el historial de `misiones_usuario` en cada carga: con meses de uso convendría una vista/RPC con solo la última aceptación por misión.
- `hoy()` usa la hora de Colombia fija y no se puede simular otro día en las pruebas SQL (el "mañana sí puede registrar" se prueba en Kotlin).
- Las predefinidas del diseño (A clases 2/100, Modo enfoque 2/75, Sígueme 3/100) no coinciden con la tabla de dificultades (1/75, 3/150, 5/300): confirmar con el equipo.
- Lint: el único error es `local.properties` (archivo local de Android Studio, no va en git); los avisos `UseKtx` de `AuthRepository` son anteriores.

## Módulo Misiones — cierre y prueba de punta a punta (2026-10-27)

**Resultado: 109 pruebas unitarias, 0 fallos · `pruebas_misiones.sql` → `PRUEBAS_OK` contra la base real.**

| # | Severidad | Hallazgo | Corrección |
|---|---|---|---|
| M11 | **Alta** | Ninguna pantalla llamaba a "¡Reclamar recompensa!": la XP nunca llegaba al nivel | Botón en "Completadas" + aviso "Ganaste +N XP. ¡Subiste al nivel N!" |
| M12 | Media | "Detalles de misión" mostraba "Aceptar" en misiones que el servidor rechaza (cumplida sin reclamar, dentro de su semana, Única ya hecha) | `MisionConProgreso.motivoNoAceptable(hoy)`: se muestra el motivo en lugar del botón |
| M13 | Media | "Ajustes de misión" no guardaba la dificultad hasta aceptar | Tabla `misiones_preferencias` + `ajustar_dificultad()`; "Guardar cambios" vuelve a la lista con veces y XP nuevas |

Prueba en el emulador (cuenta del equipo): ajustar Ir al gym y A clases a Fácil → aceptar → aumentar progreso (1/1, vuelve a la lista) → pestaña Completadas (tarjetas verdes) → Reclamar → "Ganaste +150 XP. ¡Subiste al nivel 2!" → Perfil: **Nvl. 2 · 50 / 283 XP** (igual en la base).

Pendientes: el aviso de recompensa usa textos provisionales (el diseño no lo trae); íconos `ic_mision_cumplida` e `ic_recompensa_gema` recortados del diseño (reemplazar por exportación de Figma con el mismo nombre); el PR #7 (`misiones-completo`) choca con `main` y quedó superado por el commit de Joshep.
