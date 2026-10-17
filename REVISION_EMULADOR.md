# Revisión visual de Levelia en el emulador (instrucciones para Claude Code)

> **Para Claude Code:** lee este archivo completo y ejecuta los pasos en orden, en **PowerShell** (Windows). Tu trabajo es **ver** la app en un emulador de Android, tomar capturas de cada pantalla del módulo Perfil y compararlas con el diseño de Figma que está en `docs/figma/`. **No rediseñes nada**: si algo no coincide, anótalo en el reporte (paso 9). Solo corrige errores evidentes de código (crasheos, textos cortados, pantallas que no abren) y dilo en el reporte. No hagas `git commit` ni `git push` sin que el usuario lo pida.
>
> El usuario es estudiante y principiante: explica en español, corto, qué hiciste y qué se ve.

## 0. Qué hay que saber antes

- Proyecto: Kotlin + Jetpack Compose, paquete `com.levelia.levelia`, activity `com.levelia.levelia.MainActivity`.
- La app **necesita un proyecto de Supabase real** (no hay modo sin conexión): el login, el perfil y el progreso salen de la base de datos.
- Las capturas de Figma de `docs/figma/` son de **412 × 917 dp**. El emulador **Pixel 6** (1080 × 2400 px, densidad 2,625) da 412 dp de ancho, así que la comparación es directa.
- Los textos del diseño original tenían faltas de ortografía. **En la app están corregidos a propósito** (p. ej. "Cambiar avatar", "Editar nombre de usuario", "Mínimo 8 caracteres"). Esa diferencia de texto NO es un error; las diferencias de posición, tamaño, color o ícono sí.
- Esta revisión se escribió sin poder compilar. Si Gradle falla, copia las líneas que empiezan con `e:` , corrígelas con el cambio mínimo y vuelve a compilar.

## 1. Requisitos (verifica y dime qué falta)

```powershell
java -version                      # debe ser 17 o superior
$env:ANDROID_HOME                  # si está vacío, usa el valor de abajo
Test-Path "$env:LOCALAPPDATA\Android\Sdk"
```

