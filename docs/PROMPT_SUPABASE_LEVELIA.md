# Prompt para crear el backend de Levelia en Supabase (vía MCP) — v2 con módulo Perfil

Notas internas. Todo lo de abajo sale de lo que hay hoy en el repo (`levelia-main.zip` con el módulo Perfil) y de `CONTEXTO_LEVELIA.md`. El SQL de este prompt es el mismo de `supabase/setup.sql` y fue probado en PostgreSQL 16 con un `auth` simulado.

## 0. Qué hace falta ANTES de pegar el prompt

1. **Cuenta de Supabase** con una organización creada (la gratis sirve). Si no tienes, créala en https://supabase.com/dashboard.
2. **MCP de Supabase conectado a nivel de cuenta**, es decir SIN `project_ref` en la URL. Si lo conectas a un proyecto, Claude no podrá crear proyectos nuevos. Sin modo `read_only`, porque tiene que crear cosas.

   En PowerShell (Claude Code):

   ```powershell
   claude mcp add --transport http supabase https://mcp.supabase.com/mcp
   ```

   Luego, dentro de Claude Code, escribe `/mcp`, elige `supabase` y autentícate (se abre el navegador). Debe aparecer como **connected**.
   Verifica el comando y las herramientas disponibles en la doc oficial: https://supabase.com/docs/guides/getting-started/mcp
3. **Deja activada la aprobación manual** de cada llamada del MCP. Crear un proyecto puede tener costo y el MCP puede ejecutar SQL.
4. Si ya tienes el repo clonado, abre Claude Code en esa carpeta (`levelia`) para que pueda guardar `supabase/setup.sql`.

## 1. Lo que el MCP puede y NO puede hacer

| Cosa | ¿Por MCP? |
| --- | --- |
| Crear el proyecto | Sí (pide confirmar el costo) |
| Crear tabla, RLS, políticas (migración) | Sí |
| Probar las políticas con SQL | Sí |
| Revisar alertas de seguridad (advisors) | Sí |
| Obtener URL y clave pública | Sí |
| Plantillas de correo (código `{{ .Token }}`) | **No**, se hace a mano en el Dashboard |
| "Confirm email" activado y SMTP propio | **No**, a mano en el Dashboard |

Por eso el prompt termina con una lista de lo que haces tú a mano.

## 2. El prompt (copia desde la línea de abajo hasta el final del bloque)

````text
Eres el encargado de crear el backend de Supabase para la app Android "Levelia" (Kotlin + Jetpack Compose, paquete com.levelia.levelia). Usa el MCP de Supabase conectado en esta sesión. Responde en español, corto y directo. Soy estudiante y principiante: dime qué hiciste en cada paso y qué debo ver.

## Reglas de trabajo
- Antes de empezar, lista las herramientas del MCP de Supabase que tienes disponibles y dime cuáles vas a usar. Los nombres pueden variar; usa los reales que aparezcan, no los inventes.
- No inventes datos. Si falta algo (organización, región, nombre), pregúntame UNA vez, todo junto.
- Todo cambio de esquema se hace con migraciones (apply_migration), no con SQL suelto. execute_sql solo para consultar y probar.
- Nunca me muestres ni guardes en el repo la contraseña de la base de datos ni la clave service_role. La contraseña de la BD me la entregas UNA sola vez al final, en el chat.
- No toques el código de la app Android. Solo backend. Si hay un repo en la carpeta actual, lo único que puedes escribir es supabase/setup.sql y supabase/migrations/ (copia exacta del SQL de la migración).

## Paso 1. Crear el proyecto
1. Lista mis organizaciones. Si hay una sola, úsala; si hay varias, pregúntame cuál.
2. Revisa los proyectos existentes por si ya hay uno llamado "levelia". Si existe, dime y NO crees otro; pregúntame si usamos ese.
3. Calcula el costo (get_cost o equivalente). Dime el costo y espera mi "sí" antes de confirmar (confirm_cost o equivalente).
4. Crea el proyecto:
   - Nombre: levelia
   - Región: sa-east-1 (São Paulo, la más cercana a Colombia). Si no está disponible, dime las opciones.
   - Contraseña de BD: genera una aleatoria y fuerte (mínimo 24 caracteres).
