-- Pruebas del módulo Logros (misiones.sql y logros.sql deben estar aplicados).
-- Crea 2 usuarios falsos, prueba todo y al final lanza un error A PROPÓSITO para que NADA quede guardado.
-- Resultado esperado: un error cuyo mensaje contiene PRUEBAS_OK. Cualquier mensaje "FALLO: ..." es un problema.
do $$
declare
  a uuid := '00000000-0000-0000-0000-0000000000c1';
  b uuid := '00000000-0000-0000-0000-0000000000c2';
  gym bigint;
  mu public.misiones_usuario;
  r record;
  n int;
  fallo boolean;
begin
  insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (a, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'logro-a@levelia.test', 'x', now(), now(), now()),
    (b, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'logro-b@levelia.test', 'x', now(), now(), now());
  insert into public.perfiles (id, nombre, email, edad, avatar) values
    (a, 'Logro_A', 'logro-a@levelia.test', 20, 'AVATAR_1'),
    (b, 'Logro_B', 'logro-b@levelia.test', 20, 'AVATAR_2');

  select id into gym from public.misiones where user_id is null and titulo = 'Ir al gym';
  select public.categoria_mision(icono) as categoria into r from public.misiones where id = gym;
  if r.categoria <> 'FISICO' then raise exception 'FALLO: Ir al gym no quedó como FISICO (%)', r.categoria; end if;

  -- ===== Usuario A =====
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);

  -- 1) Sin hacer nada: 10 logros, ninguno obtenido
  select count(*) as total, count(obtenido_at) as obtenidos into r from public.mis_logros();
  if r.total <> 10 or r.obtenidos <> 0 then raise exception 'FALLO: catálogo inicial (% logros, % obtenidos)', r.total, r.obtenidos; end if;

  -- 2) Nadie se da logros ni artículos a mano, ni llama la evaluación interna
  begin
    insert into public.logros_usuario (user_id, logro_id) values (a, 'SENIOR');
    raise exception 'FALLO: A se dio un logro a mano';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.inventario (user_id, articulo_id) values (a, 'ESPADA_ORO');
    raise exception 'FALLO: A se dio un artículo a mano';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.evaluar_logros_de(b);
    raise exception 'FALLO: A ejecutó evaluar_logros_de';
  exception when insufficient_privilege then null;
  end;

  -- 3) Cumplir la primera misión (Fácil, 1 vez) desbloquea ¡Primeros pasos! y entrega la pechera
  mu := public.aceptar_mision(gym, 'FACIL');
  mu := public.registrar_progreso(mu.id);
  if mu.estado <> 'COMPLETADA' then raise exception 'FALLO: la misión de prueba no se cumplió'; end if;
  select count(*) into n from public.registros_progreso where mision_usuario_id = mu.id;
  if n <> 1 then raise exception 'FALLO: registrar_progreso no guardó el día en registros_progreso'; end if;
  select count(*) into n from public.logros_usuario where logro_id = 'PRIMEROS_PASOS';
  if n <> 1 then raise exception 'FALLO: cumplir la primera misión no desbloqueó ¡Primeros pasos!'; end if;
  select count(*) into n from public.inventario where articulo_id = 'PECHERA_BASICA';
  if n <> 1 then raise exception 'FALLO: ¡Primeros pasos! no entregó la pechera básica'; end if;

  select * into r from public.mis_logros() where id = 'CALENTAMIENTO';
  if r.progreso <> 33 or r.obtenido_at is not null or r.siguiente_id <> 'EN_FORMA' then
    raise exception 'FALLO: avance de ¡Calentamiento! (progreso %, siguiente %)', r.progreso, r.siguiente_id;
  end if;
  if (r.requisitos -> 0 ->> 'actual')::int <> 1 or (r.requisitos -> 0 ->> 'objetivo')::int <> 3 then
    raise exception 'FALLO: requisito de ¡Calentamiento! (%)', r.requisitos;
  end if;

  -- 4) Vistos: el aviso se marca una vez
  select count(*) into n from public.mis_logros() where obtenido_at is not null and not visto;
  if n <> 1 then raise exception 'FALLO: debería haber 1 logro nuevo sin ver (hay %)', n; end if;
  if public.marcar_logros_vistos() <> 1 then raise exception 'FALLO: marcar_logros_vistos'; end if;
  select count(*) into n from public.mis_logros() where obtenido_at is not null and not visto;
  if n <> 0 then raise exception 'FALLO: quedaron logros sin ver'; end if;

  -- 5) 3 misiones físicas → ¡Calentamiento! (se simulan 2 cumplidas como administrador)
  reset role;
  insert into public.misiones_usuario (user_id, mision_id, dificultad, meta, xp, progreso, fecha_limite, estado, recompensa_reclamada)
  values (a, gym, 'FACIL', 1, 75, 1, public.hoy() - 20, 'COMPLETADA', true),
         (a, gym, 'FACIL', 1, 75, 1, public.hoy() - 10, 'COMPLETADA', true);
  set local role authenticated;
  select * into r from public.mis_logros() where id = 'CALENTAMIENTO';
  if r.obtenido_at is null or r.progreso <> 100 then raise exception 'FALLO: 3 físicas no desbloquearon ¡Calentamiento!'; end if;
  select count(*) into n from public.inventario where articulo_id = 'BOTAS_BASICAS';
  if n <> 1 then raise exception 'FALLO: ¡Calentamiento! no entregó las botas'; end if;

  -- 6) Racha de 7 días: avanza ¡En forma! pero le faltan misiones (3/10, 7/7, 1/1 → 77 %)
  reset role;
  insert into public.registros_progreso (user_id, mision_usuario_id, fecha)
  select a, mu.id, public.hoy() - g from generate_series(1, 6) g;
  set local role authenticated;
  select * into r from public.mis_logros() where id = 'EN_FORMA';
  if r.obtenido_at is not null or r.progreso <> 77 then raise exception 'FALLO: avance de ¡En forma! (%)', r.progreso; end if;
  if (r.requisitos -> 1 ->> 'actual')::int <> 7 then raise exception 'FALLO: racha de 7 días (%)', r.requisitos; end if;
  select avance_siguiente into r from public.mis_logros() where id = 'CALENTAMIENTO';
  if r.avance_siguiente <> 77 then raise exception 'FALLO: avance hacia la siguiente medalla (%)', r.avance_siguiente; end if;

  -- 7) Subir a nivel 10 desbloquea Aprendiz y ¡Junior! en la misma evaluación (cadena)
  reset role;
  update public.progreso set nivel = 10, xp = 0 where user_id = a;
  set local role authenticated;
  select count(*) into n from public.mis_logros() where id in ('APRENDIZ', 'JUNIOR') and obtenido_at is not null;
  if n <> 2 then raise exception 'FALLO: nivel 10 no desbloqueó Aprendiz y ¡Junior! (%)', n; end if;
  select count(*) into n from public.inventario where articulo_id in ('ESPADA_BASICA', 'ESPADA_PLATA');
  if n <> 2 then raise exception 'FALLO: faltan las espadas de Aprendiz / ¡Junior!'; end if;

  -- 8) Reclamar recompensas evalúa sin abrir la pantalla: llegar al nivel 25 da ¡¡Senior!!
  reset role;
  update public.progreso set nivel = 24, xp = public.xp_para_subir(24) - 10 where user_id = a;
  set local role authenticated;
  select * into r from public.reclamar_recompensas();  -- la misión del paso 3 (+75 XP) seguía sin reclamar
  if r.nivel <> 25 then raise exception 'FALLO: no llegó al nivel 25 (%)', r.nivel; end if;
  select count(*) into n from public.logros_usuario where logro_id = 'SENIOR';
  if n <> 1 then raise exception 'FALLO: reclamar no desbloqueó ¡¡Senior!!'; end if;

  -- 9) B no ve nada de A
  perform set_config('request.jwt.claims', json_build_object('sub', b, 'role', 'authenticated')::text, true);
  select count(*) into n from public.logros_usuario;
  if n <> 0 then raise exception 'FALLO: B ve logros de A'; end if;
  select count(*) into n from public.inventario;
  if n <> 0 then raise exception 'FALLO: B ve el inventario de A'; end if;
  select count(*) into n from public.registros_progreso;
  if n <> 0 then raise exception 'FALLO: B ve el historial de A'; end if;
  select count(obtenido_at) into n from public.mis_logros();
  if n <> 0 then raise exception 'FALLO: B tiene logros sin haber hecho nada'; end if;

  -- 10) Borrar la cuenta de A arrastra sus logros, artículos e historial
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  perform public.eliminar_mi_cuenta();
  reset role;
  select (select count(*) from public.logros_usuario where user_id = a)
       + (select count(*) from public.inventario where user_id = a)
       + (select count(*) from public.registros_progreso where user_id = a) into n;
  if n <> 0 then raise exception 'FALLO: quedaron datos de logros de una cuenta borrada'; end if;

  raise exception 'PRUEBAS_OK: todas las pruebas de logros pasaron (este error revierte los datos de prueba)';
end;
$$;
