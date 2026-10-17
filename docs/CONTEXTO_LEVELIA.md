# Levelia: contexto del proyecto para continuar el trabajo

Este archivo resume todo lo que se hizo hasta el 9 de octubre de 2026 en la app Levelia y cómo se trabaja. Está escrito para que una IA (o una persona) pueda seguir sin perder detalles, sobre todo los del branding. Léelo completo antes de tocar código.

## 0. Cómo debe trabajar la IA con este equipo

- Responder en español, natural y directo, respuestas cortas. Son estudiantes y principiantes: explicar cada comando y decir qué debe aparecer al correrlo.
- Cuando se consulte algo (documentación, librerías), dejar registradas las fuentes con enlace.
- No rediseñar nada por iniciativa propia. El diseño de referencia es el de Figma; si falta un dato (color, tamaño, texto, imagen), preguntarlo o pedir la exportación, no inventarlo.
- No se puede compilar en el entorno de la IA (no hay Android SDK). El código se prueba compilando en GitHub (sección 7). Por eso se cambia poco por vez y se revisa dos veces cada API antes de usarla.
- Si el cambio se entrega como archivos, mantener la misma estructura de carpetas del repo para poder copiarlo encima.
- Entregar siempre los pasos exactos en PowerShell (PC de la universidad, Windows) o bash (máquina virtual Ubuntu).

## 1. Qué es el proyecto

- App Android llamada **Levelia**, con estética pixel-art de videojuego ("Continúa tu aventura", "Tu próximo nivel te espera").
- Paquete: `com.levelia.levelia`. Repositorio: https://github.com/JUSHEP/levelia
- Stack: Kotlin, Jetpack Compose, Material 3, Hilt, Navigation Compose, StateFlow, Timber, Supabase (auth + base de datos).
- Versiones clave: AGP 8.13.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.09.00, compileSdk 36, targetSdk 36, minSdk 26 (antes 24; ver sección 6).
- Hoy solo está el módulo de autenticación (login, registro, recuperar contraseña, Home provisional).
- Probado en un Infinix HOT 50 5G.

## 2. Estado actual

Hecho y probado en el celular:
- Login rediseñado igual a Figma.
- Registro paso 1 ("1 de 2") rediseñado igual a Figma, con errores por campo y cuatro diálogos de error.
- Fondo con degradado + cuadrícula, pie con banner o pasto, fuentes pixel.
- Ojo para ver/ocultar contraseña en el login y en el registro (dentro de `LeveliaTextField`).
- Flujo de recuperar contraseña con código de 6 dígitos: "Recupera tu contraseña" → "Verifica tu email" → "Restablecer contraseña" (2026-10-15).
- Pantalla "Sin conexión" que tapa la app cuando el teléfono no tiene internet.
- Selección de avatar con 6 personajes (de `Characters.png`).
- Un workflow de GitHub Actions que compila el APK.

Entregado en un zip y todavía por subir/probar (confirmar con el equipo si ya se hizo):
- **Base de datos con Supabase** (`cambios-supabase.zip`): `AuthRepository` real, módulo Hilt, tabla `perfiles` con seguridad. Requiere crear el proyecto de Supabase y pegar la URL y la clave (sección 6). Aún no se ha compilado ni probado.

Pendiente:
- Rehacer con el diseño nuevo: código de verificación (6 casillas y "Reenviar código (00:45)"), avatares + "¿Cómo quieres que te llamemos?", diálogo "Cuenta creada", paso 4. Siguen con el diseño viejo (`AuthColumn`).
- Home real (hoy es un marcador de posición).
- Reemplazar los recursos recortados por las exportaciones originales de Figma (sección 4.9).

## 3. Arquitectura y archivos

