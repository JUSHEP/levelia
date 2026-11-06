-- =============================================================================
-- Levelia · módulo Personalización (versión 1). Se corre DESPUÉS de supabase/logros.sql (versión 6).
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run. Es idempotente.
-- Pruebas: supabase/pruebas_personalizacion.sql (debe terminar con un error que dice PRUEBAS_OK).
--
-- Qué crea:
--   esmeraldas_dificultad()  esmeraldas que da una misión al reclamarla, según su dificultad
--   skins                    catálogo: el guerrero por nivel (1, 5, 10, 25, 50, 100) y la tienda
--   skins_usuario            skins de cada usuario (las de nivel se desbloquean solas; visto = ya se avisó)
--   personaje                la skin equipada de cada usuario
--   movimientos_esmeraldas   cada esmeralda que entra (logros, misiones) o sale (compras); el saldo es la suma
--   evaluar_personalizacion_de() paga lo pendiente y desbloquea las skins de nivel (interna)
--   mi_personalizacion()     lo que dibujan Inicio y Personalización: saldo, nivel, skin equipada y catálogo
--   comprar_skin()           "Comprar" en la tienda: descuenta las esmeraldas y la deja equipada
--   equipar_skin()           "Equipar"
--   marcar_skins_vistas()    la app ya mostró "¡Skin desbloqueada!"
--
-- Reglas (acordadas con el equipo el 2026-11-05):
--   - Las esmeraldas son la única moneda. Se ganan con logros (columna logros.esmeraldas: bronce 50,
--     plata 100, oro 200) y al reclamar misiones (FACIL 10, MEDIA 20, DIFICIL 40, UNICA 10).
--     Los mismos valores están en la app (Dificultad.esmeraldas): si se cambia uno, cambiar el otro.
--   - Cada logro y cada misión se paga UNA sola vez (índice único). Lo ganado antes de instalar este
--     módulo también se paga, la primera vez que se evalúa al usuario.
--   - Skins de nivel: se desbloquean al llegar al nivel. La de nivel 1 (Novato) la tiene todo el mundo y es
--     la equipada por defecto. Skins de tienda: se compran una vez con esmeraldas y quedan equipadas.
--   - Las "habilidades" son solo texto descriptivo: no cambian la XP ni las reglas de las misiones.
--   - El saldo nunca queda negativo: comprar revisa el saldo con el personaje bloqueado.
--
-- La app solo LEE estas tablas; todo cambio pasa por las funciones (security definer).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Catálogo de skins (todos los usuarios con sesión lo leen)
-- -----------------------------------------------------------------------------
create table if not exists public.skins (
  id              text    primary key,
  nombre          text    not null,
  -- Panel "Habilidades": una línea por elemento, solo descriptivas
  habilidades     text[]  not null default '{}',
  tipo            text    not null check (tipo in ('NIVEL', 'TIENDA')),
  nivel_requerido integer check (nivel_requerido between 1 and 100),
  precio          integer check (precio > 0),
  orden           integer not null,
  -- NIVEL lleva nivel y no precio; TIENDA, al revés
  constraint skins_tipo_datos check (
    (tipo = 'NIVEL' and nivel_requerido is not null and precio is null) or
    (tipo = 'TIENDA' and precio is not null and nivel_requerido is null)
  )
);

comment on table public.skins is
  'Catálogo de skins del personaje: por nivel (se desbloquean solas) y de tienda (se compran con esmeraldas).';

alter table public.skins enable row level security;
revoke all on public.skins from anon, authenticated;
grant select on public.skins to authenticated;

drop policy if exists "skins_select" on public.skins;
create policy "skins_select" on public.skins for select to authenticated using (true);

