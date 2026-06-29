-- Concept: a board-agnostic, granular unit of knowledge ("Diffraction").
-- Questions, notes and spec sections are tagged against concepts via join tables (added later).
create table concept (
    id          bigint generated always as identity primary key,
    name        varchar(200) not null unique,
    description text not null,
    created_at  timestamptz  not null default now()
);
