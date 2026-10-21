-- =============================================================================
-- Levelia · módulo Misiones (versión 3). Se corre DESPUÉS del esquema del Perfil.
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run. Es idempotente.
-- Pruebas: supabase/pruebas_misiones.sql (debe terminar con un error que dice PRUEBAS_OK).
--
-- Qué crea:
--   misiones           catálogo: las predefinidas (user_id null) y las que crea cada usuario
--   misiones_usuario   cada misión ACEPTADA por un usuario, con su progreso y estado
--   xp_para_subir()    XP que pide un nivel para pasar al siguiente (curva exponencial)
--   crear_mision()     "Crea tu propia misión"
--   aceptar_mision()   "Aceptar misión" (opcionalmente con la dificultad de "Ajustar dificultad")
--   registrar_progreso() "Registrar progreso +"
--   vencer_mis_misiones() marca como perdidas las que pasaron su fecha límite
--   reclamar_recompensas() "¡Reclamar recompensa!": suma la XP y sube de nivel
--
-- Reglas (acordadas con el equipo el 2026-10-21):
--   - Dificultades: FACIL 1 vez/semana +75 XP, MEDIA 3 +150, DIFICIL 5 +300, UNICA 1 +50.
--   - Por veces: máximo 1 registro por día y 7 días desde que se acepta; si no llega, PERDIDA.
--     Se puede volver a aceptar cuando terminan esos 7 días y su XP ya se reclamó (UNICA, una sola vez).
--   - UNICA: tiene fecha límite; registrar progreso hasta esa fecha la cumple, después queda PERDIDA.
--   - La XP se suma al reclamar, no al cumplir.
--   - Nivel n → n+1 pide round(100 · n^1.5) XP (100, 283, 520 … ≈98 500 del 99 al 100). Máximo nivel 100.
--     La misma fórmula está en la app (domain/ReglasNivel.kt): si se cambia una, cambiar la otra.
--
-- La app solo LEE estas tablas; todo cambio pasa por las funciones (security definer).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Funciones comunes
-- -----------------------------------------------------------------------------

-- "Hoy" según la hora de Colombia (la base corre en UTC: a las 8 p. m. ya sería mañana)
-- shortcut: zona horaria fija del equipo; si la app se usa en otro país, guardar la zona en perfiles
create or replace function public.hoy()
returns date
language sql
stable
set search_path = ''
as $$ select (now() at time zone 'America/Bogota')::date $$;

create or replace function public.xp_para_subir(p_nivel integer)
returns integer
language sql
immutable
set search_path = ''
as $$ select round(100 * power(greatest(p_nivel, 1)::numeric, 1.5))::integer $$;

grant execute on function public.hoy() to authenticated;
grant execute on function public.xp_para_subir(integer) to anon, authenticated;

-- -----------------------------------------------------------------------------
-- 1. misiones (catálogo)
-- -----------------------------------------------------------------------------
create table if not exists public.misiones (
  id           bigint      generated always as identity primary key,
  -- null = predefinida (la ven todos); si no, solo la ve quien la creó
  user_id      uuid        references public.perfiles (id) on delete cascade,
  titulo       text        not null check (char_length(btrim(titulo)) between 1 and 40),
  descripcion  text        not null default '' check (char_length(descripcion) <= 300),
  icono        text        not null default 'PORTAPAPELES'
                           check (icono in ('PESA', 'CEREBRO', 'PORTAPAPELES', 'PERSONAS')),
  dificultad   text        not null check (dificultad in ('FACIL', 'MEDIA', 'DIFICIL', 'UNICA')),
  meta         integer     not null check (meta between 1 and 7),
  xp           integer     not null check (xp > 0),
  fecha_limite date,
  created_at   timestamptz not null default now(),
  -- Solo la dificultad UNICA tiene fecha límite
  constraint misiones_fecha_solo_unica check ((dificultad = 'UNICA') = (fecha_limite is not null))
);

comment on table public.misiones is
  'Catálogo de misiones. user_id null = predefinida. Solo se escribe con crear_mision().';

create unique index if not exists misiones_predefinida_titulo on public.misiones (titulo) where user_id is null;
create index if not exists misiones_user_id on public.misiones (user_id);

