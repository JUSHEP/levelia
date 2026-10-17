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