5. Espera a que el estado sea ACTIVE_HEALTHY (revisa el estado cada cierto tiempo, sin spamear).

## Paso 2. Migración "crear_esquema_levelia"
Contexto: la autenticación (correo + contraseña + código OTP de 6 dígitos) la maneja Supabase Auth. Esa parte NO se crea a mano, ya viene en el esquema `auth`. Nosotros creamos: `perfiles` (nombre de usuario único, edad, avatar), `progreso` (nivel y XP, solo lectura desde la app), `historial_nombres` (registro de cada cambio de nombre) y dos funciones que llama la app: `nombre_disponible` y `eliminar_mi_cuenta`. NUNCA guardes contraseñas en tablas propias.

Antes de aplicar, revisa con list_tables si ya existe `public.perfiles`. Si existe (versión 1 del esquema), el mismo SQL la actualiza sin perder datos: dímelo y aplícalo igual.

Aplica EXACTAMENTE este SQL con apply_migration (nombre: crear_esquema_levelia). No lo cambies ni lo resumas:

```sql
-- =============================================================================
-- Levelia · esquema de Supabase (versión 2: módulo Perfil)
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run.
-- Se puede ejecutar varias veces sin romper nada (es idempotente) y también
-- sirve para actualizar un proyecto que ya tenía la versión 1 (solo `perfiles`).
--
-- Qué crea:
--   perfiles          datos públicos del usuario (nombre único, edad, avatar)
--   progreso          nivel y XP (la app solo lo LEE; lo escribe el servidor)
--   historial_nombres un registro de cada cambio de nombre de usuario
--   nombre_disponible() ¿el nombre está libre? (la usa el registro y "Editar nombre")
--   eliminar_mi_cuenta() borra la cuenta del usuario que la llama y todo lo suyo
--
-- Contraseñas, códigos OTP y sesiones los guarda Supabase Auth (esquema `auth`).
-- NUNCA se guardan en tablas propias.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Función común: mantiene updated_at al día
-- -----------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- -----------------------------------------------------------------------------
-- 1. perfiles
-- -----------------------------------------------------------------------------
create table if not exists public.perfiles (
  id         uuid primary key references auth.users (id) on delete cascade,
  nombre     text        not null,
  email      text        not null,
  edad       smallint    not null,
  avatar     text        not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Por si la tabla venía de la versión 1
alter table public.perfiles add column if not exists updated_at timestamptz not null default now();

comment on table public.perfiles is
  'Perfil de la app Levelia. id = auth.users.id. Contraseñas solo viven en Supabase Auth.';

-- Reglas (las mismas de ReglasPerfil.kt en la app)
alter table public.perfiles drop constraint if exists perfiles_nombre_minimo;
alter table public.perfiles drop constraint if exists perfiles_nombre_formato;
alter table public.perfiles add constraint perfiles_nombre_formato
  check (nombre ~ '^[A-Za-z0-9_ÁÉÍÓÚÜÑáéíóúüñ]{3,20}$') not valid;

alter table public.perfiles drop constraint if exists perfiles_edad_rango;
alter table public.perfiles add constraint perfiles_edad_rango
  check (edad between 13 and 120);

alter table public.perfiles drop constraint if exists perfiles_avatar_valido;
alter table public.perfiles add constraint perfiles_avatar_valido
  check (avatar in ('AVATAR_1','AVATAR_2','AVATAR_3','AVATAR_4','AVATAR_5',
                    'AVATAR_6','AVATAR_7','AVATAR_8','AVATAR_9'));

alter table public.perfiles drop constraint if exists perfiles_email_minuscula;
alter table public.perfiles add constraint perfiles_email_minuscula
  check (email = lower(email));

alter table public.perfiles drop constraint if exists perfiles_email_unico;
alter table public.perfiles add constraint perfiles_email_unico unique (email);

-- Nombre de usuario único SIN distinguir mayúsculas ("Ronald_123" = "ronald_123")
create unique index if not exists perfiles_nombre_unico on public.perfiles (lower(nombre));

drop trigger if exists perfiles_updated_at on public.perfiles;
create trigger perfiles_updated_at
  before update on public.perfiles
  for each row execute function public.set_updated_at();

-- Seguridad: cada persona solo ve y modifica SU fila
alter table public.perfiles enable row level security;

revoke all on public.perfiles from anon;
revoke all on public.perfiles from authenticated;
grant select on public.perfiles to authenticated;
grant insert (id, nombre, email, edad, avatar) on public.perfiles to authenticated;
-- Desde la app solo se pueden cambiar el nombre y el avatar (email y edad NO)
grant update (nombre, avatar) on public.perfiles to authenticated;

drop policy if exists "perfiles_select_propio" on public.perfiles;
create policy "perfiles_select_propio"
  on public.perfiles for select to authenticated
  using ((select auth.uid()) = id);

drop policy if exists "perfiles_insert_propio" on public.perfiles;
create policy "perfiles_insert_propio"
  on public.perfiles for insert to authenticated
  with check ((select auth.uid()) = id);

drop policy if exists "perfiles_update_propio" on public.perfiles;
create policy "perfiles_update_propio"
  on public.perfiles for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

-- No hay política de DELETE a propósito: el perfil se borra con eliminar_mi_cuenta().

-- -----------------------------------------------------------------------------
-- 2. progreso (nivel y XP). La app solo lo lee.
-- -----------------------------------------------------------------------------
create table if not exists public.progreso (
  user_id            uuid primary key references auth.users (id) on delete cascade,
  nivel              integer     not null default 1    check (nivel >= 1),
  xp                 integer     not null default 0    check (xp >= 0),
  xp_siguiente_nivel integer     not null default 1000 check (xp_siguiente_nivel > 0),
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);

comment on table public.progreso is
  'Nivel y XP del usuario. Solo lectura desde la app; lo modifica el servidor (misiones, logros).';

drop trigger if exists progreso_updated_at on public.progreso;
create trigger progreso_updated_at
  before update on public.progreso
  for each row execute function public.set_updated_at();

alter table public.progreso enable row level security;

revoke all on public.progreso from anon;
revoke all on public.progreso from authenticated;
grant select on public.progreso to authenticated;

drop policy if exists "progreso_select_propio" on public.progreso;
create policy "progreso_select_propio"
  on public.progreso for select to authenticated
  using ((select auth.uid()) = user_id);

-- Al crearse un perfil se crea su fila de progreso (nivel 1, 0 XP)
create or replace function public.crear_progreso_inicial()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.progreso (user_id) values (new.id) on conflict (user_id) do nothing;
  return new;
end;
$$;
revoke all on function public.crear_progreso_inicial() from public, anon, authenticated;

drop trigger if exists perfiles_crear_progreso on public.perfiles;
create trigger perfiles_crear_progreso
  after insert on public.perfiles
  for each row execute function public.crear_progreso_inicial();

-- Perfiles que ya existían (versión 1) también reciben su progreso
insert into public.progreso (user_id)
select id from public.perfiles
on conflict (user_id) do nothing;

-- -----------------------------------------------------------------------------
-- 3. historial_nombres (registro de cada cambio de nombre de usuario)
-- -----------------------------------------------------------------------------
create table if not exists public.historial_nombres (
  id              bigint generated always as identity primary key,
  user_id         uuid        not null references auth.users (id) on delete cascade,
  nombre_anterior text        not null,
  nombre_nuevo    text        not null,
  cambiado_en     timestamptz not null default now()
);

create index if not exists historial_nombres_usuario_idx
  on public.historial_nombres (user_id, cambiado_en desc);

alter table public.historial_nombres enable row level security;

revoke all on public.historial_nombres from anon;
revoke all on public.historial_nombres from authenticated;
grant select on public.historial_nombres to authenticated;

drop policy if exists "historial_nombres_select_propio" on public.historial_nombres;
create policy "historial_nombres_select_propio"
  on public.historial_nombres for select to authenticated
  using ((select auth.uid()) = user_id);

create or replace function public.registrar_cambio_de_nombre()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.historial_nombres (user_id, nombre_anterior, nombre_nuevo)
  values (new.id, old.nombre, new.nombre);
  return new;
end;
$$;
revoke all on function public.registrar_cambio_de_nombre() from public, anon, authenticated;

drop trigger if exists perfiles_historial_nombre on public.perfiles;
create trigger perfiles_historial_nombre
  after update of nombre on public.perfiles
  for each row
  when (old.nombre is distinct from new.nombre)
  execute function public.registrar_cambio_de_nombre();

-- -----------------------------------------------------------------------------
-- 4. Funciones que llama la app
-- -----------------------------------------------------------------------------

-- ¿Está libre el nombre? Se llama ANTES de registrarse (sin sesión) y al editar el nombre.
-- Tu propio nombre cuenta como libre para ti.
create or replace function public.nombre_disponible(p_nombre text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select not exists (
    select 1
    from public.perfiles
    where lower(nombre) = lower(btrim(p_nombre))
      and id is distinct from auth.uid()
  );
$$;
revoke all on function public.nombre_disponible(text) from public;
grant execute on function public.nombre_disponible(text) to anon, authenticated;

-- Borra la cuenta de QUIEN LA LLAMA. Elimina al usuario de Auth y, en cascada,
-- su perfil, su progreso y su historial. Es irreversible.
create or replace function public.eliminar_mi_cuenta()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then
    raise exception 'No hay sesión iniciada' using errcode = '28000';
  end if;
  delete from auth.users where id = v_id;
end;
$$;
revoke all on function public.eliminar_mi_cuenta() from public, anon;
grant execute on function public.eliminar_mi_cuenta() to authenticated;
```

