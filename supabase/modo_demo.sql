-- =============================================================================
-- Levelia · Modo demo (versión 1). Se corre AL FINAL, después de supabase/personalizacion.sql.
--
-- Sirve para mostrar la app (por ejemplo al profesor) sin esperar días: una cuenta marcada como demo ve
-- "Modo demo" en el menú de Inicio y desde ahí adelanta días, sube de nivel y suma esmeraldas. Las demás
-- cuentas no ven la opción y el servidor les rechaza estas funciones (NO_ES_DEMO).
--
-- Cómo usarlo: Supabase → SQL Editor → pegar TODO → Run. Es idempotente.
-- Pruebas: supabase/pruebas_modo_demo.sql (debe terminar con un error que dice PRUEBAS_OK).
--
-- Marcar una cuenta como demo (cambiar NombreDeUsuario por el nombre de la cuenta en la app):
--   insert into public.cuentas_demo (user_id)
--   select id from public.perfiles where lower(nombre) = lower('NombreDeUsuario')
--   on conflict do nothing;
-- Quitarle el modo demo:
--   delete from public.cuentas_demo
--   where user_id = (select id from public.perfiles where lower(nombre) = lower('NombreDeUsuario'));
--
-- Qué crea:
--   cuentas_demo             cuentas con modo demo (solo se llena desde el SQL Editor, nunca desde la app)
--   mi_modo_demo()           la app pregunta si la cuenta es demo y cuántos días lleva adelantados
--   demo_avanzar_dia()       "Avanzar 1 día": corre un día hacia atrás las fechas de las misiones de la cuenta,
--                            que es lo mismo que si hubiera pasado un día (vuelve a dejar registrar progreso,
--                            vencen los plazos y las rachas siguen). La app y la base no cambian su fecha.
--   demo_preparar_nivel(n)   deja la cuenta en el nivel n − 1 a 1 XP de subir: la siguiente misión que se
--                            reclame la sube al nivel n y salen los avisos de logro y de skin, como de verdad
--   demo_subir_nivel()       sube un nivel ya, con sus logros y skins
--   demo_sumar_esmeraldas()  +500 esmeraldas para probar la tienda
--   demo_reiniciar()         deja la cuenta como nueva: nivel 1, sin misiones, logros, skins ni esmeraldas
--
-- Ninguna de estas funciones existe para las cuentas normales: todo pasa por exigir_cuenta_demo().
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Cuentas demo. La app no lee la tabla (usa mi_modo_demo()) ni puede escribirla.
-- -----------------------------------------------------------------------------
create table if not exists public.cuentas_demo (
  user_id          uuid        primary key references public.perfiles (id) on delete cascade,
  -- Solo informativo: cuántas veces se usó "Avanzar 1 día" desde el último reinicio
  dias_adelantados integer     not null default 0 check (dias_adelantados >= 0),
  created_at       timestamptz not null default now()
);

comment on table public.cuentas_demo is
  'Cuentas con modo demo (supabase/modo_demo.sql). Se llena a mano desde el SQL Editor.';

alter table public.cuentas_demo enable row level security;
revoke all on public.cuentas_demo from anon, authenticated;

-- Las esmeraldas regaladas por el modo demo quedan en el historial con su propio motivo
alter table public.movimientos_esmeraldas drop constraint if exists movimientos_esmeraldas_motivo_check;
alter table public.movimientos_esmeraldas add constraint movimientos_esmeraldas_motivo_check
  check (motivo in ('LOGRO', 'MISION', 'COMPRA', 'DEMO'));

-- -----------------------------------------------------------------------------
-- 2. Funciones. Los errores esperados usan códigos en MAYÚSCULAS que la app traduce (ModoDemoRepository).
-- -----------------------------------------------------------------------------

-- La cuenta con sesión, si es demo; si no, error (interna)
create or replace function public.exigir_cuenta_demo()
returns uuid
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  if not exists (select 1 from public.cuentas_demo where user_id = v_id) then raise exception 'NO_ES_DEMO'; end if;
  return v_id;
end;
$$;

-- ¿Es demo? Para cualquier cuenta: a las normales les responde activo = false (no es un error)
create or replace function public.mi_modo_demo(out activo boolean, out dias integer)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then raise exception 'SIN_SESION' using errcode = '28000'; end if;
  select cd.dias_adelantados into dias from public.cuentas_demo cd where cd.user_id = v_id;
  activo := found;
  dias := coalesce(dias, 0);
end;
$$;

-- "Avanzar 1 día"
create or replace function public.demo_avanzar_dia()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := public.exigir_cuenta_demo();
  r    record;
