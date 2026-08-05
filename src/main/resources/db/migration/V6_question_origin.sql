create table question_origin
(
    id          bigint generated always as identity primary key,
    question_id bigint      not null references question (id) on delete cascade,
    source      varchar(20) not null check (source in ('AUTHORED', 'EXTRACTED', 'GENERATED')),
    document_id bigint references document (id),
    created_at  timestamptz not null default now()
);