Después de aplicarla, guarda ese mismo SQL en supabase/setup.sql (y en supabase/migrations/ con un nombre con fecha) SOLO si estoy dentro del repo.

## Paso 3. Probar que la seguridad y las reglas funcionan
Ejecuta UNA llamada de execute_sql con este bloque. Crea 2 usuarios falsos, prueba las reglas y al final lanza un error a propósito para que TODO se revierta (no debe quedar nada guardado). Si el mensaje final contiene PRUEBAS_OK, todo salió bien. Si aparece cualquier mensaje que empiece por "FALLO", dime cuál y NO sigas.

```sql
-- Pruebas de seguridad y reglas del esquema de Levelia.
-- Crea 2 usuarios falsos, prueba todo y al final lanza un error A PROPÓSITO para que NADA quede guardado.
-- Resultado esperado: un error cuyo mensaje contiene PRUEBAS_OK. Cualquier mensaje "FALLO: ..." es un problema.
do $$
declare
  a uuid := '00000000-0000-0000-0000-0000000000a1';
  b uuid := '00000000-0000-0000-0000-0000000000a2';
  n int;
  libre boolean;
begin
  insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (a, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'prueba-a@levelia.test', 'x', now(), now(), now()),
    (b, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'prueba-b@levelia.test', 'x', now(), now(), now());

  -- Perfil de B creado como administrador (para probar que A no lo ve ni lo toca)
  insert into public.perfiles (id, nombre, email, edad, avatar)
  values (b, 'Beto_01', 'prueba-b@levelia.test', 30, 'AVATAR_9');

  select count(*) into n from public.progreso where user_id = b;
  if n <> 1 then raise exception 'FALLO: no se creó el progreso inicial de B'; end if;

  -- ===== A partir de aquí actúo como el usuario A (rol authenticated) =====
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);

  -- 1) A crea su perfil (con uno de los avatares nuevos)
  insert into public.perfiles (id, nombre, email, edad, avatar)
  values (a, 'Ana_01', 'prueba-a@levelia.test', 20, 'AVATAR_7');

  -- 2) A no puede crear el perfil de otro
  begin
    insert into public.perfiles (id, nombre, email, edad, avatar)
    values (gen_random_uuid(), 'Intruso', 'x@levelia.test', 20, 'AVATAR_1');
    raise exception 'FALLO: A pudo insertar un perfil con id ajeno';
  exception when insufficient_privilege then null;
  end;

  -- 3) A solo ve su perfil y su progreso
  select count(*) into n from public.perfiles;
  if n <> 1 then raise exception 'FALLO: A ve % perfiles y debía ver 1', n; end if;
  select count(*) into n from public.progreso;
  if n <> 1 then raise exception 'FALLO: A ve % filas de progreso y debía ver 1', n; end if;
  select count(*) into n from public.progreso where nivel = 1 and xp = 0 and xp_siguiente_nivel = 1000;
  if n <> 1 then raise exception 'FALLO: el progreso inicial de A no es nivel 1, 0 XP, 1000'; end if;

  -- 4) A no puede tocar el perfil de B
  update public.perfiles set avatar = 'AVATAR_1' where id = b;
  get diagnostics n = row_count;
  if n <> 0 then raise exception 'FALLO: A modificó el perfil de B'; end if;

  -- 5) El nombre de usuario es único sin distinguir mayúsculas
  begin
    update public.perfiles set nombre = 'BETO_01' where id = a;
    raise exception 'FALLO: aceptó un nombre repetido (BETO_01 = Beto_01)';
  exception when unique_violation then null;
  end;

  -- 6) nombre_disponible
  select public.nombre_disponible('beto_01') into libre;
  if libre then raise exception 'FALLO: nombre_disponible dijo que beto_01 está libre'; end if;
  select public.nombre_disponible('Libre_99') into libre;
  if not libre then raise exception 'FALLO: nombre_disponible dijo que Libre_99 está ocupado'; end if;
  select public.nombre_disponible('ana_01') into libre;
  if not libre then raise exception 'FALLO: mi propio nombre debe contar como libre para mí'; end if;

  -- 7) Cambiar el nombre y el avatar funciona y queda registro en el historial
  update public.perfiles set nombre = 'Ana_02' where id = a;
  get diagnostics n = row_count;
  if n <> 1 then raise exception 'FALLO: A no pudo cambiar su nombre'; end if;
  update public.perfiles set avatar = 'AVATAR_8' where id = a;
  get diagnostics n = row_count;
  if n <> 1 then raise exception 'FALLO: A no pudo cambiar su avatar'; end if;
  select count(*) into n from public.historial_nombres where nombre_anterior = 'Ana_01' and nombre_nuevo = 'Ana_02';
  if n <> 1 then raise exception 'FALLO: el cambio de nombre no quedó en el historial'; end if;
  -- cambiar solo el avatar NO debe agregar filas al historial
  select count(*) into n from public.historial_nombres;
  if n <> 1 then raise exception 'FALLO: el historial tiene % filas y debía tener 1', n; end if;

  -- 8) Email y edad NO se pueden cambiar desde la app
  begin
    update public.perfiles set edad = 40 where id = a;
    raise exception 'FALLO: A pudo cambiar su edad';
  exception when insufficient_privilege then null;
  end;
  begin
    update public.perfiles set email = 'otro@levelia.test' where id = a;
    raise exception 'FALLO: A pudo cambiar su email';
  exception when insufficient_privilege then null;
  end;

  -- 9) Reglas de formato
  begin
    update public.perfiles set avatar = 'AVATAR_10' where id = a;
    raise exception 'FALLO: aceptó un avatar inexistente';
  exception when check_violation then null;
  end;
  begin
    update public.perfiles set nombre = 'Ana 02' where id = a;
    raise exception 'FALLO: aceptó un nombre con espacio';
  exception when check_violation then null;
  end;
  begin
    update public.perfiles set nombre = 'Al' where id = a;
    raise exception 'FALLO: aceptó un nombre de 2 letras';
  exception when check_violation then null;
  end;
  begin
    update public.perfiles set nombre = 'abcdefghijklmnopqrstu' where id = a;
    raise exception 'FALLO: aceptó un nombre de 21 letras';
  exception when check_violation then null;
  end;

  -- 10) A no puede escribir en progreso ni en historial, ni borrar su perfil directamente
  begin
    update public.progreso set xp = 999 where user_id = a;
    raise exception 'FALLO: A pudo subirse el XP';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.historial_nombres (user_id, nombre_anterior, nombre_nuevo) values (a, 'x', 'y');
    raise exception 'FALLO: A pudo escribir en el historial';
  exception when insufficient_privilege then null;
  end;
  begin
    delete from public.perfiles where id = a;
    raise exception 'FALLO: A pudo borrar su perfil directamente';
  exception when insufficient_privilege then null;
  end;

  -- 11) Un usuario SIN sesión (anon) no ve nada pero sí puede preguntar si un nombre está libre
  reset role;
  set local role anon;
  perform set_config('request.jwt.claims', '', true);
  begin
    perform 1 from public.perfiles;
    raise exception 'FALLO: anon pudo leer perfiles';
  exception when insufficient_privilege then null;
  end;
  select public.nombre_disponible('beto_01') into libre;
  if libre then raise exception 'FALLO: anon: nombre_disponible dijo que beto_01 está libre'; end if;
  begin
    perform public.eliminar_mi_cuenta();
    raise exception 'FALLO: anon pudo llamar eliminar_mi_cuenta';
  exception when insufficient_privilege then null;
  end;

  -- 12) A elimina su cuenta: desaparece TODO lo suyo y lo de B se queda
  reset role;
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  perform public.eliminar_mi_cuenta();
  reset role;

  select count(*) into n from auth.users where id = a;
  if n <> 0 then raise exception 'FALLO: el usuario A sigue en auth.users'; end if;
  select count(*) into n from public.perfiles where id = a;
  if n <> 0 then raise exception 'FALLO: el perfil de A sigue existiendo'; end if;
  select count(*) into n from public.progreso where user_id = a;
  if n <> 0 then raise exception 'FALLO: el progreso de A sigue existiendo'; end if;
  select count(*) into n from public.historial_nombres where user_id = a;
  if n <> 0 then raise exception 'FALLO: el historial de A sigue existiendo'; end if;
  select count(*) into n from public.perfiles where id = b;
  if n <> 1 then raise exception 'FALLO: se borró el perfil de B'; end if;

  raise exception 'PRUEBAS_OK (se revierte todo a propósito)';
end $$;
```