-- Datos (se pueden editar aquí y volver a correr). Los dibujos están en la app: skin_<id en minúsculas>.png
insert into public.skins (id, nombre, habilidades, tipo, nivel_requerido, precio, orden) values
  ('NOVATO',    'Novato',    array['Acaba de empezar su aventura.', 'No tiene armadura: solo su ropa de viaje.', 'Pelea con un palo de madera.'],
                'NIVEL', 1, null, 1),
  ('ESCUDERO',  'Escudero',  array['Ya superó sus primeras misiones.', 'Chaleco y botas de cuero.', 'Lleva su primera espada de verdad.'],
                'NIVEL', 5, null, 2),
  ('SOLDADO',   'Soldado',   array['Constante y disciplinado.', 'Casco y cota de malla de hierro.', 'Espada de hierro y escudo de madera.'],
                'NIVEL', 10, null, 3),
  ('CABALLERO', 'Caballero', array['Un ejemplo para los demás aventureros.', 'Armadura completa de acero.', 'Escudo y espada de acero.'],
                'NIVEL', 25, null, 4),
  ('PALADIN',   'Paladín',   array['Su disciplina es famosa en todo el reino.', 'Armadura de plata con detalles dorados.', 'Capa azul y escudo con emblema.'],
                'NIVEL', 50, null, 5),
  ('LEYENDA',   'Leyenda',   array['Llegó al nivel máximo.', 'Armadura de oro y capa roja.', 'Espada legendaria que brilla.'],
                'NIVEL', 100, null, 6),
  ('ARQUERO',   'Arquero',   array['Vigila el bosque desde los árboles.', 'Capucha verde y arco largo.', 'Nunca falla un tiro.'],
                'TIENDA', null, 200, 7),
  ('VIKINGO',   'Vikingo',   array['Llegó del norte en su barco.', 'Casco con cuernos y capa de piel.', 'Hacha de guerra.'],
                'TIENDA', null, 300, 8),
  ('SAMURAI',   'Samurái',   array['Sigue el camino del honor.', 'Armadura roja de placas.', 'Katana afilada.'],
                'TIENDA', null, 450, 9),
  ('MAGO',      'Mago de batalla', array['Mezcla la espada con la magia.', 'Túnica morada y sombrero puntiagudo.', 'Bastón con una gema brillante.'],
                'TIENDA', null, 600, 10),
  ('NINJA',     'Ninja',     array['Se mueve entre las sombras.', 'Traje negro y bandana roja.', 'Espada corta en la espalda.'],
                'TIENDA', null, 800, 11),
  ('CABALLERO_OSCURO', 'Caballero oscuro', array['Nadie conoce su rostro.', 'Armadura negra de ojos rojos.', 'Espada de obsidiana.'],
                'TIENDA', null, 1200, 12)
on conflict (id) do update
  set nombre = excluded.nombre, habilidades = excluded.habilidades, tipo = excluded.tipo,
      nivel_requerido = excluded.nivel_requerido, precio = excluded.precio, orden = excluded.orden;

-- -----------------------------------------------------------------------------
-- 2. Lo de cada usuario (solo lo lee el dueño; lo escriben las funciones)
-- -----------------------------------------------------------------------------
create table if not exists public.skins_usuario (
  user_id     uuid        not null references public.perfiles (id) on delete cascade,
  skin_id     text        not null references public.skins (id) on delete cascade,
  obtenida_at timestamptz not null default now(),
  -- false = desbloqueada por nivel y todavía no se avisó en la app ("¡Skin desbloqueada!")
  visto       boolean     not null default false,
  primary key (user_id, skin_id)
);

create table if not exists public.personaje (
  user_id    uuid        primary key references public.perfiles (id) on delete cascade,
  -- La skin equipada. Si la borran del catálogo se borra la fila y se vuelve a crear con la inicial.
  skin_id    text        not null references public.skins (id) on delete cascade,
  updated_at timestamptz not null default now()
);

create table if not exists public.movimientos_esmeraldas (
  id         bigint      generated always as identity primary key,
  user_id    uuid        not null references public.perfiles (id) on delete cascade,
  -- Positivo = ganadas; negativo = gastadas
  cantidad   integer     not null check (cantidad <> 0),
  motivo     text        not null check (motivo in ('LOGRO', 'MISION', 'COMPRA')),
  -- Id del logro, de la misión aceptada (misiones_usuario.id) o de la skin comprada
  referencia text        not null,
  created_at timestamptz not null default now(),
  -- Así nada se paga ni se cobra dos veces
  unique (user_id, motivo, referencia)
);

comment on table public.movimientos_esmeraldas is
  'Cada esmeralda que entra o sale. El saldo es la suma. Solo se escribe con las funciones de personalizacion.sql.';

create index if not exists skins_usuario_skin_id on public.skins_usuario (skin_id);
create index if not exists personaje_skin_id on public.personaje (skin_id);