```
app/src/main/java/com/levelia/levelia/
  MainActivity.kt                      Surface + NavGraph + pantalla Sin conexión encima
  LeveliaApplication.kt                @HiltAndroidApp, Timber en debug
  di/SupabaseModule.kt                 crea el cliente de Supabase (Hilt)
  data/remote/SupabaseConfig.kt        URL y clave pública de Supabase (se llenan a mano)
  data/repository/AuthRepository.kt    toda la lógica de cuentas y perfiles (devuelve AuthResult)
  domain/models/Models.kt              RegisterState, errores, DialogoAuth, UserData, AuthResult, Destinations, Avatar
  presentation/navigation/NavGraph.kt  rutas; el paso del registro vive en el ViewModel y la navegación lo sigue
  presentation/viewmodel/AuthViewModel.kt  estado y reglas (validaciones, intentos, cuenta regresiva)
  presentation/screens/                LoginScreen, RegisterStep1..4Screen, RecoverPasswordScreen,
                                       RecoverCodeScreen, RestablecerContrasenaScreen, SinConexionScreen, HomeScreen
  presentation/screens/components/     LeveliaUi.kt (diseño nuevo), Conexion.kt, AuthComponents.kt (viejo)
  ui/theme/                            Color.kt, Type.kt, Theme.kt
app/src/main/res/font/                 press_start_2p.ttf, plus_jakarta_sans_{regular,medium,semibold,bold,extrabold}.ttf
app/src/main/res/drawable-nodpi/       bg_banner_nivel, bg_pasto, ic_dialog_*, avatar_1..6 (PNG)
supabase/setup.sql                     tabla perfiles + seguridad (se pega una vez en Supabase)
.github/workflows/build-apk.yml        compila el APK en GitHub
```

Reglas de la arquitectura:
- Las pantallas no navegan entre pasos del registro por su cuenta: llaman funciones del ViewModel y `NavGraph` sigue `registerState.pasoActual`.
- Un solo `AuthViewModel` compartido, creado en `MainActivity` y pasado a `NavGraph`. Si cada pantalla creara el suyo, el estado del registro se perdería.
- Todas las funciones del repositorio devuelven `AuthResult` (Success, Error con mensaje). El ViewModel no ve excepciones.
- Las pantallas viejas siguen usando `AuthColumn`, `AuthTextField` y `LoadingButton` (en `AuthComponents.kt`). Cuando se rehagan, hay que moverlas a `LeveliaScreen` y borrar lo viejo.

## 4. Branding y diseño (valores exactos, no cambiar sin pedirlo)

Marco de diseño de Figma: **412 × 917**. Todas las medidas están en dp/sp de ese marco.

### 4.1 Colores (`ui/theme/Color.kt`)

| Uso | Valor |
| --- | --- |
| Fondo (degradado vertical, 12 paradas) | 0.00 `0D1031`, 0.09 `18103E`, 0.18 `24114C`, 0.26 `331258`, 0.35 `49165E`, 0.44 `5E1A64`, 0.53 `721D68`, 0.61 `87246D`, 0.70 `982E6F`, 0.79 `AA3670`, 0.88 `B23A70`, 1.00 `B23A70` |
| Cuadrícula del fondo | blanco al 10 % de opacidad, línea de 1 dp |
| Etiqueta dentro del campo | `7B61FF` |
| Placeholder | `979797` |
| Texto escrito en el campo | `1A1A2E` |
| Campo con error (fondo) | `4F1126` con borde blanco de 1 dp |
| Enlaces (menta) | `81DEA5` |
| Sombra de títulos pixel | `852A7A` |
| Botón verde (degradado horizontal) | `087D62` → `0DA77E` |
| Botón atrás (fondo) | blanco al 16 % |
| Velo detrás del diálogo | `B3241F22` |
| Diálogo (degradado vertical) | 0.00 `0F0F36`, 0.35 `1A1040`, 0.62 `33134C`, 0.82 `59195C`, 1.00 `7E2672` |
| Botón del diálogo (degradado horizontal) | `0E6B55` → `1C8A70` |
| Cuadrícula del diálogo | blanco al 9 %, celda de 24,4 dp |

Paleta vieja (todavía la usan las pantallas sin rehacer): `FondoPrincipal 1A1A2E`, `Superficie 26264A`, `VerdeBoton 22C55E`, `MoradoAcento C084FC`, `MoradoInput E8D4F8`, `TextoSecundario CCCCCC`, `ErrorClaro F87171`.

### 4.2 Tipografías