Luego confirma con execute_sql que no quedó basura:
`select (select count(*) from public.perfiles) as perfiles, (select count(*) from public.progreso) as progreso, (select count(*) from public.historial_nombres) as historial, (select count(*) from auth.users where email like '%@levelia.test') as usuarios_prueba;`
Si es un proyecto nuevo, todo debe dar 0. Si ya había datos reales, solo `usuarios_prueba` debe dar 0.

## Paso 4. Seguridad y revisión
- Ejecuta los advisors de seguridad (get_advisors, tipo security) y de rendimiento. Muéstrame cualquier alerta. Lo esperado: ninguna crítica. Si sale "leaked password protection disabled" o similar, anótalo para que yo lo active a mano.
- Lista las tablas del esquema public (list_tables) y confirma que `perfiles`, `progreso` y `historial_nombres` tienen RLS activado (rls_enabled = true).

## Paso 5. Entregarme los datos de conexión
Dame, en una tabla corta:
- Project URL (get_project_url)
- Clave PÚBLICA (la publishable/anon; usa la herramienta que la entregue). NUNCA la service_role.
- Referencia del proyecto (project ref) y región
- La contraseña de la base de datos (una sola vez, aquí en el chat; dime que la guarde en un gestor de contraseñas)

Dime exactamente dónde pegar URL y clave en la app: archivo `app/src/main/java/com/levelia/levelia/data/remote/SupabaseConfig.kt`, constantes `URL` y `CLAVE_PUBLICA`.

