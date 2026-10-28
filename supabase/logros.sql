-- =============================================================================
-- Levelia · módulo Logros (versión 5). Se corre DESPUÉS de supabase/misiones.sql.
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run. Es idempotente.
-- Pruebas: supabase/pruebas_logros.sql (debe terminar con un error que dice PRUEBAS_OK).
--
-- Qué crea:
--   categoria_mision()   FISICO / ESTUDIO / SOCIAL / OTRO, sacada del ícono de la misión
--   articulos            equipamiento del personaje (casco, pechera, espada, botas; básica, plata, oro)
--   logros               catálogo de medallas (familia y nivel 1–3) con el artículo que entregan
--   logros_requisitos    lo que pide cada logro (misiones, misiones de una categoría, racha, nivel, otro logro)
--   logros_usuario       qué logro obtuvo cada usuario y cuándo (visto = ya se le avisó en la app)
--   inventario           artículos ganados (equipado lo usará el módulo Personalización)
--   evaluar_logros_de()  desbloquea lo cumplido y entrega su artículo (la llaman registrar_progreso,
--                        reclamar_recompensas y mis_logros; la app no la llama directo)
--   mis_logros()         lo que dibujan las pantallas: avance de cada requisito, % general y siguiente medalla
--   marcar_logros_vistos() la app ya mostró "¡Logro desbloqueado!"
--
-- Reglas (acordadas con el equipo el 2026-10-28):
--   - Una misión cuenta al CUMPLIRLA (estado COMPLETADA), aunque su XP no se haya reclamado.
--   - La categoría sale del ícono: PESA = físico, CEREBRO = estudio, PERSONAS = social, PORTAPAPELES = otro.
--   - Racha = la mayor cantidad de días seguidos con al menos un "Aumentar progreso" (tabla registros_progreso
--     de misiones.sql; antes de esa tabla solo se conoce el último día de cada misión).
--   - Los logros no dan XP: dan un artículo de equipamiento.
--   - Las metas y artículos son datos de este archivo: se cambian aquí y se vuelve a correr.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Categoría de las misiones. Es una función y NO una columna nueva de `misiones`: la app lee esa tabla
--    con JSON estricto y una columna desconocida rompería la lista en las versiones ya instaladas.
-- -----------------------------------------------------------------------------
create or replace function public.categoria_mision(p_icono text)
returns text
language sql
immutable
set search_path = ''
as $$
  select case p_icono when 'PESA' then 'FISICO' when 'CEREBRO' then 'ESTUDIO' when 'PERSONAS' then 'SOCIAL' else 'OTRO' end
$$;

-- -----------------------------------------------------------------------------
-- 2. Catálogos (todos los usuarios con sesión los leen)
-- -----------------------------------------------------------------------------
create table if not exists public.articulos (
  id      text    primary key,
  nombre  text    not null,
  espacio text    not null check (espacio in ('CASCO', 'PECHERA', 'ESPADA', 'BOTAS')),
  calidad text    not null check (calidad in ('BASICA', 'PLATA', 'ORO')),
  orden   integer not null default 0
);

create table if not exists public.logros (
  id          text    primary key,
  titulo      text    not null,
  descripcion text    not null,
  familia     text    not null check (familia in ('GENERAL', 'FISICO', 'ESTUDIO', 'NIVEL')),
  nivel       integer not null check (nivel between 1 and 3),
  orden       integer not null,
  articulo_id text    references public.articulos (id)
);

create table if not exists public.logros_requisitos (
  id              bigint  generated always as identity primary key,
  logro_id        text    not null references public.logros (id) on delete cascade,
  orden           integer not null,
  tipo            text    not null check (tipo in ('MISIONES', 'MISIONES_CATEGORIA', 'RACHA_DIAS', 'NIVEL', 'LOGRO')),
  categoria       text    check (categoria in ('FISICO', 'ESTUDIO', 'SOCIAL', 'OTRO')),
  logro_requerido text    references public.logros (id),
  objetivo        integer not null check (objetivo > 0),
  unique (logro_id, orden),
  constraint logros_requisitos_categoria check ((tipo = 'MISIONES_CATEGORIA') = (categoria is not null)),
  constraint logros_requisitos_logro check ((tipo = 'LOGRO') = (logro_requerido is not null))
);

create index if not exists logros_requisitos_logro_requerido on public.logros_requisitos (logro_requerido);
create index if not exists logros_articulo_id on public.logros (articulo_id);

alter table public.articulos enable row level security;
alter table public.logros enable row level security;
alter table public.logros_requisitos enable row level security;
revoke all on public.articulos, public.logros, public.logros_requisitos from anon, authenticated;
grant select on public.articulos, public.logros, public.logros_requisitos to authenticated;

