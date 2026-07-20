create table question_attempt
(
    id            bigint generated always as identity primary key,
    question_id   bigint      not null references question (id) on delete cascade,
    session_token text        not null,
    body          text        not null,
    created_at    timestamptz not null default now()
);