alter table public.skins_usuario enable row level security;
alter table public.personaje enable row level security;
alter table public.movimientos_esmeraldas enable row level security;
revoke all on public.skins_usuario, public.personaje, public.movimientos_esmeraldas from anon, authenticated;
grant select on public.skins_usuario, public.personaje, public.movimientos_esmeraldas to authenticated;

drop policy if exists "skins_usuario_select_propias" on public.skins_usuario;
create policy "skins_usuario_select_propias" on public.skins_usuario for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "personaje_select_propio" on public.personaje;
create policy "personaje_select_propio" on public.personaje for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "movimientos_esmeraldas_select_propios" on public.movimientos_esmeraldas;
create policy "movimientos_esmeraldas_select_propios" on public.movimientos_esmeraldas for select to authenticated
  using (user_id = (select auth.uid()));

-- -----------------------------------------------------------------------------
-- 3. Funciones internas (no las llama la app)
-- -----------------------------------------------------------------------------

-- Esmeraldas por misión reclamada (mismos valores que Dificultad.esmeraldas en domain/models/MisionesModels.kt)
create or replace function public.esmeraldas_dificultad(p_dificultad text)
returns integer
language sql
immutable
set search_path = ''
as $$
  select case p_dificultad when 'FACIL' then 10 when 'MEDIA' then 20 when 'DIFICIL' then 40 when 'UNICA' then 10 else 0 end
$$;

create or replace function public.saldo_esmeraldas(p_user uuid)
returns integer
language sql
stable
set search_path = ''
as $$
  select coalesce(sum(m.cantidad), 0)::integer from public.movimientos_esmeraldas m where m.user_id = p_user
$$;

-- Paga lo ganado que falte, desbloquea las skins de nivel y se asegura de que haya una skin equipada.
-- Se puede llamar cuantas veces sea: lo ya pagado o desbloqueado no se repite.
create or replace function public.evaluar_personalizacion_de(p_user uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_nivel   integer;
  v_inicial text;
begin
  -- 1) Logros obtenidos
  insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia)
  select lu.user_id, l.esmeraldas, 'LOGRO', l.id
  from public.logros_usuario lu
  join public.logros l on l.id = lu.logro_id
  where lu.user_id = p_user and l.esmeraldas > 0
  on conflict (user_id, motivo, referencia) do nothing;

  -- 2) Misiones cuya recompensa ya se reclamó
  insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia)
  select mu.user_id, public.esmeraldas_dificultad(mu.dificultad), 'MISION', mu.id::text
  from public.misiones_usuario mu
  where mu.user_id = p_user and mu.recompensa_reclamada and public.esmeraldas_dificultad(mu.dificultad) > 0
  on conflict (user_id, motivo, referencia) do nothing;

  -- 3) Skins de nivel alcanzadas. La inicial no se anuncia (visto = true)
  v_nivel := coalesce((select p.nivel from public.progreso p where p.user_id = p_user), 1);
  insert into public.skins_usuario (user_id, skin_id, visto)
  select p_user, s.id, s.nivel_requerido <= 1
  from public.skins s
  where s.tipo = 'NIVEL' and s.nivel_requerido <= v_nivel
  on conflict (user_id, skin_id) do nothing;

  -- 4) Skin equipada: la inicial si no hay ninguna o si la equipada ya no es suya
  v_inicial := (select s.id from public.skins s
                 where s.tipo = 'NIVEL' and s.nivel_requerido <= v_nivel
                 order by s.nivel_requerido, s.orden limit 1);
  if v_inicial is not null then
    insert into public.personaje as pj (user_id, skin_id) values (p_user, v_inicial)
    on conflict (user_id) do update set skin_id = excluded.skin_id, updated_at = now()
      where not exists (select 1 from public.skins_usuario su where su.user_id = pj.user_id and su.skin_id = pj.skin_id);
  end if;
end;
$$;

-- -----------------------------------------------------------------------------
-- 4. Funciones que usa la app. Los errores esperados usan códigos en MAYÚSCULAS que la app traduce
--    (PersonalizacionRepository).
-- -----------------------------------------------------------------------------