drop policy if exists "articulos_select" on public.articulos;
create policy "articulos_select" on public.articulos for select to authenticated using (true);
drop policy if exists "logros_select" on public.logros;
create policy "logros_select" on public.logros for select to authenticated using (true);
drop policy if exists "logros_requisitos_select" on public.logros_requisitos;
create policy "logros_requisitos_select" on public.logros_requisitos for select to authenticated using (true);

-- -----------------------------------------------------------------------------
-- 3. Lo de cada usuario (solo lo lee el dueño; lo escriben las funciones)
-- -----------------------------------------------------------------------------
create table if not exists public.logros_usuario (
  user_id     uuid        not null references public.perfiles (id) on delete cascade,
  logro_id    text        not null references public.logros (id) on delete cascade,
  obtenido_at timestamptz not null default now(),
  visto       boolean     not null default false,
  primary key (user_id, logro_id)
);

create table if not exists public.inventario (
  user_id     uuid        not null references public.perfiles (id) on delete cascade,
  articulo_id text        not null references public.articulos (id) on delete cascade,
  obtenido_at timestamptz not null default now(),
  equipado    boolean     not null default false,
  primary key (user_id, articulo_id)
);

create index if not exists logros_usuario_logro_id on public.logros_usuario (logro_id);
create index if not exists inventario_articulo_id on public.inventario (articulo_id);

alter table public.logros_usuario enable row level security;
alter table public.inventario enable row level security;
revoke all on public.logros_usuario, public.inventario from anon, authenticated;
grant select on public.logros_usuario, public.inventario to authenticated;

drop policy if exists "logros_usuario_select_propios" on public.logros_usuario;
create policy "logros_usuario_select_propios" on public.logros_usuario for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "inventario_select_propio" on public.inventario;
create policy "inventario_select_propio" on public.inventario for select to authenticated
  using (user_id = (select auth.uid()));

-- -----------------------------------------------------------------------------
-- 4. Datos: artículos, logros y requisitos (se pueden editar aquí y volver a correr)
-- -----------------------------------------------------------------------------
insert into public.articulos (id, nombre, espacio, calidad, orden) values
  ('CASCO_BASICO',   'Casco básico',     'CASCO',   'BASICA', 1),
  ('CASCO_PLATA',    'Casco de plata',   'CASCO',   'PLATA',  2),
  ('CASCO_ORO',      'Casco de oro',     'CASCO',   'ORO',    3),
  ('PECHERA_BASICA', 'Pechera básica',   'PECHERA', 'BASICA', 4),
  ('PECHERA_PLATA',  'Pechera de plata', 'PECHERA', 'PLATA',  5),
  ('PECHERA_ORO',    'Pechera de oro',   'PECHERA', 'ORO',    6),
  ('ESPADA_BASICA',  'Espada básica',    'ESPADA',  'BASICA', 7),
  ('ESPADA_PLATA',   'Espada de plata',  'ESPADA',  'PLATA',  8),
  ('ESPADA_ORO',     'Espada de oro',    'ESPADA',  'ORO',    9),
  ('BOTAS_BASICAS',  'Botas básicas',    'BOTAS',   'BASICA', 10)
on conflict (id) do update
  set nombre = excluded.nombre, espacio = excluded.espacio, calidad = excluded.calidad, orden = excluded.orden;

-- El orden es el de la pestaña "Todos" del diseño
insert into public.logros (id, titulo, descripcion, familia, nivel, orden, articulo_id) values
  ('PRIMEROS_PASOS',  '¡Primeros pasos!',    'Completa tu primera misión en la app.',                              'GENERAL', 1, 1,  'PECHERA_BASICA'),
  ('CALENTAMIENTO',   '¡Calentamiento!',     'Completa 3 misiones físicas.',                                       'FISICO',  1, 2,  'BOTAS_BASICAS'),
  ('ESTUDIOSO',       'Estudioso',           'Completa 3 misiones de estudio.',                                    'ESTUDIO', 1, 3,  'CASCO_BASICO'),
  ('PENSADOR',        '¡Pensador!',          'Completa 10 misiones de estudio y mantén una racha de 7 días.',      'ESTUDIO', 2, 4,  'CASCO_PLATA'),
  ('EN_FORMA',        '¡En forma!',          'Completa 10 misiones físicas y mantén una racha de 7 días.',         'FISICO',  2, 5,  'PECHERA_PLATA'),
  ('UNIDAD_ABSOLUTA', '¡¡Unidad absoluta!!', 'Completa 30 misiones físicas y mantén una racha de 30 días.',        'FISICO',  3, 6,  'PECHERA_ORO'),
  ('APRENDIZ',        'Aprendiz',            'Llega al nivel 5.',                                                  'NIVEL',   1, 7,  'ESPADA_BASICA'),
  ('JUNIOR',          '¡Junior!',            'Llega al nivel 10.',                                                 'NIVEL',   2, 8,  'ESPADA_PLATA'),
  ('SENIOR',          '¡¡Senior!!',          'Llega al nivel 25.',                                                 'NIVEL',   3, 9,  'ESPADA_ORO'),
  ('SABIO',           '¡¡Sabio!!',           'Completa 30 misiones de estudio y mantén una racha de 30 días.',     'ESTUDIO', 3, 10, 'CASCO_ORO')