- **Press Start 2P** (pixel): títulos y títulos de diálogo. Tamaño de título 28 sp (login: 26 sp), interlineado 1,3 × el tamaño. Sombra del título: color `852A7A`, desplazamiento (6, 6) px, sin desenfoque. Incluye acentos en español.
- **Plus Jakarta Sans**: todo el texto normal. Archivos estáticos por peso (400, 500, 600, 700, 800) generados de la fuente variable, para que funcione igual en todas las versiones de Android.
- En `Type.kt`, los estilos display y headline usan Press Start 2P; el resto, Plus Jakarta Sans.
- Tamaños: subtítulo 14 sp; etiqueta dentro del campo 12 sp medium; texto del campo 16 sp; mensaje de error 12 sp blanco; "Recordarme" 13 sp; enlace "¿Olvidaste tu contraseña?" 13 sp semibold menta; enlaces inferiores 14 sp (enlace en negrita, menta); botón verde 24 sp bold blanco; indicador de paso 14 sp blanco; texto legal 13 sp, interlineado 18 sp, centrado.

### 4.3 Fondo (`LeveliaBackground`)

- Degradado vertical de arriba (azul oscuro `0D1031`) a abajo (magenta `B23A70`) de toda la pantalla, también bajo la barra de estado y de navegación (la app es de borde a borde).
- Encima, una cuadrícula de líneas blancas al 10 %, celdas de 412 / 14 ≈ 29,4 dp, dibujada con `drawBehind`.
- La app es siempre oscura: iconos claros en las barras del sistema (`SystemBarStyle.dark`).

### 4.4 Medidas de los componentes

- Margen lateral del contenido: **43 dp**. Fila superior (flecha atrás + indicador de paso): 12 dp desde arriba, alto 40 dp.
- Espacio entre la fila superior y el título: login 110 dp, registro 64 dp, otras pantallas 64 dp por defecto del diseño nuevo.
- Campo de texto: alto **65 dp**, esquinas 20 dp, fondo blanco; etiqueta morada arriba (12 sp), texto 16 sp debajo. Separación entre campos: 20 dp.
- Campo con error: fondo `4F1126`, borde blanco de 1 dp, etiqueta con asterisco ("\*Nombre"), texto en blanco, mensaje de error debajo (12 sp, blanco, margen izquierdo 4 dp).
- Campo de contraseña: ojo a la derecha (iconos Visibility / VisibilityOff, 40 dp), en el color del texto del campo.
- Botón verde: alto **66 dp**, esquinas 22 dp, degradado horizontal, sombra de 8 dp, texto 24 sp bold. Deshabilitado: 45 % de opacidad. Cargando: círculo blanco de 26 dp.
- Botón atrás: cuadrado de 40 dp, esquinas 10 dp, fondo blanco al 16 %, flecha blanca de 22 dp.
- Casilla "Recordarme": cuadro de 18 dp con borde blanco de 1,5 dp y esquinas de 3 dp; palomita blanca.
- Enlace inferior: "¿No tienes cuenta? **Regístrate**", centrado; la pregunta en blanco al 90 %, el enlace en menta y negrita.
- Diálogo: tarjeta con esquinas de **38 dp**, margen lateral 38 dp, ancho máximo 360 dp, relleno 24 (lados) / 28 (arriba) / 26 (abajo). Título Press Start 2P 13 sp centrado; imagen de 64 dp de alto (22 dp arriba y 18 abajo); mensaje 13 sp centrado, interlineado 18 sp; botón "Entendido" en píldora, alto 38 dp, ancho mínimo 118 dp, texto 16 sp bold.

### 4.5 Pie de pantalla (imagen, no código)

El pie es un **PNG** pegado abajo, escalado al ancho de la pantalla (`ContentScale.FillWidth`) y dibujado con `FilterQuality.None` para que el pixel-art se vea nítido. Está en `drawable-nodpi` para que Android no lo reescale. El alto del pie se calcula con la proporción de la imagen, así que se puede reemplazar el PNG sin tocar código.

- `PieDePantalla.BANNER` → `bg_banner_nivel.png` (412 × 257): el banner "TU PRÓXIMO NIVEL TE ESPERA". Se usa en el login (y es el destinado a recuperar contraseña según el diseño).
- `PieDePantalla.PASTO_LEGAL` → `bg_pasto.png` (412 × 156): el pasto pixelado. Encima va el texto legal "Al registrarte aceptas nuestros / Términos y Política de privacidad." (13 sp, centrado, a 64 dp del borde inferior).
- El teclado tapa el pie en lugar de empujarlo.

### 4.6 Burbujas decorativas del login

Círculos blancos translúcidos dibujados con `Canvas`, medidos sobre el marco de 412 de ancho (x, y, radio, opacidad): (410, 105, 70, 0,12), (380, 115, 11, 0,35), (30, 170, 52, 0,12), (8, 208, 10, 0,30), (412, 590, 46, 0,12), (388, 598, 11, 0,30), (404, 48, 4, 0,35).

