-- Pruebas del módulo Misiones (supabase/misiones.sql debe estar aplicado).
-- Crea 2 usuarios falsos, prueba todo y al final lanza un error A PROPÓSITO para que NADA quede guardado.
-- Resultado esperado: un error cuyo mensaje contiene PRUEBAS_OK. Cualquier mensaje "FALLO: ..." es un problema.
do $$
declare
  a uuid := '00000000-0000-0000-0000-0000000000b1';
  b uuid := '00000000-0000-0000-0000-0000000000b2';
  gym bigint;
  clases bigint;
  enfoque bigint;
  propia bigint;
  unica bigint;
  mu public.misiones_usuario;
  mu2 public.misiones_usuario;
  r record;
  n int;
  fallo boolean;
begin
  insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (a, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mision-a@levelia.test', 'x', now(), now(), now()),
    (b, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mision-b@levelia.test', 'x', now(), now(), now());
  insert into public.perfiles (id, nombre, email, edad, avatar) values
    (a, 'Mision_A', 'mision-a@levelia.test', 20, 'AVATAR_1'),
    (b, 'Mision_B', 'mision-b@levelia.test', 20, 'AVATAR_2');

  -- Curva de niveles (la misma que domain/ReglasNivel.kt)
  if public.xp_para_subir(1) <> 100 or public.xp_para_subir(2) <> 283 or public.xp_para_subir(99) <> 98504 then
    raise exception 'FALLO: xp_para_subir no da 100 / 283 / 98504';
  end if;

  select id into gym from public.misiones where user_id is null and titulo = 'Ir al gym';
  if gym is null then raise exception 'FALLO: falta la misión predefinida Ir al gym'; end if;
  select id into clases from public.misiones where user_id is null and titulo = 'A clases';
  select id into enfoque from public.misiones where user_id is null and titulo = 'Modo enfoque';

  -- ===== Usuario A =====
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);

  -- 1) A ve las 4 predefinidas y no puede escribir directo en las tablas
  select count(*) into n from public.misiones;
  if n < 4 then raise exception 'FALLO: A ve % misiones predefinidas', n; end if;
  begin
    insert into public.misiones (titulo, dificultad, meta, xp) values ('Trampa', 'MEDIA', 3, 9999);
    raise exception 'FALLO: A insertó en misiones sin la función';
  exception when insufficient_privilege then null;
  end;
  begin
    update public.progreso set xp = 99999 where user_id = a;
    raise exception 'FALLO: A se puso XP a mano';
  exception when insufficient_privilege then null;
  end;

  -- 2) Crear misión: validaciones
  fallo := false;
  begin perform public.crear_mision('   ', '', 'MEDIA'); exception when others then fallo := sqlerrm = 'TITULO_VACIO'; end;
  if not fallo then raise exception 'FALLO: aceptó un nombre vacío'; end if;
  fallo := false;
  begin perform public.crear_mision('Tarea', '', 'SUAVE'); exception when others then fallo := sqlerrm = 'DIFICULTAD_INVALIDA'; end;
  if not fallo then raise exception 'FALLO: aceptó una dificultad inexistente'; end if;
  fallo := false;
  begin perform public.crear_mision('Tarea', '', 'UNICA'); exception when others then fallo := sqlerrm = 'FECHA_REQUERIDA'; end;
  if not fallo then raise exception 'FALLO: UNICA sin fecha'; end if;
  fallo := false;
  begin perform public.crear_mision('Tarea', '', 'UNICA', public.hoy() - 1); exception when others then fallo := sqlerrm = 'FECHA_PASADA'; end;
  if not fallo then raise exception 'FALLO: UNICA con fecha pasada'; end if;

  -- Siempre «select * from f()»: «select (f()).*» ejecuta la función una vez por columna
  select * into r from public.crear_mision('  Leer  ', 'Un capítulo', 'DIFICIL', public.hoy() + 30);
  if r.titulo <> 'Leer' or r.meta <> 5 or r.xp <> 300 or r.fecha_limite is not null or r.user_id <> a then
    raise exception 'FALLO: misión DIFICIL creada mal (titulo %, meta %, xp %, fecha %)', r.titulo, r.meta, r.xp, r.fecha_limite;
  end if;
  propia := r.id;
  select id into unica from public.crear_mision('Tarea de cálculo #02', 'Avanzar en el proyecto', 'UNICA', public.hoy() + 3);

  -- 3) Aceptar: con dificultad de la misión, ajustada, repetida
  mu := public.aceptar_mision(gym);
  if mu.meta <> 3 or mu.xp <> 150 or mu.fecha_limite <> public.hoy() + 6 or mu.estado <> 'ACTIVA' then
    raise exception 'FALLO: aceptar Ir al gym (meta %, xp %, fecha %)', mu.meta, mu.xp, mu.fecha_limite;
  end if;
  fallo := false;
  begin perform public.aceptar_mision(gym); exception when others then fallo := sqlerrm = 'MISION_YA_ACTIVA'; end;
  if not fallo then raise exception 'FALLO: aceptó dos veces la misma misión'; end if;
  fallo := false;
  begin perform public.aceptar_mision(propia, 'UNICA'); exception when others then fallo := sqlerrm = 'DIFICULTAD_INVALIDA'; end;
  if not fallo then raise exception 'FALLO: ajustó a UNICA sin fecha'; end if;

  select * into r from public.aceptar_mision(propia, 'FACIL');
  if r.meta <> 1 or r.xp <> 75 then raise exception 'FALLO: Ajustar dificultad a FACIL (meta %, xp %)', r.meta, r.xp; end if;

  -- 4) Registrar progreso: 1 por día, completa al llegar a la meta
  mu := public.registrar_progreso(mu.id);
  if mu.progreso <> 1 or mu.estado <> 'ACTIVA' then raise exception 'FALLO: primer registro'; end if;
  fallo := false;
  begin perform public.registrar_progreso(mu.id); exception when others then fallo := sqlerrm = 'YA_REGISTRADO_HOY'; end;
  if not fallo then raise exception 'FALLO: dejó registrar dos veces el mismo día'; end if;

  select * into r from public.registrar_progreso(r.id);  -- FACIL: 1/1
  if r.estado <> 'COMPLETADA' or r.terminada_at is null then raise exception 'FALLO: FACIL no se completó con 1 registro'; end if;

  mu2 := public.aceptar_mision(unica);
  select * into r from public.registrar_progreso(mu2.id);  -- UNICA antes de la fecha
  if r.estado <> 'COMPLETADA' then raise exception 'FALLO: UNICA a tiempo no se completó'; end if;

  -- 4b) Ajustes de misión: se guarda sin aceptar y aceptar usa la guardada
  perform public.ajustar_dificultad(clases, 'DIFICIL');
  select count(*) into n from public.misiones_preferencias where mision_id = clases;
  if n <> 1 then raise exception 'FALLO: ajustar_dificultad no guardó la preferencia'; end if;
  select count(*) into n from public.misiones_usuario where mision_id = clases;
  if n <> 0 then raise exception 'FALLO: ajustar_dificultad aceptó la misión'; end if;
  select * into r from public.aceptar_mision(clases);
  if r.dificultad <> 'DIFICIL' or r.meta <> 5 or r.xp <> 300 then
    raise exception 'FALLO: aceptar no usó la dificultad guardada (%, %, %)', r.dificultad, r.meta, r.xp;
  end if;
  fallo := false;
  begin perform public.ajustar_dificultad(clases, 'FACIL'); exception when others then fallo := sqlerrm = 'MISION_YA_ACTIVA'; end;
  if not fallo then raise exception 'FALLO: ajustó una misión en curso'; end if;
  fallo := false;
  begin perform public.ajustar_dificultad(unica, 'MEDIA'); exception when others then fallo := sqlerrm = 'DIFICULTAD_INVALIDA'; end;
  if not fallo then raise exception 'FALLO: ajustó una misión UNICA'; end if;
  fallo := false;
  begin perform public.ajustar_dificultad(enfoque, 'UNICA'); exception when others then fallo := sqlerrm = 'DIFICULTAD_INVALIDA'; end;
  if not fallo then raise exception 'FALLO: ajustó a UNICA'; end if;
  -- Volver a la dificultad de la misión borra la preferencia (reaparecen sus veces y XP originales)
  perform public.ajustar_dificultad(enfoque, 'MEDIA');
  perform public.ajustar_dificultad(enfoque, 'FACIL');
  select count(*) into n from public.misiones_preferencias where mision_id = enfoque;
  if n <> 0 then raise exception 'FALLO: volver a la dificultad original no borró la preferencia'; end if;
  perform public.ajustar_dificultad(enfoque, 'DIFICIL');

  -- 5) B no ve ni toca lo de A
  perform set_config('request.jwt.claims', json_build_object('sub', b, 'role', 'authenticated')::text, true);
  select count(*) into n from public.misiones where user_id = a;
  if n <> 0 then raise exception 'FALLO: B ve misiones creadas por A'; end if;
  select count(*) into n from public.misiones_usuario;
  if n <> 0 then raise exception 'FALLO: B ve misiones aceptadas por A'; end if;
  select count(*) into n from public.misiones_preferencias;
  if n <> 0 then raise exception 'FALLO: B ve las dificultades guardadas por A'; end if;
  begin
    insert into public.misiones_preferencias (user_id, mision_id, dificultad) values (b, enfoque, 'MEDIA');
    raise exception 'FALLO: B escribió en misiones_preferencias sin la función';
  exception when insufficient_privilege then null;
  end;
  fallo := false;
  begin perform public.registrar_progreso(mu.id); exception when others then fallo := sqlerrm = 'MISION_NO_EXISTE'; end;
  if not fallo then raise exception 'FALLO: B registró progreso en una misión de A'; end if;
  fallo := false;
  begin perform public.aceptar_mision(propia); exception when others then fallo := sqlerrm = 'MISION_NO_EXISTE'; end;
  if not fallo then raise exception 'FALLO: B aceptó una misión creada por A'; end if;
  select * into r from public.reclamar_recompensas();
  if r.xp_ganada <> 0 then raise exception 'FALLO: B reclamó XP de A'; end if;

  -- 6) A reclama: 75 (FACIL) + 50 (UNICA) = 125 XP → nivel 2 con 25 XP (el 1 pide 100)
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  select * into r from public.reclamar_recompensas();
  if r.xp_ganada <> 125 or r.nivel <> 2 or r.xp <> 25 or r.niveles_subidos <> 1 then
    raise exception 'FALLO: reclamar (ganada %, nivel %, xp %, subidos %)', r.xp_ganada, r.nivel, r.xp, r.niveles_subidos;
  end if;
  select * into r from public.reclamar_recompensas();
  if r.xp_ganada <> 0 or r.nivel <> 2 then raise exception 'FALLO: reclamó dos veces la misma recompensa'; end if;

  -- 6b) No se puede ganar XP repitiendo: la semanal espera a que termine su plazo; la UNICA, nunca más
  fallo := false;
  begin perform public.aceptar_mision(propia); exception when others then fallo := sqlerrm = 'MISION_EN_CURSO'; end;
  if not fallo then raise exception 'FALLO: volvió a aceptar una misión cumplida dentro de su semana'; end if;
  fallo := false;
  begin perform public.aceptar_mision(unica); exception when others then fallo := sqlerrm = 'MISION_YA_HECHA'; end;
  if not fallo then raise exception 'FALLO: volvió a aceptar una misión UNICA'; end if;

  -- 7) Vencer: se simula que pasó la semana (como administrador)
  reset role;
  update public.misiones_usuario set fecha_limite = public.hoy() - 1 where id = mu.id;
  set local role authenticated;
  if public.vencer_mis_misiones() <> 1 then raise exception 'FALLO: no venció la misión de la semana pasada'; end if;
  select estado into r from public.misiones_usuario where id = mu.id;
  if r.estado <> 'PERDIDA' then raise exception 'FALLO: la misión vencida no quedó PERDIDA'; end if;
  mu := public.aceptar_mision(gym);  -- perdida: se puede volver a aceptar
  if mu.estado <> 'ACTIVA' then raise exception 'FALLO: no dejó volver a aceptar tras perderla'; end if;

  -- 8) Subir varios niveles de una vez y tope en 100
  reset role;
  update public.progreso set nivel = 99, xp = 98500 where user_id = a;
  update public.misiones_usuario set estado = 'COMPLETADA', fecha_limite = public.hoy() - 1 where id = mu.id;
  set local role authenticated;
  -- 8a) Con la XP sin reclamar no se puede repetir (aunque ya pasó su semana)
  fallo := false;
  begin perform public.aceptar_mision(gym); exception when others then fallo := sqlerrm = 'RECOMPENSA_PENDIENTE'; end;
  if not fallo then raise exception 'FALLO: repitió una misión con la recompensa sin reclamar'; end if;
  select * into r from public.reclamar_recompensas();
  if r.nivel <> 100 or r.xp <> 146 then raise exception 'FALLO: tope de nivel (nivel %, xp %)', r.nivel, r.xp; end if;
  -- 8b) Reclamada y con su semana terminada: se puede volver a aceptar
  mu := public.aceptar_mision(gym);
  if mu.estado <> 'ACTIVA' then raise exception 'FALLO: no dejó repetir tras su semana'; end if;

  -- 9) Borrar la cuenta arrastra sus misiones
  perform public.eliminar_mi_cuenta();
  reset role;
  select count(*) into n from public.misiones where user_id = a;
  if n <> 0 then raise exception 'FALLO: quedaron misiones de una cuenta borrada'; end if;
  select count(*) into n from public.misiones_usuario where user_id = a;
  if n <> 0 then raise exception 'FALLO: quedó progreso de misiones de una cuenta borrada'; end if;
  select count(*) into n from public.misiones_preferencias where user_id = a;
  if n <> 0 then raise exception 'FALLO: quedaron dificultades guardadas de una cuenta borrada'; end if;

  raise exception 'PRUEBAS_OK: todas las pruebas de misiones pasaron (este error revierte los datos de prueba)';
end;
$$;