on conflict (id) do update
  set titulo = excluded.titulo, descripcion = excluded.descripcion, familia = excluded.familia,
      nivel = excluded.nivel, orden = excluded.orden, articulo_id = excluded.articulo_id;

insert into public.logros_requisitos (logro_id, orden, tipo, categoria, logro_requerido, objetivo) values
  ('PRIMEROS_PASOS',  1, 'MISIONES',           null,      null,            1),
  ('CALENTAMIENTO',   1, 'MISIONES_CATEGORIA', 'FISICO',  null,            3),
  ('ESTUDIOSO',       1, 'MISIONES_CATEGORIA', 'ESTUDIO', null,            3),
  ('PENSADOR',        1, 'MISIONES_CATEGORIA', 'ESTUDIO', null,            10),
  ('PENSADOR',        2, 'RACHA_DIAS',         null,      null,            7),
  ('PENSADOR',        3, 'LOGRO',              null,      'ESTUDIOSO',     1),
  ('EN_FORMA',        1, 'MISIONES_CATEGORIA', 'FISICO',  null,            10),
  ('EN_FORMA',        2, 'RACHA_DIAS',         null,      null,            7),
  ('EN_FORMA',        3, 'LOGRO',              null,      'CALENTAMIENTO', 1),
  ('UNIDAD_ABSOLUTA', 1, 'MISIONES_CATEGORIA', 'FISICO',  null,            30),
  ('UNIDAD_ABSOLUTA', 2, 'RACHA_DIAS',         null,      null,            30),
  ('UNIDAD_ABSOLUTA', 3, 'LOGRO',              null,      'EN_FORMA',      1),
  ('APRENDIZ',        1, 'NIVEL',              null,      null,            5),
  ('JUNIOR',          1, 'NIVEL',              null,      null,            10),
  ('JUNIOR',          2, 'LOGRO',              null,      'APRENDIZ',      1),
  ('SENIOR',          1, 'NIVEL',              null,      null,            25),
  ('SENIOR',          2, 'LOGRO',              null,      'JUNIOR',        1),
  ('SABIO',           1, 'MISIONES_CATEGORIA', 'ESTUDIO', null,            30),
  ('SABIO',           2, 'RACHA_DIAS',         null,      null,            30),
  ('SABIO',           3, 'LOGRO',              null,      'PENSADOR',      1)
on conflict (logro_id, orden) do update
  set tipo = excluded.tipo, categoria = excluded.categoria, logro_requerido = excluded.logro_requerido,
      objetivo = excluded.objetivo;

-- -----------------------------------------------------------------------------
-- 5. Funciones internas (no las llama la app)
-- -----------------------------------------------------------------------------

-- Mayor cantidad de días seguidos con al menos un registro de progreso
create or replace function public.racha_maxima(p_user uuid)
returns integer
language sql
stable
set search_path = ''
as $$
  select coalesce(max(dias), 0)::integer
  from (
    select count(*) as dias
    from (
      -- Días seguidos comparten el mismo "fecha - número de fila"
      select d.fecha - (row_number() over (order by d.fecha))::integer as grupo
      from (select distinct rp.fecha from public.registros_progreso rp where rp.user_id = p_user) d
    ) g
    group by g.grupo
  ) rachas
$$;

-- Cuánto lleva el usuario en un requisito (se compara contra su objetivo)
create or replace function public.valor_requisito(p_user uuid, p_tipo text, p_categoria text, p_logro text)
returns integer
language sql
stable
set search_path = ''
as $$
  select case p_tipo
    when 'MISIONES' then
      (select count(*) from public.misiones_usuario mu where mu.user_id = p_user and mu.estado = 'COMPLETADA')::integer
    when 'MISIONES_CATEGORIA' then
      (select count(*) from public.misiones_usuario mu join public.misiones m on m.id = mu.mision_id
        where mu.user_id = p_user and mu.estado = 'COMPLETADA' and public.categoria_mision(m.icono) = p_categoria)::integer
    when 'RACHA_DIAS' then public.racha_maxima(p_user)
    when 'NIVEL' then coalesce((select pr.nivel from public.progreso pr where pr.user_id = p_user), 1)
    when 'LOGRO' then
      (select count(*) from public.logros_usuario lu where lu.user_id = p_user and lu.logro_id = p_logro)::integer
    else 0
  end
$$;