### 4.7 Textos exactos de las pantallas ya hechas

Login:
- Título "¡Bienvenido!" (26 sp). Subtítulo "Continúa tu **aventura**." (la palabra en negrita, con punto final).
- Campos: etiqueta "correo" (placeholder "nombre@correo.com") y "contraseña" (placeholder "Mínimo 8 caracteres"), en minúscula.
- "Recordarme", "¿Olvidaste tu contraseña?", botón "Iniciar sesión", enlace "¿No tienes cuenta? **Regístrate**".

Registro paso 1:
- Indicador "1 de 2". Título "REGISTRATE" (sin tilde, así está en el diseño). Subtítulo "Comienza tu **aventura**.".
- Campos: "Nombre" (placeholder "¿Como quieres que te llamemos?"), "Edad", "Correo" (placeholder "nombre@correo.com"), "Contraseña" (placeholder "minimo 8 caracteres"). Los placeholders conservan las faltas de ortografía del diseño; consultar con diseño antes de corregirlas.
- Botón "Crear cuenta", enlace "¿Ya tienes cuenta? **Inicia sesión**".

Mensajes bajo los campos: "Este campo es obligatorio.", "Prueba con otro nombre.", "Introduce una edad válida.", "Correo incorrecto.", "Contraseña incorrecta.". En login, una credencial mala marca los dos campos ("Correo incorrecto." y "Contraseña incorrecta.").

Diálogos (título / mensaje / icono):
- "Error de validación" / "Por favor, completa todos los campos obligatorios." / `ic_dialog_validacion`
- "Correo inválido" / "Por favor, ingresa un correo electrónico válido." / `ic_dialog_correo_invalido`
- "Contraseña débil" / "La contraseña debe tener al menos 8 caracteres." / `ic_dialog_contrasena_debil`
- "Error en el servidor" / "Algo salió mal con nuestros servidores. Inténtalo más tarde." / `ic_dialog_error_servidor`

Cuál diálogo sale: campos vacíos → validación; si no, correo inválido; si no, contraseña débil. Nombre y edad solo muestran el mensaje bajo el campo.

### 4.8 Pantallas hechas sin referencia de Figma (revisar contra el diseño)

`SinConexionScreen` se armó con los componentes del diseño nuevo (fondo, pie de pasto, `TituloPixel` de 20–22 sp en dos líneas, campo, botón) porque en ese momento no se estaban usando las capturas de Figma. Los textos son:
- "SIN CONEXIÓN" con el ícono de wifi tachado (Material `WifiOff`, 88 dp) / "Parece que te quedaste sin internet. Revisa tu conexión e inténtalo de nuevo." / botón "Reintentar".

Hay que compararla con Figma y ajustarla: ícono real, textos.

Recuperar contraseña (2026-10-15): "Recupera tu contraseña" sigue la captura de Figma del equipo (pie banner), pero dice "código" en vez de "enlace" ("...te enviaremos un código para restablecerla." / botón "Enviar código") porque el correo trae un código. "Verifica tu email" es igual al paso 2 del registro (diseño viejo, pedido así). "Restablecer contraseña" es "Cambiar contraseña" sin la contraseña actual ni la barra inferior (no hay sesión).

### 4.9 Recursos gráficos

- `bg_banner_nivel.png`, `bg_pasto.png` e `ic_dialog_*.png` son **recortes provisionales** de las capturas. Hay que reemplazarlos por las exportaciones de Figma (PNG @3x, fondo transparente) con el mismo nombre. No hace falta cambiar código.
- Faltan por exportar: íconos de corazón, sobre, X, reloj y wifi tachado, y el resto de imágenes de las pantallas por rehacer.
- Los avatares vienen de `Characters.png` (carpeta AppImagenes): seis PNG cuadrados de unos 167 px (`avatar_1` a `avatar_6`), en este orden con su color de marco: 1 arquero verde `3FBF4A`, 2 bruja morada `9B3FE0`, 3 pelirrojo rojo `E23A3A`, 4 rubia dorada `F2B21E`, 5 ninja cian `1FD3E0`, 6 chica con audífonos rosa `EC4F9B`. Se dibujan sin suavizado, a 84 dp en la selección (3 por fila, 2 filas) y a 64 dp en Home. El seleccionado lleva borde blanco de 3 dp y una palomita del color del marco; los demás se ven al 70 % de opacidad. El enum `Avatar` tiene `AVATAR_1` a `AVATAR_6`.

