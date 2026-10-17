-- =============================================================================
-- Levelia · esquema de Supabase (versión 2: módulo Perfil)
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run.
--
-- OJO (2026-10-15): el proyecto real `levelia` se creó con la migración
-- crear_esquema_levelia_v2_perfil, no con este archivo. Diferencias: allí `progreso`
-- NO tiene xp_siguiente_nivel ni created_at (la app usa 1000 por defecto). La regla del
-- nombre (sin tildes ni ñ) sí es la misma. No correr este archivo sobre ese proyecto sin revisarlo.
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
  check (nombre ~ '^[A-Za-z0-9_]{3,20}$') not valid;

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
