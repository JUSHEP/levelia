-- Pruebas del modo demo (misiones.sql, logros.sql, personalizacion.sql y modo_demo.sql aplicados).
-- Crea 2 usuarios falsos, prueba todo y al final lanza un error A PROPÓSITO para que NADA quede guardado.
-- Resultado esperado: un error cuyo mensaje contiene PRUEBAS_OK. Cualquier mensaje "FALLO: ..." es un problema.
do $$
declare
  a uuid := '00000000-0000-0000-0000-0000000000f1';  -- cuenta demo
  b uuid := '00000000-0000-0000-0000-0000000000f2';  -- cuenta normal
  gym bigint;
  caminar bigint;
  mu public.misiones_usuario;
  r record;
  n int;
  fallo boolean;
begin
  insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (a, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'demo-a@levelia.test', 'x', now(), now(), now()),
    (b, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'demo-b@levelia.test', 'x', now(), now(), now());
  insert into public.perfiles (id, nombre, email, edad, avatar) values
    (a, 'Demo_A', 'demo-a@levelia.test', 20, 'AVATAR_1'),
    (b, 'Demo_B', 'demo-b@levelia.test', 20, 'AVATAR_2');
  select id into gym from public.misiones where user_id is null and titulo = 'Ir al gym';
  select id into caminar from public.misiones where user_id is null and titulo = 'Salir a caminar';
  if caminar is null then raise exception 'FALLO: falta la misión predefinida Salir a caminar (misiones.sql versión 5)'; end if;

  -- ===== 1) Una cuenta normal (B) no tiene modo demo =====
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', b, 'role', 'authenticated')::text, true);
  select * into r from public.mi_modo_demo();
  if r.activo or r.dias <> 0 then raise exception 'FALLO: B aparece como demo'; end if;
  fallo := false;
  begin perform public.demo_subir_nivel(); exception when others then fallo := sqlerrm = 'NO_ES_DEMO'; end;
  if not fallo then raise exception 'FALLO: una cuenta normal subió de nivel con el modo demo'; end if;
  fallo := false;
  begin perform public.demo_sumar_esmeraldas(); exception when others then fallo := sqlerrm = 'NO_ES_DEMO'; end;
  if not fallo then raise exception 'FALLO: una cuenta normal se regaló esmeraldas'; end if;
  fallo := false;
  begin perform public.demo_reiniciar(); exception when others then fallo := sqlerrm = 'NO_ES_DEMO'; end;
  if not fallo then raise exception 'FALLO: una cuenta normal usó Reiniciar'; end if;
  -- Ni se marca sola como demo, ni ve quién lo es
  begin
    insert into public.cuentas_demo (user_id) values (b);
    raise exception 'FALLO: B se marcó como demo';
  exception when insufficient_privilege then null;
  end;
  begin
    perform 1 from public.cuentas_demo;
    raise exception 'FALLO: B puede leer cuentas_demo';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.exigir_cuenta_demo();
    raise exception 'FALLO: B ejecutó la función interna exigir_cuenta_demo';
  exception when insufficient_privilege then null;
  end;

  -- ===== 2) El administrador marca a A (lo que se hace en el SQL Editor) =====
  reset role;
  insert into public.cuentas_demo (user_id) select id from public.perfiles where lower(nombre) = lower('demo_a')
  on conflict do nothing;
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  select * into r from public.mi_modo_demo();
  if not r.activo or r.dias <> 0 then raise exception 'FALLO: A no quedó como demo (activo %, días %)', r.activo, r.dias; end if;

  -- ===== 3) Avanzar 1 día: se puede volver a registrar y la racha sigue =====
  mu := public.aceptar_mision(gym);  -- MEDIA: 3 veces en 7 días
  mu := public.registrar_progreso(mu.id);
  fallo := false;
  begin perform public.registrar_progreso(mu.id); exception when others then fallo := sqlerrm = 'YA_REGISTRADO_HOY'; end;
  if not fallo then raise exception 'FALLO: registró dos veces el mismo día'; end if;
  perform public.demo_avanzar_dia();
  mu := public.registrar_progreso(mu.id);
  if mu.progreso <> 2 or mu.ultimo_registro <> public.hoy() or mu.fecha_limite <> public.hoy() + 5 then
    raise exception 'FALLO: después de avanzar un día (progreso %, último %, límite %)', mu.progreso, mu.ultimo_registro, mu.fecha_limite;
  end if;
  select count(*) into n from public.registros_progreso where mision_usuario_id = mu.id and fecha in (public.hoy() - 1, public.hoy());
  if n <> 2 then raise exception 'FALLO: los registros no quedaron en días seguidos (%)', n; end if;
  -- Más días: la racha se corre entera sin chocar con el índice único
  perform public.demo_avanzar_dia();
  mu := public.registrar_progreso(mu.id);
  if mu.estado <> 'COMPLETADA' then raise exception 'FALLO: 3 días con Avanzar no cumplieron la misión (%)', mu.estado; end if;
  reset role;
  if public.racha_maxima(a) <> 3 then raise exception 'FALLO: la racha debería ser de 3 días (%)', public.racha_maxima(a); end if;
  set local role authenticated;
  select * into r from public.mi_modo_demo();
  if r.dias <> 2 then raise exception 'FALLO: días adelantados (%)', r.dias; end if;

  -- Una misión que no se termina queda perdida al pasar su plazo
  mu := public.aceptar_mision(caminar, 'DIFICIL');
  for n in 1..7 loop perform public.demo_avanzar_dia(); end loop;
  select * into mu from public.misiones_usuario where id = mu.id;
  if mu.estado <> 'PERDIDA' then raise exception 'FALLO: la misión no venció al adelantar 7 días (%)', mu.estado; end if;

  -- ===== 4) A punto de subir al nivel 5: la siguiente misión reclamada lo sube =====
  fallo := false;
  begin perform public.demo_preparar_nivel(101); exception when others then fallo := sqlerrm = 'NIVEL_INVALIDO'; end;
  if not fallo then raise exception 'FALLO: preparó el nivel 101'; end if;
  perform public.demo_preparar_nivel(5);
  select nivel, xp into r from public.progreso where user_id = a;
  if r.nivel <> 4 or r.xp <> public.xp_para_subir(4) - 1 then raise exception 'FALLO: preparar nivel 5 (nivel %, xp %)', r.nivel, r.xp; end if;
  fallo := false;
  begin perform public.demo_preparar_nivel(3); exception when others then fallo := sqlerrm = 'NIVEL_YA_ALCANZADO'; end;
  if not fallo then raise exception 'FALLO: preparar un nivel que ya pasó lo bajó de nivel'; end if;
  -- La misión de gym ya estaba cumplida (paso 3): reclamarla (+150 XP) lo sube
  select * into r from public.reclamar_recompensas();
  if r.nivel < 5 then raise exception 'FALLO: reclamar no subió al nivel 5 (%)', r.nivel; end if;
  select count(*) into n from public.logros_usuario where user_id = a and logro_id = 'APRENDIZ';
  if n <> 1 then raise exception 'FALLO: llegar al nivel 5 no dio Aprendiz'; end if;
  select * into r from public.mi_personalizacion();
  select count(*) into n from jsonb_array_elements(r.skins) e
   where e ->> 'id' = 'ESCUDERO' and e ->> 'obtenida_at' is not null and not (e ->> 'visto')::boolean;
  if n <> 1 then raise exception 'FALLO: no se desbloqueó Escudero como nueva'; end if;

  -- ===== 5) +1 nivel y tope en 100 =====
  select nivel into n from public.progreso where user_id = a;
  perform public.demo_subir_nivel();
  select nivel, xp into r from public.progreso where user_id = a;
  if r.nivel <> n + 1 or r.xp <> 0 then raise exception 'FALLO: +1 nivel (nivel %, xp %)', r.nivel, r.xp; end if;
  perform public.demo_preparar_nivel(100);
  perform public.demo_subir_nivel();
  select nivel into n from public.progreso where user_id = a;
  if n <> 100 then raise exception 'FALLO: no llegó al nivel 100 (%)', n; end if;
  select count(*) into n from public.mis_logros() where id in ('SENIOR', 'VETERANO', 'MAESTRO', 'LEYENDA_VIVA') and obtenido_at is not null;
  if n <> 4 then raise exception 'FALLO: el nivel 100 no desbloqueó los logros de nivel (%)', n; end if;
  select * into r from public.mi_personalizacion();
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'tipo' = 'NIVEL' and e ->> 'obtenida_at' is not null;
  if n <> 6 then raise exception 'FALLO: en el nivel 100 debería tener las 6 skins de nivel (%)', n; end if;
  fallo := false;
  begin perform public.demo_subir_nivel(); exception when others then fallo := sqlerrm = 'NIVEL_MAXIMO'; end;
  if not fallo then raise exception 'FALLO: pasó del nivel 100'; end if;

  -- ===== 6) +500 esmeraldas para la tienda =====
  select esmeraldas into n from public.mi_personalizacion();
  perform public.demo_sumar_esmeraldas();
  perform public.demo_sumar_esmeraldas();
  select * into r from public.mi_personalizacion();
  if r.esmeraldas <> n + 1000 then raise exception 'FALLO: +500 esmeraldas dos veces (% → %)', n, r.esmeraldas; end if;
  perform public.comprar_skin('CABALLERO_OSCURO');
  select * into r from public.mi_personalizacion();
  if r.skin_equipada <> 'CABALLERO_OSCURO' or r.esmeraldas <> n + 1000 - 1200 then
    raise exception 'FALLO: comprar con esmeraldas demo (equipada %, saldo %)', r.skin_equipada, r.esmeraldas;
  end if;

  -- ===== 7) Reiniciar: como nueva, pero sigue siendo demo =====
  perform public.crear_mision('Mi misión demo', '', 'FACIL');
  perform public.demo_reiniciar();
  select * into r from public.mi_personalizacion();
  if r.nivel <> 1 or r.esmeraldas <> 0 or r.skin_equipada <> 'NOVATO' then
    raise exception 'FALLO: reiniciar (nivel %, esmeraldas %, equipada %)', r.nivel, r.esmeraldas, r.skin_equipada;
  end if;
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'obtenida_at' is not null;
  if n <> 1 then raise exception 'FALLO: después de reiniciar solo debería tener a Novato (%)', n; end if;
  select (select count(*) from public.misiones_usuario where user_id = a)
       + (select count(*) from public.registros_progreso where user_id = a)
       + (select count(*) from public.logros_usuario where user_id = a)
       + (select count(*) from public.misiones where user_id = a) into n;
  if n <> 0 then raise exception 'FALLO: reiniciar dejó misiones, historial o logros (%)', n; end if;
  select xp into n from public.progreso where user_id = a;
  if n <> 0 then raise exception 'FALLO: reiniciar dejó XP (%)', n; end if;
  select * into r from public.mi_modo_demo();
  if not r.activo or r.dias <> 0 then raise exception 'FALLO: reiniciar quitó el modo demo o los días (activo %, días %)', r.activo, r.dias; end if;

  -- ===== 8) B sigue igual y no ve nada de A =====
  perform set_config('request.jwt.claims', json_build_object('sub', b, 'role', 'authenticated')::text, true);
  select * into r from public.mi_personalizacion();
  if r.nivel <> 1 or r.esmeraldas <> 0 then raise exception 'FALLO: el modo demo de A cambió a B'; end if;
  select count(*) into n from public.movimientos_esmeraldas;
  if n <> 0 then raise exception 'FALLO: B ve movimientos de A'; end if;

  -- ===== 9) Borrar la cuenta demo la saca de cuentas_demo =====
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  perform public.eliminar_mi_cuenta();
  reset role;
  select count(*) into n from public.cuentas_demo where user_id = a;
  if n <> 0 then raise exception 'FALLO: quedó en cuentas_demo una cuenta borrada'; end if;

  raise exception 'PRUEBAS_OK: todas las pruebas del modo demo pasaron (este error revierte los datos de prueba)';
end;
$$;