- Si no hay Android SDK, pídele al usuario instalar **Android Studio** (https://developer.android.com/studio) y abrirlo una vez; instala el SDK en `%LOCALAPPDATA%\Android\Sdk`. Alternativa: solo "command-line tools" desde la misma página.
- El emulador necesita **virtualización activada** (en Windows: característica "Plataforma del hipervisor de Windows" o Hyper-V, y virtualización activada en la BIOS). Se comprueba después con `emulator -accel-check`.

Variables para esta sesión de PowerShell:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin"
adb version
emulator -accel-check
```

## 2. Crear el emulador (solo la primera vez)

```powershell
# Paquetes necesarios (acepta las licencias con "y")
sdkmanager --licenses
sdkmanager "platform-tools" "emulator" "platforms;android-36"

# Elige una imagen del sistema con Google APIs para x86_64 (35 o 36)
sdkmanager --list | Select-String "system-images;android-3[56];google_apis;x86_64"
sdkmanager "system-images;android-35;google_apis;x86_64"     # cambia 35 por 36 si prefieres

# Crea el dispositivo Pixel 6 (412 dp de ancho, igual que el diseño)
avdmanager create avd -n levelia_pixel6 -k "system-images;android-35;google_apis;x86_64" -d pixel_6 --force
```

Si `avdmanager` o `sdkmanager` no se encuentran, usa la ruta completa `$env:ANDROID_HOME\cmdline-tools\latest\bin\...`.

## 3. Arrancar el emulador

```powershell
Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList "-avd","levelia_pixel6","-no-snapshot","-gpu","auto","-no-boot-anim"
adb wait-for-device
do { Start-Sleep -Seconds 3; $listo = (adb shell getprop sys.boot_completed).Trim() } until ($listo -eq "1")
"Emulador listo"
adb shell wm size      # esperado: Physical size: 1080x2400
adb shell wm density   # esperado: 420 (≈ 2,625)
```

Si el emulador no arranca, ejecuta `emulator -avd levelia_pixel6` en primer plano para ver el error (casi siempre es virtualización).

## 4. Conectar la app a Supabase y crear usuarios de prueba

1. Abre `app/src/main/java/com/levelia/levelia/data/remote/SupabaseConfig.kt`. Si `URL` y `CLAVE_PUBLICA` todavía tienen el texto de relleno, **pídele al usuario** la *Project URL* y la *clave pública (anon / publishable)* de su proyecto de Supabase (Dashboard → Project Settings → API) y pégalas. **Nunca** uses ni pidas la clave `service_role` ni la contraseña de la base de datos.
2. Verifica que ya se corrió `supabase/setup.sql` en ese proyecto (SQL Editor → pegar → Run). Si no, pídeselo al usuario.
3. Crea **dos usuarios de prueba** en el Dashboard → Authentication → Users → *Add user → Create new user* (marca **Auto Confirm User**):
   - `demo1` → correo en minúsculas, p. ej. `demo1.levelia@gmail.com`, contraseña `Levelia2026`
   - `demo2` → p. ej. `demo2.levelia@gmail.com`, contraseña `Levelia2026` (este se usa para probar "Eliminar cuenta")
4. En el SQL Editor ejecuta esto (cambia los correos por los reales, en minúsculas):

```sql
insert into public.perfiles (id, nombre, email, edad, avatar)
select id, 'Ronald_123', email, 25, 'AVATAR_7' from auth.users where email = 'demo1.levelia@gmail.com';

insert into public.perfiles (id, nombre, email, edad, avatar)
select id, 'Demo_Dos', email, 25, 'AVATAR_1' from auth.users where email = 'demo2.levelia@gmail.com';

-- Para ver la barra de experiencia como en el diseño (650 / 1000 XP)
update public.progreso set xp = 650
where user_id = (select id from auth.users where email = 'demo1.levelia@gmail.com');
```

## 5. Compilar, instalar y abrir

```powershell
.\gradlew.bat installDebug
adb shell am start -n com.levelia.levelia/.MainActivity
```

La primera compilación tarda varios minutos (descarga Gradle y dependencias). Si hay errores `e:`, corrige y repite.

## 6. Funciones de ayuda (pégalas en la sesión de PowerShell)

```powershell
# Captura de pantalla -> capturas\<nombre>.png  (se usa pull: la redirección de PowerShell dañaría el PNG)
function Captura([string]$nombre) {
  New-Item -ItemType Directory -Force capturas | Out-Null
  adb shell screencap -p /sdcard/s.png
  adb pull /sdcard/s.png "capturas\$nombre.png" | Out-Null
  "Captura: capturas\$nombre.png"
}

# Toca un elemento por su texto o descripción. -Ultimo elige el último que coincida (p. ej. el botón de un diálogo).
function Tocar([string]$texto, [switch]$Ultimo) {
  adb shell uiautomator dump /sdcard/ui.xml | Out-Null
  adb pull /sdcard/ui.xml "$env:TEMP\ui.xml" | Out-Null
  [xml]$ui = Get-Content "$env:TEMP\ui.xml" -Raw -Encoding UTF8
  $nodos = @($ui.SelectNodes("//node") | Where-Object { $_.text -eq $texto -or $_.'content-desc' -eq $texto })
  if ($nodos.Count -eq 0) { Write-Host "No encontré '$texto' en pantalla"; return }
  $nodo = if ($Ultimo) { $nodos[-1] } else { $nodos[0] }
  $b = [regex]::Matches($nodo.bounds, '\d+') | ForEach-Object { [int]$_.Value }
  adb shell input tap ([int](($b[0] + $b[2]) / 2)) ([int](($b[1] + $b[3]) / 2))
  Start-Sleep -Milliseconds 700
}

# Toca el campo de texto número N de la pantalla (1 = el de más arriba). Sirve aunque el campo esté vacío.
function TocarCampo([int]$n) {
  adb shell uiautomator dump /sdcard/ui.xml | Out-Null
  adb pull /sdcard/ui.xml "$env:TEMP\ui.xml" | Out-Null
  [xml]$ui = Get-Content "$env:TEMP\ui.xml" -Raw -Encoding UTF8
  $campos = @($ui.SelectNodes("//node[@class='android.widget.EditText']"))
  if ($campos.Count -lt $n) { Write-Host "Solo hay $($campos.Count) campos de texto"; return }
  $b = [regex]::Matches($campos[$n - 1].bounds, '\d+') | ForEach-Object { [int]$_.Value }
  adb shell input tap ([int](($b[0] + $b[2]) / 2)) ([int](($b[1] + $b[3]) / 2))
  Start-Sleep -Milliseconds 500
}

# Escribe en el campo que tenga el foco (los espacios se envían como %s)
function Escribir([string]$texto) { adb shell input text ($texto -replace ' ', '%s'); Start-Sleep -Milliseconds 300 }

function Atras { adb shell input keyevent 4; Start-Sleep -Milliseconds 700 }
function BorrarCampo { 1..30 | ForEach-Object { adb shell input keyevent 67 } }   # 30 veces "borrar"
function OcultarTeclado { adb shell input keyevent 111; Start-Sleep -Milliseconds 400 }
```

Si `Tocar` no encuentra algo, mira `adb shell uiautomator dump` y busca el texto exacto; o toma una captura y calcula las coordenadas (1 dp = 2,625 px).

## 7. Recorrido y capturas

Guarda cada captura con el nombre indicado y compárala con la imagen de Figma indicada (puedes abrir ambas con la herramienta de lectura de imágenes).

### 7.1 Inicio de sesión

1. `Captura "00_login"` (para ver que no cambió).
2. `Tocar "correo"` → `Escribir "demo1.levelia@gmail.com"` → `OcultarTeclado`.
3. `Tocar "contraseña"` → `Escribir "Levelia2026"` → `OcultarTeclado`.
4. `Tocar "Iniciar sesión"` → espera 4 s → `Captura "00b_home"`. Debe verse la **barra inferior** con "Inicio" resaltado.

### 7.2 Módulo Perfil

| # | Cómo llegar | Captura | Figma | Qué verificar |
| --- | --- | --- | --- | --- |
| 1 | En Home: `Tocar "Perfil"` | `01_perfil` | `01_perfil.png` | Título "Perfil", avatar 7, **@Ronald_123** (viene de la base de datos), tarjeta "Tu progreso" con "Nvl. 1" y "650 / 1000 XP", 4 opciones con sus íconos, barra con "Perfil" resaltado |
| 2 | `Tocar "Eliminar cuenta"` | `02_perfil_dialogo_eliminar` | `02_perfil_dialogo_eliminar.png` | Diálogo "¿Estás seguro?", triángulo rojo, botón rojo "Eliminar cuenta" y "Cancelar". Luego `Tocar "Cancelar" -Ultimo` |
| 3 | `Tocar "Editar nombre de usuario"` → `Tocar "Guardar"` (campo vacío) | `03_editar_nombre_error` | `03_editar_nombre_error.png` | X roja dentro del campo, contador 0/20 y diálogo "Usuario inválido" (cierra con `Tocar "Aceptar" -Ultimo`) |
| 4 | `TocarCampo 1` (el campo "Nuevo nombre de usuario") → `Escribir "R0N41D01031"` → `OcultarTeclado` | `04_editar_nombre_con_texto` | `04_editar_nombre_con_texto.png` | "Nombre actual" = Ronald_123, contador 11/20 |
| 5 | `TocarCampo 1` → `BorrarCampo` → `Escribir "ab c"` → `OcultarTeclado` → `Tocar "Guardar"` | `05_editar_nombre_dialogo_invalido` | `05_editar_nombre_dialogo_invalido.png` | Diálogo "Usuario inválido" con ícono azul "i" y botón "Aceptar" verde |
| 6 | `Atras` hasta Perfil → `Tocar "Cambiar avatar"` | `06_cambiar_avatar` | `06_cambiar_avatar.png` | 9 personajes en 3 filas (orden del diseño), el actual con palomita y apagado |

Contraseña (entra con `Tocar "Cambia la contraseña"`). Usa `TocarCampo 1` (actual), `TocarCampo 2` (nueva) y `TocarCampo 3` (confirmar), escribe y `OcultarTeclado`; antes de cada prueba nueva usa `BorrarCampo` en el campo que cambies. La **contraseña actual** debe ser `Levelia2026`:

| # | "Nueva contraseña" | Captura | Figma | Debe verse |
| --- | --- | --- | --- | --- |
| 7 | `Abcdefg1` | `07_contrasena_todo_cumplido` | `07_contrasena_todo_cumplido.png` | 4 palomitas verdes |
| 8 | `Abc1` | `08_contrasena_falta_minimo` | `08_contrasena_falta_minimo.png` | X roja en "Mínimo 8 caracteres" |
| 9 | `abcdefg1` | `09_contrasena_falta_mayuscula` | `09_contrasena_falta_mayuscula.png` | X en "Una mayúscula" |
| 10 | `ABCDEFG1` | `10_contrasena_falta_minuscula` | `10_contrasena_falta_minuscula.png` | X en "Una minúscula" |
| 11 | `Abcdefgh` | `11_contrasena_falta_numero` | `11_contrasena_falta_numero.png` | X en "Un número" |
| 12 | `Abcdefg1` pero "Confirmar" distinta (`Zzz`) → `Tocar "Guardar"` | `12_contrasena_dialogo_invalida` | `12_contrasena_dialogo_invalida.png` | Diálogo "Contraseña inválida" |
| 13 | `Abcdefg1` y confirmar `Abcdefg1` → `Tocar "Guardar"` | `13_contrasena_dialogo_actualizada` | `13_contrasena_dialogo_actualizada.png` | Diálogo "Contraseña actualizada" con check verde |

Después del paso 13 la contraseña de `demo1` pasa a ser `Abcdefg1`; si quieres repetir, usa esa como "actual" o restáurala desde el Dashboard.

Para cada captura 7–11 usa el ojo del campo (`Tocar "Mostrar contraseña"`) solo si necesitas ver lo escrito.

### 7.3 Eliminar cuenta (usa SOLO `demo2`)

1. Cierra sesión: en Home toca "Cerrar sesión"; inicia sesión con `demo2.levelia@gmail.com` / `Levelia2026`.
2. `Tocar "Perfil"` → `Tocar "Eliminar cuenta"` → `Tocar "Eliminar cuenta" -Ultimo` (el botón rojo del diálogo).
3. `Captura "14_cuenta_eliminada"` → compara con `14_cuenta_eliminada.png`: candado rojo con X, "Cuenta eliminada", "Tu cuenta ha sido eliminada satisfactoriamente" y "Volver al inicio".
4. `Tocar "Volver al inicio"` → debe abrir el login. Intenta iniciar sesión con `demo2`: debe decir "Email o contraseña incorrectos."
5. En el Dashboard (Table Editor) confirma que `perfiles`, `progreso` y `historial_nombres` ya no tienen filas de `demo2`.

## 8. Comprobaciones extra (rápidas)

- Cambia el nombre a uno válido (p. ej. `Ronald_456`) → debe volver a Perfil mostrando **@Ronald_456**, y en la tabla `historial_nombres` debe aparecer una fila con el nombre anterior y el nuevo.
- Intenta poner el nombre `Demo_Dos` (si `demo2` aún existe) → "Ese nombre de usuario ya está en uso."
- Cambia el avatar → Perfil muestra el nuevo personaje y la tabla `perfiles` lo refleja.
- Cierra la app por completo y ábrela: debe entrar directo a Home sin pedir login (sesión restaurada).
- Gira o cambia el tamaño de fuente del sistema solo si el usuario lo pide.

## 9. Reporte final

Crea `capturas/REPORTE.md` en Markdown con:

1. Qué se pudo ejecutar y qué no (compilación, emulador, cada pantalla).
2. Una tabla: pantalla · captura · estado (**coincide / diferencia menor / diferencia importante**) · detalle (qué cambia respecto a Figma: posición, tamaño, color, ícono, texto cortado).
3. Errores de código que corregiste (archivo y línea) y los que NO corregiste.
4. Pendientes que dependen del usuario o de diseño (el botón de menú ☰ del Perfil no hace nada; las pestañas Misiones, Logros y Personalización no navegan; faltan textos reales de algunos avisos).

Responde al usuario, en español y en pocas líneas, con el resumen y la ruta de `capturas/REPORTE.md`.

## 10. Al terminar

```powershell
adb emu kill        # cierra el emulador
```

No dejes contraseñas, la URL ni la clave de Supabase en archivos que vayan a subirse a GitHub salvo que el usuario lo pida (la clave pública sí puede ir en `SupabaseConfig.kt`; la `service_role` y la contraseña de la base de datos, nunca).
