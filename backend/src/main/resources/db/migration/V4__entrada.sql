-- Fase 3: entradas emitidas al confirmar una reserva (con código para el QR).
create table entrada (
    id           uuid        primary key,
    reserva_id   uuid        not null references reserva(id),
    evento_id    uuid        not null references evento(id),
    codigo       varchar(20) not null unique,
    estado       varchar(20) not null,
    usada_en     timestamptz,
    validada_por uuid        references usuario(id),
    constraint ck_entrada_estado check (estado in ('VALIDA', 'USADA', 'ANULADA'))
);

create index ix_entrada_reserva on entrada (reserva_id);
create index ix_entrada_evento on entrada (evento_id);
