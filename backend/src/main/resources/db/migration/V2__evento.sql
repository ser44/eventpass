-- Fase 2: eventos con cupo.
create table evento (
    id              uuid         primary key,
    organizador_id  uuid         not null references usuario(id),
    titulo          varchar(160) not null,
    descripcion     text,
    lugar           varchar(200) not null,
    fecha_inicio    timestamptz  not null,
    cupo_total      integer      not null,
    cupo_disponible integer      not null,
    estado          varchar(20)  not null,
    imagen_url      varchar(500),
    version         bigint       not null default 0,
    creado_en       timestamptz  not null default now(),
    actualizado_en  timestamptz  not null default now(),
    constraint ck_evento_estado check (estado in ('BORRADOR', 'PUBLICADO', 'CANCELADO', 'FINALIZADO')),
    constraint ck_evento_cupo_total_pos check (cupo_total > 0),
    -- Invariante RN4: el cupo disponible nunca es negativo (última línea de defensa).
    constraint ck_evento_cupo_no_negativo check (cupo_disponible >= 0),
    constraint ck_evento_cupo_max check (cupo_disponible <= cupo_total)
);

create index ix_evento_estado_fecha on evento (estado, fecha_inicio);