begin
  -- registros_progreso no deja dos registros de la misma misión el mismo día: se mueven de la fecha más vieja
  -- a la más nueva, así cada uno pasa a un día que ya quedó libre
  for r in select rp.id from public.registros_progreso rp where rp.user_id = v_id order by rp.fecha loop
    update public.registros_progreso set fecha = fecha - 1 where id = r.id;
  end loop;

  update public.misiones_usuario
     set fecha_limite    = fecha_limite - 1,
         ultimo_registro = ultimo_registro - 1,
         aceptada_at     = aceptada_at - interval '1 day',
         terminada_at    = terminada_at - interval '1 day'
   where user_id = v_id;

  -- La fecha objetivo de sus misiones únicas también se acerca un día
  update public.misiones set fecha_limite = fecha_limite - 1 where user_id = v_id and fecha_limite is not null;

  update public.cuentas_demo set dias_adelantados = dias_adelantados + 1 where user_id = v_id;
  -- Lo que pasó su plazo queda perdido, como cuando de verdad cambia el día
  perform public.vencer_mis_misiones();
end;
$$;

-- "A punto de subir al nivel n": nivel n − 1 con la barra llena menos 1 XP
create or replace function public.demo_preparar_nivel(p_nivel integer)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id    uuid := public.exigir_cuenta_demo();
  v_nivel integer;
begin
  if p_nivel is null or p_nivel < 2 or p_nivel > 100 then raise exception 'NIVEL_INVALIDO'; end if;

  insert into public.progreso (user_id) values (v_id) on conflict (user_id) do nothing;
  select p.nivel into v_nivel from public.progreso p where p.user_id = v_id for update;
  -- Bajar de nivel dejaría logros y skins que ya no corresponden: para repetir, reiniciar
  if v_nivel >= p_nivel then raise exception 'NIVEL_YA_ALCANZADO'; end if;

  update public.progreso
     set nivel = p_nivel - 1, xp = public.xp_para_subir(p_nivel - 1) - 1
   where user_id = v_id;
  -- Los logros de nivel que quedaron atrás se desbloquean ya (los del nivel n llegan al reclamar)
  perform public.evaluar_logros_de(v_id);
end;
$$;

-- "+1 nivel"
create or replace function public.demo_subir_nivel()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id    uuid := public.exigir_cuenta_demo();
  v_nivel integer;
begin
  insert into public.progreso (user_id) values (v_id) on conflict (user_id) do nothing;
  select p.nivel into v_nivel from public.progreso p where p.user_id = v_id for update;
  if v_nivel >= 100 then raise exception 'NIVEL_MAXIMO'; end if;

  update public.progreso set nivel = v_nivel + 1, xp = 0 where user_id = v_id;
  perform public.evaluar_logros_de(v_id);
end;
$$;

-- "+500 esmeraldas"
create or replace function public.demo_sumar_esmeraldas()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := public.exigir_cuenta_demo();
begin
  insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia)
  values (v_id, 500, 'DEMO', gen_random_uuid()::text);
end;
$$;

-- "Reiniciar cuenta demo": como recién creada (conserva el perfil, el avatar y el modo demo)
create or replace function public.demo_reiniciar()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := public.exigir_cuenta_demo();
begin
  -- Borrar sus misiones creadas también borra sus aceptaciones e historial (on delete cascade)
  delete from public.misiones where user_id = v_id;
  delete from public.misiones_usuario where user_id = v_id;
  delete from public.registros_progreso where user_id = v_id;
  delete from public.misiones_preferencias where user_id = v_id;
  delete from public.logros_usuario where user_id = v_id;
  delete from public.inventario where user_id = v_id;
  delete from public.movimientos_esmeraldas where user_id = v_id;
  delete from public.skins_usuario where user_id = v_id;
  delete from public.personaje where user_id = v_id;

  insert into public.progreso (user_id) values (v_id)
  on conflict (user_id) do update set nivel = 1, xp = 0;
  update public.cuentas_demo set dias_adelantados = 0 where user_id = v_id;
  -- Vuelve a tener a Novato equipado
  perform public.evaluar_personalizacion_de(v_id);
end;
$$;

revoke all on function public.exigir_cuenta_demo() from public, anon, authenticated;
revoke all on function public.mi_modo_demo() from public, anon;
revoke all on function public.demo_avanzar_dia() from public, anon;
revoke all on function public.demo_preparar_nivel(integer) from public, anon;
revoke all on function public.demo_subir_nivel() from public, anon;
revoke all on function public.demo_sumar_esmeraldas() from public, anon;
revoke all on function public.demo_reiniciar() from public, anon;
grant execute on function public.mi_modo_demo() to authenticated;
grant execute on function public.demo_avanzar_dia() to authenticated;
grant execute on function public.demo_preparar_nivel(integer) to authenticated;
grant execute on function public.demo_subir_nivel() to authenticated;
grant execute on function public.demo_sumar_esmeraldas() to authenticated;
grant execute on function public.demo_reiniciar() to authenticated;