## Paso 6. Lo que yo debo hacer a mano (el MCP no lo puede)
Termina con esta lista, adaptada con el enlace directo al Dashboard de MI proyecto:
1. Authentication → Sign In / Providers → Email: dejar activo "Confirm email".
2. Authentication → Email Templates → "Confirm signup": el contenido debe incluir `{{ .Token }}`. Ejemplo de asunto: "Tu código de Levelia". Ejemplo de cuerpo: `<h2>¡Bienvenido a Levelia!</h2><p>Tu código de verificación es: <strong>{{ .Token }}</strong></p><p>Vence en 1 hora.</p>`
3. Authentication → Email Templates → "Reset password": igual, con `{{ .Token }}` (se usará cuando terminemos "recuperar contraseña" dentro de la app).
4. Authentication → SMTP Settings: sin SMTP propio solo se envían correos a miembros de mi organización y unos 2 por hora. Para probar con varias personas hay que configurar un SMTP.
5. Authentication → Sign In / Providers → opcional: activar "Prevent use of leaked passwords" si mi plan lo permite.

## Formato de tu respuesta final
Un resumen de máximo 15 líneas: qué se creó, resultado de las pruebas (PRUEBAS_OK sí/no), alertas de los advisors, los datos del Paso 5 y la lista del Paso 6. Nada más.
````