alter table public.misiones enable row level security;
revoke all on public.misiones from anon, authenticated;
grant select on public.misiones to authenticated;

drop policy if exists "misiones_select_visibles" on public.misiones;
create policy "misiones_select_visibles"
  on public.misiones for select to authenticated
  using (user_id is null or user_id = (select auth.uid()));

-- Misiones predefinidas (las del diseño). Se pueden editar aquí y volver a correr el archivo: se actualizan
-- (las ya aceptadas conservan los valores con que se aceptaron).
insert into public.misiones (titulo, descripcion, icono, dificultad, meta, xp) values
  ('Ir al gym',          'Mejora tu estado físico.', 'PESA',         'MEDIA', 3, 150),
  ('A clases',           'Asiste a la universidad.',                            'CEREBRO',      'MEDIA', 2, 100),
  ('Modo enfoque',       'Estudia 2 horas.',                                    'PORTAPAPELES', 'FACIL', 2, 75),
  ('Sígueme y te sigo',  'Interactúa con 3 extraños.',                          'PERSONAS',     'MEDIA', 3, 100)
on conflict (titulo) where user_id is null do update
  set descripcion = excluded.descripcion, icono = excluded.icono, dificultad = excluded.dificultad,
      meta = excluded.meta, xp = excluded.xp;

-- -----------------------------------------------------------------------------
-- 2. misiones_usuario (misiones aceptadas)
-- -----------------------------------------------------------------------------
create table if not exists public.misiones_usuario (
  id                   bigint      generated always as identity primary key,
  user_id              uuid        not null references public.perfiles (id) on delete cascade,
  mision_id            bigint      not null references public.misiones (id) on delete cascade,
  -- Copia al aceptar: "Ajustar dificultad" cambia esto sin tocar el catálogo
  dificultad           text        not null check (dificultad in ('FACIL', 'MEDIA', 'DIFICIL', 'UNICA')),
  meta                 integer     not null check (meta between 1 and 7),
  xp                   integer     not null check (xp > 0),
  progreso             integer     not null default 0,
  fecha_limite         date        not null,
  ultimo_registro      date,
  estado               text        not null default 'ACTIVA' check (estado in ('ACTIVA', 'COMPLETADA', 'PERDIDA')),
  recompensa_reclamada boolean     not null default false,
  aceptada_at          timestamptz not null default now(),
  terminada_at         timestamptz,
  constraint misiones_usuario_progreso_valido check (progreso between 0 and meta)
);

comment on table public.misiones_usuario is
  'Misiones aceptadas por cada usuario. Solo se escribe con aceptar_mision(), registrar_progreso() y reclamar_recompensas().';

-- Una misión no puede estar dos veces ACTIVA para el mismo usuario (sí se puede repetir después)
create unique index if not exists misiones_usuario_una_activa
  on public.misiones_usuario (user_id, mision_id) where estado = 'ACTIVA';
create index if not exists misiones_usuario_user_id on public.misiones_usuario (user_id);
create index if not exists misiones_usuario_mision_id on public.misiones_usuario (mision_id);

alter table public.misiones_usuario enable row level security;
revoke all on public.misiones_usuario from anon, authenticated;
grant select on public.misiones_usuario to authenticated;

drop policy if exists "misiones_usuario_select_propias" on public.misiones_usuario;
create policy "misiones_usuario_select_propias"
  on public.misiones_usuario for select to authenticated
  using (user_id = (select auth.uid()));

-- -----------------------------------------------------------------------------
-- 3. Funciones que usa la app
-- Los errores esperados usan códigos en MAYÚSCULAS que la app traduce (MisionesRepository).
-- -----------------------------------------------------------------------------

-- Veces y XP de cada dificultad (mismos valores que domain/models/MisionesModels.kt)
create or replace function public.datos_dificultad(p_dificultad text, out meta integer, out xp integer)
language sql
immutable
set search_path = ''
as $$
  select v.meta, v.xp
  from (values ('FACIL', 1, 75), ('MEDIA', 3, 150), ('DIFICIL', 5, 300), ('UNICA', 1, 50)) as v (dificultad, meta, xp)
  where v.dificultad = p_dificultad
