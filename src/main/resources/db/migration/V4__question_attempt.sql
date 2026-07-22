create table question_attempt
(
    id            bigint generated always as identity primary key,
    question_id   bigint      not null references question (id) on delete cascade,
    session_token text        not null,
    body          text        not null,
    created_at    timestamptz not null default now()
);

create index idx_question_attempt_question_session
    on question_attempt (question_id, session_token, created_at desc, id asc);