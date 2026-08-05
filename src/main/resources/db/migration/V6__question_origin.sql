create table question_origin
(
    id          bigint generated always as identity primary key,
    question_id bigint      not null unique references question (id) on delete cascade,
    authorship  varchar(20) not null check (authorship in ('AUTHORED', 'EXTRACTED', 'GENERATED')),
    document_id bigint references document (id),
    created_at  timestamptz not null default now()
);