### 4.10 Reglas para mantener el branding

1. Usar siempre `LeveliaScreen`, `LeveliaTextField`, `LeveliaButton`, `LeveliaDialog`, `TituloPixel`, `SubtituloAventura`, `EnlaceInferior`, `CasillaRecordarme`. No usar `OutlinedTextField`, `Button` ni colores sueltos en pantallas nuevas.
2. Los colores y las fuentes salen de `Color.kt` y `Type.kt`; no escribir valores hexadecimales dentro de las pantallas (la excepción actual son los colores de marco de los avatares).
3. Imágenes pixel-art: `drawable-nodpi`, `FilterQuality.None`, nunca estirar con suavizado.
4. Los textos de Figma se copian tal cual, incluidos los errores de ortografía, hasta que diseño confirme la corrección.
5. Fondo y barras del sistema: pantalla de borde a borde, sin `Scaffold`; cada pantalla maneja su espacio con `LeveliaScreen`.
6. Teclado: el contenido hace scroll y usa `imePadding`; el pie queda fijo y el teclado lo tapa.

## 5. Flujo de usuario

Registro (4 pasos internos, el diseño muestra "1 de 2" y "2 de 2"):
1. Datos (nombre, edad, correo, contraseña) → crea la cuenta en Supabase y envía el código al correo.
2. Código de 6 dígitos (3 intentos; reenvío cada 60 s) → verifica el correo.
3. Elegir avatar → guarda el perfil en la tabla `perfiles`.
4. Cuenta creada → "Empezar" lleva a Home.

Reglas de validación: nombre mínimo 3 letras; edad entre 13 y 120; correo con formato válido; contraseña mínimo 8 caracteres.

Login: correo + contraseña (+ "Recordarme", que guarda el correo en `SharedPreferences`; nunca la contraseña). La sesión de Supabase queda guardada: al reabrir la app entra directo a Home.

Recuperar contraseña: "¿Olvidaste tu contraseña?" → correo → Supabase envía un código de 6 dígitos → "Verifica tu email" (3 intentos, reenviar cada 60 s) → "Restablecer contraseña" (nueva + confirmar, 4 requisitos) → diálogo "Contraseña actualizada" → login. Verificar el código deja una sesión temporal; al guardar o cancelar se cierra.

Sin conexión: `MainActivity` observa la conectividad y muestra `SinConexionScreen` encima de todo mientras no haya red. Solo exige que exista una red con internet, no que Android la haya "validado", para no bloquear redes con portal cautivo.

## 6. Base de datos: Supabase

Decisión del equipo: Supabase (porque envía un código de 6 dígitos por correo, que es lo que ya diseñó la app; Firebase verifica con un enlace). Nota: el profesor pidió que el módulo tuviera impacto en una base de datos.

Dependencias (en `gradle/libs.versions.toml` y `app/build.gradle.kts`):
- `supabase-kt` 3.0.2 (BOM) con `auth-kt` y `postgrest-kt`, Ktor 3.0.1 (motor `ktor-client-okhttp`), `kotlinx-serialization-json` 1.7.3 y el plugin `org.jetbrains.kotlin.plugin.serialization`.
- Se eligió la 3.0.2 porque su registro de cambios declara compatibilidad con Kotlin 2.0.21. Las versiones más nuevas se compilan con Kotlin 2.3, que no es compatible con este proyecto sin actualizar todo.
- minSdk subió de 24 a 26 porque la librería lo exige sin desugaring.

Qué hace `AuthRepository` (siempre devuelve `AuthResult`):
- `signUp` → `auth.signUpWith(Email)`. Con "Confirm email" activo, un correo ya registrado no da error: devuelve un usuario sin identidades (se detecta así y se responde "Ya existe una cuenta con ese email. Inicia sesión.").
- `verifyCode` → `auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, ...)`; si es correcto, deja la sesión iniciada.
- `createUserProfile` → `upsert` en la tabla `perfiles` con el id del usuario autenticado.
- `login` → `signInWith(Email)` y lee el perfil; si la cuenta no tiene perfil (abandonó el registro), crea un `UserData` mínimo con el nombre sacado del correo.
- `resendCode` → `resendEmail(OtpType.Email.SIGNUP, ...)`.
- `solicitarRecuperacion` → `resetPasswordForEmail` (también para reenviar). `verificarCodigoRecuperacion` → `verifyEmailOtp(type = OtpType.Email.RECOVERY, ...)`. `restablecerContrasena` → `updateUser { password = ... }` y cierra la sesión.
- `logout`, `restaurarSesion` (usa `awaitInitialization`), correo recordado en `SharedPreferences`.
- Errores: sin internet, credenciales incorrectas, código vencido, límite de envíos y error del servidor se traducen a mensajes en español en la función `falla`.

