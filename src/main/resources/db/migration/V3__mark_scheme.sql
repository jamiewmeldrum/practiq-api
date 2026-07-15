-- Mark scheme for a question. Separate entity, not a column on `question` (D-018):
-- mark schemes are tweaked through normal review procedure.
--
-- 1:1 with question, enforced by the unique constraint on question_id.
--
-- Absence of a row is meaningful: extraction may not have produced a mark scheme yet
create table mark_scheme
(
    id          bigint generated always as identity primary key,
    question_id bigint      not null unique references question (id) on delete cascade,
    version     integer     not null default 0,
    body        text        not null,
    created_at  timestamptz not null default now()
);