-- Desbloquea los logros cumplidos y entrega sus artículos. Repite hasta que no haya nuevos, porque un
-- logro puede pedir otro que se acaba de desbloquear (Aprendiz → ¡Junior! en la misma subida de nivel).
create or replace function public.evaluar_logros_de(p_user uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_total integer := 0;
  v_n     integer;
begin
  loop
    with nuevos as (
      insert into public.logros_usuario (user_id, logro_id)
      select p_user, l.id
      from public.logros l
      where not exists (select 1 from public.logros_usuario lu where lu.user_id = p_user and lu.logro_id = l.id)
        and exists (select 1 from public.logros_requisitos r where r.logro_id = l.id)
        and not exists (
          select 1 from public.logros_requisitos r
          where r.logro_id = l.id
            and public.valor_requisito(p_user, r.tipo, r.categoria, r.logro_requerido) < r.objetivo
        )
      on conflict do nothing
      returning logro_id
    ), premios as (
      insert into public.inventario (user_id, articulo_id)
      select p_user, l.articulo_id from nuevos n join public.logros l on l.id = n.logro_id
      where l.articulo_id is not null
      on conflict do nothing
      returning 1
    )
    select count(*) into v_n from nuevos;
    v_total := v_total + v_n;
    exit when v_n = 0;
  end loop;
  return v_total;
end;
$$;

-- -----------------------------------------------------------------------------
-- 6. Funciones que usa la app
-- -----------------------------------------------------------------------------

-- Todos los logros con el avance del usuario. Antes evalúa, así también ven sus logros quienes ya
-- habían cumplido misiones antes de que existiera este módulo.
create or replace function public.mis_logros()
returns table (
  id text, titulo text, descripcion text, familia text, nivel integer, orden integer,
  articulo_id text, articulo_nombre text, articulo_espacio text, articulo_calidad text,
  obtenido_at timestamptz, visto boolean, requisitos jsonb, progreso integer,
  siguiente_id text, avance_siguiente integer
)
language plpgsql
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  perform public.evaluar_logros_de(v_id);

  return query
  with req as (
    select r.logro_id, r.orden, r.tipo, r.categoria, r.logro_requerido, rl.titulo as titulo_requerido, r.objetivo,
           least(public.valor_requisito(v_id, r.tipo, r.categoria, r.logro_requerido), r.objetivo) as actual
    from public.logros_requisitos r
    left join public.logros rl on rl.id = r.logro_requerido
  ), base as (
    select l.id, l.titulo, l.descripcion, l.familia, l.nivel, l.orden, l.articulo_id, lu.obtenido_at, lu.visto,
           coalesce((
             select jsonb_agg(jsonb_build_object(
                      'tipo', q.tipo, 'categoria', q.categoria, 'logro_requerido', q.logro_requerido,
                      'titulo_requerido', q.titulo_requerido, 'actual', q.actual, 'objetivo', q.objetivo)
                    order by q.orden)
             from req q where q.logro_id = l.id), '[]'::jsonb) as requisitos,
           -- % general: promedio de lo cumplido de cada requisito (desbloqueado = 100)
           case when lu.logro_id is not null then 100
                else coalesce((select round(avg(q.actual::numeric / q.objetivo) * 100) from req q
                               where q.logro_id = l.id), 0)::integer
           end as progreso
    from public.logros l
    left join public.logros_usuario lu on lu.logro_id = l.id and lu.user_id = v_id
  )
  select b.id, b.titulo, b.descripcion, b.familia, b.nivel, b.orden, b.articulo_id,
         a.nombre, a.espacio, a.calidad, b.obtenido_at, coalesce(b.visto, false), b.requisitos, b.progreso,
         -- Siguiente medalla de la misma familia ("Avance para el siguiente nivel")
         s.id, s.progreso
  from base b
  left join public.articulos a on a.id = b.articulo_id
  left join base s on s.familia = b.familia and s.nivel = b.nivel + 1 and b.familia <> 'GENERAL'
  order by b.orden;
end;
$$;

-- La app ya avisó "¡Logro desbloqueado!": esos logros dejan de contar como nuevos
create or replace function public.marcar_logros_vistos()
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
  update public.logros_usuario set visto = true where user_id = v_id and not visto;
  get diagnostics v_n = row_count;
  return v_n;
end;
$$;

revoke all on function public.racha_maxima(uuid) from public, anon, authenticated;
revoke all on function public.valor_requisito(uuid, text, text, text) from public, anon, authenticated;
revoke all on function public.evaluar_logros_de(uuid) from public, anon, authenticated;
revoke all on function public.mis_logros() from public, anon;
revoke all on function public.marcar_logros_vistos() from public, anon;
grant execute on function public.mis_logros() to authenticated;
grant execute on function public.categoria_mision(text) to authenticated;
grant execute on function public.marcar_logros_vistos() to authenticated;