Tabla y seguridad: `supabase/setup.sql`. Tabla `perfiles` (id = id del usuario de Auth, nombre, email, edad, avatar, created_at) con RLS: cada persona solo lee, inserta y modifica su propia fila. Nunca guardar contraseñas en tablas propias.

Lo que hay que hacer en Supabase (una vez, lo hace una persona del equipo):
1. Crear el proyecto. En Project Settings → API copiar la **Project URL** y la **clave pública** (anon o publishable). Nunca la `service_role`.
2. Pegarlas en `data/remote/SupabaseConfig.kt` (`URL` y `CLAVE_PUBLICA`). La clave pública puede ir en el repo porque la protegen las políticas.
3. SQL Editor → pegar `supabase/setup.sql` → Run.
4. Authentication → Email Templates → "Confirm signup": el contenido debe incluir `{{ .Token }}` (por ejemplo "Tu código de Levelia es: **{{ .Token }}**"). Sin eso el correo no trae el código.
5. Dejar activado "Confirm email".
6. Correos: el servicio incluido en Supabase solo envía a correos del equipo de la organización y permite unos 2 mensajes por hora. Para probar con varias personas, configurar un SMTP propio (Authentication → SMTP Settings). Fuente: documentación de Supabase sobre SMTP.

7. Authentication → Email Templates → "Reset password": igual que el paso 4, el contenido debe incluir `{{ .Token }}` (por ejemplo "Tu código para restablecer la contraseña de Levelia es: **{{ .Token }}**") y no el enlace `{{ .ConfirmationURL }}`. Sin eso, recuperar contraseña no trae el código.

## 7. Flujo de trabajo, compilación y APK

Entornos:
- PC de la universidad (Windows, sin permisos de administrador): no se puede instalar Java ni el SDK. Solo se usa para clonar, copiar archivos, `git commit` y `git push`.
- Máquina virtual Ubuntu en casa: VS Code + terminal (sin Android Studio), Java 17, SDK de Android con las herramientas oficiales (platform-tools), depuración por wifi (`adb pair` / `adb connect`) en el Infinix.
- Un solo comando de compilación local: `./gradlew assembleDebug` (si `gradlew` falla con `sh\r`, quitar los saltos de línea de Windows: `sed -i 's/\r$//' gradlew`; el repo ya tiene `.gitattributes`).

Cómo subir cambios y sacar el APK sin instalar nada:
1. Clonar el repo: `git clone https://github.com/JUSHEP/levelia.git levelia` y entrar a la carpeta.
2. Copiar los archivos nuevos encima (`Copy-Item <origen>\app . -Recurse -Force`, igual con `.github`, `gradle`, `supabase` y `build.gradle.kts` si vienen).
3. `git config user.name` / `user.email` (solo en ese repo, no global), `git checkout -b <rama>`, `git add .`, `git commit -m "..."`, `git push -u origin <rama>`. Al hacer push se abre una ventana de login de GitHub.
4. El workflow `.github/workflows/build-apk.yml` compila solo en cada push (5 a 10 minutos). En GitHub: Actions → la ejecución más reciente → **Summary** → sección **Artifacts** → **levelia-apk** (un .zip con `app-debug.apk`).
5. Pasar el APK al celular (WhatsApp, Drive o cable) y permitir "instalar apps desconocidas". Si WhatsApp rechaza el .apk, mandarlo renombrado a .zip.
6. Si la compilación sale en rojo: abrir el paso "Compilar APK debug", copiar las líneas que empiezan con `e:` o `error:` y corregir.
7. Para llevar los cambios a `main`: abrir un Pull Request de la rama a `main` en GitHub y hacer merge.

