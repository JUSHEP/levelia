-- Pruebas del módulo Personalización (misiones.sql, logros.sql versión 6 y personalizacion.sql aplicados).
-- Crea 2 usuarios falsos, prueba todo y al final lanza un error A PROPÓSITO para que NADA quede guardado.
-- Resultado esperado: un error cuyo mensaje contiene PRUEBAS_OK. Cualquier mensaje "FALLO: ..." es un problema.
do $$
declare
  a uuid := '00000000-0000-0000-0000-0000000000d1';
  b uuid := '00000000-0000-0000-0000-0000000000d2';
  gym bigint;
  mu public.misiones_usuario;
  r record;
  n int;
begin
  insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (a, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'skin-a@levelia.test', 'x', now(), now(), now()),
    (b, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'skin-b@levelia.test', 'x', now(), now(), now());
  insert into public.perfiles (id, nombre, email, edad, avatar) values
    (a, 'Skin_A', 'skin-a@levelia.test', 20, 'AVATAR_1'),
    (b, 'Skin_B', 'skin-b@levelia.test', 20, 'AVATAR_2');
  select id into gym from public.misiones where user_id is null and titulo = 'Ir al gym';

  -- ===== Usuario A =====
  set local role authenticated;
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);

  -- 1) Al empezar: 0 esmeraldas, nivel 1, Novato equipado y sin avisos; 12 skins en el catálogo
  select * into r from public.mi_personalizacion();
  if r.esmeraldas <> 0 or r.nivel <> 1 or r.skin_equipada <> 'NOVATO' then
    raise exception 'FALLO: estado inicial (% esmeraldas, nivel %, equipada %)', r.esmeraldas, r.nivel, r.skin_equipada;
  end if;
  if jsonb_array_length(r.skins) <> 12 then raise exception 'FALLO: el catálogo debería tener 12 skins (%)', jsonb_array_length(r.skins); end if;
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'obtenida_at' is not null;
  if n <> 1 then raise exception 'FALLO: al empezar solo debería tener a Novato (%)', n; end if;
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'id' = 'NOVATO' and (e ->> 'visto')::boolean;
  if n <> 1 then raise exception 'FALLO: Novato no debería anunciarse como nueva'; end if;
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'id' = 'ESCUDERO'
    and (e ->> 'nivel_requerido')::int = 5 and e ->> 'precio' is null and jsonb_array_length(e -> 'habilidades') = 3;
  if n <> 1 then raise exception 'FALLO: datos de Escudero en mi_personalizacion'; end if;

  -- 2) Nadie se da esmeraldas, skins ni equipa a mano, ni llama las funciones internas
  begin
    insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia) values (a, 9999, 'LOGRO', 'TRAMPA');
    raise exception 'FALLO: A se dio esmeraldas a mano';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.skins_usuario (user_id, skin_id) values (a, 'LEYENDA');
    raise exception 'FALLO: A se dio una skin a mano';
  exception when insufficient_privilege then null;
  end;
  begin
    update public.personaje set skin_id = 'LEYENDA' where user_id = a;
    raise exception 'FALLO: A se equipó una skin a mano';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.evaluar_personalizacion_de(b);
    raise exception 'FALLO: A ejecutó evaluar_personalizacion_de';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.saldo_esmeraldas(b);
    raise exception 'FALLO: A consultó el saldo de otro usuario';
  exception when insufficient_privilege then null;
  end;

  -- 3) Reglas de equipar y comprar
  begin
    perform public.equipar_skin('ESCUDERO');
    raise exception 'FALLO: equipó una skin de nivel 5 estando en nivel 1';
  exception when raise_exception then
    if sqlerrm <> 'SKIN_BLOQUEADA' then raise; end if;
  end;
  begin
    perform public.comprar_skin('LEYENDA');
    raise exception 'FALLO: compró una skin de nivel';
  exception when raise_exception then
    if sqlerrm <> 'SKIN_NO_SE_VENDE' then raise; end if;
  end;
  begin
    perform public.comprar_skin('NO_EXISTE');
    raise exception 'FALLO: compró una skin que no existe';
  exception when raise_exception then
    if sqlerrm <> 'SKIN_NO_EXISTE' then raise; end if;
  end;
  begin
    perform public.comprar_skin('ARQUERO');
    raise exception 'FALLO: compró sin esmeraldas';
  exception when raise_exception then
    if sqlerrm <> 'ESMERALDAS_INSUFICIENTES' then raise; end if;
  end;

  -- 4) Cumplir la primera misión desbloquea ¡Primeros pasos! (+50). La misión paga al reclamarla (Fácil +10)
  mu := public.aceptar_mision(gym, 'FACIL');
  mu := public.registrar_progreso(mu.id);
  select esmeraldas into n from public.mi_personalizacion();
  if n <> 50 then raise exception 'FALLO: ¡Primeros pasos! debería dar 50 esmeraldas (%)', n; end if;
  perform public.reclamar_recompensas();
  select esmeraldas into n from public.mi_personalizacion();
  if n <> 60 then raise exception 'FALLO: reclamar una misión Fácil debería sumar 10 esmeraldas (%)', n; end if;
  -- Volver a leer no paga otra vez
  select esmeraldas into n from public.mi_personalizacion();
  if n <> 60 then raise exception 'FALLO: se pagó dos veces (%)', n; end if;
  select count(*) into n from public.movimientos_esmeraldas;
  if n <> 2 then raise exception 'FALLO: debería haber 2 movimientos (logro y misión), hay %', n; end if;

  -- 5) Subir al nivel 10 desbloquea Escudero (5) y Soldado (10), sin Caballero (25); se anuncian una vez
  reset role;
  update public.progreso set nivel = 10, xp = 0 where user_id = a;
  set local role authenticated;
  select * into r from public.mi_personalizacion();
  select count(*) into n from jsonb_array_elements(r.skins) e
   where e ->> 'id' in ('ESCUDERO', 'SOLDADO') and e ->> 'obtenida_at' is not null and not (e ->> 'visto')::boolean;
  if n <> 2 then raise exception 'FALLO: nivel 10 debería desbloquear Escudero y Soldado como nuevas (%)', n; end if;
  select count(*) into n from jsonb_array_elements(r.skins) e where e ->> 'id' = 'CABALLERO' and e ->> 'obtenida_at' is not null;
  if n <> 0 then raise exception 'FALLO: Caballero se desbloqueó antes del nivel 25'; end if;
  if r.skin_equipada <> 'NOVATO' then raise exception 'FALLO: subir de nivel no debe cambiar la skin equipada'; end if;
  if public.marcar_skins_vistas() <> 2 then raise exception 'FALLO: marcar_skins_vistas'; end if;
  if public.marcar_skins_vistas() <> 0 then raise exception 'FALLO: quedaron skins sin ver'; end if;

  -- 6) Equipar una desbloqueada
  perform public.equipar_skin('SOLDADO');
  select skin_equipada into r from public.mi_personalizacion();
  if r.skin_equipada <> 'SOLDADO' then raise exception 'FALLO: no quedó equipado Soldado (%)', r.skin_equipada; end if;

  -- 7) Comprar en la tienda: descuenta, queda suya y equipada; no se compra dos veces
  reset role;
  insert into public.movimientos_esmeraldas (user_id, cantidad, motivo, referencia) values (a, 190, 'LOGRO', 'PRUEBA');
  set local role authenticated;
  select esmeraldas into n from public.mi_personalizacion();
  if n <> 250 then raise exception 'FALLO: saldo de prueba (%)', n; end if;
  perform public.comprar_skin('ARQUERO');
  select * into r from public.mi_personalizacion();
  if r.esmeraldas <> 50 or r.skin_equipada <> 'ARQUERO' then
    raise exception 'FALLO: después de comprar (% esmeraldas, equipada %)', r.esmeraldas, r.skin_equipada;
  end if;
  select count(*) into n from jsonb_array_elements(r.skins) e
   where e ->> 'id' = 'ARQUERO' and e ->> 'obtenida_at' is not null and (e ->> 'visto')::boolean;
  if n <> 1 then raise exception 'FALLO: la skin comprada no quedó como suya (o salió como nueva)'; end if;
  begin
    perform public.comprar_skin('ARQUERO');
    raise exception 'FALLO: compró dos veces la misma skin';
  exception when raise_exception then
    if sqlerrm <> 'SKIN_YA_COMPRADA' then raise; end if;
  end;
  begin
    perform public.comprar_skin('CABALLERO_OSCURO');
    raise exception 'FALLO: compró con saldo insuficiente';
  exception when raise_exception then
    if sqlerrm <> 'ESMERALDAS_INSUFICIENTES' then raise; end if;
  end;
  select esmeraldas into n from public.mi_personalizacion();
  if n <> 50 then raise exception 'FALLO: el saldo cambió tras una compra rechazada (%)', n; end if;
  -- Se puede volver a una skin de nivel
  perform public.equipar_skin('NOVATO');
  select skin_equipada into r from public.mi_personalizacion();
  if r.skin_equipada <> 'NOVATO' then raise exception 'FALLO: no volvió a Novato'; end if;

  -- 8) Si la skin equipada deja de ser suya (cambio de catálogo), vuelve a la inicial
  perform public.equipar_skin('ARQUERO');
  reset role;
  delete from public.skins_usuario where user_id = a and skin_id = 'ARQUERO';
  set local role authenticated;
  select skin_equipada into r from public.mi_personalizacion();
  if r.skin_equipada <> 'NOVATO' then raise exception 'FALLO: la equipada perdida no volvió a Novato (%)', r.skin_equipada; end if;

  -- 9) B no ve nada de A y empieza de cero
  perform set_config('request.jwt.claims', json_build_object('sub', b, 'role', 'authenticated')::text, true);
  select count(*) into n from public.movimientos_esmeraldas;
  if n <> 0 then raise exception 'FALLO: B ve las esmeraldas de A'; end if;
  select count(*) into n from public.skins_usuario where user_id = a;
  if n <> 0 then raise exception 'FALLO: B ve las skins de A'; end if;
  select count(*) into n from public.personaje where user_id = a;
  if n <> 0 then raise exception 'FALLO: B ve el personaje de A'; end if;
  select * into r from public.mi_personalizacion();
  if r.esmeraldas <> 0 or r.skin_equipada <> 'NOVATO' then raise exception 'FALLO: B no empezó de cero'; end if;

  -- 10) Borrar la cuenta de A arrastra sus esmeraldas, skins y personaje
  perform set_config('request.jwt.claims', json_build_object('sub', a, 'role', 'authenticated')::text, true);
  perform public.eliminar_mi_cuenta();
  reset role;
  select (select count(*) from public.movimientos_esmeraldas where user_id = a)
       + (select count(*) from public.skins_usuario where user_id = a)
       + (select count(*) from public.personaje where user_id = a) into n;
  if n <> 0 then raise exception 'FALLO: quedaron datos de personalización de una cuenta borrada'; end if;

  raise exception 'PRUEBAS_OK: todas las pruebas de personalización pasaron (este error revierte los datos de prueba)';
end;
$$;