-- Saldo, nivel, skin equipada y el catálogo con lo que tiene el usuario. Antes evalúa, así el saldo y las
-- skins están al día aunque se hayan ganado en otra pantalla.
create or replace function public.mi_personalizacion(
  out esmeraldas integer,
  out nivel integer,
  out skin_equipada text,
  out skins jsonb
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  perform public.evaluar_personalizacion_de(v_id);

  esmeraldas := public.saldo_esmeraldas(v_id);
  nivel := coalesce((select p.nivel from public.progreso p where p.user_id = v_id), 1);
  skin_equipada := (select pj.skin_id from public.personaje pj where pj.user_id = v_id);
  skins := coalesce((
    select jsonb_agg(jsonb_build_object(
             'id', s.id, 'nombre', s.nombre, 'habilidades', to_jsonb(s.habilidades), 'tipo', s.tipo,
             'nivel_requerido', s.nivel_requerido, 'precio', s.precio, 'orden', s.orden,
             'obtenida_at', su.obtenida_at, 'visto', coalesce(su.visto, false))
           order by s.orden)
    from public.skins s
    left join public.skins_usuario su on su.skin_id = s.id and su.user_id = v_id
  ), '[]'::jsonb);
end;
$$;

-- "Comprar" (solo skins de tienda): descuenta el precio y la deja equipada.
create or replace function public.comprar_skin(p_skin_id text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id   uuid := auth.uid();
  v_skin public.skins;
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;

  select * into v_skin from public.skins where id = p_skin_id;
  if not found then raise exception 'SKIN_NO_EXISTE'; end if;
  if v_skin.tipo <> 'TIENDA' then raise exception 'SKIN_NO_SE_VENDE'; end if;

  -- Crea el personaje si no existe y paga lo pendiente (así cuenta todo lo ganado)
  perform public.evaluar_personalizacion_de(v_id);
  -- for update: dos compras a la vez esperan su turno y no gastan el mismo saldo
  perform 1 from public.personaje where user_id = v_id for update;

  if exists (select 1 from public.skins_usuario where user_id = v_id and skin_id = p_skin_id) then
    raise exception 'SKIN_YA_COMPRADA';
  end if;
  if public.saldo_esmeraldas(v_id) < v_skin.precio then raise exception 'ESMERALDAS_INSUFICIENTES'; end if;

  insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia)
  values (v_id, -v_skin.precio, 'COMPRA', v_skin.id);
  insert into public.skins_usuario (user_id, skin_id, visto) values (v_id, v_skin.id, true);
  update public.personaje set skin_id = v_skin.id, updated_at = now() where user_id = v_id;
end;
$$;

-- "Equipar": solo una skin que el usuario ya tiene
create or replace function public.equipar_skin(p_skin_id text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  if not exists (select 1 from public.skins where id = p_skin_id) then raise exception 'SKIN_NO_EXISTE'; end if;

  -- Desbloquea lo alcanzado antes de revisar (p. ej. justo después de subir de nivel)
  perform public.evaluar_personalizacion_de(v_id);
  if not exists (select 1 from public.skins_usuario where user_id = v_id and skin_id = p_skin_id) then
    raise exception 'SKIN_BLOQUEADA';
  end if;

  update public.personaje set skin_id = p_skin_id, updated_at = now() where user_id = v_id;
end;
$$;

-- La app ya mostró "¡Skin desbloqueada!": esas skins dejan de contar como nuevas
create or replace function public.marcar_skins_vistas()
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
  update public.skins_usuario set visto = true where user_id = v_id and not visto;
  get diagnostics v_n = row_count;
  return v_n;
end;
$$;

revoke all on function public.saldo_esmeraldas(uuid) from public, anon, authenticated;
revoke all on function public.evaluar_personalizacion_de(uuid) from public, anon, authenticated;
revoke all on function public.esmeraldas_dificultad(text) from public, anon;
revoke all on function public.mi_personalizacion() from public, anon;
revoke all on function public.comprar_skin(text) from public, anon;
revoke all on function public.equipar_skin(text) from public, anon;
revoke all on function public.marcar_skins_vistas() from public, anon;
grant execute on function public.esmeraldas_dificultad(text) to authenticated;
grant execute on function public.mi_personalizacion() to authenticated;
grant execute on function public.comprar_skin(text) to authenticated;
grant execute on function public.equipar_skin(text) to authenticated;
grant execute on function public.marcar_skins_vistas() to authenticated;