Al terminar de usar un PC compartido: cerrar sesión de GitHub, borrar las credenciales guardadas de git en Windows (`cmdkey /list`, `cmdkey /delete:...`), borrar la carpeta del repo y los zips, vaciar la papelera y revocar "Git Credential Manager" en https://github.com/settings/applications.

## 8. Problemas ya resueltos (para no repetirlos)

- `JDK 17 or higher is required` en VS Code → instalar OpenJDK 17.
- `dpkg` interrumpido → `sudo dpkg --configure -a` y `apt --fix-broken install`.
- Diferencias de saltos de línea (CRLF) en `git diff` → `autocrlf`, `.gitattributes`.
- Push con 403 → token personal sin permiso `repo` o cuenta equivocada guardada; reemplazar credencial.
- `adb pair` con "protocol fault" → usar el adb oficial de platform-tools, no el de apt, y escribir IP:puerto y código reales.
- `Image(painter = ..., filterQuality = ...)` no existe → usar `ImageBitmap.imageResource` + `Image(bitmap = ..., filterQuality = FilterQuality.None)`.
- Un archivo .zip descargado en el PC de la universidad puede quedar en otra carpeta ("Descargas" o en Documents); buscarlo con `Get-ChildItem $HOME -Recurse -Filter "nombre*"`.

## 9. Riesgos y puntos a vigilar

- El código de Supabase y de las pantallas nuevas (recuperar contraseña, avatares, sin conexión) se escribió sin compilar localmente. Si GitHub reporta errores de nombres o imports de `supabase-kt`, corregirlos contra la documentación de esa versión, no asumir la API más nueva.
- La documentación de Supabase en línea es de la versión más reciente; la app usa la 3.0.2, así que hay que confirmar los nombres.
- Las pantallas de verificación, avatares y paso 4 aún usan el diseño viejo; mezclar ambos estilos puede notarse en la demo.
- En el diseño el registro tiene 2 pasos ("1 de 2", "2 de 2") y el código 4; hay que acordar con diseño cómo mostrarlo.
- El campo "Nombre" se usa como apodo ("¿Como quieres que te llamemos?"); en el diseño parece haber otra pantalla para el apodo con avatares. Falta decidir si se separa en dos campos.
- Si el servicio de correo de Supabase no está configurado, el registro se queda sin enviar el código.
- La app no se ha publicado ni firmado para producción: el APK es de depuración.

## 10. Prompt sugerido para arrancar la IA de tu compañero

> Lee el archivo CONTEXTO_LEVELIA.md completo. Eres el asistente de desarrollo de la app Android Levelia (Kotlin, Compose). Respeta el branding y los valores de la sección 4 sin cambiarlos, usa los componentes de `LeveliaUi.kt`, responde en español, corto y directo, deja las fuentes de lo que consultes y no inventes datos de diseño: pregunta o pide la exportación de Figma. No puedes compilar: entrega cambios pequeños, con la misma estructura de carpetas del repo, y dame los comandos de PowerShell para subirlos y sacar el APK desde GitHub Actions. Empecemos por: <tarea>.

## 11. Módulo Perfil (agregado después de la revisión)

Se agregó con 14 pantallas de Figma. **Escrito sin compilar** (el entorno no tiene el SDK de Android): si GitHub Actions marca error, corregir según el mensaje `e:`.

### 11.1 Pantallas y rutas (`domain/models/Models.kt`, `Destinations`)

| Pantalla | Ruta | Archivo |
| --- | --- | --- |
| Perfil (avatar, @nombre, "Tu progreso", 4 opciones) | `perfil` | `PerfilScreen.kt` |
| Editar nombre de usuario | `perfil_editar_nombre` | `EditarNombreScreen.kt` |
| Cambiar avatar (9 personajes) | `perfil_cambiar_avatar` | `CambiarAvatarScreen.kt` |
| Cambiar contraseña (4 requisitos en vivo) | `perfil_cambiar_contrasena` | `CambiarContrasenaScreen.kt` |
| Cuenta eliminada | `cuenta_eliminada` | `CuentaEliminadaScreen.kt` |

Se entra desde la pestaña **Perfil** de la barra inferior (`BarraInferior.kt`, solo Inicio y Perfil navegan por ahora). Diálogos del módulo: `DialogoPerfilUi` en `PerfilUi.kt` (usa `LeveliaDialog`).

### 11.2 Lógica

