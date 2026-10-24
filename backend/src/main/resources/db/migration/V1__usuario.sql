-- Fase 1: usuarios y roles.
create table usuario (
    id            uuid         primary key,
    email         varchar(255) not null,
    nombre        varchar(120) not null,
    password_hash varchar(100) not null,
    rol           varchar(20)  not null,
    activo        boolean      not null default true,
    creado_en     timestamptz  not null default now(),
    constraint ck_usuario_rol check (rol in ('ASISTENTE', 'ORGANIZADOR', 'ADMIN'))
);

-- El email se guarda siempre en minúscula; unicidad case-insensitive garantizada.
create unique index ux_usuario_email on usuario (lower(email));