$$;

-- Marca como PERDIDA lo que pasó su fecha límite sin cumplirse. La app la llama antes de leer.
create or replace function public.vencer_mis_misiones()
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
  v_n  integer;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  update public.misiones_usuario
     set estado = 'PERDIDA', terminada_at = now()
   where user_id = v_id and estado = 'ACTIVA' and fecha_limite < public.hoy();
  get diagnostics v_n = row_count;
  return v_n;
end;
$$;

create or replace function public.crear_mision(
  p_titulo text,
  p_descripcion text,
  p_dificultad text,
  p_fecha_limite date default null
)
returns public.misiones
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id   uuid := auth.uid();
  v_dif  record;
  v_fila public.misiones;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  if char_length(btrim(coalesce(p_titulo, ''))) = 0 then raise exception 'TITULO_VACIO'; end if;
  if char_length(btrim(p_titulo)) > 40 then raise exception 'TITULO_LARGO'; end if;
  if char_length(coalesce(p_descripcion, '')) > 300 then raise exception 'DESCRIPCION_LARGA'; end if;

  -- datos_dificultad siempre devuelve una fila (con nulos si la dificultad no existe)
  select * into v_dif from public.datos_dificultad(p_dificultad);
  if v_dif.meta is null then raise exception 'DIFICULTAD_INVALIDA'; end if;

  if p_dificultad = 'UNICA' then
    if p_fecha_limite is null then raise exception 'FECHA_REQUERIDA'; end if;
    if p_fecha_limite < public.hoy() then raise exception 'FECHA_PASADA'; end if;
  end if;

  insert into public.misiones (user_id, titulo, descripcion, dificultad, meta, xp, fecha_limite)
  values (v_id, btrim(p_titulo), btrim(coalesce(p_descripcion, '')), p_dificultad, v_dif.meta, v_dif.xp,
          case when p_dificultad = 'UNICA' then p_fecha_limite end)
  returning * into v_fila;
  return v_fila;
end;
$$;

-- p_dificultad null = la de la misión. Otra dificultad ("Ajustar dificultad") usa los valores de la tabla.
create or replace function public.aceptar_mision(p_mision_id bigint, p_dificultad text default null)
returns public.misiones_usuario
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id     uuid := auth.uid();
  v_mision public.misiones;
  v_dif    text;
  v_meta   integer;
  v_xp     integer;
  v_fila   public.misiones_usuario;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;

  select * into v_mision from public.misiones
   where id = p_mision_id and (user_id is null or user_id = v_id);
  if not found then raise exception 'MISION_NO_EXISTE'; end if;

  perform public.vencer_mis_misiones();
  if exists (select 1 from public.misiones_usuario
              where user_id = v_id and mision_id = p_mision_id and estado = 'ACTIVA') then
    raise exception 'MISION_YA_ACTIVA';
  end if;
  -- Sin esto se podría cumplir, reclamar y volver a aceptar la misma misión varias veces al día
  if exists (select 1 from public.misiones_usuario
              where user_id = v_id and mision_id = p_mision_id and estado = 'COMPLETADA' and not recompensa_reclamada) then
    raise exception 'RECOMPENSA_PENDIENTE';
  end if;
  if v_mision.dificultad = 'UNICA' and exists (select 1 from public.misiones_usuario
                                                where user_id = v_id and mision_id = p_mision_id) then
    raise exception 'MISION_YA_HECHA';
  end if;
  if exists (select 1 from public.misiones_usuario
              where user_id = v_id and mision_id = p_mision_id and fecha_limite >= public.hoy()) then
    raise exception 'MISION_EN_CURSO';
  end if;

  v_dif := coalesce(p_dificultad, v_mision.dificultad);
  if v_dif = v_mision.dificultad then
    v_meta := v_mision.meta;
    v_xp := v_mision.xp;
  else
    -- UNICA necesita una fecha: solo se acepta así si la misión ya era UNICA
    if v_dif = 'UNICA' or v_mision.dificultad = 'UNICA' then raise exception 'DIFICULTAD_INVALIDA'; end if;
    select d.meta, d.xp into v_meta, v_xp from public.datos_dificultad(v_dif) d;
    if v_meta is null then raise exception 'DIFICULTAD_INVALIDA'; end if;
  end if;

  if v_dif = 'UNICA' and v_mision.fecha_limite < public.hoy() then raise exception 'FECHA_PASADA'; end if;

  insert into public.misiones_usuario (user_id, mision_id, dificultad, meta, xp, fecha_limite)
  values (v_id, p_mision_id, v_dif, v_meta, v_xp,
          -- Por veces: 7 días contando el de hoy
          case when v_dif = 'UNICA' then v_mision.fecha_limite else public.hoy() + 6 end)
  returning * into v_fila;
  return v_fila;