- `PerfilViewModel` (una sola instancia, creada en `MainActivity` junto a `AuthViewModel`). Lee el perfil y el progreso **de la base de datos** al abrir Perfil (`cargarPerfil`) y guarda cada cambio en Supabase antes de reflejarlo.
- `AuthRepository` (ahora con Supabase real): `obtenerPerfil`, `obtenerProgreso`, `nombreDisponible`, `actualizarNombre`, `actualizarAvatar`, `cambiarContrasena` (verifica la actual iniciando sesión y luego `updateUser`), `eliminarCuenta` (RPC `eliminar_mi_cuenta`).
- `ReglasPerfil.kt`: nombre 3–20, solo letras/números/`_`, sin espacios, único sin distinguir mayúsculas; contraseña ≥ 8 con mayúscula, minúscula y número. **Las mismas reglas están en `supabase/setup.sql`.** Las reglas del nombre también se aplican ahora en el registro.
- La sesión se restaura al abrir la app (`restaurandoSesion` en `AuthViewModel`); mientras tanto se muestra solo el fondo.
- Tras borrar la cuenta: NavGraph abre "Cuenta eliminada" y después cierra la sesión local.

### 11.3 Textos y ortografía

Los textos de Figma traían faltas de ortografía; **en la app están corregidos** (p. ej. "Editar nombre de usuario", "Cambiar avatar", "Cambiar contraseña", "Mantén tu cuenta segura", "Esta acción es irreversible", "Mínimo 8 caracteres", "Una mayúscula", "Una minúscula", "Un número", "Usuario inválido", "Contraseña inválida", "¿Estás seguro?"). También se corrigieron "REGÍSTRATE", "¿Cómo quieres que te llamemos?" y "Mínimo 8 caracteres" en el registro. Los mensajes de los diálogos están en `PerfilViewModel` (constantes `MENSAJE_*`); el de "Contraseña actualizada" es nuevo porque el diseño repetía el texto de error. Las capturas originales de Figma están en `docs/figma/` solo como referencia de posición, color y tamaño.

### 11.4 Recursos y colores nuevos

- Imágenes en `res/drawable-nodpi/`: `avatar_7..9`, `ic_candado_morado`, `ic_candado_x`, `ic_perfil_candado`, `ic_perfil_pluma`, `ic_perfil_avatar`, `ic_xp_brujula`, `ic_req_ok`, `ic_req_x`, `ic_dialog_ok`, `ic_dialog_info`, `ic_dialog_peligro`, `nav_*` (5 pestañas con la etiqueta ya dibujada). Siempre dibujar con `FilterQuality.None`.
- Colores nuevos al final de `Color.kt` (sección "Módulo Perfil").
- `LeveliaScreen` ganó parámetros opcionales (`PieDePantalla.NINGUNO`, `margenHorizontal`, `onMenu`, `barraInferior`); `TituloPixel` ganó `textAlign`/`interlineado`; `LeveliaDialog` ganó segundo botón, `altoIcono` y `colorBoton`. Las pantallas anteriores no cambian.

### 11.5 Supabase v2 (ver `supabase/setup.sql`, `supabase/pruebas.sql` y `PROMPT_SUPABASE_LEVELIA.md`)

Tablas `perfiles`, `progreso` (solo lectura desde la app), `historial_nombres`; funciones `nombre_disponible` y `eliminar_mi_cuenta`; RLS en todo; desde la app solo se puede actualizar `nombre` y `avatar`. El SQL se probó en PostgreSQL 16 con un `auth` simulado (12 pruebas, todas OK).

### 11.6 Revisión visual

`REVISION_EMULADOR.md` (en la raíz del repo) explica a Claude Code cómo abrir el emulador, recorrer las pantallas, tomar capturas y compararlas con `docs/figma/`.

### 11.7 Pendiente / decisiones a confirmar con diseño

- El botón de menú (hamburguesa) de Perfil no hace nada: no hay pantalla de destino en el diseño.
- Las pestañas Misiones, Logros y Personalización no navegan (módulos aún no hechos).
- Pegar URL y clave pública en `data/remote/SupabaseConfig.kt` (nunca la contraseña de la BD ni la `service_role`).
- Dashboard (a mano): plantilla de correo con `{{ .Token }}`, "Confirm email" activo y SMTP propio.
- Avisos que el diseño no trae (nombre ya en uso, contraseña actual incorrecta, error de servidor) reutilizan los diálogos existentes.