## 3. Qué hay que hacer después (en tu máquina)

1. Con el MCP ya habrás obtenido **URL** y **clave pública**. Pégalas en `SupabaseConfig.kt` (ya viene dentro del zip modificado, con textos de relleno que debes reemplazar).
2. Haz las 4–5 cosas manuales del Paso 6 en el Dashboard.
3. Sube a GitHub y compila el APK con el workflow (sección 7 del contexto).

> Si prefieres no usar el MCP: abre el **SQL Editor** del Dashboard, pega todo `supabase/setup.sql` y pulsa **Run**. Es lo mismo que hace el Paso 2. `supabase/pruebas.sql` es el Paso 3.

## 4. Por qué el esquema es así (por si te preguntan)

- **Tres tablas**: `perfiles` (lo que ve el Perfil), `progreso` (nivel y XP, que lee la tarjeta "Tu progreso") y `historial_nombres` (cada cambio de nombre de usuario). Usuarios, contraseñas, códigos OTP y sesiones los guarda Supabase Auth (`auth.users`); no se duplican.
- **Nombre de usuario único** sin distinguir mayúsculas (índice sobre `lower(nombre)`), de 3 a 20 caracteres, solo letras, números y guion bajo (sin espacios para poder mostrarlo como `@nombre`). Las mismas reglas están en `ReglasPerfil.kt` de la app.
- **`nombre_disponible(p_nombre)`**: la app la llama antes de registrarse y al editar el nombre, para avisar "ya está en uso" antes de intentar guardar.
- **`avatar`** guarda el nombre del enum (`AVATAR_1`…`AVATAR_9`), igual que `Avatar.entries` en `Models.kt`.
- **Permisos por columna**: desde la app solo se pueden cambiar `nombre` y `avatar` (`GRANT UPDATE (nombre, avatar)`); el correo y la edad no. `progreso` y `historial_nombres` son de solo lectura para la app: los llenan triggers del servidor.
- **RLS**: cada persona solo lee y edita sus propias filas.
- **`eliminar_mi_cuenta()`**: borra al usuario de Auth de quien la llama y, por `on delete cascade`, su perfil, su progreso y su historial. La app no tiene (ni debe tener) la clave `service_role`.
- **Trigger de progreso**: al crearse el perfil se crea su fila de progreso (nivel 1, 0 XP). El perfil no se crea al registrarse en Auth porque necesita nombre, edad y avatar, que se conocen hasta el paso 3 del registro.
- Cuando la app crezca (misiones, logros, personalización), se agregan como **nuevas migraciones**; no se toca esta.
