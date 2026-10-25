-- Fase 3: claves de idempotencia para POST de reservas (evita duplicar por doble clic).
create table idempotency_key (
    clave       varchar(100) not null,
    usuario_id  uuid         not null,
    endpoint    varchar(200) not null,
    status_code integer      not null,
    respuesta   jsonb        not null,
    creada_en   timestamptz  not null default now(),
    primary key (clave, usuario_id)
);