end;
$$;

create or replace function public.registrar_progreso(p_id bigint)
returns public.misiones_usuario
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id   uuid := auth.uid();
  v_fila public.misiones_usuario;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;

  -- for update: dos toques seguidos no suman dos veces
  select * into v_fila from public.misiones_usuario where id = p_id and user_id = v_id for update;
  if not found then raise exception 'MISION_NO_EXISTE'; end if;
  if v_fila.estado <> 'ACTIVA' then raise exception 'MISION_TERMINADA'; end if;

  -- Pasó la fecha límite: queda perdida (UNICA se valida aquí contra la fecha objetivo)
  if v_fila.fecha_limite < public.hoy() then
    update public.misiones_usuario set estado = 'PERDIDA', terminada_at = now()
     where id = p_id returning * into v_fila;
    return v_fila;
  end if;

  if v_fila.ultimo_registro = public.hoy() then raise exception 'YA_REGISTRADO_HOY'; end if;

  update public.misiones_usuario
     set progreso        = progreso + 1,
         ultimo_registro = public.hoy(),
         estado          = case when progreso + 1 >= meta then 'COMPLETADA' else 'ACTIVA' end,
         terminada_at    = case when progreso + 1 >= meta then now() end
   where id = p_id
  returning * into v_fila;
  return v_fila;
end;
$$;

-- Suma la XP de todas las misiones COMPLETADAS sin reclamar y sube los niveles que alcance.
create or replace function public.reclamar_recompensas(
  out xp_ganada integer,
  out niveles_subidos integer,
  out nivel integer,
  out xp integer
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
  v_nivel_inicial integer;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;

  with reclamadas as (
    update public.misiones_usuario mu set recompensa_reclamada = true
     where mu.user_id = v_id and mu.estado = 'COMPLETADA' and not mu.recompensa_reclamada
    returning mu.xp
  )
  select coalesce(sum(r.xp), 0)::integer into xp_ganada from reclamadas r;

  -- for update: dos reclamos a la vez no se pisan
  select p.nivel, p.xp into nivel, xp from public.progreso p where p.user_id = v_id for update;
  if not found then
    insert into public.progreso (user_id) values (v_id) returning progreso.nivel, progreso.xp into nivel, xp;
  end if;
  v_nivel_inicial := nivel;

  xp := xp + xp_ganada;
  while nivel < 100 and xp >= public.xp_para_subir(nivel) loop
    xp := xp - public.xp_para_subir(nivel);
    nivel := nivel + 1;
  end loop;
  niveles_subidos := nivel - v_nivel_inicial;

  if xp_ganada > 0 then
    update public.progreso p set nivel = reclamar_recompensas.nivel, xp = reclamar_recompensas.xp
     where p.user_id = v_id;
  end if;
end;
$$;

revoke all on function public.datos_dificultad(text) from public, anon;
revoke all on function public.vencer_mis_misiones() from public, anon;
revoke all on function public.crear_mision(text, text, text, date) from public, anon;
revoke all on function public.aceptar_mision(bigint, text) from public, anon;
revoke all on function public.registrar_progreso(bigint) from public, anon;
revoke all on function public.reclamar_recompensas() from public, anon;
grant execute on function public.datos_dificultad(text) to authenticated;
grant execute on function public.vencer_mis_misiones() to authenticated;
grant execute on function public.crear_mision(text, text, text, date) to authenticated;
grant execute on function public.aceptar_mision(bigint, text) to authenticated;
grant execute on function public.registrar_progreso(bigint) to authenticated;
grant execute on function public.reclamar_recompensas() to authenticated;
