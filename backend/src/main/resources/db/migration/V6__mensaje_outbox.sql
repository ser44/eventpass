-- Fase 4: Transactional Outbox para notificaciones confiables.
create table mensaje_outbox (
    id              uuid        primary key,
    tipo            varchar(60) not null,
    payload         jsonb       not null,
    estado          varchar(20) not null,
    intentos        integer     not null default 0,
    proximo_intento timestamptz not null default now(),
    creado_en       timestamptz not null default now(),
    enviado_en      timestamptz,
    ultimo_error    text,
    constraint ck_outbox_estado check (estado in ('PENDIENTE', 'ENVIADO', 'FALLIDO'))
);

create index ix_outbox_estado_proximo on mensaje_outbox (estado, proximo_intento);
