-- Fase 3: reservas con retención temporal de cupo.
create table reserva (
    id            uuid        primary key,
    evento_id     uuid        not null references evento(id),
    usuario_id    uuid        not null references usuario(id),
    cantidad      integer     not null,
    estado        varchar(20) not null,
    expira_en     timestamptz,
    creada_en     timestamptz not null default now(),
    confirmada_en timestamptz,
    cancelada_en  timestamptz,
    constraint ck_reserva_cantidad check (cantidad between 1 and 4),
    constraint ck_reserva_estado check (estado in ('PENDIENTE', 'CONFIRMADA', 'EXPIRADA', 'CANCELADA'))
);

-- Índice parcial para el job de expiración (busca pendientes vencidas).
create index ix_reserva_pendiente_expira on reserva (expira_en) where estado = 'PENDIENTE';
create index ix_reserva_usuario on reserva (usuario_id);
